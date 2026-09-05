package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Spatial authority for devices exposed through the active surveillance room. */
public final class Scp079RoomInteractionPolicy {
    private static final int FLOOR_BORDER = 1;

    private Scp079RoomInteractionPolicy() {
    }

    public static boolean allows(ServerPlayer player, BlockPos target) {
        if (player == null || target == null
                || !Scp079PlayableManager.isController(player)) return false;
        ServerLevel level = player.serverLevel();
        FacilityRoom room = FacilityMappingManager.roomForPosition(level,
                BlockPos.containing(player.position()));
        return room != null && withinExpandedFloor(room, target, FLOOR_BORDER);
    }

    public static boolean withinExpandedFloor(FacilityRoom room,
            BlockPos target, int border) {
        if (room == null || target == null) return false;
        int extra = Math.max(0, border);
        for (FacilityFloorPatch patch : room.patches()) {
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
