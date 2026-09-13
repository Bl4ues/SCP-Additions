package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Surface-aware alarm light renderer based on overlapping soft emissive splats.
 *
 * <p>The previous implementation stretched one large projected texture over a
 * tessellated mesh. That guaranteed geometric-looking boundaries and ugly seams
 * when the footprint crossed a wall/ceiling fold. This renderer instead samples
 * the desired footprint at a few points and paints independent soft light blobs
 * onto the real surface hit by each ray. Because every blob fades to transparent
 * before its quad edge, folds and missing samples disappear softly instead of
 * revealing straight polygon cuts.</p>
 */
final class AlarmLightSplatRenderer {
    private static final ResourceLocation SPLAT = id(
            "textures/effect/alarm_light_splat.png");
    private static final ResourceLocation SPLAT_EMISSIVE = id(
            "textures/effect/alarm_light_splat_emissive.png");

    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    private static final double PROJECTOR_DISTANCE_SQR = 24.0D * 24.0D;
    private static final double PER_FRAME_DISTANCE_SQR = 6.0D * 6.0D;
    private static final double WALL_PLANE_INSET = 0.0625D;
    private static final double PROJECTOR_OUTSET = 0.34D;
    private static final double RAY_OVERSHOOT = 0.06D;
    private static final double SURFACE_EPSILON = 0.0032D;
    private static final double BLOOM_EPSILON = 0.0015D;

    private static final int RINGS = 9;
    private static final double[] LANES = {
            -0.78D, -0.38D, 0.0D, 0.38D, 0.78D
    };
    private static final double MIN_DISTANCE = 0.16D;
    private static final double MAX_DISTANCE = 3.85D;
    private static final double MAX_HALF_WIDTH = 1.52D;

    private static final Map<ClientLevel, Map<BlockPos, SplatCache>> CACHE =
            new WeakHashMap<>();

    private AlarmLightSplatRenderer() {
    }

    static void render(AlarmModule.AlarmBlockEntity alarm,
            PoseStack poseStack, MultiBufferSource buffers,
            float rotorAngle, double mountYOffset) {
        if (!(alarm.getLevel() instanceof ClientLevel level)) return;
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) return;

        Vec3 center = Vec3.atCenterOf(alarm.getBlockPos());
        double cameraDistanceSqr = camera.position().distanceToSqr(center);
        if (cameraDistanceSqr > PROJECTOR_DISTANCE_SQR) return;

        List<Splat> splats = projection(level, camera, alarm,
                rotorAngle, mountYOffset,
                cameraDistanceSqr <= PER_FRAME_DISTANCE_SQR);
        if (splats.isEmpty()) return;

        RenderType baseType = RenderType.entityTranslucentEmissive(SPLAT);
        VertexConsumer base = buffers.getBuffer(baseType);
        for (Splat splat : splats) {
            emitSplat(base, poseStack, alarm.getBlockPos(),
                    splat, false);
        }
        flush(buffers, baseType);

