package com.bl4ues.scpclassifieddirective.facility.mapping;

import net.minecraft.core.BlockPos;

import java.util.UUID;

/** Client-safe camera-to-room association exposed by the Facility Mapping Tool. */
public record FacilityCameraMappingSnapshot(UUID cameraId, BlockPos anchorPos,
        UUID roomId, boolean manual, boolean detached) {
    public FacilityCameraMappingSnapshot {
        anchorPos = anchorPos == null ? BlockPos.ZERO : anchorPos.immutable();
        if (detached) roomId = null;
    }

    public boolean associated() {
        return roomId != null && !detached;
    }
}
