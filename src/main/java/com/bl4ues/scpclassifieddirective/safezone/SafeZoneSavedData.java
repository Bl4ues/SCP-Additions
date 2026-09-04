package com.bl4ues.scpclassifieddirective.safezone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** World-persistent source of truth for every dimension's safe zones. */
public final class SafeZoneSavedData extends SavedData {
    private static final String DATA_NAME =
            "scp_classified_directive_safe_zones";
    private static final String ZONES_KEY = "Zones";

    private final Map<UUID, SafeZone> zones = new LinkedHashMap<>();

    private SafeZoneSavedData() {
    }

    public static SafeZoneSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SafeZoneSavedData::load, SafeZoneSavedData::new, DATA_NAME);
    }

    private static SafeZoneSavedData load(CompoundTag tag) {
        SafeZoneSavedData data = new SafeZoneSavedData();
        ListTag list = tag.getList(ZONES_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            SafeZone zone = SafeZone.load(list.getCompound(index));
            if (zone != null) data.zones.put(zone.id(), zone);
        }
        return data;
    }

    public synchronized List<SafeZone> all() {
        return List.copyOf(zones.values());
    }

    public synchronized SafeZone get(UUID id) {
        return zones.get(id);
    }

    public synchronized void put(SafeZone zone) {
        if (zone == null) return;
        zones.put(zone.id(), zone);
        setDirty();
    }

    public synchronized boolean delete(UUID id) {
        if (id == null || zones.remove(id) == null) return false;
        setDirty();
        return true;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (SafeZone zone : zones.values()) list.add(zone.save());
        tag.put(ZONES_KEY, list);
        return tag;
    }
}
