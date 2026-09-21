package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared client-side geometric union of every authored floor patch belonging
 * to one mapped room.
 *
 * The SCP-079 map consumes the flattened union, while the in-world Mapping
 * overlay consumes per-Y layers so separate physical elevations are not
 * visually collapsed onto one plane.
 */
public final class FacilityRoomOutlineGeometry {
    private static final double FLATNESS = 0.008D;
    // A shallow, cached contour refinement in world units; sharp 90-degree
    // map corners and authored room hit regions remain unchanged.
    private static final double MAX_CURVE_SMOOTHING = 0.045D;

    private final Area merged;
    private final List<List<FacilityFloorPatch.Vertex>> contours;
    private final Rectangle2D bounds;
    private final List<Layer> layers;

    private FacilityRoomOutlineGeometry(Area merged,
            List<List<FacilityFloorPatch.Vertex>> contours,
            Rectangle2D bounds, List<Layer> layers) {
        this.merged = merged;
        this.contours = contours;
        this.bounds = bounds;
        this.layers = layers;
    }

    public static FacilityRoomOutlineGeometry of(FacilityRoomSnapshot room) {
        Area merged = new Area();
        Map<Integer, Area> byY = new LinkedHashMap<>();
        if (room != null) {
            for (FacilityFloorPatch patch : room.patches()) {
                Area patchArea = patchArea(patch);
                if (patchArea.isEmpty()) continue;
                merged.add(new Area(patchArea));
                byY.computeIfAbsent(patch.y(), ignored -> new Area())
                        .add(patchArea);
            }
        }

        List<Layer> layers = new ArrayList<>(byY.size());
        for (Map.Entry<Integer, Area> entry : byY.entrySet()) {
            Area layerArea = entry.getValue();
            if (layerArea.isEmpty()) continue;
            layers.add(new Layer(entry.getKey(),
                    contours(layerArea), layerArea.getBounds2D()));
        }
        layers.sort(java.util.Comparator.comparingInt(Layer::y));

        return new FacilityRoomOutlineGeometry(merged,
                contours(merged), merged.getBounds2D(),
                List.copyOf(layers));
    }

    public boolean empty() {
        return merged.isEmpty();
    }

    public boolean contains(double x, double z) {
        return merged.contains(x, z);
    }

    public boolean intersects(double x, double z,
            double width, double depth) {
        return merged.intersects(x, z, width, depth);
    }

    public List<List<FacilityFloorPatch.Vertex>> contours() {
        return contours;
    }

    public Rectangle2D bounds() {
        return bounds;
    }

    public List<Layer> layers() {
        return layers;
    }

    private static Area patchArea(FacilityFloorPatch patch) {
        if (patch == null) return new Area();
        List<FacilityFloorPatch.Vertex> vertices = patch.outline();
        if (vertices.size() < 3) return new Area();

        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        path.moveTo(vertices.get(0).x(), vertices.get(0).z());
        for (int index = 1; index < vertices.size(); index++) {
            path.lineTo(vertices.get(index).x(), vertices.get(index).z());
        }
        path.closePath();
        return new Area(path);
    }

    private static List<List<FacilityFloorPatch.Vertex>> contours(Area area) {
        if (area == null || area.isEmpty()) return List.of();
        List<List<FacilityFloorPatch.Vertex>> result = new ArrayList<>();
        PathIterator iterator = area.getPathIterator(null, FLATNESS);
        double[] coords = new double[6];
        List<FacilityFloorPatch.Vertex> current = null;
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO) {
                if (current != null && current.size() >= 3) {
                    result.add(refineCurve(current));
                }
                current = new ArrayList<>();
                current.add(new FacilityFloorPatch.Vertex(
                        coords[0], coords[1]));
            } else if (type == PathIterator.SEG_LINETO && current != null) {
                FacilityFloorPatch.Vertex next =
                        new FacilityFloorPatch.Vertex(coords[0], coords[1]);
                if (current.isEmpty() || !same(current.get(current.size() - 1),
                        next)) {
                    current.add(next);
                }
            } else if (type == PathIterator.SEG_CLOSE && current != null) {
                trimClosingDuplicate(current);
                if (current.size() >= 3) result.add(refineCurve(current));
                current = null;
            }
            iterator.next();
        }
        if (current != null) {
            trimClosingDuplicate(current);
            if (current.size() >= 3) result.add(refineCurve(current));
        }
        return List.copyOf(result);
    }

    /**
     * Refine the polygon once when room geometry is cached, not every frame.
     * Small neighbouring segments with a shallow turn are noisy curve samples;
     * blending them by at most 0.045 block removes visible pixel spikes. A
     * right-angle corner, an isolated vertex, or a long straight edge is kept
     * exact, as are the underlying Area and its hit-testing semantics.
     */
    private static List<FacilityFloorPatch.Vertex> refineCurve(
            List<FacilityFloorPatch.Vertex> contour) {
        int size = contour.size();
        if (size < 5) return List.copyOf(contour);
        List<FacilityFloorPatch.Vertex> refined = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            FacilityFloorPatch.Vertex before = contour.get((i + size - 1) % size);
            FacilityFloorPatch.Vertex point = contour.get(i);
            FacilityFloorPatch.Vertex after = contour.get((i + 1) % size);
            double ax = point.x() - before.x();
            double az = point.z() - before.z();
            double bx = after.x() - point.x();
            double bz = after.z() - point.z();
            double lenA = Math.hypot(ax, az);
            double lenB = Math.hypot(bx, bz);
            if (lenA < 0.015D || lenB < 0.015D
                    || lenA > 0.8D || lenB > 0.8D
                    || ax * bx + az * bz < 0.94D * lenA * lenB) {
                refined.add(point);
                continue;
            }
            double midX = (before.x() + after.x()) * 0.5D;
            double midZ = (before.z() + after.z()) * 0.5D;
            double deltaX = (midX - point.x()) * 0.5D;
            double deltaZ = (midZ - point.z()) * 0.5D;
            double distance = Math.hypot(deltaX, deltaZ);
            if (distance > MAX_CURVE_SMOOTHING) {
                double factor = MAX_CURVE_SMOOTHING / distance;
                deltaX *= factor;
                deltaZ *= factor;
            }
            refined.add(new FacilityFloorPatch.Vertex(
                    point.x() + deltaX, point.z() + deltaZ));
        }
        return List.copyOf(refined);
    }

    private static void trimClosingDuplicate(
            List<FacilityFloorPatch.Vertex> vertices) {
        if (vertices.size() > 1
                && same(vertices.get(0), vertices.get(vertices.size() - 1))) {
            vertices.remove(vertices.size() - 1);
        }
    }

    private static boolean same(FacilityFloorPatch.Vertex a,
            FacilityFloorPatch.Vertex b) {
        return Math.abs(a.x() - b.x()) < 1.0E-7D
                && Math.abs(a.z() - b.z()) < 1.0E-7D;
    }

    public record Layer(int y,
            List<List<FacilityFloorPatch.Vertex>> contours,
            Rectangle2D bounds) {
        public Layer {
            contours = contours == null ? List.of() : List.copyOf(contours);
        }

        public boolean empty() {
            return contours.isEmpty();
        }
    }
}
