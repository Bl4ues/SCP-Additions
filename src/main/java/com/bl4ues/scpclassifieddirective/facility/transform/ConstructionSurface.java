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
        Vec3 curveOffset, Map<SurfaceSlot, SurfaceAttachment> attachments,
        boolean flipped) {
    private static final int ARC_SAMPLES = 32;

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
        attachments = attachments == null ? Map.of()
                : Map.copyOf(attachments);
    }

    public ConstructionSurface(UUID id, ResourceLocation dimension,
            Vec3 bottomStart, Vec3 bottomEnd, Vec3 topStart, Vec3 topEnd,
            Vec3 curveOffset, Map<SurfaceSlot, SurfaceAttachment> attachments) {
        this(id, dimension, bottomStart, bottomEnd, topStart, topEnd,
                curveOffset, attachments, false);
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
        return bottom.scale(1.0D - v).add(top.scale(v));
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
        Vec3 bottom = TransformMath.quadratic(bottomStart, bottomControl(),
                bottomEnd, u);
        Vec3 top = TransformMath.quadratic(topStart, topControl(), topEnd, u);
        return TransformMath.safeNormalize(top.subtract(bottom),
                new Vec3(0.0D, 1.0D, 0.0D));
    }

    public Vec3 normal(double u, double v) {
        Vec3 tangent = tangent(u, v);
        Vec3 vertical = vertical(u);
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
        double targetFraction = Math.max(0.0D, Math.min(1.0D, fraction));
        if (targetFraction <= 0.0D || targetFraction >= 1.0D) {
            return targetFraction;
        }
        double[] lengths = new double[ARC_SAMPLES + 1];
        Vec3 previous = point(0.0D, 0.5D);
        double total = 0.0D;
        for (int index = 1; index <= ARC_SAMPLES; index++) {
            Vec3 current = point(index / (double) ARC_SAMPLES, 0.5D);
            total += current.distanceTo(previous);
            lengths[index] = total;
            previous = current;
        }
        if (total < 1.0E-8D) return targetFraction;
        double target = total * targetFraction;
        for (int index = 1; index <= ARC_SAMPLES; index++) {
            if (lengths[index] < target) continue;
            double segment = lengths[index] - lengths[index - 1];
            double local = segment < 1.0E-8D ? 0.0D
                    : (target - lengths[index - 1]) / segment;
            return ((index - 1) + local) / ARC_SAMPLES;
        }
        return 1.0D;
    }

    public Vec3 gridPoint(double u, double v) {
        return point(gridParameter(u), v);
    }

    public Vec3 gridTangent(double u, double v) {
        return tangent(gridParameter(u), v);
    }

    public Vec3 gridVertical(double u) {
        return vertical(gridParameter(u));
    }

    public Vec3 gridNormal(double u, double v) {
        return normal(gridParameter(u), v);
    }

    public double width() {
        double length = 0.0D;
        Vec3 previous = point(0.0D, 0.5D);
        for (int index = 1; index <= ARC_SAMPLES; index++) {
            Vec3 current = point(index / (double) ARC_SAMPLES, 0.5D);
            length += current.distanceTo(previous);
            previous = current;
        }
        return length;
    }

    public double height() {
        double total = 0.0D;
        for (int index = 0; index <= 8; index++) {
            double u = index / 8.0D;
            total += TransformMath.quadratic(topStart, topControl(), topEnd, u)
                    .distanceTo(TransformMath.quadratic(bottomStart,
                            bottomControl(), bottomEnd, u));
        }
        return total / 9.0D;
    }

    public int columns() {
        return Math.max(1, Math.min(512, (int) Math.round(width())));
    }

    public int rows() {
        return Math.max(1, Math.min(256, (int) Math.round(height())));
    }

    public ConstructionSurface withGeometry(Vec3 nextBottomStart,
            Vec3 nextBottomEnd, Vec3 nextTopStart, Vec3 nextTopEnd,
            Vec3 nextCurveOffset) {
        ConstructionSurface geometry = new ConstructionSurface(id, dimension,
                nextBottomStart, nextBottomEnd, nextTopStart, nextTopEnd,
                nextCurveOffset, Map.of(), flipped);
        if (attachments.isEmpty()) return geometry;

        int oldColumns = columns();
        int oldRows = rows();
        int newColumns = geometry.columns();
        int newRows = geometry.rows();
        Map<SurfaceSlot, SurfaceAttachment> remapped = new LinkedHashMap<>();
        for (Map.Entry<SurfaceSlot, SurfaceAttachment> entry
                : attachments.entrySet()) {
            double u = (entry.getKey().column() + 0.5D) / oldColumns;
            double v = (entry.getKey().row() + 0.5D) / oldRows;
            int column = Math.max(0, Math.min(newColumns - 1,
                    (int) Math.floor(u * newColumns)));
            int row = Math.max(0, Math.min(newRows - 1,
                    (int) Math.floor(v * newRows)));
            remapped.putIfAbsent(new SurfaceSlot(column, row), entry.getValue());
        }
        return new ConstructionSurface(id, dimension, nextBottomStart,
                nextBottomEnd, nextTopStart, nextTopEnd, nextCurveOffset,
                remapped, flipped);
    }

    public ConstructionSurface withAttachment(SurfaceSlot slot,
            BlockState state, boolean deform) {
        Map<SurfaceSlot, SurfaceAttachment> next =
                new LinkedHashMap<>(attachments);
        next.put(slot, new SurfaceAttachment(state, deform));
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, next, flipped);
    }

    public ConstructionSurface withFlipped(boolean nextFlipped) {
        if (nextFlipped == flipped) return this;
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, attachments, nextFlipped);
    }

    public ConstructionSurface withoutAttachment(SurfaceSlot slot) {
        if (slot == null || !attachments.containsKey(slot)) return this;
        Map<SurfaceSlot, SurfaceAttachment> next =
                new LinkedHashMap<>(attachments);
        next.remove(slot);
        return new ConstructionSurface(id, dimension, bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, next, flipped);
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
        return new ConstructionSurface(tag.getUUID("Id"), dimension,
                getVec(tag, "BottomStart"), getVec(tag, "BottomEnd"),
                getVec(tag, "TopStart"), getVec(tag, "TopEnd"),
                getVec(tag, "CurveOffset"), attachments,
                tag.getBoolean("Flipped"));
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

    public record SurfaceAttachment(BlockState state, boolean deform) {
        public SurfaceAttachment {
            state = state == null ? Blocks.AIR.defaultBlockState() : state;
        }
    }
}
