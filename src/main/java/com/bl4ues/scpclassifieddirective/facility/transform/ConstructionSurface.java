package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Editable parametric construction surface. Bottom/top edges are quadratic
 * curves that share a world-space bend offset. The grid is redistributed from
 * arc length and height so authored cells remain close to one block in size.
 */
public record ConstructionSurface(UUID id, ResourceLocation dimension,
        Vec3 bottomStart, Vec3 bottomEnd, Vec3 topStart, Vec3 topEnd,
        Vec3 curveOffset, Vec3 heightCurveOffset,
        Map<SurfaceSlot, SurfaceAttachment> attachments,
        Map<SurfaceOverlaySlot, SurfaceAttachment> overlays,
        boolean flipped) {
    private static final int ARC_SAMPLES = 32;
    private static final int METRIC_CACHE_LIMIT = 192;
    private static final Map<GeometryKey, GeometryMetrics> METRICS =
            new LinkedHashMap<>(64, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<GeometryKey, GeometryMetrics> eldest) {
                    return size() > METRIC_CACHE_LIMIT;
                }
            };

    // A mesh samples thousands of vertices from the same immutable surface.
    // Avoid allocating a GeometryKey and locking the global LRU for every one.
    // Identity is intentional: two records may share geometry yet differ in
    // attachments; changing the geometry always creates a new record.
    private static final ThreadLocal<MetricAccess> HOT_METRICS =
            new ThreadLocal<>();

    private record MetricAccess(ConstructionSurface surface,
                                GeometryMetrics metrics) { }

    public ConstructionSurface {
        id = id == null ? UUID.randomUUID() : id;
        dimension = dimension == null
                ? new ResourceLocation("minecraft", "overworld") : dimension;
        bottomStart = bottomStart == null ? Vec3.ZERO : bottomStart;
        bottomEnd = bottomEnd == null ? bottomStart : bottomEnd;
        topStart = topStart == null ? bottomStart.add(0.0D, 3.0D, 0.0D)
                : topStart;
        topEnd = topEnd == null ? bottomEnd.add(0.0D, 3.0D, 0.0D) : topEnd;
        curveOffset = curveOffset == null ? Vec3.ZERO : curveOffset;
        heightCurveOffset = heightCurveOffset == null
                ? Vec3.ZERO : heightCurveOffset;
        attachments = attachments == null ? Map.of()
                : Map.copyOf(attachments);
        overlays = overlays == null ? Map.of() : Map.copyOf(overlays);
    }

    /** Compatibility constructor for every pre-overlay authored surface. */
    public ConstructionSurface(UUID id, ResourceLocation dimension,
            Vec3 bottomStart, Vec3 bottomEnd, Vec3 topStart, Vec3 topEnd,
            Vec3 curveOffset, Vec3 heightCurveOffset,
            Map<SurfaceSlot, SurfaceAttachment> attachments, boolean flipped) {
        this(id, dimension, bottomStart, bottomEnd, topStart, topEnd,
                curveOffset, heightCurveOffset, attachments, Map.of(), flipped);
    }

    public ConstructionSurface(UUID id, ResourceLocation dimension,
            Vec3 bottomStart, Vec3 bottomEnd, Vec3 topStart, Vec3 topEnd,
            Vec3 curveOffset, Map<SurfaceSlot, SurfaceAttachment> attachments) {
        this(id, dimension, bottomStart, bottomEnd, topStart, topEnd,
                curveOffset, Vec3.ZERO, attachments, false);
    }

    public ConstructionSurface(UUID id, ResourceLocation dimension,
            Vec3 bottomStart, Vec3 bottomEnd, Vec3 topStart, Vec3 topEnd,
            Vec3 curveOffset, Map<SurfaceSlot, SurfaceAttachment> attachments,
            boolean flipped) {
        this(id, dimension, bottomStart, bottomEnd, topStart, topEnd,
                curveOffset, Vec3.ZERO, attachments, flipped);
    }

    public static ConstructionSurface wall(ResourceLocation dimension,
            Vec3 start, Vec3 end, double height) {
        double safeHeight = Math.max(0.125D, height);
        return new ConstructionSurface(UUID.randomUUID(), dimension, start, end,
                start.add(0.0D, safeHeight, 0.0D),
                end.add(0.0D, safeHeight, 0.0D), Vec3.ZERO, Map.of());
    }

    public Vec3 bottomControl() {
        return bottomStart.add(bottomEnd).scale(0.5D).add(curveOffset);
    }

    public Vec3 topControl() {
        return topStart.add(topEnd).scale(0.5D).add(curveOffset);
    }

    public Vec3 point(double u, double v) {
        Vec3 bottom = TransformMath.quadratic(bottomStart, bottomControl(),
                bottomEnd, u);
        Vec3 top = TransformMath.quadratic(topStart, topControl(), topEnd, u);
        double bulge = 4.0D * v * (1.0D - v);
        return bottom.scale(1.0D - v).add(top.scale(v))
                .add(heightCurveOffset.scale(bulge));
    }

    public Vec3 tangent(double u, double v) {
        Vec3 bottom = TransformMath.quadraticTangent(bottomStart,
                bottomControl(), bottomEnd, u);
        Vec3 top = TransformMath.quadraticTangent(topStart, topControl(),
                topEnd, u);
        return TransformMath.safeNormalize(bottom.scale(1.0D - v)
                .add(top.scale(v)), new Vec3(1.0D, 0.0D, 0.0D));
    }

    public Vec3 vertical(double u) {
        return vertical(u, 0.5D);
    }

    public Vec3 vertical(double u, double v) {
        Vec3 bottom = TransformMath.quadratic(bottomStart, bottomControl(),
                bottomEnd, u);
        Vec3 top = TransformMath.quadratic(topStart, topControl(), topEnd, u);
        Vec3 derivative = top.subtract(bottom)
                .add(heightCurveOffset.scale(4.0D * (1.0D - 2.0D * v)));
        return TransformMath.safeNormalize(derivative,
                new Vec3(0.0D, 1.0D, 0.0D));
    }

    public Vec3 normal(double u, double v) {
        Vec3 tangent = tangent(u, v);
        Vec3 vertical = vertical(u, v);
        Vec3 base = TransformMath.safeNormalize(tangent.cross(vertical),
                new Vec3(0.0D, 0.0D, 1.0D));
        return flipped ? base.scale(-1.0D) : base;
    }

    /**
     * Converts an evenly-spaced grid fraction into the quadratic parameter.
     * The authoring grid therefore follows arc length instead of bunching cells
     * around one end of a strongly curved wall.
     */
    public double gridParameter(double fraction) {
        return metrics().horizontal().parameter(fraction);
    }

    private double gridVerticalParameter(double uParameter, double fraction) {
        double targetFraction = Math.max(0.0D, Math.min(1.0D, fraction));
        if (targetFraction <= 0.0D || targetFraction >= 1.0D
                || heightCurveOffset.lengthSqr() < 1.0E-10D) {
            return targetFraction;
        }
        return metrics().vertical(this, uParameter).parameter(targetFraction);
    }

    public Vec3 gridPoint(double u, double v) {
        double parameterU = gridParameter(u);
        return point(parameterU, gridVerticalParameter(parameterU, v));
    }

    public Vec3 gridTangent(double u, double v) {
        double parameterU = gridParameter(u);
        return tangent(parameterU, gridVerticalParameter(parameterU, v));
    }

    /**
     * Local +X used by rigid payloads. Flipping a wall reverses both local X
     * and local Z, which is a real 180-degree rotation around local vertical
     * rather than an impossible mirror transform.
     */
    public Vec3 gridFrameTangent(double u, double v) {
        Vec3 value = gridTangent(u, v);
        return flipped ? value.scale(-1.0D) : value;
    }

    public Vec3 gridVertical(double u) {
        return gridVertical(u, 0.5D);
    }

    public Vec3 gridVertical(double u, double v) {
        double parameterU = gridParameter(u);
        return vertical(parameterU, gridVerticalParameter(parameterU, v));
    }

    public Vec3 gridNormal(double u, double v) {
        double parameterU = gridParameter(u);
        double parameterV = gridVerticalParameter(parameterU, v);
        return normal(parameterU, parameterV);
    }

    public double width() {
        return metrics().width();
    }

    public double height() {
        return metrics().height();
    }

    public int columns() {
        return Math.max(1, Math.min(512, (int) Math.round(width())));
    }

    public int rows() {
        return Math.max(1, Math.min(256, (int) Math.round(height())));
    }

    private GeometryMetrics metrics() {
        MetricAccess recent = HOT_METRICS.get();
        if (recent != null && recent.surface() == this)
            return recent.metrics();
        GeometryKey key = new GeometryKey(bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset);
        GeometryMetrics resolved;
        synchronized (METRICS) {
            resolved = METRICS.computeIfAbsent(key,
                    ignored -> GeometryMetrics.build(this));
        }
        HOT_METRICS.set(new MetricAccess(this, resolved));
        return resolved;
    }

    private record GeometryKey(Vec3 bottomStart, Vec3 bottomEnd,
            Vec3 topStart, Vec3 topEnd, Vec3 curveOffset,
            Vec3 heightCurveOffset) {
    }

    private static final class GeometryMetrics {
        private static final int VERTICAL_SAMPLES = 24;
        private static final int HEIGHT_U_SAMPLES = 8;
        private final ArcTable horizontal;
        private final double width;
        private final double height;
        private final Map<Long, ArcTable> verticalTables = new LinkedHashMap<>();

        private GeometryMetrics(ArcTable horizontal, double width,
                double height) {
            this.horizontal = horizontal;
            this.width = width;
            this.height = height;
        }

        private static GeometryMetrics build(ConstructionSurface surface) {
            ArcTable horizontal = ArcTable.sample(ARC_SAMPLES,
                    t -> surface.point(t, 0.5D));
            double heightTotal = 0.0D;
            for (int ui = 0; ui <= HEIGHT_U_SAMPLES; ui++) {
                double u = horizontal.parameter(
                        ui / (double) HEIGHT_U_SAMPLES);
                ArcTable vertical = ArcTable.sample(VERTICAL_SAMPLES,
                        v -> surface.point(u, v));
                heightTotal += vertical.total();
            }
            return new GeometryMetrics(horizontal, horizontal.total(),
                    heightTotal / (HEIGHT_U_SAMPLES + 1.0D));
        }

        private ArcTable horizontal() {
            return horizontal;
        }

        private double width() {
            return width;
        }

        private double height() {
            return height;
        }

        private synchronized ArcTable vertical(ConstructionSurface surface,
                double u) {
            // The old million-step key built nearly one 24-sample arc table
            // per distinct tessellated vertex. 1024 horizontal samples bound
            // that work while keeping the height reparameterization sub-pixel.
            long key = Math.round(Math.max(0.0D,
                    Math.min(1.0D, u)) * 1024.0D);
            return verticalTables.computeIfAbsent(key,
                    ignored -> ArcTable.sample(VERTICAL_SAMPLES,
                            v -> surface.point(key / 1024.0D, v)));
        }
    }

    private static final class ArcTable {
        private final double[] cumulative;
        private final double total;

        private ArcTable(double[] cumulative, double total) {
            this.cumulative = cumulative;
            this.total = total;
        }

        private static ArcTable sample(int samples,
                java.util.function.DoubleFunction<Vec3> point) {
            double[] cumulative = new double[samples + 1];
            Vec3 previous = point.apply(0.0D);
            double total = 0.0D;
            for (int index = 1; index <= samples; index++) {
                Vec3 current = point.apply(index / (double) samples);
                total += current.distanceTo(previous);
                cumulative[index] = total;
                previous = current;
            }
            return new ArcTable(cumulative, total);
        }

        private double total() {
            return total;
        }

        private double parameter(double fraction) {
            double targetFraction = Math.max(0.0D, Math.min(1.0D, fraction));
            if (targetFraction <= 0.0D || targetFraction >= 1.0D
                    || total < 1.0E-8D) {
                return targetFraction;
            }
            double target = total * targetFraction;
            int low = 1;
            int high = cumulative.length - 1;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (cumulative[mid] < target) low = mid + 1;
                else high = mid;
            }
            int index = low;
            double segment = cumulative[index] - cumulative[index - 1];
            double local = segment < 1.0E-8D ? 0.0D
                    : (target - cumulative[index - 1]) / segment;
            return ((index - 1) + local) / (cumulative.length - 1.0D);
        }
    }

    public ConstructionSurface withGeometry(Vec3 nextBottomStart,
            Vec3 nextBottomEnd, Vec3 nextTopStart, Vec3 nextTopEnd,
            Vec3 nextCurveOffset) {
        return withGeometry(nextBottomStart, nextBottomEnd, nextTopStart,
                nextTopEnd, nextCurveOffset, heightCurveOffset);
    }

    public ConstructionSurface withGeometry(Vec3 nextBottomStart,
            Vec3 nextBottomEnd, Vec3 nextTopStart, Vec3 nextTopEnd,
            Vec3 nextCurveOffset, Vec3 nextHeightCurveOffset) {
        ConstructionSurface geometry = new ConstructionSurface(id, dimension,
                nextBottomStart, nextBottomEnd, nextTopStart, nextTopEnd,
                nextCurveOffset, nextHeightCurveOffset, Map.of(), Map.of(),
                flipped);
        if (attachments.isEmpty() && overlays.isEmpty()) return geometry;

        int oldColumns = columns();
        int oldRows = rows();
        int newColumns = geometry.columns();
        int newRows = geometry.rows();
        Map<SurfaceSlot, SurfaceAttachment> remapped = new LinkedHashMap<>();
        for (Map.Entry<SurfaceSlot, SurfaceAttachment> entry
                : attachments.entrySet()) {
            SurfaceSlot mapped = remapSlot(entry.getKey(), oldColumns, oldRows,
                    newColumns, newRows);
            remapped.putIfAbsent(mapped, entry.getValue());
        }
        Map<SurfaceOverlaySlot, SurfaceAttachment> remappedOverlays =
                new LinkedHashMap<>();
        for (Map.Entry<SurfaceOverlaySlot, SurfaceAttachment> entry
                : overlays.entrySet()) {
            SurfaceSlot mapped = remapSlot(entry.getKey().slot(), oldColumns,
                    oldRows, newColumns, newRows);
            remappedOverlays.putIfAbsent(new SurfaceOverlaySlot(mapped,
                    entry.getKey().normalSign()), entry.getValue());
        }
        return new ConstructionSurface(id, dimension, nextBottomStart,
                nextBottomEnd, nextTopStart, nextTopEnd, nextCurveOffset,
                nextHeightCurveOffset, remapped, remappedOverlays, flipped);
    }

    private static SurfaceSlot remapSlot(SurfaceSlot slot, int oldColumns,
            int oldRows, int newColumns, int newRows) {
        double u = (slot.column() + 0.5D) / oldColumns;
        double v = (slot.row() + 0.5D) / oldRows;
        int column = Math.max(0, Math.min(newColumns - 1,
                (int) Math.floor(u * newColumns)));
        int row = Math.max(0, Math.min(newRows - 1,
                (int) Math.floor(v * newRows)));
        return new SurfaceSlot(column, row);
    }

    public ConstructionSurface withAttachment(SurfaceSlot slot,
            BlockState state, boolean deform) {
        Map<SurfaceSlot, SurfaceAttachment> next =
                new LinkedHashMap<>(attachments);
        next.put(slot, new SurfaceAttachment(state, deform));
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset, next,
                overlays, flipped);
    }

    public SurfaceAttachment overlay(SurfaceSlot slot, int normalSign) {
        if (slot == null) return null;
        return overlays.get(new SurfaceOverlaySlot(slot, normalSign));
    }

    public ConstructionSurface withOverlay(SurfaceSlot slot, int normalSign,
            BlockState state, boolean deform) {
        if (slot == null) return this;
        Map<SurfaceOverlaySlot, SurfaceAttachment> next =
                new LinkedHashMap<>(overlays);
        next.put(new SurfaceOverlaySlot(slot, normalSign),
                new SurfaceAttachment(state, deform));
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset, attachments,
                next, flipped);
    }

    public ConstructionSurface withoutOverlay(SurfaceSlot slot,
            int normalSign) {
        SurfaceOverlaySlot key = new SurfaceOverlaySlot(slot, normalSign);
        if (slot == null || !overlays.containsKey(key)) return this;
        Map<SurfaceOverlaySlot, SurfaceAttachment> next =
                new LinkedHashMap<>(overlays);
        next.remove(key);
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset, attachments,
                next, flipped);
    }

    public ConstructionSurface withFlipped(boolean nextFlipped) {
        if (nextFlipped == flipped) return this;
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset, attachments,
                overlays, nextFlipped);
    }

    public ConstructionSurface withoutAttachment(SurfaceSlot slot) {
        if (slot == null || !attachments.containsKey(slot)) return this;
        Map<SurfaceSlot, SurfaceAttachment> next =
                new LinkedHashMap<>(attachments);
        next.remove(slot);
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset, next,
                overlays, flipped);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Dimension", dimension.toString());
        putVec(tag, "BottomStart", bottomStart);
        putVec(tag, "BottomEnd", bottomEnd);
        putVec(tag, "TopStart", topStart);
        putVec(tag, "TopEnd", topEnd);
        putVec(tag, "CurveOffset", curveOffset);
        putVec(tag, "HeightCurveOffset", heightCurveOffset);
        tag.putBoolean("Flipped", flipped);
        ListTag list = new ListTag();
        for (Map.Entry<SurfaceSlot, SurfaceAttachment> entry
                : attachments.entrySet()) {
            CompoundTag attachment = new CompoundTag();
            attachment.putInt("Column", entry.getKey().column());
            attachment.putInt("Row", entry.getKey().row());
            attachment.putBoolean("Deform", entry.getValue().deform());
            attachment.put("State", BlockStateCodec.save(
                    entry.getValue().state()));
            list.add(attachment);
        }
        tag.put("Attachments", list);
        ListTag overlayList = new ListTag();
        for (Map.Entry<SurfaceOverlaySlot, SurfaceAttachment> entry
                : overlays.entrySet()) {
            CompoundTag attachment = new CompoundTag();
            attachment.putInt("Column", entry.getKey().slot().column());
            attachment.putInt("Row", entry.getKey().slot().row());
            attachment.putInt("NormalSign", entry.getKey().normalSign());
            attachment.putBoolean("Deform", entry.getValue().deform());
            attachment.put("State", BlockStateCodec.save(
                    entry.getValue().state()));
            overlayList.add(attachment);
        }
        tag.put("Overlays", overlayList);
        return tag;
    }

    public static ConstructionSurface load(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("Id")) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(
                tag.getString("Dimension"));
        if (dimension == null) return null;
        Map<SurfaceSlot, SurfaceAttachment> attachments =
                new LinkedHashMap<>();
        ListTag list = tag.getList("Attachments", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag attachment = list.getCompound(index);
            SurfaceSlot slot = new SurfaceSlot(attachment.getInt("Column"),
                    attachment.getInt("Row"));
            attachments.put(slot, new SurfaceAttachment(
                    BlockStateCodec.load(attachment.getCompound("State")),
                    attachment.getBoolean("Deform")));
        }
        Map<SurfaceOverlaySlot, SurfaceAttachment> overlays =
                new LinkedHashMap<>();
        ListTag overlayList = tag.getList("Overlays", Tag.TAG_COMPOUND);
        for (int index = 0; index < overlayList.size(); index++) {
            CompoundTag attachment = overlayList.getCompound(index);
            SurfaceSlot slot = new SurfaceSlot(attachment.getInt("Column"),
                    attachment.getInt("Row"));
            overlays.put(new SurfaceOverlaySlot(slot,
                            attachment.getInt("NormalSign")),
                    new SurfaceAttachment(
                            BlockStateCodec.load(
                                    attachment.getCompound("State")),
                            attachment.getBoolean("Deform")));
        }
        return new ConstructionSurface(tag.getUUID("Id"), dimension,
                getVec(tag, "BottomStart"), getVec(tag, "BottomEnd"),
                getVec(tag, "TopStart"), getVec(tag, "TopEnd"),
                getVec(tag, "CurveOffset"),
                tag.contains("HeightCurveOffset", Tag.TAG_COMPOUND)
                        ? getVec(tag, "HeightCurveOffset") : Vec3.ZERO,
                attachments, overlays, tag.getBoolean("Flipped"));
    }

    private static void putVec(CompoundTag tag, String key, Vec3 value) {
        CompoundTag vector = new CompoundTag();
        vector.putDouble("X", value.x);
        vector.putDouble("Y", value.y);
        vector.putDouble("Z", value.z);
        tag.put(key, vector);
    }

    private static Vec3 getVec(CompoundTag tag, String key) {
        CompoundTag vector = tag.getCompound(key);
        return new Vec3(vector.getDouble("X"), vector.getDouble("Y"),
                vector.getDouble("Z"));
    }

    public record SurfaceSlot(int column, int row) {
    }

    public record SurfaceOverlaySlot(SurfaceSlot slot, int normalSign) {
        public SurfaceOverlaySlot {
            slot = slot == null ? new SurfaceSlot(0, 0) : slot;
            normalSign = normalSign < 0 ? -1 : 1;
        }

        public SurfaceOverlaySlot(int column, int row, int normalSign) {
            this(new SurfaceSlot(column, row), normalSign);
        }
    }

    public record SurfaceAttachment(BlockState state, boolean deform) {
        public SurfaceAttachment {
            state = state == null ? Blocks.AIR.defaultBlockState() : state;
        }
    }
}
