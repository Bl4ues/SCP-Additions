package com.bl4ues.scpclassifieddirective.facility.mapping;

import net.minecraft.core.BlockPos;

/** Client-safe description of one configured Core Room Floor Station. */
public record FacilityFloorStationOption(BlockPos pos, String longLabel,
        String shortLabel) {
    public FacilityFloorStationOption {
        pos = pos == null ? BlockPos.ZERO : pos.immutable();
        longLabel = longLabel == null ? "" : longLabel;
        shortLabel = shortLabel == null ? "" : shortLabel;
    }
}
