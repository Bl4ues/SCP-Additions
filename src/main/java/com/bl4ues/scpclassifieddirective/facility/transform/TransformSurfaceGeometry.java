package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** Shared world-space geometry for rigid and curve-deformed surface payloads. */
public final class TransformSurfaceGeometry {
    private static final int CURVE_SUBDIVISIONS = 3;

    private TransformSurfaceGeometry() {
    }

    public static List<AABB> collisionBoxes(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment) {
        if (surface == null || slot == null || attachment == null
                || attachment.state().isAir()) return List.of();
        VoxelShape shape = attachment.state().getCollisionShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                CollisionContext.empty());
        if (shape.isEmpty()) return List.of();
        List<AABB> result = new ArrayList<>();
        for (AABB box : shape.toAabbs()) {
            if (attachment.deform()) addDeformed(surface, slot, box, result);
            else result.add(rigidBounds(surface, slot, box));
        }
        return List.copyOf(result);
    }

    private static void addDeformed(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, AABB box,
            List<AABB> output) {
        double step = (box.maxX - box.minX) / CURVE_SUBDIVISIONS;
        if (step < 1.0E-6D) {
            output.add(deformedBounds(surface, slot, box));
            return;
        }
        for (int index = 0; index < CURVE_SUBDIVISIONS; index++) {
            double minX = box.minX + step * index;
            double maxX = index == CURVE_SUBDIVISIONS - 1
                    ? box.maxX : box.minX + step * (index + 1);
            output.add(deformedBounds(surface, slot,
                    new AABB(minX, box.minY, box.minZ,
                            maxX, box.maxY, box.maxZ)));
        }
    }

    private static AABB rigidBounds(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, AABB local) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 tangent = surface.gridFrameTangent(u, v);
        Vec3 normal = surface.gridNormal(u, v);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u));
        Vec3 center = surface.gridPoint(u, v);
        return bounds((x, y, z) -> center
                .add(tangent.scale(x - 0.5D))
                .add(vertical.scale(y - 0.5D))
                // The authored surface is the BACK face of the placed block.
                // This keeps walls/equipment on the chosen side instead of
                // burying half of every payload through the guide plane.
                .add(normal.scale(z)), local);
    }

    private static AABB deformedBounds(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, AABB local) {
        return bounds((x, y, z) -> {
            double localX = surface.flipped() ? 1.0D - x : x;
            double u = (slot.column() + localX) / surface.columns();
            double v = (slot.row() + y) / surface.rows();
            Vec3 normal = surface.gridNormal(u, v);
            return surface.gridPoint(u, v).add(normal.scale(z));
        }, local);
    }

    private static AABB bounds(PointTransform transform, AABB local) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int xi = 0; xi < 2; xi++) {
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    Vec3 point = transform.apply(
                            xi == 0 ? local.minX : local.maxX,
                            yi == 0 ? local.minY : local.maxY,
                            zi == 0 ? local.minZ : local.maxZ);
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    minZ = Math.min(minZ, point.z);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                    maxZ = Math.max(maxZ, point.z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @FunctionalInterface
    private interface PointTransform {
        Vec3 apply(double x, double y, double z);
    }
}
