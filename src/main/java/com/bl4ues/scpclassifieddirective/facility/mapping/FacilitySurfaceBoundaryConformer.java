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
 * Refines a newly mapped floor selection against authored construction
 * surfaces. A vertical Surface is treated as a wall boundary; its -normal side
 * is the room interior because transformed blocks grow along +normal.
 *
 * Curves are sampled into a polygonal one-sided region and intersected with the
 * builder's original floor patch. The result is persisted as ordinary
 * FacilityFloorPatch geometry, so surveillance, room lookup and future
 * procedural capture all consume the same shape.
 */
public final class FacilitySurfaceBoundaryConformer {
    private static final double FLOOR_TOLERANCE = 1.25D;
    private static final double WALL_VERTICAL_MIN = 0.45D;
    private static final double BOUNDS_MARGIN = 1.0D;
    private static final int MIN_SAMPLES = 10;
    private static final int MAX_SAMPLES = 56;
    private static final int MAX_OUTPUT_VERTICES = 192;
    private static final double EPSILON = 1.0E-5D;

    private FacilitySurfaceBoundaryConformer() {
    }

    public static FacilityFloorPatch conform(ServerLevel level,
            FacilityFloorPatch base) {
        if (level == null || base == null) return base;
        Area result = area(base.outline());
        if (result.isEmpty()) return base;

        double spanX = base.maxX() - base.minX() + 1.0D;
        double spanZ = base.maxZ() - base.minZ() + 1.0D;
        double far = Math.hypot(spanX, spanZ) * 3.0D + 12.0D;
        boolean changed = false;

        for (ConstructionSurface surface
                : TransformConstructionManager.surfaces(level)) {
            if (!isRelevant(base, surface)) continue;
            Area interior = interiorSide(surface, far);
            if (interior == null || interior.isEmpty()) continue;

            Area clipped = new Area(result);
            clipped.intersect(interior);
            if (clipped.isEmpty()) continue;

            double before = boundsArea(result);
            double after = boundsArea(clipped);
            // A Surface that merely grazes a corner or whose authored side is
            // wholly unrelated to this selection should not erase the room.
            if (after < 0.02D || after < before * 0.015D) continue;
            if (!clipped.equals(result)) changed = true;
            result = clipped;
        }

        if (!changed) return base;
        List<FacilityFloorPatch.Vertex> vertices = largestPolygon(result);
        FacilityFloorPatch refined = FacilityFloorPatch.polygon(base.y(),
                vertices);
        return refined == null ? base : refined;
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

    private static Area interiorSide(ConstructionSurface surface, double far) {
        int samples = samples(surface);
        List<Vec3> edge = new ArrayList<>(samples + 3);
        List<Vec3> inward = new ArrayList<>(samples + 3);
        for (int i = 0; i <= samples; i++) {
            double u = i / (double) samples;
            Vec3 point = surface.gridPoint(u, 0.0D);
            Vec3 normal = surface.gridNormal(u, 0.0D);
            Vec3 inside = new Vec3(-normal.x, 0.0D, -normal.z);
            if (inside.lengthSqr() < 1.0E-8D) return null;
            edge.add(point);
            inward.add(inside.normalize());
        }

        Vec3 startTangent = horizontal(edge.get(1).subtract(edge.get(0)));
        Vec3 endTangent = horizontal(edge.get(edge.size() - 1)
                .subtract(edge.get(edge.size() - 2)));
        if (startTangent.lengthSqr() < 1.0E-8D
                || endTangent.lengthSqr() < 1.0E-8D) return null;
        startTangent = startTangent.normalize();
        endTangent = endTangent.normalize();

        edge.add(0, edge.get(0).subtract(startTangent.scale(far)));
        inward.add(0, inward.get(0));
        edge.add(edge.get(edge.size() - 1).add(endTangent.scale(far)));
        inward.add(inward.get(inward.size() - 1));

        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        Vec3 first = edge.get(0);
        path.moveTo(first.x, first.z);
        for (int i = 1; i < edge.size(); i++) {
            Vec3 point = edge.get(i);
            path.lineTo(point.x, point.z);
        }
        for (int i = edge.size() - 1; i >= 0; i--) {
            Vec3 point = edge.get(i).add(inward.get(i).scale(far));
            path.lineTo(point.x, point.z);
        }
        path.closePath();
        return new Area(path);
    }

    private static int samples(ConstructionSurface surface) {
        return Math.max(MIN_SAMPLES, Math.min(MAX_SAMPLES,
                (int) Math.ceil(surface.width() * 4.0D)));
    }

    private static Vec3 horizontal(Vec3 value) {
        return new Vec3(value.x, 0.0D, value.z);
    }

    private static Area area(List<FacilityFloorPatch.Vertex> vertices) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        if (vertices == null || vertices.size() < 3) return new Area();
        path.moveTo(vertices.get(0).x(), vertices.get(0).z());
        for (int i = 1; i < vertices.size(); i++) {
            path.lineTo(vertices.get(i).x(), vertices.get(i).z());
        }
        path.closePath();
        return new Area(path);
    }

    private static double boundsArea(Area area) {
        var bounds = area.getBounds2D();
        return Math.max(0.0D, bounds.getWidth() * bounds.getHeight());
    }

    private static List<FacilityFloorPatch.Vertex> largestPolygon(Area area) {
        PathIterator iterator = area.getPathIterator(null, 0.01D);
        double[] coords = new double[6];
        List<FacilityFloorPatch.Vertex> current = new ArrayList<>();
        List<FacilityFloorPatch.Vertex> best = List.of();
        double bestArea = 0.0D;

        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO) {
                if (!current.isEmpty()) {
                    double value = Math.abs(area(current));
                    if (value > bestArea) {
                        bestArea = value;
                        best = simplify(current);
                    }
                }
                current = new ArrayList<>();
                current.add(new FacilityFloorPatch.Vertex(coords[0], coords[1]));
            } else if (type == PathIterator.SEG_LINETO) {
                current.add(new FacilityFloorPatch.Vertex(coords[0], coords[1]));
            } else if (type == PathIterator.SEG_CLOSE) {
                double value = Math.abs(area(current));
                if (value > bestArea) {
                    bestArea = value;
                    best = simplify(current);
                }
                current = new ArrayList<>();
            }
            iterator.next();
        }
        if (!current.isEmpty() && Math.abs(area(current)) > bestArea) {
            best = simplify(current);
        }
        return limit(best, MAX_OUTPUT_VERTICES);
    }

    private static List<FacilityFloorPatch.Vertex> simplify(
            List<FacilityFloorPatch.Vertex> source) {
        List<FacilityFloorPatch.Vertex> clean = new ArrayList<>();
        for (FacilityFloorPatch.Vertex vertex : source) {
            if (clean.isEmpty() || distanceSqr(clean.get(clean.size() - 1),
                    vertex) > EPSILON * EPSILON) clean.add(vertex);
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

    private static double area(List<FacilityFloorPatch.Vertex> vertices) {
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
