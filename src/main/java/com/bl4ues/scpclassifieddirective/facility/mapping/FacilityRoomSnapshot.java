package com.bl4ues.scpclassifieddirective.facility.mapping;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/** Network/client view of a room with its resolved Floor Station labels. */
public record FacilityRoomSnapshot(UUID id, ResourceLocation dimension,
        List<FacilityFloorPatch> patches, String name, BlockPos floorStation,
        String floorLongLabel, String floorShortLabel) {
    public FacilityRoomSnapshot {
        patches = patches == null ? List.of() : List.copyOf(patches);
        name = name == null ? "" : name;
        floorStation = floorStation == null ? null : floorStation.immutable();
        floorLongLabel = floorLongLabel == null ? "" : floorLongLabel;
        floorShortLabel = floorShortLabel == null ? "" : floorShortLabel;
    }

    public boolean containsFloor(BlockPos pos) {
        for (FacilityFloorPatch patch : patches) {
            if (patch.containsFloor(pos)) return true;
        }
        return false;
    }

    public boolean containsColumn(BlockPos pos) {
        for (FacilityFloorPatch patch : patches) {
            if (patch.containsColumn(pos, FacilityRoom.CAMERA_COLUMN_HEIGHT)) {
                return true;
            }
        }
        return false;
    }
}
