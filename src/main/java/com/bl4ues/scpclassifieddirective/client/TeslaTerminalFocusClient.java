package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.SCP079SystemControlBlock;
import com.bl4ues.scpclassifieddirective.block.TeslaTerminalBlockBlock;
import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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
 * Camera/input session for physical computer screens. Both facility terminals
 * use Minecraft's real Camera after vanilla setup, avoiding detached-entity
 * interpolation and preserving the stable symmetric approach/return behavior.
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

    // Authored diagnostic CRT plane. Its monitor bone is yawed 12.5 degrees,
    // and the GeckoLib model's X axis is mirrored into Minecraft block space.
    public static final double DIAGNOSTIC_SCREEN_CENTER_X =
            2.902130788109321D / 16.0D;
    public static final double DIAGNOSTIC_SCREEN_CENTER_Y = 6.25D / 16.0D;
    public static final double DIAGNOSTIC_SCREEN_CENTER_Z =
            9.105966868662493D / 16.0D;
    public static final double DIAGNOSTIC_SCREEN_YAW_DEGREES = 12.5D;
    public static final double DIAGNOSTIC_SCREEN_WIDTH = 7.5D / 16.0D;
    public static final double DIAGNOSTIC_SCREEN_HEIGHT = 5.5D / 16.0D;

    public static final double VIEW_HEIGHT_FRACTION = 0.72D;
    public static final double FOCUS_DISTANCE = 0.72D;
    public static final double DIAGNOSTIC_FOCUS_DISTANCE = 0.42D;

    private static final double TARGET_FOV = 60.0D;
    private static final long APPROACH_NANOS = 260_000_000L;
    private static final long RETURN_NANOS = 260_000_000L;

    private static BlockPos activePos;
    private static FocusKind activeKind;
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
        begin(pos, FocusKind.TESLA);
    }

    public static void beginDiagnostic(BlockPos pos) {
        begin(pos, FocusKind.DIAGNOSTIC);
    }

    private static void begin(BlockPos pos, FocusKind kind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || pos == null
                || kind == null) {
            return;
        }
        if (activePos != null && activePos.equals(pos) && activeKind == kind
                && !returning) {
            return;
        }
        if (active()) finishEnd(minecraft);

        activePos = pos.immutable();
        activeKind = kind;
        previousCameraType = minecraft.options.getCameraType();
        startPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        startYaw = minecraft.gameRenderer.getMainCamera().getYRot();
        startPitch = minecraft.gameRenderer.getMainCamera().getXRot();
        originalFovDegrees = minecraft.options.fov().get();
        currentFovDegrees = originalFovDegrees;
        approachStarted = System.nanoTime();
        returning = false;
        returnStarted = 0L;
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
    }

    /**
     * Starts a symmetric return to the exact view from which the terminal was
     * entered. Movement remains locked until the interpolation is complete.
     */
    public static void end() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || returning) return;
        Pose current = cameraPose();
        if (current == null) {
            finishEnd(minecraft);
            return;
        }

        returning = true;
        returnStarted = System.nanoTime();
        returnStartPosition = current.position;
        returnStartYaw = current.yaw;
        returnStartPitch = current.pitch;
        returnStartFovDegrees = currentFovDegrees;
    }

    public static boolean active() {
        return activePos != null && activeKind != null;
    }

    public static boolean activeFor(BlockPos pos) {
        return pos != null && pos.equals(activePos);
    }

    public static boolean diagnosticActiveFor(BlockPos pos) {
        return activeKind == FocusKind.DIAGNOSTIC && activeFor(pos);
    }

    public static boolean inputReady() {
        return active() && !returning && approachProgress() >= 0.985D;
    }

    public static Frame frame(BlockPos pos, Direction facing) {
        return PhysicalBlockScreenGeometry.fromNorthFacing(pos, facing,
                SCREEN_CENTER_X, SCREEN_CENTER_Y, SCREEN_CENTER_Z,
                SCREEN_TILT_DEGREES, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    public static Frame diagnosticFrame(BlockPos pos, Direction facing) {
        return PhysicalBlockScreenGeometry.fromNorthFacing(pos, facing,
                DIAGNOSTIC_SCREEN_CENTER_X, DIAGNOSTIC_SCREEN_CENTER_Y,
                DIAGNOSTIC_SCREEN_CENTER_Z, DIAGNOSTIC_SCREEN_YAW_DEGREES,
                0.0D, DIAGNOSTIC_SCREEN_WIDTH, DIAGNOSTIC_SCREEN_HEIGHT);
    }

    /**
     * Physical CRT height in GUI-space as a fraction of the viewport. Input
     * mapping uses the same FOV and target distance as the actual focused view.
     */
    public static double projectedHeightFraction() {
        double height = activeKind == FocusKind.DIAGNOSTIC
                ? DIAGNOSTIC_SCREEN_HEIGHT : SCREEN_HEIGHT;
        double distance = activeKind == FocusKind.DIAGNOSTIC
                ? DIAGNOSTIC_FOCUS_DISTANCE : FOCUS_DISTANCE;
        double halfFov = Math.toRadians(Mth.clamp(currentFovDegrees,
                20.0D, 150.0D) * 0.5D);
        double tangent = Math.tan(halfFov);
        if (!Double.isFinite(tangent) || tangent <= 1.0E-6D) {
            return VIEW_HEIGHT_FRACTION;
        }
        return Mth.clamp(height / (2.0D * distance * tangent),
                0.10D, 0.95D);
    }

    /** Pose consumed by TeslaTerminalCameraMixin after vanilla Camera.setup. */
    public static Pose cameraPose() {
        if (!active()) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return null;

        if (returning) {
            float eased = smooth((float) returnProgress());
            Vec3 position = returnStartPosition.lerp(startPosition, eased);
            float yaw = returnStartYaw
                    + Mth.wrapDegrees(startYaw - returnStartYaw) * eased;
            float pitch = Mth.lerp(eased, returnStartPitch, startPitch);
            return new Pose(position, yaw, pitch);
        }

        FocusTarget target = focusTarget(minecraft);
        if (target == null) return null;
        Frame frame = target.frame();
        Vec3 targetEye = frame.center().add(frame.outward()
                .scale(target.distance()));
        Vec3 look = frame.center().subtract(targetEye);
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

    private static FocusTarget focusTarget(Minecraft minecraft) {
        BlockState state = minecraft.level.getBlockState(activePos);
        if (activeKind == FocusKind.DIAGNOSTIC) {
            if (!state.is(ScpClassifiedDirectiveModBlocks
                    .SCP_079_SYSTEM_CONTROL.get())) {
                return null;
            }
            Direction facing = state.hasProperty(SCP079SystemControlBlock.FACING)
                    ? state.getValue(SCP079SystemControlBlock.FACING)
                    : Direction.NORTH;
            return new FocusTarget(diagnosticFrame(activePos, facing),
                    DIAGNOSTIC_FOCUS_DISTANCE);
        }

        if (!state.is(ScpClassifiedDirectiveModBlocks.TESLA_TERMINAL_BLOCK.get())) {
            return null;
        }
        Direction facing = state.hasProperty(TeslaTerminalBlockBlock.FACING)
                ? state.getValue(TeslaTerminalBlockBlock.FACING)
                : Direction.NORTH;
        return new FocusTarget(frame(activePos, facing), FOCUS_DISTANCE);
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            finishEnd(minecraft);
            return;
        }

        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        if (returning) {
            if (returnProgress() >= 1.0D) finishEnd(minecraft);
            return;
        }

        boolean correctScreen = activeKind == FocusKind.DIAGNOSTIC
                ? minecraft.screen instanceof FacilityDiagnosticsScreen screen
                        && screen.isFor(activePos)
                : minecraft.screen instanceof TeslaTerminalScreen;
        if (!correctScreen) {
            end();
            return;
        }

        if (focusTarget(minecraft) == null) {
            if (activeKind == FocusKind.DIAGNOSTIC) {
                minecraft.setScreen(null);
            } else {
                minecraft.player.closeContainer();
            }
            end();
        }
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
        if (previousCameraType != null) {
            minecraft.options.setCameraType(previousCameraType);
        }
        activePos = null;
        activeKind = null;
        previousCameraType = null;
        approachStarted = 0L;
        returnStarted = 0L;
        returning = false;
        currentFovDegrees = originalFovDegrees;
    }

    private enum FocusKind {
        TESLA,
        DIAGNOSTIC
    }

    private record FocusTarget(Frame frame, double distance) { }

    public record Pose(Vec3 position, float yaw, float pitch) { }
}
