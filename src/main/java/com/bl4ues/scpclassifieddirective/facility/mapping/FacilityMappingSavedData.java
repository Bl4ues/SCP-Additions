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
    // No entry means automatic spatial association. A stored entry either pins
    // one camera to a room or explicitly detaches it from automatic association.
    private final Map<UUID, CameraRoomBinding> cameraBindings =
            new LinkedHashMap<>();

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
        ListTag bindingList = tag.getList("CameraBindings", Tag.TAG_COMPOUND);
        for (int index = 0; index < bindingList.size(); index++) {
            CameraRoomBinding binding = CameraRoomBinding.load(
                    bindingList.getCompound(index));
            if (binding != null) {
                data.cameraBindings.put(binding.cameraId(), binding);
            }
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

    public synchronized CameraRoomBinding cameraBinding(UUID cameraId) {
        return cameraId == null ? null : cameraBindings.get(cameraId);
    }

    public synchronized void bindCamera(UUID cameraId, UUID roomId) {
        if (cameraId == null || roomId == null) return;
        cameraBindings.put(cameraId,
                new CameraRoomBinding(cameraId, roomId, false));
        setDirty();
    }

    public synchronized void detachCamera(UUID cameraId) {
        if (cameraId == null) return;
        cameraBindings.put(cameraId,
                new CameraRoomBinding(cameraId, null, true));
        setDirty();
    }

    public synchronized boolean clearCameraBinding(UUID cameraId) {
        if (cameraId == null || cameraBindings.remove(cameraId) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public synchronized boolean rebindRoom(UUID oldRoomId, UUID newRoomId) {
        if (oldRoomId == null || newRoomId == null
                || oldRoomId.equals(newRoomId)) return false;
        boolean changed = false;
        for (Map.Entry<UUID, CameraRoomBinding> entry
                : cameraBindings.entrySet()) {
            CameraRoomBinding binding = entry.getValue();
            if (!binding.detached() && oldRoomId.equals(binding.roomId())) {
                entry.setValue(new CameraRoomBinding(
                        binding.cameraId(), newRoomId, false));
                changed = true;
            }
        }
        if (changed) setDirty();
        return changed;
    }

    public synchronized boolean clearBindingsForRoom(UUID roomId) {
        if (roomId == null) return false;
        boolean changed = cameraBindings.entrySet().removeIf(entry -> {
            CameraRoomBinding binding = entry.getValue();
            return !binding.detached() && roomId.equals(binding.roomId());
        });
        if (changed) setDirty();
        return changed;
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
        ListTag bindingList = new ListTag();
        for (CameraRoomBinding binding : cameraBindings.values()) {
            bindingList.add(binding.save());
        }
        tag.put("CameraBindings", bindingList);
        return tag;
    }

    public record CameraRoomBinding(UUID cameraId, UUID roomId,
            boolean detached) {
        public CameraRoomBinding {
            if (detached) roomId = null;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Camera", cameraId);
            tag.putBoolean("Detached", detached);
            if (roomId != null) tag.putUUID("Room", roomId);
            return tag;
        }

        public static CameraRoomBinding load(CompoundTag tag) {
            if (tag == null || !tag.hasUUID("Camera")) return null;
            boolean detached = tag.getBoolean("Detached");
            UUID roomId = tag.hasUUID("Room") ? tag.getUUID("Room") : null;
            if (!detached && roomId == null) return null;
            return new CameraRoomBinding(tag.getUUID("Camera"), roomId,
                    detached);
        }
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
