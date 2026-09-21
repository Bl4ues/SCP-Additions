package com.bl4ues.scpclassifieddirective.facility.mapping;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Refines a mapped rectangle against authored construction surfaces.
 *
 * Each wall segment contributes a one-sided strip toward the Surface's authored
 * interior (-normal). Segment strips are UNIONED first, then the room is
 * intersected with that curved region. This matters for concave/convex corridor
 * walls: intersecting every tangent half-plane turns an arc into a wedge.
 */
public final class FacilitySurfaceBoundaryConformer {
    private static final double FLOOR_TOLERANCE = 1.25D;
    private static final double WALL_VERTICAL_MIN = 0.45D;
    private static final double BOUNDS_MARGIN = 1.25D;
    private static final int MIN_SAMPLES = 20;
    private static final int MAX_SAMPLES = 192;
    private static final int MAX_OUTPUT_VERTICES = 768;
    private static final double EPSILON = 1.0E-5D;

    private FacilitySurfaceBoundaryConformer() {
    }

    public static FacilityFloorPatch conform(ServerLevel level,
            FacilityFloorPatch base) {
        return conform(level, base, null);
    }

    public static FacilityFloorPatch conform(ServerLevel level,
            FacilityFloorPatch base, BlockPos interiorProbe) {
        if (level == null || base == null) return base;
        Area result = polygonArea(base.outline());
        if (result.isEmpty()) return base;

        double spanX = base.maxX() - base.minX() + 1.0D;
        double spanZ = base.maxZ() - base.minZ() + 1.0D;
        double far = Math.hypot(spanX, spanZ) + 8.0D;
        boolean changed = false;

        for (ConstructionSurface surface
                : TransformConstructionManager.surfaces(level)) {
            if (!isRelevant(base, surface)) continue;
            Area exterior = exteriorRegion(surface, far);
            if (exterior.isEmpty()) continue;

            // A wall only excludes the space on its placement side (+normal).
            // Intersecting the whole room with an "interior strip" made every
            // unrelated part of the selection disappear at the strip's ends.
            Area clipped = new Area(result);
            clipped.subtract(exterior);
            if (clipped.isEmpty()) continue;

            double before = areaMagnitude(result);
            double after = areaMagnitude(clipped);
            if (after < 0.02D || after < before * 0.015D) continue;
            if (!sameArea(result, clipped)) changed = true;
            result = clipped;
        }

        Vec3 interior = interiorProbe == null
                ? new Vec3((base.minX() + base.maxX() + 1.0D) * 0.5D,
                        base.y() + 1.0D,
                        (base.minZ() + base.maxZ() + 1.0D) * 0.5D)
                : Vec3.atCenterOf(interiorProbe);
        for (TransformGroup group : TransformConstructionManager.groups(level)) {
            Area exterior = offGridExteriorRegion(group, base, interior, far);
            if (exterior.isEmpty()) continue;
            Area clipped = new Area(result);
            clipped.subtract(exterior);
            if (clipped.isEmpty()) continue;
            double before = areaMagnitude(result);
            double after = areaMagnitude(clipped);
            if (after < 0.02D || after < before * 0.015D) continue;
            if (!sameArea(result, clipped)) changed = true;
            result = clipped;
        }

        if (!changed) return base;
        List<FacilityFloorPatch.Vertex> vertices =
                selectedPolygon(result, interior.x, interior.z);
        FacilityFloorPatch refined = FacilityFloorPatch.polygon(base.y(),
                vertices);
        return refined == null ? base : refined;
    }

