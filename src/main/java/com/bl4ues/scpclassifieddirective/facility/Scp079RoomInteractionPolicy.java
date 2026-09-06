package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Spatial authority for devices exposed through the active surveillance room. */
public final class Scp079RoomInteractionPolicy {
    private static final int TARGET_BORDER = 1;
    private static final int CAMERA_BORDER = 4;

    private Scp079RoomInteractionPolicy() {
    }

    public static boolean allows(ServerPlayer player, BlockPos target) {
        if (player == null || target == null
                || !Scp079PlayableManager.isController(player)) return false;
        ServerLevel level = player.serverLevel();
        BlockPos viewpoint = BlockPos.containing(player.position());

        // Do not resolve the camera to the first nearby room and then test the
        // target. Wall-mounted/angled cameras can sit on a boundary shared by two
        // mapped rooms, making iteration order choose the wrong one. Instead,
        // accept any authored room that contains both the camera vicinity and the
        // aimed device. The target allowance stays tight so doors on the border
        // work without granting control of devices several blocks outside.
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (withinExpandedFloor(room, target, TARGET_BORDER)
                    && withinExpandedFloor(room, viewpoint, CAMERA_BORDER)) {
                return true;
            }
        }
        return false;
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
