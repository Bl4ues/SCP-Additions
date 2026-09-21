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

    /** A separate rendering mask; the source's selectable room Area is never
     * modified when another room covers it in the SCP-079 map. */
    public Area areaCopy() {
        return new Area(merged);
    }

    public FacilityRoomOutlineGeometry visibleOutside(Area covered) {
        if (covered == null || covered.isEmpty()
                || !covered.getBounds2D().intersects(bounds)) return this;
        Area visible = new Area(merged);
        visible.subtract(covered);
        if (visible.equals(merged)) return this;
        return new FacilityRoomOutlineGeometry(visible,
                contours(visible), visible.getBounds2D(), layers);
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

    /** The union's Area is also the filled and selectable geometry. Do not
     * shift only its contour vertices: moving them independently produces a
     * bright/dark one-pixel halo and small spikes where 45-degree edges meet.
     * The map rasterizer performs screen-space antialiasing instead. */
    private static List<FacilityFloorPatch.Vertex> refineCurve(
            List<FacilityFloorPatch.Vertex> contour) {
        return List.copyOf(contour);
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