    private static Area offGridExteriorRegion(TransformGroup group,
            FacilityFloorPatch patch, Vec3 interior, double distance) {
        Area result = new Area();
        if (group == null || group.cells().isEmpty()) return result;

        Vec3 worldUp = TransformMath.rotate(new Vec3(0.0D, 1.0D, 0.0D),
                group.rotationX(), group.rotationY(), group.rotationZ());
        if (Math.abs(worldUp.y) < 0.72D) return result;

        java.util.Set<TransformGroup.GridPos> solids =
                new java.util.LinkedHashSet<>();
        for (var entry : group.cells().entrySet()) {
            if (entry.getValue() == null || entry.getValue().isAir()
                    || entry.getValue().getCollisionShape(
                            EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                            CollisionContext.empty()).isEmpty()) {
                continue;
            }
            Vec3 center = group.cellCenter(entry.getKey());
            if (Math.abs(center.y - (patch.y() + 1.5D)) > 1.15D) continue;
            if (center.x < patch.minX() - BOUNDS_MARGIN - 1.0D
                    || center.x > patch.maxX() + 1.0D + BOUNDS_MARGIN
                    || center.z < patch.minZ() - BOUNDS_MARGIN - 1.0D
                    || center.z > patch.maxZ() + 1.0D + BOUNDS_MARGIN) {
                continue;
            }
            solids.add(entry.getKey());
        }
        if (solids.isEmpty()) return result;

        for (TransformGroup.GridPos cell : solids) {
            boolean alongX = solids.contains(cell.offset(-1, 0, 0))
                    || solids.contains(cell.offset(1, 0, 0));
            boolean alongZ = solids.contains(cell.offset(0, 0, -1))
                    || solids.contains(cell.offset(0, 0, 1));

            List<LocalFace> faces = new ArrayList<>(4);
            if (!solids.contains(cell.offset(-1, 0, 0))
                    && (!alongX || alongZ)) {
                faces.add(LocalFace.WEST);
            }
            if (!solids.contains(cell.offset(1, 0, 0))
                    && (!alongX || alongZ)) {
                faces.add(LocalFace.EAST);
            }
            if (!solids.contains(cell.offset(0, 0, -1))
                    && (!alongZ || alongX)) {
                faces.add(LocalFace.NORTH);
            }
            if (!solids.contains(cell.offset(0, 0, 1))
                    && (!alongZ || alongX)) {
                faces.add(LocalFace.SOUTH);
            }
            if (faces.isEmpty()) continue;

            FaceEdge best = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (LocalFace face : faces) {
                FaceEdge edge = worldFaceEdge(group, cell, face);
                double value = pointSegmentDistanceSqr(interior.x, interior.z,
                        edge.a().x, edge.a().z, edge.b().x, edge.b().z);
                if (value < bestDistance) {
                    bestDistance = value;
                    best = edge;
                }
            }
            if (best == null) continue;
            FaceEdge clippedEdge = clipToPatch(best, patch);
            if (clippedEdge == null) continue;

            Vec3 midpoint = clippedEdge.a().add(clippedEdge.b()).scale(0.5D);
            Vec3 tangent = horizontal(clippedEdge.b().subtract(
                    clippedEdge.a()));
            if (tangent.lengthSqr() < 1.0E-10D) continue;
            tangent = tangent.normalize();
            Vec3 normal = new Vec3(-tangent.z, 0.0D, tangent.x);
            if (interior.subtract(midpoint).dot(normal) < 0.0D) {
                normal = normal.scale(-1.0D);
            }
            Vec3 outward = normal.scale(-1.0D);
            // Only the part of the transformed wall that actually crosses the
            // authored floor selection may clip it. Do not extend end caps or
            // invent a continuation toward the player's second corner.
            addStrip(result, clippedEdge.a(), clippedEdge.b(), outward,
                    distance);
        }
        return result;
    }

    private static FaceEdge worldFaceEdge(TransformGroup group,
            TransformGroup.GridPos cell, LocalFace face) {
        double x0 = cell.x() - 0.5D;
        double x1 = cell.x() + 0.5D;
        double z0 = cell.z() - 0.5D;
        double z1 = cell.z() + 0.5D;
        double y = cell.y();
        Vec3 a;
        Vec3 b;
        switch (face) {
            case WEST -> {
                a = new Vec3(x0, y, z0);
                b = new Vec3(x0, y, z1);
            }
            case EAST -> {
                a = new Vec3(x1, y, z0);
                b = new Vec3(x1, y, z1);
            }
            case NORTH -> {
                a = new Vec3(x0, y, z0);
                b = new Vec3(x1, y, z0);
            }
            case SOUTH -> {
                a = new Vec3(x0, y, z1);
                b = new Vec3(x1, y, z1);
            }
            default -> throw new IllegalStateException();
        }
        return new FaceEdge(
                TransformMath.localToWorld(group.origin(), a,
                        group.rotationX(), group.rotationY(),
                        group.rotationZ()),
                TransformMath.localToWorld(group.origin(), b,
                        group.rotationX(), group.rotationY(),
                        group.rotationZ()));
    }

