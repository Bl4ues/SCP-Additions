package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079CameraTravelRules;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayerPower;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceSavedData;
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
        FacilityRoomSnapshot current =
                scpclassifieddirective$currentCameraRoom(level, player);
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
        FacilityRoomSnapshot current =
                scpclassifieddirective$currentCameraRoom(level, player);
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

    private static FacilityRoomSnapshot scpclassifieddirective$currentCameraRoom(
            ServerLevel level, ServerPlayer player) {
        return Scp079PlayableManager.currentCameraRoom(player);
    }

    private static FacilityRoomSnapshot scpclassifieddirective$roomForCamera(
            ServerLevel level, FacilityCameraDefinition camera) {
        return FacilityMappingManager.roomSnapshotForCamera(level, camera);
    }
}
