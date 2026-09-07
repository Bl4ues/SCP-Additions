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
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
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
    public static final double SCREEN_WIDTH = 11.2D / 16.0D;
    public static final double SCREEN_HEIGHT = 10.7D / 16.0D;
    public static final double VIEW_HEIGHT_FRACTION = 0.72D;
    public static final double FOCUS_DISTANCE = 0.72D;

    private static final double TARGET_FOV = 60.0D;
    private static final long APPROACH_NANOS = 220_000_000L;
    private static final long RETURN_NANOS = 220_000_000L;

    private static BlockPos activePos;
    private static ArmorStand cameraRig;
    private static ClientLevel rigLevel;
    private static Entity previousCameraEntity;
    private static CameraType previousCameraType;
    private static Vec3 startPosition = Vec3.ZERO;
    private static float startYaw;
    private static float startPitch;
    private static long approachStarted;
    private static double originalFovDegrees = 70.0D;
    private static double currentFovDegrees = 70.0D;

    private static boolean returning;
    private static long returnStarted;
    private static Vec3 returnStartPosition = Vec3.ZERO;
    private static float returnStartYaw;
    private static float returnStartPitch;
    private static double returnStartFovDegrees = 60.0D;

    private TeslaTerminalFocusClient() { }

    public static void begin(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || pos == null) {
            return;
        }
        if (activePos != null && activePos.equals(pos)
                && cameraRig != null && rigLevel == minecraft.level
                && !returning) {
            return;
        }
        if (active()) finishEnd(minecraft);

        activePos = pos.immutable();
        previousCameraEntity = minecraft.getCameraEntity();
        previousCameraType = minecraft.options.getCameraType();
        startPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        startYaw = minecraft.player.getYRot();
        startPitch = minecraft.player.getXRot();
        originalFovDegrees = minecraft.options.fov().get();
        currentFovDegrees = originalFovDegrees;
        approachStarted = System.nanoTime();
        returning = false;
        returnStarted = 0L;
        ensureRig(minecraft);
        updateApproachCamera(minecraft);
    }

    /**
     * Begins a symmetric return trip instead of snapping the view back to the
     * player. Movement remains locked for these few frames and is released only
     * after the detached camera reaches the original eye position again.
     */
    public static void end() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || returning) return;
        if (cameraRig == null || minecraft.level == null) {
            finishEnd(minecraft);
            return;
        }

        returning = true;
        returnStarted = System.nanoTime();
        returnStartPosition = rigEyePosition();
        returnStartYaw = cameraRig.getYRot();
        returnStartPitch = cameraRig.getXRot();
        returnStartFovDegrees = currentFovDegrees;
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

    /**
     * Current physical CRT height in GUI-space as a fraction of the viewport.
     * The input mapper uses the same camera distance and FOV that render the
     * world instead of a guessed fullscreen rectangle.
     */
    public static double projectedHeightFraction() {
        double halfFov = Math.toRadians(Mth.clamp(currentFovDegrees,
                20.0D, 150.0D) * 0.5D);
        double tangent = Math.tan(halfFov);
        if (!Double.isFinite(tangent) || tangent <= 1.0E-6D) {
            return VIEW_HEIGHT_FRACTION;
        }
        return Mth.clamp(SCREEN_HEIGHT / (2.0D * FOCUS_DISTANCE * tangent),
                0.10D, 0.95D);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            finishEnd(minecraft);
            return;
        }

        if (returning) {
            updateReturnCamera(minecraft);
            return;
        }

        if (!(minecraft.screen instanceof TeslaTerminalScreen)) {
            end();
            return;
        }
        updateApproachCamera(minecraft);
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
    public static void hideGameplayHud(RenderGuiOverlayEvent.Pre event) {
        if (active()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHand(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }

    /** The detached camera rig must not render the local player's body into the CRT. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideLocalPlayer(RenderPlayerEvent.Pre event) {
        if (active() && event.getEntity() == Minecraft.getInstance().player) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void focusFov(ViewportEvent.ComputeFov event) {
        if (!active()) return;
        if (returning) {
            double t = returnProgress();
            currentFovDegrees = Mth.lerp(t, returnStartFovDegrees,
                    originalFovDegrees);
        } else {
            double t = approachProgress();
            currentFovDegrees = Mth.lerp(t, originalFovDegrees, TARGET_FOV);
        }
        event.setFOV(currentFovDegrees);
    }

    private static void updateApproachCamera(Minecraft minecraft) {
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
        Vec3 targetEye = frame.center().add(
                frame.outward().scale(FOCUS_DISTANCE));
        Vec3 look = frame.center().subtract(targetEye);
        double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float targetPitch = (float) -Math.toDegrees(
                Math.atan2(look.y, horizontal));

        float eased = smooth((float) approachProgress());
        Vec3 cameraEye = startPosition.lerp(targetEye, eased);
        float yaw = startYaw + Mth.wrapDegrees(targetYaw - startYaw) * eased;
        float pitch = Mth.lerp(eased, startPitch, targetPitch);
        placeRig(minecraft, cameraEye, yaw, pitch);
    }

    private static void updateReturnCamera(Minecraft minecraft) {
        if (cameraRig == null) {
            finishEnd(minecraft);
            return;
        }
        float progress = (float) returnProgress();
        float eased = smooth(progress);
        Vec3 cameraEye = returnStartPosition.lerp(startPosition, eased);
        float yaw = returnStartYaw
                + Mth.wrapDegrees(startYaw - returnStartYaw) * eased;
        float pitch = Mth.lerp(eased, returnStartPitch, startPitch);
        placeRig(minecraft, cameraEye, yaw, pitch);
        if (progress >= 1.0F) finishEnd(minecraft);
    }

    private static void placeRig(Minecraft minecraft, Vec3 cameraEye,
            float yaw, float pitch) {
        if (cameraRig == null) return;
        // Minecraft renders from the camera entity's eye, not its feet. Keep the
        // helper's base below the desired eye point, and seed every previous-pos
        // field so interpolation never gets a chance to invent a brief vacation
        // in the upper atmosphere.
        double eyeOffset = cameraRig.getEyeY() - cameraRig.getY();
        double rigY = cameraEye.y - eyeOffset;
        cameraRig.xo = cameraEye.x;
        cameraRig.yo = rigY;
        cameraRig.zo = cameraEye.z;
        cameraRig.xOld = cameraEye.x;
        cameraRig.yOld = rigY;
        cameraRig.zOld = cameraEye.z;
        cameraRig.setPos(cameraEye.x, rigY, cameraEye.z);
        cameraRig.setYRot(yaw);
        cameraRig.setXRot(pitch);
        cameraRig.yRotO = yaw;
        cameraRig.xRotO = pitch;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        if (minecraft.getCameraEntity() != cameraRig) {
            minecraft.setCameraEntity(cameraRig);
        }
    }

    private static Vec3 rigEyePosition() {
        if (cameraRig == null) return startPosition;
        return new Vec3(cameraRig.getX(), cameraRig.getEyeY(), cameraRig.getZ());
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

    private static void finishEnd(Minecraft minecraft) {
        if (minecraft == null) minecraft = Minecraft.getInstance();
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
        returnStarted = 0L;
        returning = false;
        currentFovDegrees = originalFovDegrees;
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