        // True shader emissive path. The texture is intentionally weaker than
        // the visible wash so BSL/Iris get a soft HDR glow rather than a solid
        // orange polygon.
        RenderType bloomType = RenderType.eyes(SPLAT_EMISSIVE);
        VertexConsumer bloom = buffers.getBuffer(bloomType);
        for (Splat splat : splats) {
            emitSplat(bloom, poseStack, alarm.getBlockPos(),
                    splat, true);
        }
        flush(buffers, bloomType);
    }

    private static List<Splat> projection(ClientLevel level, Entity camera,
            AlarmModule.AlarmBlockEntity alarm, float rotorAngle,
            double mountYOffset, boolean perFrame) {
        Map<BlockPos, SplatCache> byPos = CACHE.computeIfAbsent(level,
                ignored -> new HashMap<>());
        BlockPos pos = alarm.getBlockPos();
        long tick = level.getGameTime();
        SplatCache cached = byPos.get(pos);
        if (!perFrame && cached != null && cached.tick == tick) {
            return cached.splats;
        }

        Direction facing = alarm.getBlockState().getValue(AlarmModule.FACING);
        Vec3 outward = direction(facing);
        Vec3 inward = outward.scale(-1.0D);
        Vec3 right = direction(facing.getClockWise());
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        Vec3 tangent = right.scale(Math.sin(rotorAngle))
                .add(up.scale(-Math.cos(rotorAngle))).normalize();
        Vec3 fanSide = outward.cross(tangent);
        if (fanSide.lengthSqr() < 1.0E-8D) fanSide = right;
        fanSide = fanSide.normalize();

        Vec3 rotorCenter = modelPointToWorld(pos, facing,
                0.0D, 8.0D, 7.0D, mountYOffset);
        Vec3 wallOrigin = rotorCenter.add(inward.scale(WALL_PLANE_INSET));
        Vec3 rayStart = wallOrigin.add(outward.scale(PROJECTOR_OUTSET));

        List<Splat> result = new ArrayList<>(RINGS * LANES.length);
        for (int ring = 0; ring < RINGS; ring++) {
            double t = ring / (RINGS - 1.0D);
            double along = MIN_DISTANCE
                    + (MAX_DISTANCE - MIN_DISTANCE) * t;

            // Fast initial spread, then a broad rounded outer body like the
            // reference. Individual blobs provide the final soft silhouette.
            // Pear/teardrop profile from the reference: narrow near the
            // beacon, widest before the far end, then slightly pinched again
            // so the final soft splats create a rounded cap rather than a
            // ruler-straight terminus.
            double widthPhase = 0.05D + 0.65D * Math.pow(t, 0.82D);
            double widthT = Math.sin(Math.PI * widthPhase);
            double halfWidth = 0.035D + MAX_HALF_WIDTH * widthT;

            double sourceHot = Math.exp(-6.2D * t);
            double middleDip = 1.0D - 0.50D * Math.exp(
                    -Math.pow((t - 0.52D) / 0.24D, 2.0D));
            double endFade = 1.0D - smoothStep(0.80D, 1.0D, t);

            for (double lane : LANES) {
                double lateral = halfWidth * lane;
                Vec3 intended = wallOrigin
                        .add(tangent.scale(along))
                        .add(fanSide.scale(lateral))
                        .add(inward.scale(RAY_OVERSHOOT));

                ProjectedHit hit = cast(level, camera, pos,
                        rayStart, intended);
                if (hit == null) continue;

                double laneAbs = Math.abs(lane);
                double laneGain = 0.80D
                        + 0.46D * Math.pow(laneAbs, 1.75D);
                double intensity = (0.11D + 0.31D * sourceHot)
                        * middleDip * endFade * laneGain;
                double bloomIntensity = (0.045D + 0.11D * sourceHot)
                        * endFade
                        * (0.78D + 0.44D * laneAbs);

                double halfAlong = 0.34D + 0.18D * t;
                double halfSide = 0.38D + 0.30D * t;

                Vec3 surfaceNormal = direction(hit.face);
                Vec3 surfaceAlong = tangent.subtract(surfaceNormal.scale(
                        tangent.dot(surfaceNormal)));
                if (surfaceAlong.lengthSqr() < 1.0E-6D) {
                    surfaceAlong = fanSide.subtract(surfaceNormal.scale(
                            fanSide.dot(surfaceNormal)));
                }
                if (surfaceAlong.lengthSqr() < 1.0E-6D) {
                    surfaceAlong = orthogonal(surfaceNormal);
                }
                surfaceAlong = surfaceAlong.normalize();
                Vec3 surfaceSide = surfaceNormal.cross(surfaceAlong);
                if (surfaceSide.lengthSqr() < 1.0E-6D) {
                    surfaceSide = orthogonal(surfaceNormal);
                } else {
                    surfaceSide = surfaceSide.normalize();
                }

                result.add(new Splat(hit.position, hit.face,
                        surfaceAlong, surfaceSide,
                        halfAlong, halfSide,
                        (float) Math.min(1.0D, intensity),
                        (float) Math.min(1.0D, bloomIntensity)));
            }
        }

        List<Splat> frozen = List.copyOf(result);
        byPos.put(pos.immutable(), new SplatCache(tick, frozen));
        return frozen;
    }

    private static ProjectedHit cast(ClientLevel level, Entity context,
            BlockPos alarmPos, Vec3 start, Vec3 end) {
        Vec3 ray = end.subtract(start);
        if (ray.lengthSqr() < 1.0E-8D) return null;
        Vec3 rayDirection = ray.normalize();
        Vec3 cursor = start;

        for (int attempt = 0; attempt < 2; attempt++) {
            BlockHitResult hit = level.clip(new ClipContext(cursor, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, context));
            if (hit.getType() != HitResult.Type.BLOCK) return null;

            if (hit.getBlockPos().equals(alarmPos)) {
                cursor = skipPastBlockCell(hit.getLocation(),
                        rayDirection, alarmPos);
                if (cursor.distanceToSqr(end) < 1.0E-6D
                        || cursor.subtract(start).dot(ray) < 0.0D
                        || cursor.subtract(end).dot(ray) > 0.0D) {
                    return null;
                }
                continue;
            }

            Direction face = hit.getDirection();
            Vec3 normal = direction(face);
            return new ProjectedHit(hit.getLocation().add(
                    normal.scale(SURFACE_EPSILON)), face);
        }
        return null;
    }

    private static Vec3 skipPastBlockCell(Vec3 point, Vec3 direction,
            BlockPos blockPos) {
        double tx = exitDistance(point.x, direction.x,
                blockPos.getX(), blockPos.getX() + 1.0D);
        double ty = exitDistance(point.y, direction.y,
                blockPos.getY(), blockPos.getY() + 1.0D);
        double tz = exitDistance(point.z, direction.z,
                blockPos.getZ(), blockPos.getZ() + 1.0D);
        double distance = Math.min(tx, Math.min(ty, tz));
        if (!Double.isFinite(distance)) {
            return point.add(direction.scale(1.001D));
        }
        return point.add(direction.scale(distance + 0.002D));
    }

    private static double exitDistance(double coordinate, double direction,
            double min, double max) {
        if (Math.abs(direction) < 1.0E-9D) {
            return Double.POSITIVE_INFINITY;
        }
        double boundary = direction > 0.0D ? max : min;
        double distance = (boundary - coordinate) / direction;
        return distance >= 0.0D
                ? distance : Double.POSITIVE_INFINITY;
    }

    private static void emitSplat(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin,
            Splat splat, boolean bloomPass) {
        Vec3 normal = direction(splat.face);
        Vec3 center = bloomPass
                ? splat.position.add(normal.scale(BLOOM_EPSILON))
                : splat.position;
        Vec3 along = splat.surfaceAlong.scale(splat.halfAlong);
        Vec3 side = splat.surfaceSide.scale(splat.halfSide);

        Vec3 p0 = center.subtract(along).subtract(side);
        Vec3 p1 = center.subtract(along).add(side);
        Vec3 p2 = center.add(along).add(side);
        Vec3 p3 = center.add(along).subtract(side);

        float alpha = bloomPass ? splat.bloomAlpha : splat.alpha;
        vertex(consumer, poseStack, local(p0, blockOrigin),
                normal, 0.0F, 1.0F, alpha);
        vertex(consumer, poseStack, local(p1, blockOrigin),
                normal, 1.0F, 1.0F, alpha);
        vertex(consumer, poseStack, local(p2, blockOrigin),
                normal, 1.0F, 0.0F, alpha);
        vertex(consumer, poseStack, local(p3, blockOrigin),
                normal, 0.0F, 0.0F, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack,
            Vec3 point, Vec3 normal, float u, float v, float alpha) {
        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 255, 255, Math.max(0,
                        Math.min(255, Math.round(alpha * 255.0F))))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
    }

    private static Vec3 orthogonal(Vec3 normal) {
        Vec3 candidate = Math.abs(normal.y) < 0.9D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        return normal.cross(candidate).normalize();
    }

    private static double smoothStep(double edge0, double edge1,
            double value) {
        if (edge1 <= edge0) return value >= edge1 ? 1.0D : 0.0D;
        double x = Math.max(0.0D, Math.min(1.0D,
                (value - edge0) / (edge1 - edge0)));
        return x * x * (3.0D - 2.0D * x);
    }

    private static void flush(MultiBufferSource buffers, RenderType type) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static Vec3 modelPointToWorld(BlockPos pos, Direction facing,
            double modelX, double modelY, double modelZ,
            double mountYOffset) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 right = direction(facing.getClockWise());
        Vec3 back = direction(facing.getOpposite());
        return center
                .add(right.scale(modelX / 16.0D))
                .add(0.0D, (modelY - 8.0D) / 16.0D + mountYOffset,
                        0.0D)
                .add(back.scale(modelZ / 16.0D));
    }

    private static Vec3 local(Vec3 world, BlockPos origin) {
        return world.subtract(origin.getX(), origin.getY(), origin.getZ());
    }

    private static Vec3 direction(Direction direction) {
        return new Vec3(direction.getStepX(),
                direction.getStepY(), direction.getStepZ());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private record ProjectedHit(Vec3 position, Direction face) {
    }

    private record Splat(Vec3 position, Direction face,
            Vec3 surfaceAlong, Vec3 surfaceSide,
            double halfAlong, double halfSide,
            float alpha, float bloomAlpha) {
    }

    private record SplatCache(long tick, List<Splat> splats) {
    }
}
