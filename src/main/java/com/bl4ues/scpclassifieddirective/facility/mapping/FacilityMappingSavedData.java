package com.bl4ues.scpclassifieddirective.facility.mapping;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent rooms plus an index of discovered configured-floor candidates. */
public final class FacilityMappingSavedData extends SavedData {
    private static final String DATA_NAME =
            "scp_classified_directive_facility_mapping";
    private final Map<UUID, FacilityRoom> rooms = new LinkedHashMap<>();
    private final Set<TrackedStation> stations = new LinkedHashSet<>();

    private FacilityMappingSavedData() {
    }

    public static FacilityMappingSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                FacilityMappingSavedData::load,
                FacilityMappingSavedData::new, DATA_NAME);
    }

    private static FacilityMappingSavedData load(CompoundTag tag) {
        FacilityMappingSavedData data = new FacilityMappingSavedData();
        ListTag roomList = tag.getList("Rooms", Tag.TAG_COMPOUND);
        for (int index = 0; index < roomList.size(); index++) {
            FacilityRoom room = FacilityRoom.load(roomList.getCompound(index));
            if (room != null) data.rooms.put(room.id(), room);
        }
        ListTag stationList = tag.getList("Stations", Tag.TAG_COMPOUND);
        for (int index = 0; index < stationList.size(); index++) {
            TrackedStation station = TrackedStation.load(
                    stationList.getCompound(index));
            if (station != null) data.stations.add(station);
        }
        return data;
    }

    public synchronized List<FacilityRoom> rooms() {
        return List.copyOf(rooms.values());
    }

    public synchronized FacilityRoom room(UUID id) {
        return id == null ? null : rooms.get(id);
    }

    public synchronized void putRoom(FacilityRoom room) {
        if (room == null) return;
        rooms.put(room.id(), room);
        setDirty();
    }

    public synchronized boolean deleteRoom(UUID id) {
        if (id == null || rooms.remove(id) == null) return false;
        setDirty();
        return true;
    }

    public synchronized Set<TrackedStation> stations() {
        return Set.copyOf(stations);
    }

    public synchronized boolean putStation(TrackedStation station) {
        if (station == null) return false;
        TrackedStation previous = stations.stream()
                .filter(existing -> existing.samePosition(station))
                .findFirst().orElse(null);
        if (station.equals(previous)) return false;
        if (previous != null) stations.remove(previous);
        stations.add(station);
        setDirty();
        return true;
    }

    public synchronized boolean removeStation(TrackedStation station) {
        if (station == null) return false;
        boolean changed = stations.removeIf(existing ->
                existing.samePosition(station));
        if (changed) setDirty();
        return changed;
    }

    public synchronized boolean removeStationsInChunk(ResourceLocation dimension,
            int chunkX, int chunkZ) {
        boolean changed = stations.removeIf(station ->
                station.dimension().equals(dimension)
                        && station.chunkX() == chunkX
                        && station.chunkZ() == chunkZ);
        if (changed) setDirty();
        return changed;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        ListTag roomList = new ListTag();
        for (FacilityRoom room : rooms.values()) roomList.add(room.save());
        tag.put("Rooms", roomList);
        ListTag stationList = new ListTag();
        for (TrackedStation station : stations) stationList.add(station.save());
        tag.put("Stations", stationList);
        return tag;
    }

    public record TrackedStation(ResourceLocation dimension, long packedPos,
            String longLabel, String shortLabel) {
        public int chunkX() {
            return net.minecraft.core.BlockPos.getX(packedPos) >> 4;
        }

        public int chunkZ() {
            return net.minecraft.core.BlockPos.getZ(packedPos) >> 4;
        }

        public TrackedStation {
            longLabel = longLabel == null ? "" : longLabel;
            shortLabel = shortLabel == null ? "" : shortLabel;
        }

        public boolean samePosition(TrackedStation other) {
            return other != null && packedPos == other.packedPos
                    && dimension.equals(other.dimension);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension.toString());
            tag.putLong("Pos", packedPos);
            tag.putString("LongLabel", longLabel);
            tag.putString("ShortLabel", shortLabel);
            return tag;
        }

        public static TrackedStation load(CompoundTag tag) {
            ResourceLocation dimension = tag == null ? null
                    : ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimension == null || !tag.contains("Pos", Tag.TAG_LONG)) {
                return null;
            }
            return new TrackedStation(dimension, tag.getLong("Pos"),
                    tag.getString("LongLabel"), tag.getString("ShortLabel"));
        }
    }
}