    private static FaceEdge clipToPatch(FaceEdge edge,
            FacilityFloorPatch patch) {
        double minX = patch.minX();
        double maxX = patch.maxX() + 1.0D;
        double minZ = patch.minZ();
        double maxZ = patch.maxZ() + 1.0D;
        double x0 = edge.a().x;
        double z0 = edge.a().z;
        double dx = edge.b().x - x0;
        double dz = edge.b().z - z0;
        double[] range = {0.0D, 1.0D};
        if (!clip(-dx, x0 - minX, range)
                || !clip(dx, maxX - x0, range)
                || !clip(-dz, z0 - minZ, range)
                || !clip(dz, maxZ - z0, range)) {
            return null;
        }
        if (range[1] - range[0] < 1.0E-6D) return null;
        Vec3 a = new Vec3(x0 + dx * range[0], edge.a().y,
                z0 + dz * range[0]);
        Vec3 b = new Vec3(x0 + dx * range[1], edge.b().y,
                z0 + dz * range[1]);
        return new FaceEdge(a, b);
    }

    private static boolean clip(double p, double q, double[] range) {
        if (Math.abs(p) < 1.0E-12D) return q >= 0.0D;
        double r = q / p;
        if (p < 0.0D) {
            if (r > range[1]) return false;
            if (r > range[0]) range[0] = r;
        } else {
            if (r < range[0]) return false;
            if (r < range[1]) range[1] = r;
        }
        return true;
    }

    private static double pointSegmentDistanceSqr(double px, double pz,
            double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double length = dx * dx + dz * dz;
        if (length < 1.0E-10D) {
            double ox = px - ax;
            double oz = pz - az;
            return ox * ox + oz * oz;
        }
        double t = Math.max(0.0D, Math.min(1.0D,
                ((px - ax) * dx + (pz - az) * dz) / length));
        double ox = px - (ax + dx * t);
        double oz = pz - (az + dz * t);
        return ox * ox + oz * oz;
    }

    private static Area exteriorRegion(ConstructionSurface surface,
            double distance) {
        int samples = samples(surface);
        Area result = new Area();
        Vec3 first = null;
        Vec3 firstNext = null;
        Vec3 firstOutward = null;
        Vec3 lastPrevious = null;
        Vec3 last = null;
        Vec3 lastOutward = null;
        for (int index = 0; index < samples; index++) {
            double u0 = index / (double) samples;
            double u1 = (index + 1.0D) / samples;
            double um = (u0 + u1) * 0.5D;
            Vec3 a = surface.gridPoint(u0, 0.0D);
            Vec3 b = surface.gridPoint(u1, 0.0D);
            Vec3 normal = surface.gridNormal(um, 0.0D);
            Vec3 outward = new Vec3(normal.x, 0.0D, normal.z);
            if (outward.lengthSqr() < 1.0E-10D) continue;
            outward = outward.normalize();
            if (first == null) {
                first = a;
                firstNext = b;
                firstOutward = outward;
            }
            lastPrevious = a;
            last = b;
            lastOutward = outward;

            addStrip(result, a, b, outward, distance);
        }

        // Tiny endpoint padding closes floating-point seams between authored
        // segments without turning a finite wall into an infinite cutting plane.
        double seam = 0.02D;
        if (first != null && firstNext != null && firstOutward != null) {
            Vec3 tangent = horizontal(firstNext.subtract(first));
            if (tangent.lengthSqr() > 1.0E-10D) {
                Vec3 extended = first.subtract(tangent.normalize().scale(seam));
                addStrip(result, extended, first, firstOutward, distance);
            }
        }
        if (lastPrevious != null && last != null && lastOutward != null) {
            Vec3 tangent = horizontal(last.subtract(lastPrevious));
            if (tangent.lengthSqr() > 1.0E-10D) {
                Vec3 extended = last.add(tangent.normalize().scale(seam));
                addStrip(result, last, extended, lastOutward, distance);
            }
        }
        return result;
    }

    private static void addStrip(Area result, Vec3 a, Vec3 b,
            Vec3 outwardUnit, double distance) {
        Vec3 outward = outwardUnit.scale(distance);
        Path2D.Double quad = new Path2D.Double(Path2D.WIND_NON_ZERO);
        quad.moveTo(a.x, a.z);
        quad.lineTo(b.x, b.z);
        quad.lineTo(b.x + outward.x, b.z + outward.z);
        quad.lineTo(a.x + outward.x, a.z + outward.z);
        quad.closePath();
        result.add(new Area(quad));
    }

