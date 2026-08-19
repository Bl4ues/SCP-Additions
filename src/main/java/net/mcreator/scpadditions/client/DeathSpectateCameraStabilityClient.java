package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Final render-frame correction for the detached live-feed camera.
 *
 * Minecraft renders remote entities from xOld/yOld/zOld, while the original
 * feed camera followed xo/yo/zo and then applied a second wall-clock smoothing
 * pass. Those are different interpolation timelines, so the target model could
 * visibly oscillate relative to a camera that was technically moving smoothly.
 * This pass runs after the normal feed update and anchors the rig to the exact
 * position LevelRenderer will use for the watched player in the same frame.
 *
 * The world itself is still rendered across the whole game window and the death
 * screen merely masks a right-hand viewport over it. Consequently looking
 * directly at the target centers them behind the report card, not inside the
 * feed. The final aim below offsets the view ray so the watched player lands at
 * the actual Live Personnel Feed viewport center while orbit geometry remains
 * independent of that screen-space correction.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeathSpectateCameraStabilityClient {
    private static final double CAMERA_DISTANCE = 3.25D;
    private static final double CAMERA_WALL_MARGIN = 0.16D;
    private static final double CAMERA_PROBE_RADIUS = 0.10D;
    private static final double CAMERA_MIN_CLEARANCE = 0.06D;
    private static final double CAMERA_RETURN_SMOOTHING = 0.22D;

    private static String lastTargetName = "";
    private static double currentDistance = CAMERA_DISTANCE;

    private DeathSpectateCameraStabilityClient() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START
                || !MineZeroSpectateClient.active()
                || MineZeroSpectateClient.connectionLost()) {
            resetIfInactive();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !(minecraft.getCameraEntity() instanceof ArmorStand rig)) {
            return;
        }

        String targetName = MineZeroSpectateClient.targetName();
        AbstractClientPlayer target = findTarget(minecraft, targetName);
        if (target == null) return;

        if (!targetName.equals(lastTargetName)) {
            lastTargetName = targetName;
            currentDistance = CAMERA_DISTANCE;
        }

        float partialTick = Mth.clamp(event.renderTickTime, 0.0F, 1.0F);
        Vec3 focus = renderedFocus(target, partialTick);

        // Derive the orbit radius from the rig's world position, not from its
        // view yaw. The view yaw now includes a deliberate screen-space offset;
        // feeding that offset back into the next orbit frame would slowly walk
        // the camera around the player even when the mouse is untouched.
        Vec3 outward = new Vec3(rig.getX() - focus.x, 0.0D,
                rig.getZ() - focus.z);
        if (outward.lengthSqr() < 1.0E-6D) {
            Vec3 forward = Vec3.directionFromRotation(0.0F, target.getYRot());
            outward = new Vec3(-forward.x, 0.0D, -forward.z);
        }
        if (outward.lengthSqr() < 1.0E-6D) {
            outward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            outward = outward.normalize();
        }

        double allowedDistance = cameraDistanceForObstacles(
                minecraft, target, focus, outward, CAMERA_DISTANCE);
        if (allowedDistance < currentDistance) {
            currentDistance = allowedDistance;
        } else {
            currentDistance += (allowedDistance - currentDistance)
                    * CAMERA_RETURN_SMOOTHING;
        }
        currentDistance = Mth.clamp(currentDistance,
                CAMERA_MIN_CLEARANCE, CAMERA_DISTANCE);

        Vec3 cameraEye = focus.add(outward.scale(currentDistance));
        Vec3 toFocus = focus.subtract(cameraEye);
        double horizontal = Math.sqrt(toFocus.x * toFocus.x
                + toFocus.z * toFocus.z);
        float lookYaw = (float) (Math.toDegrees(
                Math.atan2(toFocus.z, toFocus.x)) - 90.0D);
        float lookPitch = horizontal < 1.0E-5D ? 0.0F
                : (float) -Math.toDegrees(Math.atan2(toFocus.y, horizontal));

        // Shift the target from the full-screen projection center to the center
        // of the visible right-hand feed. This is an angular projection offset,
        // so it remains correct across resolutions, GUI scales, and aspect ratios.
        AimOffset aim = feedAimOffset(minecraft);
        lookYaw -= aim.yaw();
        lookPitch -= aim.pitch();

        double eyeOffset = rig.getEyeY() - rig.getY();
        double rigY = cameraEye.y - eyeOffset;
        rig.setPos(cameraEye.x, rigY, cameraEye.z);
        rig.setYRot(lookYaw);
        rig.setXRot(lookPitch);

        // Camera#setup interpolates xo/yo/zo; LivingEntity view yaw interpolates
        // head rotation. LevelRenderer separately uses xOld/yOld/zOld. Pin every
        // old/current channel because this rig is authored once per render frame.
        rig.setOldPosAndRot();
        rig.yHeadRot = lookYaw;
        rig.yHeadRotO = lookYaw;
        rig.yBodyRot = lookYaw;
        rig.yBodyRotO = lookYaw;
    }

    private static AbstractClientPlayer findTarget(Minecraft minecraft,
            String targetName) {
        if (targetName == null || targetName.isBlank()
                || minecraft.level == null) {
            return null;
        }
        for (AbstractClientPlayer candidate : minecraft.level.players()) {
            if (candidate.isAlive() && !candidate.isSpectator()
                    && targetName.equals(candidate.getGameProfile().getName())) {
                return candidate;
            }
        }
        return null;
    }

    private static Vec3 renderedFocus(AbstractClientPlayer target,
            float partialTick) {
        double x = Mth.lerp(partialTick, target.xOld, target.getX());
        double y = Mth.lerp(partialTick, target.yOld, target.getY());
        double z = Mth.lerp(partialTick, target.zOld, target.getZ());
        return new Vec3(x, y + Math.max(0.95D,
                target.getBbHeight() * 0.58D), z);
    }

    private static AimOffset feedAimOffset(Minecraft minecraft) {
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        if (guiWidth <= 0 || guiHeight <= 0) return new AimOffset(0.0F, 0.0F);

        // Keep these bounds mathematically identical to ScpDeathScreen#viewport.
        int left = Math.max(guiWidth / 2 + 28, Math.round(guiWidth * 0.53F));
        int right = Math.max(left + 120, guiWidth - 28);
        int top = Math.max(46, Math.round(guiHeight * 0.095F));
        int bottom = Math.max(top + 100, guiHeight - 64);

        double centerX = (left + right) * 0.5D;
        double centerY = (top + bottom) * 0.5D;
        double normalizedX = (centerX - guiWidth * 0.5D)
                / (guiWidth * 0.5D);
        double normalizedDown = (centerY - guiHeight * 0.5D)
                / (guiHeight * 0.5D);

        double verticalFov = Math.toRadians(
                minecraft.options.fov().get().doubleValue());
        double tanHalfVertical = Math.tan(verticalFov * 0.5D);
        int framebufferHeight = minecraft.getWindow().getHeight();
        double aspect = framebufferHeight <= 0
                ? guiWidth / (double) guiHeight
                : minecraft.getWindow().getWidth()
                        / (double) framebufferHeight;

        float yaw = (float) Math.toDegrees(Math.atan(
                normalizedX * tanHalfVertical * aspect));
        float pitch = (float) Math.toDegrees(Math.atan(
                normalizedDown * tanHalfVertical));
        return new AimOffset(yaw, pitch);
    }

    private static double cameraDistanceForObstacles(Minecraft minecraft,
            AbstractClientPlayer target, Vec3 focus, Vec3 outward,
            double desiredDistance) {
        double allowed = clipDistance(minecraft, target, focus,
                focus.add(outward.scale(desiredDistance)), desiredDistance);

        double r = CAMERA_PROBE_RADIUS;
        for (int i = 0; i < 8; i++) {
            double ox = (i & 1) == 0 ? -r : r;
            double oy = (i & 2) == 0 ? -r : r;
            double oz = (i & 4) == 0 ? -r : r;
            Vec3 start = focus.add(ox, oy, oz);
            Vec3 end = start.add(outward.scale(desiredDistance));
            allowed = Math.min(allowed, clipDistance(minecraft, target,
                    start, end, desiredDistance));
        }
        return Mth.clamp(allowed, CAMERA_MIN_CLEARANCE, desiredDistance);
    }

    private static double clipDistance(Minecraft minecraft,
            AbstractClientPlayer target, Vec3 start, Vec3 end,
            double desiredDistance) {
        BlockHitResult hit = minecraft.level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
        if (hit.getType() == HitResult.Type.MISS) return desiredDistance;
        return Math.max(CAMERA_MIN_CLEARANCE,
                start.distanceTo(hit.getLocation()) - CAMERA_WALL_MARGIN);
    }

    private static void resetIfInactive() {
        if (MineZeroSpectateClient.active()) return;
        lastTargetName = "";
        currentDistance = CAMERA_DISTANCE;
    }

    private record AimOffset(float yaw, float pitch) {
    }
}
