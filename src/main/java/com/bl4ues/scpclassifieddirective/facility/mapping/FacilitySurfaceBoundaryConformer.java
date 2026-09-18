package com.bl4ues.scpclassifieddirective.facility.mapping;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Refines a newly mapped floor selection against authored construction
 * surfaces. A vertical Surface is a wall boundary; transformed blocks grow
 * along +normal, so the mapped room remains on the -normal side.
 *
 * The clipping code intentionally stays independent from java.awt. Common
 * server-side facility geometry must also work on headless/dedicated runtimes.
 */
public final class FacilitySurfaceBoundaryConformer {
    private static final double FLOOR_TOLERANCE = 1.25D;
    private static final double WALL_VERTICAL_MIN = 0.45D;
    private static final double BOUNDS_MARGIN = 1.0D;
    private static final int MIN_SAMPLES = 10;
    private static final int MAX_SAMPLES = 56;
    private static final int MAX_OUTPUT_VERTICES = 192;
    private static final double EPSILON = 1.0E-6D;

    private FacilitySurfaceBoundaryConformer() {
    }

    public static FacilityFloorPatch conform(ServerLevel level,
            FacilityFloorPatch base) {
        if (level == null || base == null) return base;
        List<FacilityFloorPatch.Vertex> polygon =
                new ArrayList<>(base.outline());
        if (polygon.size() < 3) return base;

        boolean changed = false;
        for (ConstructionSurface surface
                : TransformConstructionManager.surfaces(level)) {
            if (!isRelevant(base, surface)) continue;
            List<FacilityFloorPatch.Vertex> next =
                    clipToSurfaceInterior(polygon, surface);
            if (next.size() < 3) continue;

            double oldArea = Math.abs(area(polygon));
            double newArea = Math.abs(area(next));
            if (newArea < 0.02D || newArea < oldArea * 0.015D) continue;
            if (!samePolygon(polygon, next)) changed = true;
            polygon = next;
        }

        if (!changed) return base;
        List<FacilityFloorPatch.Vertex> clean = limit(
                simplify(polygon), MAX_OUTPUT_VERTICES);
        FacilityFloorPatch refined = FacilityFloorPatch.polygon(base.y(), clean);
        return refined == null ? base : refined;
    }

    private static List<FacilityFloorPatch.Vertex> clipToSurfaceInterior(
            List<FacilityFloorPatch.Vertex> input,
            ConstructionSurface surface) {
        List<FacilityFloorPatch.Vertex> result = new ArrayList<>(input);
        int samples = samples(surface);
        for (int index = 0; index < samples && result.size() >= 3; index++) {
            double u0 = index / (double) samples;
            double u1 = (index + 1.0D) / samples;
            double um = (u0 + u1) * 0.5D;

            Vec3 a3 = surface.gridPoint(u0, 0.0D);
            Vec3 b3 = surface.gridPoint(u1, 0.0D);
            Vec3 normal3 = surface.gridNormal(um, 0.0D);
            double inwardX = -normal3.x;
            double inwardZ = -normal3.z;
            double inwardLength = Math.hypot(inwardX, inwardZ);
            if (inwardLength < EPSILON) continue;
            inwardX /= inwardLength;
            inwardZ /= inwardLength;

            FacilityFloorPatch.Vertex a =
                    new FacilityFloorPatch.Vertex(a3.x, a3.z);
            FacilityFloorPatch.Vertex b =
                    new FacilityFloorPatch.Vertex(b3.x, b3.z);

            // The tangent line is the wall segment. Choose the sign of the
            // half-plane with an explicit point on the authored interior side,
            // so reversing a surface or pressing F remains unambiguous.
            double interiorX = (a.x() + b.x()) * 0.5D + inwardX;
            double interiorZ = (a.z() + b.z()) * 0.5D + inwardZ;
            double interiorSign = cross(a, b, interiorX, interiorZ);
            if (Math.abs(interiorSign) < EPSILON) continue;
            result = clipHalfPlane(result, a, b,
                    interiorSign > 0.0D ? 1.0D : -1.0D);
        }
        return simplify(result);
    }

