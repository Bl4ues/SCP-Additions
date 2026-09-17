package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityCameraMappingSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityFineGeometryNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Client copy used by the map tool and SCP-079 map renderer. */
public final class FacilityMappingClientState {
    private static final Map<ResourceLocation, List<FacilityRoomSnapshot>> ROOMS =
            new HashMap<>();
    private static final Map<ResourceLocation, List<FacilityCameraMappingSnapshot>>
            CAMERAS = new HashMap<>();
    private static final Map<ResourceLocation, Map<FinePatchKey, FacilityFloorPatch>>
            FINE_PATCHES = new HashMap<>();
    private static BlockPos selectionStart;
    private static UUID cameraLinkSelection;

    private FacilityMappingClientState() {
    }

    public static void setSelectionStart(BlockPos pos) {
        selectionStart = pos == null ? null : pos.immutable();
    }

    public static BlockPos selectionStart() {
        return selectionStart;
    }

    public static void setCameraLinkSelection(UUID cameraId) {
        cameraLinkSelection = cameraId;
    }

    public static UUID cameraLinkSelection() {
        return cameraLinkSelection;
    }

    public static void sync(ResourceLocation dimension,
            List<FacilityRoomSnapshot> rooms) {
        if (dimension == null) return;
        List<FacilityRoomSnapshot> snapshot = rooms == null
                ? List.of() : List.copyOf(rooms);
        ROOMS.put(dimension, snapshot);

        // Precision previews are optimistic client overlays. If a room is
        // deleted through its editor, discard any cached polygon fragments for
        // that room immediately instead of leaving ghost handles/patches behind.
        Map<FinePatchKey, FacilityFloorPatch> fine = FINE_PATCHES.get(dimension);
        if (fine != null && !fine.isEmpty()) {
            Set<UUID> roomIds = new HashSet<>();
            for (FacilityRoomSnapshot room : snapshot) roomIds.add(room.id());
            fine.keySet().removeIf(key -> !roomIds.contains(key.roomId()));
            if (fine.isEmpty()) FINE_PATCHES.remove(dimension);
        }
        reapplyFineGeometry(dimension);
    }

    public static void syncFineGeometry(ResourceLocation dimension,
            List<FacilityFineGeometryNetwork.PatchGeometry> geometry) {
        if (dimension == null) return;
        Map<FinePatchKey, FacilityFloorPatch> fine = new LinkedHashMap<>();
        if (geometry != null) {
            for (FacilityFineGeometryNetwork.PatchGeometry entry : geometry) {
                if (entry == null) continue;
                FacilityFloorPatch patch = FacilityFloorPatch.polygon(entry.y(),
                        entry.vertices());
                if (patch != null) {
                    fine.put(new FinePatchKey(entry.roomId(), entry.patchIndex()),
                            patch);
                }
            }
        }
        FINE_PATCHES.put(dimension, fine);
        reapplyFineGeometry(dimension);
    }

    public static void replaceRoomPatch(ResourceLocation dimension,
            UUID roomId, int patchIndex, FacilityFloorPatch patch) {
        if (dimension == null || roomId == null || patch == null) return;
        FINE_PATCHES.computeIfAbsent(dimension, ignored -> new LinkedHashMap<>())
                .put(new FinePatchKey(roomId, patchIndex), patch);
        replaceRoomPatchInternal(dimension, roomId, patchIndex, patch);
    }

    private static void reapplyFineGeometry(ResourceLocation dimension) {
        Map<FinePatchKey, FacilityFloorPatch> fine = FINE_PATCHES.get(dimension);
        if (fine == null || fine.isEmpty()) return;
        for (Map.Entry<FinePatchKey, FacilityFloorPatch> entry : fine.entrySet()) {
            replaceRoomPatchInternal(dimension, entry.getKey().roomId,
                    entry.getKey().patchIndex, entry.getValue());
        }
    }

    private static void replaceRoomPatchInternal(ResourceLocation dimension,
            UUID roomId, int patchIndex, FacilityFloorPatch patch) {
        List<FacilityRoomSnapshot> current = ROOMS.get(dimension);
        if (current == null || current.isEmpty()) return;
        List<FacilityRoomSnapshot> rooms = new ArrayList<>(current);
        for (int roomIndex = 0; roomIndex < rooms.size(); roomIndex++) {
            FacilityRoomSnapshot room = rooms.get(roomIndex);
            if (!roomId.equals(room.id()) || patchIndex < 0
                    || patchIndex >= room.patches().size()) continue;
            List<FacilityFloorPatch> patches = new ArrayList<>(room.patches());
            patches.set(patchIndex, patch);
            rooms.set(roomIndex, new FacilityRoomSnapshot(room.id(),
                    room.dimension(), patches, room.name(), room.floorStation(),
                    room.floorLongLabel(), room.floorShortLabel()));
            ROOMS.put(dimension, List.copyOf(rooms));
            return;
        }
    }