    private static Vec3 horizontal(Vec3 value) {
        return new Vec3(value.x, 0.0D, value.z);
    }

    private static boolean isRelevant(FacilityFloorPatch patch,
            ConstructionSurface surface) {
        Vec3 middle = surface.gridPoint(0.5D, 0.0D);
        double floorDistance = Math.min(Math.abs(middle.y - patch.y()),
                Math.abs(middle.y - (patch.y() + 1.0D)));
        if (floorDistance > FLOOR_TOLERANCE) return false;
        if (Math.abs(surface.gridVertical(0.5D, 0.5D).y)
                < WALL_VERTICAL_MIN) return false;

        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        int samples = samples(surface);
        for (int i = 0; i <= samples; i++) {
            Vec3 point = surface.gridPoint(i / (double) samples, 0.0D);
            minX = Math.min(minX, point.x);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxZ = Math.max(maxZ, point.z);
        }
        return maxX >= patch.minX() - BOUNDS_MARGIN
                && minX <= patch.maxX() + 1.0D + BOUNDS_MARGIN
                && maxZ >= patch.minZ() - BOUNDS_MARGIN
                && minZ <= patch.maxZ() + 1.0D + BOUNDS_MARGIN;
    }

    private static int samples(ConstructionSurface surface) {
        return Math.max(MIN_SAMPLES, Math.min(MAX_SAMPLES,
                (int) Math.ceil(surface.width() * 10.0D)));
    }

    private static Area polygonArea(List<FacilityFloorPatch.Vertex> vertices) {
        if (vertices == null || vertices.size() < 3) return new Area();
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        path.moveTo(vertices.get(0).x(), vertices.get(0).z());
        for (int i = 1; i < vertices.size(); i++) {
            path.lineTo(vertices.get(i).x(), vertices.get(i).z());
        }
        path.closePath();
        return new Area(path);
    }

    private static double areaMagnitude(Area area) {
        double sum = 0.0D;
        for (List<FacilityFloorPatch.Vertex> polygon : polygons(area)) {
            sum += Math.abs(signedArea(polygon));
        }
        return sum;
    }

    private static boolean sameArea(Area a, Area b) {
        Area delta = new Area(a);
        delta.exclusiveOr(b);
        return delta.isEmpty();
    }

    private static List<FacilityFloorPatch.Vertex> selectedPolygon(
            Area area, double probeX, double probeZ) {
        List<FacilityFloorPatch.Vertex> bestContaining = List.of();
        double containingArea = 0.0D;
        List<FacilityFloorPatch.Vertex> largest = List.of();
        double largestArea = 0.0D;
        for (List<FacilityFloorPatch.Vertex> polygon : polygons(area)) {
            List<FacilityFloorPatch.Vertex> clean = simplify(polygon);
            double value = Math.abs(signedArea(clean));
            if (value > largestArea) {
                largestArea = value;
                largest = clean;
            }
            if (value > containingArea
                    && polygonArea(clean).contains(probeX, probeZ)) {
                containingArea = value;
                bestContaining = clean;
            }
        }
        return limit(bestContaining.isEmpty() ? largest : bestContaining,
                MAX_OUTPUT_VERTICES);
    }

    private static List<FacilityFloorPatch.Vertex> largestPolygon(Area area) {
        return selectedPolygon(area, Double.NaN, Double.NaN);
    }

