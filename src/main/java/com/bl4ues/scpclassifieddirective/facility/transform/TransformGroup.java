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

/** One rigid local grid whose cells may be positioned and rotated off-grid. */
public record TransformGroup(UUID id, ResourceLocation dimension, Vec3 origin,
        float rotationX, float rotationY, float rotationZ,
        Map<GridPos, BlockState> cells) {
    public TransformGroup {
        id = id == null ? UUID.randomUUID() : id;
        dimension = dimension == null
                ? new ResourceLocation("minecraft", "overworld") : dimension;
        origin = origin == null ? Vec3.ZERO : origin;
        cells = cells == null ? Map.of() : Map.copyOf(cells);
    }

    public static TransformGroup empty(ResourceLocation dimension, Vec3 origin) {
        Map<GridPos, BlockState> cells = new LinkedHashMap<>();
        cells.put(GridPos.ZERO, Blocks.AIR.defaultBlockState());
        return new TransformGroup(UUID.randomUUID(), dimension, origin,
                0.0F, 0.0F, 0.0F, cells);
    }

    public TransformGroup withTransform(Vec3 nextOrigin, float nextX,
            float nextY, float nextZ) {
        return new TransformGroup(id, dimension, nextOrigin, nextX, nextY,
                nextZ, cells);
    }

    public TransformGroup withCell(GridPos pos, BlockState state) {
        Map<GridPos, BlockState> next = new LinkedHashMap<>(cells);
        next.put(pos, state == null ? Blocks.AIR.defaultBlockState() : state);
        return new TransformGroup(id, dimension, origin, rotationX, rotationY,
                rotationZ, next);
    }

    public TransformGroup withoutCell(GridPos pos) {
        if (pos == null || !cells.containsKey(pos)) return this;
        Map<GridPos, BlockState> next = new LinkedHashMap<>(cells);
        next.remove(pos);
        return new TransformGroup(id, dimension, origin, rotationX, rotationY,
                rotationZ, next);
    }

    public Vec3 cellCenter(GridPos pos) {
        return TransformMath.localToWorld(origin,
                new Vec3(pos.x(), pos.y(), pos.z()), rotationX, rotationY,
                rotationZ);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Dimension", dimension.toString());
        tag.putDouble("OriginX", origin.x);
        tag.putDouble("OriginY", origin.y);
        tag.putDouble("OriginZ", origin.z);
        tag.putFloat("RotationX", rotationX);
        tag.putFloat("RotationY", rotationY);
        tag.putFloat("RotationZ", rotationZ);
        ListTag list = new ListTag();
        for (Map.Entry<GridPos, BlockState> entry : cells.entrySet()) {
            CompoundTag cell = entry.getKey().save();
            cell.put("State", BlockStateCodec.save(entry.getValue()));
            list.add(cell);
        }
        tag.put("Cells", list);
        return tag;
    }

    public static TransformGroup load(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("Id")) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(
                tag.getString("Dimension"));
        if (dimension == null) return null;
        Vec3 origin = new Vec3(tag.getDouble("OriginX"),
                tag.getDouble("OriginY"), tag.getDouble("OriginZ"));
        Map<GridPos, BlockState> cells = new LinkedHashMap<>();
        ListTag list = tag.getList("Cells", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag cellTag = list.getCompound(index);
            GridPos pos = GridPos.load(cellTag);
            if (pos != null) {
                cells.put(pos, BlockStateCodec.load(
                        cellTag.getCompound("State")));
            }
        }
        if (cells.isEmpty()) cells.put(GridPos.ZERO,
                Blocks.AIR.defaultBlockState());
        return new TransformGroup(tag.getUUID("Id"), dimension, origin,
                tag.getFloat("RotationX"), tag.getFloat("RotationY"),
                tag.getFloat("RotationZ"), cells);
    }

    public record GridPos(int x, int y, int z) {
        public static final GridPos ZERO = new GridPos(0, 0, 0);

        public GridPos offset(int dx, int dy, int dz) {
            return new GridPos(x + dx, y + dy, z + dz);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", x);
            tag.putInt("Y", y);
            tag.putInt("Z", z);
            return tag;
        }

        private static GridPos load(CompoundTag tag) {
            return tag == null ? null : new GridPos(tag.getInt("X"),
                    tag.getInt("Y"), tag.getInt("Z"));
        }
    }
}
