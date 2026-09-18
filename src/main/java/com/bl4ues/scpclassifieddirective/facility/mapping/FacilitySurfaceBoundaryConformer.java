package com.bl4ues.scpclassifieddirective.facility.mapping;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

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
    private static final int MIN_SAMPLES = 12;
    private static final int MAX_SAMPLES = 96;
    private static final int MAX_OUTPUT_VERTICES = 224;
    private static final double EPSILON = 1.0E-5D;

    private FacilitySurfaceBoundaryConformer() {
    }

    public static FacilityFloorPatch conform(ServerLevel level,
            FacilityFloorPatch base) {
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
            Area interior = interiorRegion(surface, far);
            if (interior.isEmpty()) continue;

            Area clipped = new Area(result);
            clipped.intersect(interior);
            if (clipped.isEmpty()) continue;

            double before = areaMagnitude(result);
            double after = areaMagnitude(clipped);
            if (after < 0.02D || after < before * 0.015D) continue;
            if (!sameArea(result, clipped)) changed = true;
            result = clipped;
        }

        if (!changed) return base;
        List<FacilityFloorPatch.Vertex> vertices = largestPolygon(result);
        FacilityFloorPatch refined = FacilityFloorPatch.polygon(base.y(),
                vertices);
        return refined == null ? base : refined;
    }

    private static Area interiorRegion(ConstructionSurface surface,
            double distance) {
        int samples = samples(surface);
        Area result = new Area();
        Vec3 first = null;
        Vec3 firstNext = null;
        Vec3 firstInward = null;
        Vec3 lastPrevious = null;
        Vec3 last = null;
        Vec3 lastInward = null;
        for (int index = 0; index < samples; index++) {
            double u0 = index / (double) samples;
            double u1 = (index + 1.0D) / samples;
            double um = (u0 + u1) * 0.5D;
            Vec3 a = surface.gridPoint(u0, 0.0D);
            Vec3 b = surface.gridPoint(u1, 0.0D);
            Vec3 normal = surface.gridNormal(um, 0.0D);
            Vec3 inward = new Vec3(-normal.x, 0.0D, -normal.z);
            if (inward.lengthSqr() < 1.0E-10D) continue;
            inward = inward.normalize();
            if (first == null) {
                first = a;
                firstNext = b;
                firstInward = inward;
            }
            lastPrevious = a;
            last = b;
            lastInward = inward;

            addStrip(result, a, b, inward, distance);
        }

        // A wall is a boundary, not a finite carpet strip. Extend its first and
        // last tangents so a selection that begins slightly before/after the
        // authored Surface is clipped by the curve rather than by an artificial
        // perpendicular cap at the endpoint.
        if (first != null && firstNext != null && firstInward != null) {
            Vec3 tangent = horizontal(firstNext.subtract(first));
            if (tangent.lengthSqr() > 1.0E-10D) {
                Vec3 extended = first.subtract(tangent.normalize()
                        .scale(distance));
                addStrip(result, extended, first, firstInward, distance);
            }
        }
        if (lastPrevious != null && last != null && lastInward != null) {
            Vec3 tangent = horizontal(last.subtract(lastPrevious));
            if (tangent.lengthSqr() > 1.0E-10D) {
                Vec3 extended = last.add(tangent.normalize().scale(distance));
                addStrip(result, last, extended, lastInward, distance);
            }
        }
        return result;
    }

    private static void addStrip(Area result, Vec3 a, Vec3 b,
            Vec3 inwardUnit, double distance) {
        Vec3 inward = inwardUnit.scale(distance);
        Path2D.Double quad = new Path2D.Double(Path2D.WIND_NON_ZERO);
        quad.moveTo(a.x, a.z);
        quad.lineTo(b.x, b.z);
        quad.lineTo(b.x + inward.x, b.z + inward.z);
        quad.lineTo(a.x + inward.x, a.z + inward.z);
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
                (int) Math.ceil(surface.width() * 6.0D)));
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

    private static List<FacilityFloorPatch.Vertex> largestPolygon(Area area) {
        List<FacilityFloorPatch.Vertex> best = List.of();
        double bestArea = 0.0D;
        for (List<FacilityFloorPatch.Vertex> polygon : polygons(area)) {
            List<FacilityFloorPatch.Vertex> clean = simplify(polygon);
            double value = Math.abs(signedArea(clean));
            if (value > bestArea) {
                bestArea = value;
                best = clean;
            }
        }
        return limit(best, MAX_OUTPUT_VERTICES);
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
        List<FacilityFloorPatch.Vertex> reduced = new ArrayList<>(maximum);
        for (int i = 0; i < maximum; i++) {
            int index = (int) Math.floor(i * source.size() / (double) maximum);
            reduced.add(source.get(Math.min(source.size() - 1, index)));
        }
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
}
