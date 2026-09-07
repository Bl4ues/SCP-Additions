package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.TeslaTerminalBlockBlock;
import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Camera/input session for physical computer screens. The first implementation
 * is the Tesla Gate terminal, but the rig is intentionally independent from its
 * menu logic so later monitors can use the same presentation pattern.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class TeslaTerminalFocusClient {
    public static final double SCREEN_CENTER_X = 8.66366D / 16.0D;
    public static final double SCREEN_CENTER_Y = 5.98447D / 16.0D;
    public static final double SCREEN_CENTER_Z = 4.42612D / 16.0D;
    public static final double SCREEN_TILT_DEGREES = 22.5D;
    public static final double SCREEN_WIDTH = 10.8D / 16.0D;
    public static final double SCREEN_HEIGHT = SCREEN_WIDTH * 1080.0D / 1410.0D;
    public static final double VIEW_HEIGHT_FRACTION = 0.72D;

    private static final double FOCUS_DISTANCE = 0.62D;
    private static final long APPROACH_NANOS = 220_000_000L;

    private static BlockPos activePos;
    private static ArmorStand cameraRig;
    private static ClientLevel rigLevel;
    private static Entity previousCameraEntity;
    private static CameraType previousCameraType;
    private static Vec3 startPosition = Vec3.ZERO;
    private static float startYaw;
    private static float startPitch;
    private static long approachStarted;

    private TeslaTerminalFocusClient() { }

    public static void begin(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || pos == null) {
            return;
        }
        if (activePos != null && activePos.equals(pos)
                && cameraRig != null && rigLevel == minecraft.level) {
            return;
        }
        end();
        activePos = pos.immutable();
        previousCameraEntity = minecraft.getCameraEntity();
        previousCameraType = minecraft.options.getCameraType();
        startPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        startYaw = minecraft.player.getYRot();
        startPitch = minecraft.player.getXRot();
        approachStarted = System.nanoTime();
        ensureRig(minecraft);
        updateCamera(minecraft);
    }

    public static void end() {
        Minecraft minecraft = Minecraft.getInstance();
        if (cameraRig != null && minecraft.getCameraEntity() == cameraRig) {
            Entity restore = previousCameraEntity;
            if (restore == null || restore.isRemoved()
                    || restore.level() != minecraft.level) {
                restore = minecraft.player;
            }
            if (restore != null) minecraft.setCameraEntity(restore);
        }
        if (previousCameraType != null) {
            minecraft.options.setCameraType(previousCameraType);
        }
        activePos = null;
        cameraRig = null;
        rigLevel = null;
        previousCameraEntity = null;
        previousCameraType = null;
        approachStarted = 0L;
    }

    public static boolean active() {
        return activePos != null;
    }

    public static boolean activeFor(BlockPos pos) {
        return pos != null && pos.equals(activePos);
    }

    public static Frame frame(BlockPos pos, Direction facing) {
        return PhysicalBlockScreenGeometry.fromNorthFacing(pos, facing,
                SCREEN_CENTER_X, SCREEN_CENTER_Y, SCREEN_CENTER_Z,
                SCREEN_TILT_DEGREES, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof TeslaTerminalScreen)
                || minecraft.player == null || minecraft.level == null) {
            end();
            return;
        }
        updateCamera(minecraft);
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
    public static void hideHand(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void focusFov(ViewportEvent.ComputeFov event) {
        if (!active()) return;
        double t = approachProgress();
        event.setFOV(Mth.lerp(t, event.getFOV(), 60.0D));
    }

    private static void updateCamera(Minecraft minecraft) {
        if (activePos == null || minecraft.level == null) return;
        BlockState state = minecraft.level.getBlockState(activePos);
        if (!state.is(ScpClassifiedDirectiveModBlocks.TESLA_TERMINAL_BLOCK.get())) {
            if (minecraft.player != null) minecraft.player.closeContainer();
            end();
            return;
        }
        ensureRig(minecraft);
        if (cameraRig == null) return;
        Direction facing = state.hasProperty(TeslaTerminalBlockBlock.FACING)
                ? state.getValue(TeslaTerminalBlockBlock.FACING)
                : Direction.NORTH;
        Frame frame = frame(activePos, facing);
        Vec3 target = frame.center().add(
                frame.outward().scale(FOCUS_DISTANCE));
        Vec3 look = frame.center().subtract(target);
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float targetPitch = (float) -Math.toDegrees(
                Math.atan2(look.y, horizontal));

        float t = (float) approachProgress();
        float eased = t * t * (3.0F - 2.0F * t);
        Vec3 camera = startPosition.lerp(target, eased);
        float yaw = startYaw + Mth.wrapDegrees(targetYaw - startYaw) * eased;
        float pitch = Mth.lerp(eased, startPitch, targetPitch);

        // Camera.setup places a first-person camera at the camera entity's eye,
        // not at its feet. Store the rig's base one eye-height lower so the
        // requested camera coordinate really is the centre-normal point used by
        // the projected input mapping. Synchronizing the old position prevents
        // partial-tick interpolation from dragging this un-ticked helper entity
        // toward its creation origin.
        double rigY = camera.y - cameraRig.getEyeHeight();
        cameraRig.setPos(camera.x, rigY, camera.z);
        cameraRig.xOld = camera.x;
        cameraRig.yOld = rigY;
        cameraRig.zOld = camera.z;
        cameraRig.setYRot(yaw);
        cameraRig.setXRot(pitch);
        cameraRig.yRotO = yaw;
        cameraRig.xRotO = pitch;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        if (minecraft.getCameraEntity() != cameraRig) {
            minecraft.setCameraEntity(cameraRig);
        }
    }

    private static double approachProgress() {
        if (approachStarted == 0L) return 1.0D;
        return Mth.clamp((System.nanoTime() - approachStarted)
                / (double) APPROACH_NANOS, 0.0D, 1.0D);
    }

    private static void ensureRig(Minecraft minecraft) {
        if (minecraft.level == null) return;
        if (cameraRig != null && rigLevel == minecraft.level) return;
        ArmorStand rig = EntityType.ARMOR_STAND.create(minecraft.level);
        if (rig == null) return;
        rig.setInvisible(true);
        rig.setNoGravity(true);
        cameraRig = rig;
        rigLevel = minecraft.level;
    }
}