    private static List<List<FacilityFloorPatch.Vertex>> polygons(Area area) {
        List<List<FacilityFloorPatch.Vertex>> result = new ArrayList<>();
        PathIterator iterator = area.getPathIterator(null, 0.008D);
        double[] coords = new double[6];
        List<FacilityFloorPatch.Vertex> current = null;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO) {
                if (current != null && current.size() >= 3) {
                    result.add(List.copyOf(current));
                }
                current = new ArrayList<>();
                current.add(new FacilityFloorPatch.Vertex(coords[0], coords[1]));
            } else if (type == PathIterator.SEG_LINETO && current != null) {
                current.add(new FacilityFloorPatch.Vertex(coords[0], coords[1]));
            } else if (type == PathIterator.SEG_CLOSE && current != null) {
                if (current.size() >= 3) result.add(List.copyOf(current));
                current = null;
            }
            iterator.next();
        }
        if (current != null && current.size() >= 3) {
            result.add(List.copyOf(current));
        }
        return result;
    }

    private static List<FacilityFloorPatch.Vertex> simplify(
            List<FacilityFloorPatch.Vertex> source) {
        List<FacilityFloorPatch.Vertex> clean = new ArrayList<>();
        for (FacilityFloorPatch.Vertex vertex : source) {
            if (clean.isEmpty()
                    || distanceSqr(clean.get(clean.size() - 1), vertex)
                    > EPSILON * EPSILON) {
                clean.add(vertex);
            }
        }
        if (clean.size() > 1 && distanceSqr(clean.get(0),
                clean.get(clean.size() - 1)) <= EPSILON * EPSILON) {
            clean.remove(clean.size() - 1);
        }
        boolean removed;
        do {
            removed = false;
            if (clean.size() <= 3) break;
            for (int i = 0; i < clean.size(); i++) {
                FacilityFloorPatch.Vertex a = clean.get(
                        (i - 1 + clean.size()) % clean.size());
                FacilityFloorPatch.Vertex b = clean.get(i);
                FacilityFloorPatch.Vertex c = clean.get((i + 1) % clean.size());
                double cross = (b.x() - a.x()) * (c.z() - b.z())
                        - (b.z() - a.z()) * (c.x() - b.x());
                if (Math.abs(cross) <= 1.0E-7D) {
                    clean.remove(i);
                    removed = true;
                    break;
                }
            }
        } while (removed);
        return List.copyOf(clean);
    }

    private static List<FacilityFloorPatch.Vertex> limit(
            List<FacilityFloorPatch.Vertex> source, int maximum) {
        if (source.size() <= maximum) return source;
        // Uniform index sampling discarded authored diagonal/vertical corners
        // irrespective of their geometric significance. Remove only vertices
        // whose chord error is subpixel at typical map scale; keep actual
        // corners even if that leaves more than the soft budget.
        List<FacilityFloorPatch.Vertex> reduced = new ArrayList<>(source);
        final double maxErrorSqr = 0.008D * 0.008D;
        boolean removed;
        do {
            removed = false;
            for (int i = 0; i < reduced.size()
                    && reduced.size() > maximum; i++) {
                int size = reduced.size();
                FacilityFloorPatch.Vertex before = reduced.get(
                        (i + size - 1) % size);
                FacilityFloorPatch.Vertex point = reduced.get(i);
                FacilityFloorPatch.Vertex after = reduced.get((i + 1) % size);
                double prevX = point.x() - before.x();
                double prevZ = point.z() - before.z();
                double nextX = after.x() - point.x();
                double nextZ = after.z() - point.z();
                double lengths = Math.hypot(prevX, prevZ)
                        * Math.hypot(nextX, nextZ);
                if (lengths < 1.0E-12D
                        || prevX * nextX + prevZ * nextZ
                                < 0.97D * lengths) continue;
                if (pointSegmentDistanceSqr(point.x(), point.z(),
                        before.x(), before.z(), after.x(), after.z())
                        > maxErrorSqr) continue;
                reduced.remove(i);
                removed = true;
                i = Math.max(-1, i - 2);
            }
        } while (removed && reduced.size() > maximum);
        return List.copyOf(reduced);
    }

    private static double signedArea(List<FacilityFloorPatch.Vertex> vertices) {
        if (vertices == null || vertices.size() < 3) return 0.0D;
        double twice = 0.0D;
        for (int i = 0; i < vertices.size(); i++) {
            FacilityFloorPatch.Vertex a = vertices.get(i);
            FacilityFloorPatch.Vertex b = vertices.get((i + 1) % vertices.size());
            twice += a.x() * b.z() - b.x() * a.z();
        }
        return twice * 0.5D;
    }

    private static double distanceSqr(FacilityFloorPatch.Vertex a,
            FacilityFloorPatch.Vertex b) {
        double dx = a.x() - b.x();
        double dz = a.z() - b.z();
        return dx * dx + dz * dz;
    }
    private enum LocalFace { WEST, EAST, NORTH, SOUTH }

    private record FaceEdge(Vec3 a, Vec3 b) {
    }

}
