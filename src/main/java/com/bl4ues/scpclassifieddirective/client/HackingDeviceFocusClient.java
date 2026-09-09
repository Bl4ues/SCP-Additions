package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.gui.HackingDeviceScreen;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry.Attachment;
import com.bl4ues.scpclassifieddirective.hacking.HackingDeviceAttachmentManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Smooth camera/input session centered on the tiny physical Hacking Device CRT. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class HackingDeviceFocusClient {
    private static final long APPROACH_NANOS = 260_000_000L;
    private static final long RETURN_NANOS = 260_000_000L;
    private static final double TARGET_FOV = 50.0D;

    private static BlockPos activePos;
    private static CameraType previousCameraType;
    private static Vec3 startPosition = Vec3.ZERO;
    private static float startYaw;
    private static float startPitch;
    private static double originalFov = 70.0D;
    private static double currentFov = 70.0D;
    private static long approachStarted;
    private static boolean returning;
    private static long returnStarted;
    private static Vec3 returnStartPosition = Vec3.ZERO;
    private static float returnStartYaw;
    private static float returnStartPitch;
    private static double returnStartFov;

    private HackingDeviceFocusClient() {
    }

    public static void beginSession(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || pos == null) {
            return;
        }
        begin(pos);
        minecraft.setScreen(new HackingDeviceScreen(pos));
    }

    private static void begin(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (active()) forceClear();
        activePos = pos.immutable();
        previousCameraType = minecraft.options.getCameraType();
        startPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        startYaw = minecraft.gameRenderer.getMainCamera().getYRot();
        startPitch = minecraft.gameRenderer.getMainCamera().getXRot();
        originalFov = minecraft.options.fov().get();
        currentFov = originalFov;
        approachStarted = System.nanoTime();
        returning = false;
        returnStarted = 0L;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
    }

    public static boolean active() {
        return activePos != null;
    }

    public static void end() {
        if (!active() || returning) return;
        Pose current = cameraPose();
        if (current == null) {
            forceClear();
            return;
        }
        returning = true;
        returnStarted = System.nanoTime();
        returnStartPosition = current.position();
        returnStartYaw = current.yaw();
        returnStartPitch = current.pitch();
        returnStartFov = currentFov;
    }

    public static Pose cameraPose() {
        if (!active()) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;

        if (returning) {
            float eased = smooth((float) returnProgress());
            Vec3 position = returnStartPosition.lerp(startPosition, eased);
            float yaw = returnStartYaw
                    + Mth.wrapDegrees(startYaw - returnStartYaw) * eased;
            float pitch = Mth.lerp(eased, returnStartPitch, startPitch);
            return new Pose(position, yaw, pitch);
        }

        Attachment attachment = attachment(minecraft);
        if (attachment == null) return null;
        double seating = HackingDeviceClientState.seatingOffset(activePos);
        Vec3 center = attachment.screen().center().add(
                attachment.screen().outward().scale(seating));
        Vec3 targetEye = center.add(attachment.screen().outward()
                .scale(HackingDeviceAttachmentGeometry.FOCUS_DISTANCE));
        Vec3 look = center.subtract(targetEye);
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float targetPitch = (float) -Math.toDegrees(
                Math.atan2(look.y, horizontal));

        float eased = smooth((float) approachProgress());
        Vec3 position = startPosition.lerp(targetEye, eased);
        float yaw = startYaw + Mth.wrapDegrees(targetYaw - startYaw) * eased;
        float pitch = Mth.lerp(eased, startPitch, targetPitch);
        return new Pose(position, yaw, pitch);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            forceClear();
            return;
        }
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);

        if (returning) {
            if (returnProgress() >= 1.0D) forceClear();
            return;
        }
        if (attachment(minecraft) == null) {
            HackingDeviceMinigameClient.requestExit();
            return;
        }

        if (!(minecraft.screen instanceof HackingDeviceScreen screen)
                || !screen.isFor(activePos)) {
            HackingDeviceMinigameClient.requestExit();
        }
    }

    private static Attachment attachment(Minecraft minecraft) {
        if (activePos == null || minecraft.level == null
                || !HackingDeviceAttachmentManager.isCompatibleTarget(
                        minecraft.level, activePos)) {
            return null;
        }
        return HackingDeviceAttachmentGeometry.resolve(activePos,
                minecraft.level.getBlockState(activePos));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void lockMovement(MovementInputUpdateEvent event) {
        if (!active()) return;
        event.getInput().leftImpulse = 0.0F;
        event.getInput().forwardImpulse = 0.0F;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHud(RenderGuiOverlayEvent.Pre event) {
        if (active()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHand(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hidePlayer(RenderPlayerEvent.Pre event) {
        if (active() && event.getEntity() == Minecraft.getInstance().player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void focusFov(ViewportEvent.ComputeFov event) {
        if (!active()) return;
        if (returning) {
            currentFov = Mth.lerp(returnProgress(), returnStartFov, originalFov);
        } else {
            currentFov = Mth.lerp(approachProgress(), originalFov, TARGET_FOV);
        }
        event.setFOV(currentFov);
    }

    public static void forceClear() {
        Minecraft minecraft = Minecraft.getInstance();
        if (previousCameraType != null) {
            minecraft.options.setCameraType(previousCameraType);
        }
        activePos = null;
        previousCameraType = null;
        approachStarted = 0L;
        returning = false;
        returnStarted = 0L;
        currentFov = originalFov;
        HackingDeviceMinigameClient.clear();
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static double approachProgress() {
        if (approachStarted == 0L) return 1.0D;
        return Mth.clamp((System.nanoTime() - approachStarted)
                / (double) APPROACH_NANOS, 0.0D, 1.0D);
    }

    private static double returnProgress() {
        if (returnStarted == 0L) return 1.0D;
        return Mth.clamp((System.nanoTime() - returnStarted)
                / (double) RETURN_NANOS, 0.0D, 1.0D);
    }

    public record Pose(Vec3 position, float yaw, float pitch) {
    }
}
