package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.network.Scp914DialPacket;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Structure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

/** Client-side physical drag state for SCP-914's configuration dial. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class Scp914InteractionClient {
    private static final double MAX_DISTANCE = 2.35D;
    private static final double AIM_RADIUS_SQR = 0.18D * 0.18D;
    /** Degrees of dial travel per raw cursor pixel while the dial owns the mouse. */
    private static final float DRAG_DEGREES_PER_PIXEL = 0.55F;
    private static final int PACKET_INTERVAL_TICKS = 2;
    private static final int SETTLE_TICKS = 8;

    private static BlockPos activePos;
    private static float visualAngle;
    private static float lockedYaw;
    private static float lockedPitch;
    private static double lastCursorX;
    private static boolean ownsMouse;
    private static float lastSentDetent = Float.NaN;
    private static long lastPacketTick = Long.MIN_VALUE;

    private static BlockPos settlingPos;
    private static float settlingAngle;
    private static float settlingTarget;
    private static int settlingTicks;

    private Scp914InteractionClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            cancelDrag(minecraft, false, false);
            tickSettling();
            return;
        }
        if (minecraft.screen != null) {
            cancelDrag(minecraft, false, false);
            tickSettling();
            return;
        }

        boolean interactDown = minecraft.options.keyUse.isDown();
        if (activePos == null) {
            if (interactDown) beginDrag(minecraft);
        } else if (!interactDown) {
            finishDrag(minecraft);
        } else {
            updateDrag(minecraft);
        }
        tickSettling();
    }

    private static void beginDrag(Minecraft minecraft) {
        Scp914BlockEntity machine = findAimedDial(minecraft);
        if (machine == null || machine.isRefining()) return;

        activePos = machine.getBlockPos().immutable();
        visualAngle = machine.getDialAngle();
        lockedYaw = minecraft.player.getYRot();
        lockedPitch = minecraft.player.getXRot();
        lastSentDetent = quantize(visualAngle);
        lastPacketTick = minecraft.level.getGameTime();
        settlingPos = null;
        settlingTicks = 0;

        // Do not derive the dial from player yaw. That made Minecraft's normal
        // MouseHandler rotate the camera and this class rotate it back every tick,
        // producing the visible tug-of-war/jitter. Release camera ownership instead
        // and read the cursor directly while keeping it hidden.
        minecraft.mouseHandler.releaseMouse();
        long window = minecraft.getWindow().getWindow();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        lastCursorX = cursorX(window);
        ownsMouse = true;
        restoreView(minecraft);
    }

    private static void updateDrag(Minecraft minecraft) {
        if (!(minecraft.level.getBlockEntity(activePos)
                instanceof Scp914BlockEntity machine) || machine.isRefining()) {
            cancelDrag(minecraft, false, true);
            return;
        }

        long window = minecraft.getWindow().getWindow();
        double cursorX = cursorX(window);
        double deltaPixels = cursorX - lastCursorX;
        lastCursorX = cursorX;

        // Ignore only impossible cursor teleports (window focus/OS warp). Normal
        // fast physical drags remain fully accumulated because position is absolute.
        if (Math.abs(deltaPixels) < minecraft.getWindow().getScreenWidth() * 0.5D) {
            visualAngle = Mth.clamp(visualAngle
                            + (float) deltaPixels * DRAG_DEGREES_PER_PIXEL,
                    Scp914BlockEntity.ROUGH_ANGLE,
                    Scp914BlockEntity.VERY_FINE_ANGLE);
        }

        Scp914BlockEntity.Setting nearest =
                Scp914BlockEntity.Setting.nearest(visualAngle);
        float official = nearest.angle();
        float gap = Math.abs(official - visualAngle);
        if (gap < 3.5F && Math.abs(deltaPixels) < 3.0D) {
            visualAngle = Mth.lerp(0.32F, visualAngle, official);
        }

        float detent = quantize(visualAngle);
        long now = minecraft.level.getGameTime();
        if (Math.abs(detent - lastSentDetent) > 0.001F
                && now - lastPacketTick >= PACKET_INTERVAL_TICKS) {
            ModNetwork.CHANNEL.sendToServer(
                    new Scp914DialPacket(activePos, visualAngle, false));
            lastSentDetent = detent;
            lastPacketTick = now;
        }

        // This should be redundant while the cursor is released, but it also
        // protects against another client hook trying to alter view rotation.
        restoreView(minecraft);
    }

    private static void finishDrag(Minecraft minecraft) {
        if (activePos == null) return;
        BlockPos releasedPos = activePos;
        float releasedAngle = visualAngle;
        ModNetwork.CHANNEL.sendToServer(
                new Scp914DialPacket(releasedPos, releasedAngle, true));

        settlingPos = releasedPos;
        settlingAngle = releasedAngle;
        settlingTarget = Scp914BlockEntity.Setting.nearest(releasedAngle).angle();
        settlingTicks = SETTLE_TICKS;
        cancelDrag(minecraft, true, true);
    }

    private static double cursorX(long window) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(window, x, y);
            return x.get(0);
        }
    }

    private static void restoreView(Minecraft minecraft) {
        if (minecraft.player == null) return;
        minecraft.player.setYRot(lockedYaw);
        minecraft.player.setXRot(lockedPitch);
        minecraft.player.yRotO = lockedYaw;
        minecraft.player.xRotO = lockedPitch;
    }

    private static void cancelDrag(Minecraft minecraft,
            boolean preserveSettling, boolean regrabMouse) {
        if (ownsMouse) {
            long window = minecraft.getWindow().getWindow();
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            ownsMouse = false;
            if (regrabMouse && minecraft.screen == null
                    && minecraft.isWindowActive()) {
                minecraft.mouseHandler.grabMouse();
            }
        }
        activePos = null;
        lastSentDetent = Float.NaN;
        lastPacketTick = Long.MIN_VALUE;
        if (!preserveSettling) {
            settlingPos = null;
            settlingTicks = 0;
        }
    }

    private static void tickSettling() {
        if (settlingPos == null || settlingTicks <= 0) return;
        settlingAngle = Mth.lerp(0.38F, settlingAngle, settlingTarget);
        settlingTicks--;
        if (settlingTicks <= 0 || Math.abs(settlingAngle - settlingTarget) < 0.05F) {
            settlingAngle = settlingTarget;
            settlingTicks = 0;
            settlingPos = null;
        }
    }

    public static float renderDialAngle(Scp914BlockEntity machine) {
        BlockPos pos = machine.getBlockPos();
        if (activePos != null && activePos.equals(pos)) return visualAngle;
        if (settlingPos != null && settlingPos.equals(pos)) return settlingAngle;
        return machine.getDialAngle();
    }

    private static Scp914BlockEntity findAimedDial(Minecraft minecraft) {
        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 view = minecraft.player.getViewVector(1.0F).normalize();
        BlockPos center = minecraft.player.blockPosition();
        Scp914BlockEntity best = null;
        double bestAimDistance = AIM_RADIUS_SQR;

        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-4, -3, -4), center.offset(4, 3, 4))) {
            if (!(minecraft.level.getBlockEntity(candidate)
                    instanceof Scp914BlockEntity machine) || machine.isRefining()) {
                continue;
            }
            Vec3 anchor = Scp914Structure.dialAnchor(candidate,
                    Scp914Structure.facing(machine.getBlockState()));
            Vec3 toAnchor = anchor.subtract(eye);
            double distance = toAnchor.length();
            if (distance > MAX_DISTANCE || distance < 0.01D) continue;

            double alongView = toAnchor.dot(view);
            if (alongView <= 0.0D) continue;
            Vec3 closest = eye.add(view.scale(alongView));
            double aimDistance = closest.distanceToSqr(anchor);
            if (aimDistance <= bestAimDistance) {
                bestAimDistance = aimDistance;
                best = machine;
            }
        }
        return best;
    }

    private static float quantize(float angle) {
        float quantized = Math.round(angle / Scp914BlockEntity.DIAL_DETENT_DEGREES)
                * Scp914BlockEntity.DIAL_DETENT_DEGREES;
        return Mth.clamp(quantized, Scp914BlockEntity.ROUGH_ANGLE,
                Scp914BlockEntity.VERY_FINE_ANGLE);
    }
}
