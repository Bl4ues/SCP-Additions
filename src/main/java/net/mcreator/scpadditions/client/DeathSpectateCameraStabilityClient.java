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

        // The normal feed update already owns user input and therefore the
        // authoritative orbit angle. Its body yaw is independent of the stale
        // focus point, so reuse that angle and only replace position interpolation.
        float orbitYaw = rig.getYRot();
        Vec3 forward = Vec3.directionFromRotation(0.0F, orbitYaw);
        Vec3 outward = new Vec3(-forward.x, 0.0D, -forward.z);
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
}
