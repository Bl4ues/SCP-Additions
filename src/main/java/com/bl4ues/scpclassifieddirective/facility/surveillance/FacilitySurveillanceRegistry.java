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
        if (camera == null || !camera.dimension().equals(
                level.dimension().location())) return null;
        return normalizePlaceholder(level, camera);
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
                .map(camera -> normalizePlaceholder(level, camera))
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

    /**
     * Existing test worlds may have placeholder cameras persisted with the old
     * movement envelope. Upgrade those definitions lazily without touching
     * cameras supplied by future integrations or map-makers.
     */
    private static FacilityCameraDefinition normalizePlaceholder(
            ServerLevel level, FacilityCameraDefinition camera) {
        if (camera == null || !level.getBlockState(camera.anchorPos()).is(
                SurveillanceCameraPlaceholderModule.BLOCK.get())) {
            return camera;
        }
        float yaw = SurveillanceCameraPlaceholderModule.MANUAL_YAW_LIMIT;
        float minPitch = SurveillanceCameraPlaceholderModule.MANUAL_MIN_PITCH;
        float maxPitch = SurveillanceCameraPlaceholderModule.MANUAL_MAX_PITCH;
        if (Float.compare(camera.yawLimit(), yaw) == 0
                && Float.compare(camera.minPitch(), minPitch) == 0
                && Float.compare(camera.maxPitch(), maxPitch) == 0) {
            return camera;
        }
        FacilityCameraDefinition upgraded = new FacilityCameraDefinition(
                camera.id(), camera.dimension(), camera.anchorPos(),
                camera.eyePosition(), camera.name(), camera.baseYaw(),
                camera.basePitch(), yaw, minPitch, maxPitch,
                camera.maxZoom());
        FacilitySurveillanceSavedData.get(level.getServer()).put(upgraded);
        return upgraded;
    }
}
