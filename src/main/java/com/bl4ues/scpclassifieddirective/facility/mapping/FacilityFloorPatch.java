package com.bl4ues.scpclassifieddirective.facility.mapping;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One flat contribution to a mapped room floor. Legacy patches remain axis
 * aligned rectangles; authored patches may carry an arbitrary polygon outline
 * in X/Z world coordinates so 45-degree, off-grid and curved/rasterized rooms
 * are represented without expanding the visible map to whole block squares.
 */
public final class FacilityFloorPatch {
    private final int minX;
    private final int y;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final List<Vertex> vertices;

    public FacilityFloorPatch(int minX, int y, int minZ, int maxX, int maxZ) {
        this(minX, y, minZ, maxX, maxZ, List.of());
    }

    private FacilityFloorPatch(int minX, int y, int minZ, int maxX, int maxZ,
            List<Vertex> vertices) {
        this.minX = Math.min(minX, maxX);
        this.y = y;
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
        this.vertices = vertices == null || vertices.size() < 3
                ? List.of() : List.copyOf(vertices);
    }

    public static FacilityFloorPatch between(BlockPos first, BlockPos second) {
        return new FacilityFloorPatch(Math.min(first.getX(), second.getX()),
                first.getY(), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()),
                Math.max(first.getZ(), second.getZ()));
    }

    public static FacilityFloorPatch polygon(int y, List<Vertex> vertices) {
        if (vertices == null || vertices.size() < 3) return null;
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        List<Vertex> clean = new ArrayList<>();
        for (Vertex vertex : vertices) {
            if (vertex == null || !Double.isFinite(vertex.x())
                    || !Double.isFinite(vertex.z())) continue;
            clean.add(vertex);
            minX = Math.min(minX, vertex.x());
            minZ = Math.min(minZ, vertex.z());
            maxX = Math.max(maxX, vertex.x());
            maxZ = Math.max(maxZ, vertex.z());
        }
        if (clean.size() < 3) return null;
        return new FacilityFloorPatch((int) Math.floor(minX), y,
                (int) Math.floor(minZ),
                (int) Math.ceil(maxX) - 1, (int) Math.ceil(maxZ) - 1, clean);
    }

    public int minX() { return minX; }
    public int y() { return y; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxZ() { return maxZ; }

    /** Empty means the legacy rectangular outline should be used. */
    public List<Vertex> vertices() {
        return vertices;
    }

    public boolean isPolygon() {
        return vertices.size() >= 3;
    }

    public List<Vertex> outline() {
        if (isPolygon()) return vertices;
        return List.of(
                new Vertex(minX, minZ),
                new Vertex(maxX + 1.0D, minZ),
                new Vertex(maxX + 1.0D, maxZ + 1.0D),
                new Vertex(minX, maxZ + 1.0D));
    }

    public long area() {
        if (!isPolygon()) {
            return ((long) maxX - minX + 1L) * ((long) maxZ - minZ + 1L);
        }
        double twiceArea = 0.0D;
        for (int index = 0; index < vertices.size(); index++) {
            Vertex a = vertices.get(index);
            Vertex b = vertices.get((index + 1) % vertices.size());
            twiceArea += a.x() * b.z() - b.x() * a.z();
        }
        return Math.max(1L, Math.round(Math.abs(twiceArea) * 0.5D));
    }

    public boolean containsFloor(BlockPos pos) {
        return pos != null && pos.getY() == y
                && containsXZ(pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    public boolean containsColumn(BlockPos pos, int height) {
        return pos != null && pos.getY() >= y && pos.getY() <= y + height
                && containsXZ(pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    public boolean containsXZ(double x, double z) {
        if (x < minX || x > maxX + 1.0D || z < minZ || z > maxZ + 1.0D) {
            return false;
        }
        if (!isPolygon()) return true;
        boolean inside = false;
        for (int i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            Vertex vi = vertices.get(i);
            Vertex vj = vertices.get(j);
            boolean crosses = (vi.z() > z) != (vj.z() > z)
                    && x < (vj.x() - vi.x()) * (z - vi.z())
                    / ((vj.z() - vi.z()) == 0.0D ? 1.0E-12D : (vj.z() - vi.z()))
                    + vi.x();
            if (crosses) inside = !inside;
        }
        return inside;
    }

    public boolean overlaps(FacilityFloorPatch other) {
        if (other == null || other.y != y
                || minX > other.maxX || maxX < other.minX
                || minZ > other.maxZ || maxZ < other.minZ) return false;
        if (!isPolygon() && !other.isPolygon()) return true;
        for (Vertex vertex : outline()) {
            if (other.containsXZ(vertex.x(), vertex.z())) return true;
        }
        for (Vertex vertex : other.outline()) {
            if (containsXZ(vertex.x(), vertex.z())) return true;
        }
        return edgesIntersect(outline(), other.outline());
    }

    private static boolean edgesIntersect(List<Vertex> a, List<Vertex> b) {
        for (int ai = 0; ai < a.size(); ai++) {
            Vertex a0 = a.get(ai);
            Vertex a1 = a.get((ai + 1) % a.size());
            for (int bi = 0; bi < b.size(); bi++) {
                Vertex b0 = b.get(bi);
                Vertex b1 = b.get((bi + 1) % b.size());
                if (segmentsIntersect(a0, a1, b0, b1)) return true;
            }
        }
        return false;
    }

    private static boolean segmentsIntersect(Vertex a, Vertex b, Vertex c,
            Vertex d) {
        double abC = cross(a, b, c);
        double abD = cross(a, b, d);
        double cdA = cross(c, d, a);
        double cdB = cross(c, d, b);
        return ((abC > 0.0D && abD < 0.0D) || (abC < 0.0D && abD > 0.0D))
                && ((cdA > 0.0D && cdB < 0.0D)
                || (cdA < 0.0D && cdB > 0.0D));
    }

    private static double cross(Vertex a, Vertex b, Vertex c) {
        return (b.x() - a.x()) * (c.z() - a.z())
                - (b.z() - a.z()) * (c.x() - a.x());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MinX", minX);
        tag.putInt("Y", y);
        tag.putInt("MinZ", minZ);
        tag.putInt("MaxX", maxX);
        tag.putInt("MaxZ", maxZ);
        if (isPolygon()) {
            ListTag list = new ListTag();
            for (Vertex vertex : vertices) {
                CompoundTag point = new CompoundTag();
                point.putDouble("X", vertex.x());
                point.putDouble("Z", vertex.z());
                list.add(point);
            }
            tag.put("Vertices", list);
        }
        return tag;
    }

    public static FacilityFloorPatch load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;
        int y = tag.getInt("Y");
        ListTag list = tag.getList("Vertices", Tag.TAG_COMPOUND);
        if (list.size() >= 3) {
            List<Vertex> vertices = new ArrayList<>(list.size());
            for (int index = 0; index < list.size(); index++) {
                CompoundTag point = list.getCompound(index);
                vertices.add(new Vertex(point.getDouble("X"), point.getDouble("Z")));
            }
            FacilityFloorPatch polygon = polygon(y, vertices);
            if (polygon != null) return polygon;
        }
        return new FacilityFloorPatch(tag.getInt("MinX"), y,
                tag.getInt("MinZ"), tag.getInt("MaxX"), tag.getInt("MaxZ"));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof FacilityFloorPatch other)) return false;
        return minX == other.minX && y == other.y && minZ == other.minZ
                && maxX == other.maxX && maxZ == other.maxZ
                && vertices.equals(other.vertices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, y, minZ, maxX, maxZ, vertices);
    }

    public record Vertex(double x, double z) {
    }
}
