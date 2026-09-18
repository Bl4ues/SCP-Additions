package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Raycasts the authored parametric Surface grid directly.
 *
 * The vanilla proxy grid is only a collision bridge and is deliberately not
 * used for picking. Curved/tilted cells are tessellated into small triangles,
 * so placement and editor selection resolve against the same physical surface
 * the renderer shows instead of an axis-aligned BlockPos bounding box.
 */
public final class TransformSurfaceRaycast {
    private static final double MAX_DISTANCE = 32.0D;
    private static final int CELL_SUBDIVISIONS = 3;
    private static final double EPSILON = 1.0E-8D;

    private TransformSurfaceRaycast() {
    }

    public static Target target(LocalPlayer player, ConstructionSurface surface) {
        if (player == null || surface == null) return null;
        return target(player, java.util.List.of(surface));
    }

    public static Target target(LocalPlayer player,
            Collection<ConstructionSurface> surfaces) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null
                || surfaces == null || surfaces.isEmpty()) return null;

        Vec3 eye = player.getEyePosition();
        Vec3 ray = player.getViewVector(1.0F).normalize();
        double limit = MAX_DISTANCE;

        HitResult vanilla = minecraft.hitResult;
        if (vanilla instanceof BlockHitResult blockHit
                && vanilla.getType() == HitResult.Type.BLOCK
                && !minecraft.level.getBlockState(blockHit.getBlockPos())
                        .is(TransformConstructionModule.getProxy())) {
            // Allow a curved Surface payload sharing this exact vanilla cell to
            // remain pickable, but never raycast through unrelated terrain.
            limit = Math.min(limit,
                    eye.distanceTo(blockHit.getLocation()) + 0.08D);
        }

        Target best = null;
        double bestDistance = limit + 1.0D;
        for (ConstructionSurface surface : surfaces) {
            if (surface == null || !surface.dimension().equals(
                    minecraft.level.dimension().location())) continue;

            // Cheap sphere rejection before walking the parametric cells.
            Vec3 center = surface.gridPoint(0.5D, 0.5D);
            double radius = Math.max(surface.width(), surface.height()) * 0.75D
                    + 1.5D;
            double along = center.subtract(eye).dot(ray);
            if (along < -radius || along - radius > bestDistance) continue;
            Vec3 nearest = eye.add(ray.scale(Math.max(0.0D, along)));
            if (nearest.distanceToSqr(center) > radius * radius) continue;

            int columns = surface.columns();
            int rows = surface.rows();
            for (int column = 0; column < columns; column++) {
                double cellU0 = column / (double) columns;
                double cellU1 = (column + 1.0D) / columns;
                for (int row = 0; row < rows; row++) {
                    double cellV0 = row / (double) rows;
                    double cellV1 = (row + 1.0D) / rows;
                    ConstructionSurface.SurfaceSlot slot =
                            new ConstructionSurface.SurfaceSlot(column, row);

                    for (int su = 0; su < CELL_SUBDIVISIONS; su++) {
                        double u0 = lerp(cellU0, cellU1,
                                su / (double) CELL_SUBDIVISIONS);
                        double u1 = lerp(cellU0, cellU1,
                                (su + 1.0D) / CELL_SUBDIVISIONS);
                        for (int sv = 0; sv < CELL_SUBDIVISIONS; sv++) {
                            double v0 = lerp(cellV0, cellV1,
                                    sv / (double) CELL_SUBDIVISIONS);
                            double v1 = lerp(cellV0, cellV1,
                                    (sv + 1.0D) / CELL_SUBDIVISIONS);

                            Vec3 p00 = surface.gridPoint(u0, v0);
                            Vec3 p10 = surface.gridPoint(u1, v0);
                            Vec3 p11 = surface.gridPoint(u1, v1);
                            Vec3 p01 = surface.gridPoint(u0, v1);

                            double first = triangle(eye, ray, p00, p10, p11);
                            if (first >= 0.0D && first <= limit
                                    && first < bestDistance) {
                                bestDistance = first;
                                best = new Target(surface, slot,
                                        eye.add(ray.scale(first)), first);
                            }
                            double second = triangle(eye, ray, p00, p11, p01);
                            if (second >= 0.0D && second <= limit
                                    && second < bestDistance) {
                                bestDistance = second;
                                best = new Target(surface, slot,
                                        eye.add(ray.scale(second)), second);
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private static double triangle(Vec3 origin, Vec3 ray,
            Vec3 a, Vec3 b, Vec3 c) {
        Vec3 edge1 = b.subtract(a);
        Vec3 edge2 = c.subtract(a);
        Vec3 p = ray.cross(edge2);
        double det = edge1.dot(p);
        if (Math.abs(det) < EPSILON) return -1.0D;
        double invDet = 1.0D / det;
        Vec3 t = origin.subtract(a);
        double u = t.dot(p) * invDet;
        if (u < -EPSILON || u > 1.0D + EPSILON) return -1.0D;
        Vec3 q = t.cross(edge1);
        double v = ray.dot(q) * invDet;
        if (v < -EPSILON || u + v > 1.0D + EPSILON) return -1.0D;
        double distance = edge2.dot(q) * invDet;
        return distance >= 0.0D ? distance : -1.0D;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public record Target(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, Vec3 hit, double distance) {
    }
}
