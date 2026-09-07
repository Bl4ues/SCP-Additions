package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079CameraTravelRules;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayerPower;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.UUID;

/** Applies the authored same-floor / cross-floor / cross-zone camera tiers. */
@Mixin(Scp079PlayableManager.class)
public abstract class Scp079CrossFloorCameraCostMixin {
    @Redirect(method = "switchToRoom",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/facility/Scp079PlayerPower;trySpend(Lnet/minecraft/server/level/ServerLevel;D)Z"),
            remap = false)
    private static boolean scpclassifieddirective$roomSwitchCost(
            ServerLevel level, double baseCost, ServerPlayer player,
            UUID roomId) {
        FacilityRoomSnapshot current = scpclassifieddirective$currentRoom(
                level, player.blockPosition());
        FacilityRoomSnapshot target = scpclassifieddirective$roomById(level,
                roomId);
        double cost = baseCost * Scp079CameraTravelRules.multiplier(current,
                target);
        return Scp079PlayerPower.trySpend(level, cost);
    }

    @Redirect(method = "switchToCamera",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/facility/Scp079PlayerPower;trySpend(Lnet/minecraft/server/level/ServerLevel;D)Z"),
            remap = false)
    private static boolean scpclassifieddirective$cameraSwitchCost(
            ServerLevel level, double baseCost, ServerPlayer player,
            UUID cameraId) {
        FacilityRoomSnapshot current = scpclassifieddirective$currentRoom(
                level, player.blockPosition());
        FacilityCameraDefinition camera = FacilitySurveillanceRegistry.camera(
                level, cameraId);
        FacilityRoomSnapshot target = scpclassifieddirective$roomForCamera(
                level, camera);
        double cost = baseCost * Scp079CameraTravelRules.multiplier(current,
                target);
        return Scp079PlayerPower.trySpend(level, cost);
    }

    private static FacilityRoomSnapshot scpclassifieddirective$roomById(
            ServerLevel level, UUID roomId) {
        if (level == null || roomId == null) return null;
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (room.id().equals(roomId)) return room;
        }
        return null;
    }

    private static FacilityRoomSnapshot scpclassifieddirective$currentRoom(
            ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        List<FacilityRoomSnapshot> rooms = FacilityMappingManager.roomSnapshots(level);
        for (FacilityRoomSnapshot room : rooms) {
            if (room.containsColumn(pos)) return room;
        }
        for (FacilityRoomSnapshot room : rooms) {
            if (Scp079RoomInteractionPolicy.withinExpandedFloor(room, pos, 4)) {
                return room;
            }
        }
        return null;
    }

    private static FacilityRoomSnapshot scpclassifieddirective$roomForCamera(
            ServerLevel level, FacilityCameraDefinition camera) {
        if (level == null || camera == null) return null;
        List<FacilityRoomSnapshot> rooms = FacilityMappingManager.roomSnapshots(level);
        BlockPos eye = BlockPos.containing(camera.eyePosition());
        for (FacilityRoomSnapshot room : rooms) {
            if (room.containsColumn(eye)) return room;
        }
        for (FacilityRoomSnapshot room : rooms) {
            if (Scp079RoomInteractionPolicy.withinExpandedFloor(
                    room, camera.anchorPos(), 1)) {
                return room;
            }
        }
        return null;
    }
}
