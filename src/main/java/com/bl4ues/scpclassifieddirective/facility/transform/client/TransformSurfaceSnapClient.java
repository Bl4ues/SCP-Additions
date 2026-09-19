package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Editor-only snap resolver. Samples nearby existing geometry but never
 * rebuilds construction meshes, proxies or collision indices while dragging.
 * The committed endpoint is still validated by the normal server update.
 */
final class TransformSurfaceSnapClient {
    private static final double RANGE_SQR = 0.30D * 0.30D;
    private static final double[] POINTS = {0.0D, 0.5D, 1.0D};
    private static final double[] EDGE_SAMPLES = {
            0.0D, 0.125D, 0.25D, 0.375D, 0.5D,
            0.625D, 0.75D, 0.875D, 1.0D
    };

    private TransformSurfaceSnapClient() {
    }

    static Vec3 snap(Vec3 point, UUID editingSurfaceId) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (point == null || level == null) return point;
        Vec3 surfaceTarget = closestSurfaceBoundary(point, editingSurfaceId,
                level);
        if (surfaceTarget != null) return surfaceTarget;
        Vec3 blockTarget = closestBlockFeature(point, level);
        return blockTarget == null ? point : blockTarget;
    }

    private static Vec3 closestSurfaceBoundary(Vec3 point,
            UUID editingSurfaceId, ClientLevel level) {
        Vec3 closest = null;
        double best = RANGE_SQR;
        for (ConstructionSurface candidate
                : TransformConstructionClientState.surfaces(
                        level.dimension().location())) {
            if (candidate.id().equals(editingSurfaceId)
                    || !withinBounds(point, candidate)) continue;
            for (double fraction : EDGE_SAMPLES) {
                // The four parametric borders are the physically meaningful
                // joining features. Their midpoint samples include curved
                // edges, not merely the bounding rectangle's vertices.
                for (int border = 0; border < 4; border++) {
                    double u = border == 0 ? 0.0D
                            : border == 1 ? 1.0D : fraction;
                    double v = border == 2 ? 0.0D
                            : border == 3 ? 1.0D : fraction;
                    Vec3 anchor = candidate.gridPoint(u, v);
                    double distance = anchor.distanceToSqr(point);
                    if (distance < best) {
                        best = distance;
                        closest = anchor;
                    }
                }
            }
        }
        return closest;
    }

    private static boolean withinBounds(Vec3 point,
            ConstructionSurface surface) {
        Vec3[] controls = {surface.bottomStart(), surface.bottomEnd(),
                surface.topStart(), surface.topEnd(),
                surface.bottomControl(), surface.topControl()};
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 control : controls) {
            minX = Math.min(minX, control.x);
            minY = Math.min(minY, control.y);
            minZ = Math.min(minZ, control.z);
            maxX = Math.max(maxX, control.x);
            maxY = Math.max(maxY, control.y);
            maxZ = Math.max(maxZ, control.z);
        }
        // Interior height bending can extend beyond the corner hull.
        Vec3 bend = surface.heightCurveOffset();
        double margin = 0.31D;
        return point.x >= minX + Math.min(0.0D, bend.x) - margin
                && point.x <= maxX + Math.max(0.0D, bend.x) + margin
                && point.y >= minY + Math.min(0.0D, bend.y) - margin
                && point.y <= maxY + Math.max(0.0D, bend.y) + margin
                && point.z >= minZ + Math.min(0.0D, bend.z) - margin
                && point.z <= maxZ + Math.max(0.0D, bend.z) + margin;
    }

    private static Vec3 closestBlockFeature(Vec3 point,
            ClientLevel level) {
        BlockPos center = BlockPos.containing(point);
        Vec3 closest = null;
        double best = RANGE_SQR;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState block = level.getBlockState(pos);
                    if (block.isAir() || block.is(
                            TransformConstructionModule.getProxy())
                            || block.getRenderShape() == RenderShape.INVISIBLE) {
                        continue;
                    }
                    for (double x : POINTS) {
                        for (double y : POINTS) {
                            for (double z : POINTS) {
                                Vec3 feature = new Vec3(pos.getX() + x,
                                        pos.getY() + y, pos.getZ() + z);
                                double distance = feature.distanceToSqr(point);
                                if (distance < best) {
                                    best = distance;
                                    closest = feature;
                                }
                            }
                        }
                    }
                }
            }
        }
        return closest;
    }
}
