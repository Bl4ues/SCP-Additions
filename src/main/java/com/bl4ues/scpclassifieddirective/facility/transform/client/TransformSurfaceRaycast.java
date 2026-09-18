package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformWallFixturePlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Raycasts the Surface's own logical block grid rather than the parent-world
 * proxy shapes. Occupied cells are six-faced logical blocks following the same
 * parametric frame used by rendering/collision; empty cells retain the authored
 * guide plane for construction.
 */
public final class TransformSurfaceRaycast {
    private static final double MAX_DISTANCE = 32.0D;
    private static final int CELL_SUBDIVISIONS = 3;
    private static final int LEAF_CELL_SPAN = 3;
    private static final int MAX_PATCH_DEPTH = 14;
    private static final double PATCH_INFLATE = 0.075D;
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
        net.minecraft.core.BlockPos vanillaBlocker = null;
        double vanillaDistance = Double.POSITIVE_INFINITY;

        HitResult vanilla = minecraft.hitResult;
        if (vanilla instanceof BlockHitResult blockHit
                && vanilla.getType() == HitResult.Type.BLOCK
                && !minecraft.level.getBlockState(blockHit.getBlockPos())
                        .is(TransformConstructionModule.getProxy())) {
            vanillaBlocker = blockHit.getBlockPos();
            vanillaDistance = eye.distanceTo(blockHit.getLocation());
        }

