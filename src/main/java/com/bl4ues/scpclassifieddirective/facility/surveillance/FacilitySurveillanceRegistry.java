package com.bl4ues.scpclassifieddirective.facility.surveillance;

import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Shared camera registry consumed by SCP-079 now and human CCTV later. */
public final class FacilitySurveillanceRegistry {
    private FacilitySurveillanceRegistry() {
    }

    public static FacilityCameraDefinition register(ServerLevel level,
            UUID id, BlockPos anchorPos, Vec3 eyePosition, String name,
            float baseYaw, float basePitch, float yawLimit,
            float minPitch, float maxPitch, float maxZoom) {
        if (level == null || anchorPos == null) return null;
        FacilityCameraDefinition camera = new FacilityCameraDefinition(id,
                level.dimension().location(), anchorPos, eyePosition, name,
                baseYaw, basePitch, yawLimit, minPitch, maxPitch, maxZoom);
        FacilitySurveillanceSavedData.get(level.getServer()).put(camera);
        return camera;
    }

    public static boolean unregister(ServerLevel level, UUID id) {
        return level != null && FacilitySurveillanceSavedData
                .get(level.getServer()).remove(id);
    }

    public static FacilityCameraDefinition camera(ServerLevel level, UUID id) {
        if (level == null || id == null) return null;
        FacilityCameraDefinition camera = FacilitySurveillanceSavedData
                .get(level.getServer()).get(id);
        return camera != null && camera.dimension().equals(
                level.dimension().location()) ? camera : null;
    }

    public static List<FacilityCameraDefinition> camerasForRoom(
            ServerLevel level, UUID roomId) {
        if (level == null || roomId == null) return List.of();
        FacilityRoomSnapshot targetRoom = FacilityMappingManager.roomSnapshots(level)
                .stream().filter(room -> room.id().equals(roomId))
                .findFirst().orElse(null);
        if (targetRoom == null) return List.of();
        return FacilitySurveillanceSavedData.get(level.getServer()).all().stream()
                .filter(camera -> camera.dimension().equals(
                        level.dimension().location()))
                .filter(camera -> Scp079RoomInteractionPolicy.withinExpandedFloor(
                        targetRoom, camera.anchorPos(), 1))
                .sorted(Comparator.comparing(FacilityCameraDefinition::name,
                        String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(camera -> camera.id().toString()))
                .toList();
    }

    public static FacilityCameraDefinition nextForRoom(ServerLevel level,
            UUID roomId, UUID currentCamera) {
        List<FacilityCameraDefinition> cameras = camerasForRoom(level, roomId);
        if (cameras.isEmpty()) return null;
        if (currentCamera == null) return cameras.get(0);
        for (int index = 0; index < cameras.size(); index++) {
            if (cameras.get(index).id().equals(currentCamera)) {
                return cameras.get((index + 1) % cameras.size());
            }
        }
        return cameras.get(0);
    }
}
