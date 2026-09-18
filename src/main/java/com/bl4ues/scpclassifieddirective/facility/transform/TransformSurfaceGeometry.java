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
    private static final int CURVE_U_SUBDIVISIONS = 4;
    private static final int CURVE_V_SUBDIVISIONS = 3;
    private static final int RIGID_SUBDIVISIONS = 2;

    private TransformSurfaceGeometry() {
    }

    public static List<AABB> collisionBoxes(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment) {
        return collisionBoxes(surface, slot, attachment, 1);
    }

    public static List<AABB> collisionBoxes(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment,
            int normalSign) {
        if (surface == null || slot == null || attachment == null
                || attachment.state().isAir()) return List.of();
        int side = normalSign < 0 ? -1 : 1;
        VoxelShape shape = attachment.state().getCollisionShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                CollisionContext.empty());
        if (shape.isEmpty()) return List.of();
        List<AABB> result = new ArrayList<>();
        for (AABB box : shape.toAabbs()) {
            if (attachment.deform()) {
                addDeformed(surface, slot, box, side, result);
            } else {
                addRigid(surface, slot, box, side, result);
            }
        }
        return List.copyOf(result);
    }

    private static void addDeformed(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, AABB box, int side,
            List<AABB> output) {
        int uSteps = box.getXsize() < 1.0E-5D ? 1 : CURVE_U_SUBDIVISIONS;
        int vSteps = surface.heightCurveOffset().lengthSqr() < 1.0E-8D
                || box.getYsize() < 1.0E-5D ? 1 : CURVE_V_SUBDIVISIONS;
        double dx = box.getXsize() / uSteps;
        double dy = box.getYsize() / vSteps;
        for (int ux = 0; ux < uSteps; ux++) {
            double minX = box.minX + dx * ux;
            double maxX = ux == uSteps - 1 ? box.maxX
                    : box.minX + dx * (ux + 1);
            for (int vy = 0; vy < vSteps; vy++) {
                double minY = box.minY + dy * vy;
                double maxY = vy == vSteps - 1 ? box.maxY
                        : box.minY + dy * (vy + 1);
                output.add(deformedBounds(surface, slot,
                        new AABB(minX, minY, box.minZ,
                                maxX, maxY, box.maxZ), side));
            }
        }
    }

    private static void addRigid(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, AABB box, int side,
            List<AABB> output) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 tangent = surface.gridFrameTangent(u, v).normalize();
        Vec3 normal = surface.gridNormal(u, v).normalize();
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));

        int subdivisions = cardinal(tangent) && cardinal(vertical)
                && cardinal(normal) ? 1 : RIGID_SUBDIVISIONS;
        double dx = box.getXsize() / subdivisions;
        double dy = box.getYsize() / subdivisions;
        double dz = box.getZsize() / subdivisions;
        for (int sx = 0; sx < subdivisions; sx++) {
            for (int sy = 0; sy < subdivisions; sy++) {
                for (int sz = 0; sz < subdivisions; sz++) {
                    double minX = box.minX + dx * sx;
                    double minY = box.minY + dy * sy;
                    double minZ = box.minZ + dz * sz;
                    double maxX = sx == subdivisions - 1 ? box.maxX
                            : box.minX + dx * (sx + 1);
                    double maxY = sy == subdivisions - 1 ? box.maxY
                            : box.minY + dy * (sy + 1);
                    double maxZ = sz == subdivisions - 1 ? box.maxZ
                            : box.minZ + dz * (sz + 1);
                    output.add(rigidBounds(surface, slot,
                            new AABB(minX, minY, minZ, maxX, maxY, maxZ),
                            side));
                }
            }
        }
    }

    private static boolean cardinal(Vec3 axis) {
        double max = Math.max(Math.abs(axis.x),
                Math.max(Math.abs(axis.y), Math.abs(axis.z)));
        return max > 0.9999D;
    }

    private static AABB rigidBounds(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, AABB local, int side) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 tangent = surface.gridFrameTangent(u, v).scale(side);
        Vec3 normal = surface.gridNormal(u, v).scale(side);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));
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
            ConstructionSurface.SurfaceSlot slot, AABB local, int side) {
        return bounds((x, y, z) -> {
            double baseX = surface.flipped() ? 1.0D - x : x;
            double localX = side < 0 ? 1.0D - baseX : baseX;
            double u = (slot.column() + localX) / surface.columns();
            double v = (slot.row() + y) / surface.rows();
            Vec3 normal = surface.gridNormal(u, v).scale(side);
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