        Target best = null;
        double bestDistance = limit + 1.0D;
        for (ConstructionSurface surface : surfaces) {
            if (surface == null || !surface.dimension().equals(
                    minecraft.level.dimension().location())) continue;

            Vec3 center = surface.gridPoint(0.5D, 0.5D);
            double radius = Math.max(surface.width(), surface.height()) * 0.75D
                    + 3.0D;
            double along = center.subtract(eye).dot(ray);
            if (along < -radius || along - radius > bestDistance) continue;
            Vec3 nearest = eye.add(ray.scale(Math.max(0.0D, along)));
            if (nearest.distanceToSqr(center) > radius * radius) continue;

            Target candidate = targetSurface(surface, eye, ray,
                    Math.min(limit, bestDistance));
            if (candidate == null) continue;
            if (vanillaBlocker != null
                    && candidate.distance() > vanillaDistance + 1.0E-4D
                    && (!TransformConstructionClientState
                            .surfaceTouchesWorldCell(surface.id(),
                                    vanillaBlocker)
                    || candidate.distance() > vanillaDistance + 1.80D)) {
                continue;
            }
            if (candidate.distance() < bestDistance) {
                bestDistance = candidate.distance();
                best = candidate;
            }
        }
        return best;
    }

    private static Target targetSurface(ConstructionSurface surface,
            Vec3 eye, Vec3 ray, double limit) {
        int columns = surface.columns();
        int rows = surface.rows();
        ArrayDeque<Patch> pending = new ArrayDeque<>();
        pending.add(new Patch(0.0D, 1.0D, 0.0D, 1.0D, 0));
        Set<Long> visited = new HashSet<>();
        Target best = null;
        double bestDistance = limit + 1.0D;

        while (!pending.isEmpty()) {
            Patch patch = pending.removeFirst();
            AABB bounds = patchBounds(surface, patch);
            double entry = rayBox(eye, ray, bounds);
            if (entry < 0.0D || entry > Math.min(limit, bestDistance)) continue;

            int minColumn = clampCell((int) Math.floor(
                    patch.u0() * columns), columns);
            int maxColumn = clampCell((int) Math.ceil(
                    patch.u1() * columns) - 1, columns);
            int minRow = clampCell((int) Math.floor(
                    patch.v0() * rows), rows);
            int maxRow = clampCell((int) Math.ceil(
                    patch.v1() * rows) - 1, rows);
            int columnSpan = maxColumn - minColumn + 1;
            int rowSpan = maxRow - minRow + 1;

            if (patch.depth() >= MAX_PATCH_DEPTH
                    || columnSpan <= LEAF_CELL_SPAN
                    && rowSpan <= LEAF_CELL_SPAN) {
                for (int column = minColumn; column <= maxColumn; column++) {
                    for (int row = minRow; row <= maxRow; row++) {
                        long key = ((long) column << 32)
                                ^ (row & 0xffffffffL);
                        if (!visited.add(key)) continue;
                        Target candidate = targetCell(surface, column, row,
                                eye, ray, Math.min(limit, bestDistance));
                        if (candidate != null
                                && candidate.distance() < bestDistance) {
                            bestDistance = candidate.distance();
                            best = candidate;
                        }
                    }
                }
                continue;
            }

            if (columnSpan >= rowSpan) {
                double middle = (patch.u0() + patch.u1()) * 0.5D;
                pending.addFirst(new Patch(middle, patch.u1(),
                        patch.v0(), patch.v1(), patch.depth() + 1));
                pending.addFirst(new Patch(patch.u0(), middle,
                        patch.v0(), patch.v1(), patch.depth() + 1));
            } else {
                double middle = (patch.v0() + patch.v1()) * 0.5D;
                pending.addFirst(new Patch(patch.u0(), patch.u1(),
                        middle, patch.v1(), patch.depth() + 1));
                pending.addFirst(new Patch(patch.u0(), patch.u1(),
                        patch.v0(), middle, patch.depth() + 1));
            }
        }
        return best;
    }

    private static Target targetCell(ConstructionSurface surface,
            int column, int row, Vec3 eye, Vec3 ray, double limit) {
        ConstructionSurface.SurfaceSlot slot =
                new ConstructionSurface.SurfaceSlot(column, row);
        Target best = null;
        double bestDistance = limit + 1.0D;

        VisualAttachment positive =
                visualAttachment(surface, slot, 1, true);
        if (positive != null && !positive.attachment().state().isAir()) {
            best = nearer(best, targetLogicalCell(surface, slot,
                    positive.anchor(), positive.attachment(),
                    1, true, Layer.POSITIVE_OVERLAY, eye, ray,
                    Math.min(limit, bestDistance)));
            if (best != null) bestDistance = best.distance();
        }

        VisualAttachment negative =
                visualAttachment(surface, slot, -1, true);
        if (negative != null && !negative.attachment().state().isAir()) {
            best = nearer(best, targetLogicalCell(surface, slot,
                    negative.anchor(), negative.attachment(),
                    -1, true, Layer.NEGATIVE_OVERLAY, eye, ray,
                    Math.min(limit, bestDistance)));
            if (best != null) bestDistance = best.distance();
        }

        VisualAttachment main =
                visualAttachment(surface, slot, 1, false);
        if (main != null && !main.attachment().state().isAir()) {
            best = nearer(best, targetLogicalCell(surface, slot,
                    main.anchor(), main.attachment(),
                    1, false, Layer.MAIN, eye, ray,
                    Math.min(limit, bestDistance)));
            if (best != null) bestDistance = best.distance();
        }

        if (best != null) return best;

        // Empty authored cell: the guide itself is the placement plane.
        return targetGuideCell(surface, slot, eye, ray, limit);
    }

    private static VisualAttachment visualAttachment(
            ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot visualSlot,
            int normalSign, boolean overlay) {
        int side = normalSign < 0 ? -1 : 1;
        ConstructionSurface.SurfaceAttachment direct = overlay
                ? surface.overlay(visualSlot, side)
                : surface.attachments().get(visualSlot);
        if (direct != null
                && TransformWallFixturePlacement.visualShift(
                        direct.state()) == null) {
            return new VisualAttachment(visualSlot, direct);
        }

        int frameSign = (surface.flipped() ? -1 : 1) * side;
        for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
            if (columnOffset == 0) continue;
            int column = visualSlot.column() + columnOffset;
            if (column < 0 || column >= surface.columns()) continue;
            ConstructionSurface.SurfaceSlot anchor =
                    new ConstructionSurface.SurfaceSlot(
                            column, visualSlot.row());
            ConstructionSurface.SurfaceAttachment candidate = overlay
                    ? surface.overlay(anchor, side)
                    : surface.attachments().get(anchor);
            if (candidate == null) continue;
            Direction visualShift =
                    TransformWallFixturePlacement.visualShift(
                            candidate.state());
            if (visualShift == null) continue;
            int visualColumn = anchor.column()
                    + visualShift.getStepX() * frameSign;
            int visualRow = anchor.row() + visualShift.getStepY();
            if (visualColumn == visualSlot.column()
                    && visualRow == visualSlot.row()) {
                return new VisualAttachment(anchor, candidate);
            }
        }
        return null;
    }

    private static Target targetLogicalCell(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot visualSlot,
            ConstructionSurface.SurfaceSlot addressSlot,
            ConstructionSurface.SurfaceAttachment attachment, int normalSign,
            boolean overlay, Layer layer, Vec3 eye, Vec3 ray, double limit) {
        Target best = null;
        double bestDistance = limit + 1.0D;
        for (Direction face : Direction.values()) {
            for (int a = 0; a < CELL_SUBDIVISIONS; a++) {
                double a0 = a / (double) CELL_SUBDIVISIONS;
                double a1 = (a + 1.0D) / CELL_SUBDIVISIONS;
                for (int b = 0; b < CELL_SUBDIVISIONS; b++) {
                    double b0 = b / (double) CELL_SUBDIVISIONS;
                    double b1 = (b + 1.0D) / CELL_SUBDIVISIONS;
                    Vec3 p00 = facePoint(surface, slot, attachment.deform(),
                            normalSign, overlay, face, a0, b0);
                    Vec3 p10 = facePoint(surface, slot, attachment.deform(),
                            normalSign, overlay, face, a1, b0);
                    Vec3 p11 = facePoint(surface, slot, attachment.deform(),
                            normalSign, overlay, face, a1, b1);
                    Vec3 p01 = facePoint(surface, slot, attachment.deform(),
                            normalSign, overlay, face, a0, b1);

                    double first = triangle(eye, ray, p00, p10, p11);
                    if (first >= 0.0D && first <= limit
                            && first < bestDistance) {
                        bestDistance = first;
                        best = new Target(surface, slot,
                                eye.add(ray.scale(first)), first,
                                normalSign, face, layer);
                    }
                    double second = triangle(eye, ray, p00, p11, p01);
                    if (second >= 0.0D && second <= limit
                            && second < bestDistance) {
                        bestDistance = second;
                        best = new Target(surface, slot,
                                eye.add(ray.scale(second)), second,
                                normalSign, face, layer);
                    }
                }
            }
        }
        return best;
    }

    private static Vec3 facePoint(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, boolean deform,
            int normalSign, boolean overlay, Direction face,
            double a, double b) {
        double x;
        double y;
        double z;
        switch (face) {
            case WEST -> { x = 0.0D; y = a; z = b; }
            case EAST -> { x = 1.0D; y = a; z = b; }
            case DOWN -> { x = a; y = 0.0D; z = b; }
            case UP -> { x = a; y = 1.0D; z = b; }
            case NORTH -> { x = a; y = b; z = 0.0D; }
            case SOUTH -> { x = a; y = b; z = 1.0D; }
            default -> throw new IllegalStateException("Unexpected face " + face);
        }
        return TransformSurfaceGeometry.logicalPoint(surface, slot, deform,
                normalSign, overlay, x, y, z);
    }

    private static Target targetGuideCell(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, Vec3 eye, Vec3 ray,
            double limit) {
        int columns = surface.columns();
        int rows = surface.rows();
        double cellU0 = slot.column() / (double) columns;
        double cellU1 = (slot.column() + 1.0D) / columns;
        double cellV0 = slot.row() / (double) rows;
        double cellV1 = (slot.row() + 1.0D) / rows;
        Target best = null;
        double bestDistance = limit + 1.0D;
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
                int sign = normalSign(surface, ray,
                        (u0 + u1) * 0.5D, (v0 + v1) * 0.5D);
                Direction face = sign > 0 ? Direction.SOUTH : Direction.NORTH;

                double first = triangle(eye, ray, p00, p10, p11);
                if (first >= 0.0D && first <= limit
                        && first < bestDistance) {
                    bestDistance = first;
                    best = new Target(surface, slot,
                            eye.add(ray.scale(first)), first, sign,
                            face, Layer.GUIDE);
                }
                double second = triangle(eye, ray, p00, p11, p01);
                if (second >= 0.0D && second <= limit
                        && second < bestDistance) {
                    bestDistance = second;
                    best = new Target(surface, slot,
                            eye.add(ray.scale(second)), second, sign,
                            face, Layer.GUIDE);
                }
            }
        }
        return best;
    }

    private static Target nearer(Target current, Target candidate) {
        if (candidate == null) return current;
        return current == null || candidate.distance() < current.distance()
                ? candidate : current;
    }

    private static int normalSign(ConstructionSurface surface,
            Vec3 ray, double u, double v) {
        Vec3 normal = surface.gridNormal(u, v);
        return ray.dot(normal) <= 0.0D ? 1 : -1;
    }

    private static AABB patchBounds(ConstructionSurface surface, Patch patch) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int ui = 0; ui <= 4; ui++) {
            double u = lerp(patch.u0(), patch.u1(), ui / 4.0D);
            for (int vi = 0; vi <= 4; vi++) {
                double v = lerp(patch.v0(), patch.v1(), vi / 4.0D);
                Vec3 point = surface.gridPoint(u, v);
                Vec3 normal = surface.gridNormal(u, v);
                for (double depth : new double[]{-1.1D, 0.0D, 2.1D}) {
                    Vec3 sample = point.add(normal.scale(depth));
                    minX = Math.min(minX, sample.x);
                    minY = Math.min(minY, sample.y);
                    minZ = Math.min(minZ, sample.z);
                    maxX = Math.max(maxX, sample.x);
                    maxY = Math.max(maxY, sample.y);
                    maxZ = Math.max(maxZ, sample.z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ)
                .inflate(PATCH_INFLATE);
    }

    private static double rayBox(Vec3 origin, Vec3 direction, AABB box) {
        double tMin = 0.0D;
        double tMax = MAX_DISTANCE;
        double[] origins = {origin.x, origin.y, origin.z};
        double[] directions = {direction.x, direction.y, direction.z};
        double[] mins = {box.minX, box.minY, box.minZ};
        double[] maxs = {box.maxX, box.maxY, box.maxZ};
        for (int axis = 0; axis < 3; axis++) {
            double value = directions[axis];
            if (Math.abs(value) < EPSILON) {
                if (origins[axis] < mins[axis]
                        || origins[axis] > maxs[axis]) return -1.0D;
                continue;
            }
            double inverse = 1.0D / value;
            double near = (mins[axis] - origins[axis]) * inverse;
            double far = (maxs[axis] - origins[axis]) * inverse;
            if (near > far) {
                double swap = near;
                near = far;
                far = swap;
            }
            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMax + EPSILON < tMin) return -1.0D;
        }
        return tMin;
    }

    private static int clampCell(int value, int count) {
        return Mth.clamp(value, 0, Math.max(0, count - 1));
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

    private record VisualAttachment(
            ConstructionSurface.SurfaceSlot anchor,
            ConstructionSurface.SurfaceAttachment attachment) {
    }

    private record Patch(double u0, double u1, double v0, double v1,
            int depth) {
    }

    public enum Layer {
        GUIDE, MAIN, POSITIVE_OVERLAY, NEGATIVE_OVERLAY;

        public boolean overlay() {
            return this == POSITIVE_OVERLAY || this == NEGATIVE_OVERLAY;
        }
    }

    public record Target(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, Vec3 hit, double distance,
            int normalSign, Direction faceLocal, Layer layer) {
        public Target {
            normalSign = normalSign < 0 ? -1 : 1;
            faceLocal = faceLocal == null
                    ? (normalSign > 0 ? Direction.SOUTH : Direction.NORTH)
                    : faceLocal;
            layer = layer == null ? Layer.GUIDE : layer;
        }
    }
}