    public static List<FacilityRoomSnapshot> rooms(ResourceLocation dimension) {
        return dimension == null ? List.of()
                : ROOMS.getOrDefault(dimension, List.of());
    }

    public static void syncCameras(ResourceLocation dimension,
            List<FacilityCameraMappingSnapshot> cameras) {
        if (dimension == null) return;
        CAMERAS.put(dimension,
                cameras == null ? List.of() : List.copyOf(cameras));
        if (cameraLinkSelection != null
                && cameraById(dimension, cameraLinkSelection) == null) {
            cameraLinkSelection = null;
        }
    }

    public static List<FacilityCameraMappingSnapshot> cameras(
            ResourceLocation dimension) {
        return dimension == null ? List.of()
                : CAMERAS.getOrDefault(dimension, List.of());
    }

    public static FacilityCameraMappingSnapshot cameraAt(
            ResourceLocation dimension, BlockPos pos) {
        if (pos == null) return null;
        for (FacilityCameraMappingSnapshot camera : cameras(dimension)) {
            if (camera.anchorPos().equals(pos)) return camera;
        }
        return null;
    }

    public static FacilityCameraMappingSnapshot cameraById(
            ResourceLocation dimension, UUID cameraId) {
        if (cameraId == null) return null;
        for (FacilityCameraMappingSnapshot camera : cameras(dimension)) {
            if (cameraId.equals(camera.cameraId())) return camera;
        }
        return null;
    }

    public static FacilityRoomSnapshot roomById(ResourceLocation dimension,
            UUID roomId) {
        if (roomId == null) return null;
        for (FacilityRoomSnapshot room : rooms(dimension)) {
            if (roomId.equals(room.id())) return room;
        }
        return null;
    }

    public static FacilityRoomSnapshot roomAt(ResourceLocation dimension,
            BlockPos pos) {
        if (pos == null) return null;
        FacilityRoomSnapshot best = null;
        int bestVertical = Integer.MAX_VALUE;
        long bestArea = Long.MAX_VALUE;
        for (FacilityRoomSnapshot room : rooms(dimension)) {
            if (!room.containsColumn(pos)) continue;
            int vertical = verticalDistance(room, pos);
            long area = authoredArea(room);
            if (vertical < bestVertical
                    || vertical == bestVertical && area < bestArea) {
                best = room;
                bestVertical = vertical;
                bestArea = area;
            }
        }
        if (best != null) return best;
        for (FacilityRoomSnapshot room : rooms(dimension)) {
            if (!withinBorder(room, pos, 1)) continue;
            int vertical = verticalDistance(room, pos);
            long area = authoredArea(room);
            if (vertical < bestVertical
                    || vertical == bestVertical && area < bestArea) {
                best = room;
                bestVertical = vertical;
                bestArea = area;
            }
        }
        return best;
    }

    private static int verticalDistance(FacilityRoomSnapshot room,
            BlockPos pos) {
        int best = Integer.MAX_VALUE;
        for (FacilityFloorPatch patch : room.patches()) {
            if (patch.containsXZ(pos.getX() + 0.5D, pos.getZ() + 0.5D)) {
                best = Math.min(best, Math.abs(pos.getY() - patch.y()));
            }
        }
        if (best != Integer.MAX_VALUE) return best;
        for (FacilityFloorPatch patch : room.patches()) {
            best = Math.min(best, Math.abs(pos.getY() - patch.y()));
        }
        return best;
    }

    private static long authoredArea(FacilityRoomSnapshot room) {
        long area = 0L;
        for (FacilityFloorPatch patch : room.patches()) area += patch.area();
        return area;
    }

    private static boolean withinBorder(FacilityRoomSnapshot room,
            BlockPos pos, int border) {
        if (room == null || pos == null) return false;
        for (FacilityFloorPatch patch : room.patches()) {
            if (pos.getX() < patch.minX() - border
                    || pos.getX() > patch.maxX() + border
                    || pos.getZ() < patch.minZ() - border
                    || pos.getZ() > patch.maxZ() + border) continue;
            if (pos.getY() >= patch.y() - 1
                    && pos.getY() <= patch.y() + FacilityRoom.CAMERA_COLUMN_HEIGHT) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        selectionStart = null;
        cameraLinkSelection = null;
        ROOMS.clear();
        CAMERAS.clear();
        FINE_PATCHES.clear();
    }

    private record FinePatchKey(UUID roomId, int patchIndex) {
    }
}
