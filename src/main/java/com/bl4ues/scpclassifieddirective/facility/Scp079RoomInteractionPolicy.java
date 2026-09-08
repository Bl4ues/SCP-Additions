package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Spatial authority for devices exposed through the active surveillance room. */
public final class Scp079RoomInteractionPolicy {
    /**
     * Facility doors and their controller-facing blocks commonly sit a couple of
     * blocks outside the authored walkable floor. Keep those boundary devices in
     * the room without opening interaction into the next room wholesale.
     */
    private static final int TARGET_BORDER = 3;
    private static final int CAMERA_BORDER = 4;

    private Scp079RoomInteractionPolicy() {
    }

    public static boolean allows(ServerPlayer player, BlockPos target) {
        if (player == null || target == null
                || !Scp079PlayableManager.isController(player)) return false;
        ServerLevel level = player.serverLevel();
        List<FacilityRoomSnapshot> rooms = FacilityMappingManager.roomSnapshots(level);
        FacilityRoomSnapshot current = activeCameraRoom(player, level, rooms);
        if (current == null) {
            BlockPos viewpoint = BlockPos.containing(player.position());
            for (FacilityRoomSnapshot room : rooms) {
                if (withinExpandedFloor(room, viewpoint, CAMERA_BORDER)) {
                    current = room;
                    break;
                }
            }
        }
        if (current == null) return false;

        // A block actually contained by a neighbouring authored floor belongs
        // to that room, even if the structural border of the current room can
        // also reach it. Cross-room physical interaction is camera-only.
        if (!current.containsColumn(target)) {
            for (FacilityRoomSnapshot room : rooms) {
                if (!room.id().equals(current.id()) && room.containsColumn(target)) {
                    return false;
                }
            }
        }
        return current.containsColumn(target)
                || withinExpandedFloor(current, target, TARGET_BORDER);
    }

    private static FacilityRoomSnapshot activeCameraRoom(ServerPlayer player,
            ServerLevel level, List<FacilityRoomSnapshot> rooms) {
        if (player.getServer() == null || !Scp079PlayableManager.isCameraMode(player)) {
            return null;
        }
        FacilityCameraDefinition nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (FacilityCameraDefinition stored : FacilitySurveillanceSavedData
                .get(player.getServer()).all()) {
            if (!stored.dimension().equals(level.dimension().location())) continue;
            FacilityCameraDefinition camera = FacilitySurveillanceRegistry.camera(
                    level, stored.id());
            if (camera == null) continue;
            double distance = camera.eyePosition().distanceToSqr(player.position());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = camera;
            }
        }
        if (nearest == null || nearestDistance > 16.0D) return null;

        BlockPos eye = BlockPos.containing(nearest.eyePosition());
        for (FacilityRoomSnapshot room : rooms) {
            if (room.containsColumn(eye)) return room;
        }
        for (FacilityRoomSnapshot room : rooms) {
            if (withinExpandedFloor(room, nearest.anchorPos(), 1)) return room;
        }
        return null;
    }

    public static boolean withinExpandedFloor(FacilityRoom room,
            BlockPos target, int border) {
        if (room == null) return false;
        return withinExpandedFloor(room.patches(), target, border);
    }

    public static boolean withinExpandedFloor(FacilityRoomSnapshot room,
            BlockPos target, int border) {
        if (room == null) return false;
        return withinExpandedFloor(room.patches(), target, border);
    }

    private static boolean withinExpandedFloor(
            Iterable<FacilityFloorPatch> patches, BlockPos target, int border) {
        if (target == null) return false;
        int extra = Math.max(0, border);
        for (FacilityFloorPatch patch : patches) {
            if (target.getX() < patch.minX() - extra
                    || target.getX() > patch.maxX() + extra
                    || target.getZ() < patch.minZ() - extra
                    || target.getZ() > patch.maxZ() + extra) continue;
            if (target.getY() >= patch.y() - 1
                    && target.getY() <= patch.y() + FacilityRoom.CAMERA_COLUMN_HEIGHT) {
                return true;
            }
        }
        return false;
    }
}
