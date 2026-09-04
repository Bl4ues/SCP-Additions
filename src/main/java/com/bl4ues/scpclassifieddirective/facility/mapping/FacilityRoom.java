package com.bl4ues.scpclassifieddirective.facility.mapping;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent map-maker definition of one logical facility room. */
public record FacilityRoom(UUID id, ResourceLocation dimension,
        List<FacilityFloorPatch> patches, String name,
        BlockPos floorStation) {
    public static final int MAX_NAME_LENGTH = 64;
    public static final int CAMERA_COLUMN_HEIGHT = 12;

    public FacilityRoom {
        id = id == null ? UUID.randomUUID() : id;
        dimension = dimension == null
                ? new ResourceLocation("minecraft", "overworld") : dimension;
        patches = patches == null ? List.of() : List.copyOf(patches);
        name = sanitizeName(name);
        floorStation = floorStation == null ? null : floorStation.immutable();
    }

    public boolean containsFloor(BlockPos pos) {
        for (FacilityFloorPatch patch : patches) {
            if (patch.containsFloor(pos)) return true;
        }
        return false;
    }

    public boolean containsColumn(BlockPos pos) {
        for (FacilityFloorPatch patch : patches) {
            if (patch.containsColumn(pos, CAMERA_COLUMN_HEIGHT)) return true;
        }
        return false;
    }

    public boolean overlaps(FacilityFloorPatch patch) {
        for (FacilityFloorPatch existing : patches) {
            if (existing.overlaps(patch)) return true;
        }
        return false;
    }

    public long authoredArea() {
        long area = 0L;
        for (FacilityFloorPatch patch : patches) area += patch.area();
        return area;
    }

    public int representativeY() {
        return patches.isEmpty() ? 0 : patches.get(0).y();
    }

    public FacilityRoom withAddedPatch(FacilityFloorPatch patch) {
        List<FacilityFloorPatch> next = new ArrayList<>(patches);
        if (patch != null) next.add(patch);
        return new FacilityRoom(id, dimension, compact(next), name,
                floorStation);
    }

    public FacilityRoom merge(FacilityRoom other) {
        if (other == null) return this;
        List<FacilityFloorPatch> next = new ArrayList<>(patches);
        next.addAll(other.patches);
        String nextName = !name.isBlank() ? name : other.name;
        BlockPos nextStation = floorStation != null
                ? floorStation : other.floorStation;
        return new FacilityRoom(id, dimension, compact(next), nextName,
                nextStation);
    }

    public FacilityRoom withMetadata(String name, BlockPos floorStation) {
        return new FacilityRoom(id, dimension, patches, name, floorStation);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Dimension", dimension.toString());
        tag.putString("Name", name);
        if (floorStation != null) {
            tag.putLong("FloorStation", floorStation.asLong());
        }
        ListTag patchList = new ListTag();
        for (FacilityFloorPatch patch : patches) patchList.add(patch.save());
        tag.put("Patches", patchList);
        return tag;
    }

    public static FacilityRoom load(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("Id")) return null;
        ResourceLocation dimension = ResourceLocation.tryParse(
                tag.getString("Dimension"));
        if (dimension == null) return null;
        List<FacilityFloorPatch> patches = new ArrayList<>();
        ListTag patchList = tag.getList("Patches", Tag.TAG_COMPOUND);
        for (int index = 0; index < patchList.size(); index++) {
            FacilityFloorPatch patch = FacilityFloorPatch.load(
                    patchList.getCompound(index));
            if (patch != null) patches.add(patch);
        }
        BlockPos station = tag.contains("FloorStation", Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong("FloorStation")) : null;
        return new FacilityRoom(tag.getUUID("Id"), dimension, patches,
                tag.getString("Name"), station);
    }

    private static String sanitizeName(String value) {
        String clean = value == null ? "" : value.strip()
                .replace('\n', ' ').replace('\r', ' ');
        if (clean.length() > MAX_NAME_LENGTH) {
            clean = clean.substring(0, MAX_NAME_LENGTH);
        }
        return clean;
    }

    private static List<FacilityFloorPatch> compact(
            List<FacilityFloorPatch> source) {
        List<FacilityFloorPatch> unique = new ArrayList<>();
        for (FacilityFloorPatch patch : source) {
            if (patch != null && !unique.contains(patch)) unique.add(patch);
        }
        return List.copyOf(unique);
    }
}
