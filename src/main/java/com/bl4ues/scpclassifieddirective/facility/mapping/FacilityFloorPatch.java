package com.bl4ues.scpclassifieddirective.facility.mapping;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** One flat rectangular contribution to a mapped room floor. */
public record FacilityFloorPatch(int minX, int y, int minZ,
        int maxX, int maxZ) {
    public FacilityFloorPatch {
        if (minX > maxX) {
            int swap = minX;
            minX = maxX;
            maxX = swap;
        }
        if (minZ > maxZ) {
            int swap = minZ;
            minZ = maxZ;
            maxZ = swap;
        }
    }

    public static FacilityFloorPatch between(BlockPos first, BlockPos second) {
        return new FacilityFloorPatch(Math.min(first.getX(), second.getX()),
                first.getY(), Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()),
                Math.max(first.getZ(), second.getZ()));
    }

    public long area() {
        return ((long) maxX - minX + 1L) * ((long) maxZ - minZ + 1L);
    }

    public boolean containsFloor(BlockPos pos) {
        return pos != null && pos.getY() == y
                && pos.getX() >= minX && pos.getX() <= maxX
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public boolean containsColumn(BlockPos pos, int height) {
        return pos != null && pos.getY() >= y && pos.getY() <= y + height
                && pos.getX() >= minX && pos.getX() <= maxX
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public boolean overlaps(FacilityFloorPatch other) {
        return other != null && other.y == y
                && minX <= other.maxX && maxX >= other.minX
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MinX", minX);
        tag.putInt("Y", y);
        tag.putInt("MinZ", minZ);
        tag.putInt("MaxX", maxX);
        tag.putInt("MaxZ", maxZ);
        return tag;
    }

    public static FacilityFloorPatch load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;
        return new FacilityFloorPatch(tag.getInt("MinX"), tag.getInt("Y"),
                tag.getInt("MinZ"), tag.getInt("MaxX"), tag.getInt("MaxZ"));
    }
}