    private static List<FacilityFloorPatch.Vertex> clipHalfPlane(
            List<FacilityFloorPatch.Vertex> input,
            FacilityFloorPatch.Vertex lineA,
            FacilityFloorPatch.Vertex lineB, double sign) {
        if (input.size() < 3) return List.of();
        List<FacilityFloorPatch.Vertex> output = new ArrayList<>();
        FacilityFloorPatch.Vertex previous = input.get(input.size() - 1);
        double previousDistance = signedDistance(previous, lineA, lineB, sign);
        boolean previousInside = previousDistance >= -EPSILON;

        for (FacilityFloorPatch.Vertex current : input) {
            double currentDistance = signedDistance(current, lineA, lineB, sign);
            boolean currentInside = currentDistance >= -EPSILON;

            if (currentInside != previousInside) {
                FacilityFloorPatch.Vertex intersection = intersection(
                        previous, current, previousDistance, currentDistance);
                if (intersection != null) output.add(intersection);
            }
            if (currentInside) output.add(current);

            previous = current;
            previousDistance = currentDistance;
            previousInside = currentInside;
        }
        return output;
    }

    private static FacilityFloorPatch.Vertex intersection(
            FacilityFloorPatch.Vertex a, FacilityFloorPatch.Vertex b,
            double distanceA, double distanceB) {
        double denominator = distanceA - distanceB;
        if (Math.abs(denominator) < EPSILON) return null;
        double t = distanceA / denominator;
        t = Math.max(0.0D, Math.min(1.0D, t));
        return new FacilityFloorPatch.Vertex(
                a.x() + (b.x() - a.x()) * t,
                a.z() + (b.z() - a.z()) * t);
    }

    private static double signedDistance(FacilityFloorPatch.Vertex point,
            FacilityFloorPatch.Vertex lineA,
            FacilityFloorPatch.Vertex lineB, double sign) {
        return cross(lineA, lineB, point.x(), point.z()) * sign;
    }

    private static double cross(FacilityFloorPatch.Vertex a,
            FacilityFloorPatch.Vertex b, double x, double z) {
        return (b.x() - a.x()) * (z - a.z())
                - (b.z() - a.z()) * (x - a.x());
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
                (int) Math.ceil(surface.width() * 4.0D)));
    }

    private static List<FacilityFloorPatch.Vertex> simplify(
            List<FacilityFloorPatch.Vertex> source) {
        if (source == null || source.size() < 3) return List.of();
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
            for (int index = 0; index < clean.size(); index++) {
                FacilityFloorPatch.Vertex a = clean.get(
                        (index - 1 + clean.size()) % clean.size());
                FacilityFloorPatch.Vertex b = clean.get(index);
                FacilityFloorPatch.Vertex c = clean.get(
                        (index + 1) % clean.size());
                double value = cross(a, b, c.x(), c.z());
                if (Math.abs(value) <= 1.0E-7D) {
                    clean.remove(index);
                    removed = true;
                    break;
                }
            }
        } while (removed);
        return List.copyOf(clean);
    }

    private static boolean samePolygon(List<FacilityFloorPatch.Vertex> a,
            List<FacilityFloorPatch.Vertex> b) {
        if (a.size() != b.size()) return false;
        for (int index = 0; index < a.size(); index++) {
            if (distanceSqr(a.get(index), b.get(index))
                    > 1.0E-10D) return false;
        }
        return true;
    }

    private static List<FacilityFloorPatch.Vertex> limit(
            List<FacilityFloorPatch.Vertex> source, int maximum) {
        if (source.size() <= maximum) return source;
        List<FacilityFloorPatch.Vertex> reduced = new ArrayList<>(maximum);
        for (int index = 0; index < maximum; index++) {
            int sourceIndex = (int) Math.floor(
                    index * source.size() / (double) maximum);
            reduced.add(source.get(Math.min(source.size() - 1, sourceIndex)));
        }
        return List.copyOf(reduced);
    }

    private static double area(List<FacilityFloorPatch.Vertex> vertices) {
        if (vertices == null || vertices.size() < 3) return 0.0D;
        double twice = 0.0D;
        for (int index = 0; index < vertices.size(); index++) {
            FacilityFloorPatch.Vertex a = vertices.get(index);
            FacilityFloorPatch.Vertex b = vertices.get(
                    (index + 1) % vertices.size());
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
