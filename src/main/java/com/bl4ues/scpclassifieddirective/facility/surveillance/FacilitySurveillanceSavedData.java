package com.bl4ues.scpclassifieddirective.facility.surveillance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent source of truth for physical or generated surveillance cameras. */
public final class FacilitySurveillanceSavedData extends SavedData {
    private static final String DATA_NAME =
            "scp_classified_directive_facility_surveillance";
    private final Map<UUID, FacilityCameraDefinition> cameras =
            new LinkedHashMap<>();

    private FacilitySurveillanceSavedData() {
    }

    public static FacilitySurveillanceSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                FacilitySurveillanceSavedData::load,
                FacilitySurveillanceSavedData::new, DATA_NAME);
    }

    private static FacilitySurveillanceSavedData load(CompoundTag tag) {
        FacilitySurveillanceSavedData data =
                new FacilitySurveillanceSavedData();
        ListTag list = tag.getList("Cameras", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            FacilityCameraDefinition camera = FacilityCameraDefinition.load(
                    list.getCompound(index));
            if (camera != null) data.cameras.put(camera.id(), camera);
        }
        return data;
    }

    public synchronized List<FacilityCameraDefinition> all() {
        return List.copyOf(cameras.values());
    }

    public synchronized FacilityCameraDefinition get(UUID id) {
        return id == null ? null : cameras.get(id);
    }

    public synchronized void put(FacilityCameraDefinition camera) {
        if (camera == null) return;
        cameras.put(camera.id(), camera);
        setDirty();
    }

    public synchronized boolean remove(UUID id) {
        if (id == null || cameras.remove(id) == null) return false;
        setDirty();
        return true;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (FacilityCameraDefinition camera : cameras.values()) {
            list.add(camera.save());
        }
        tag.put("Cameras", list);
        return tag;
    }
}
