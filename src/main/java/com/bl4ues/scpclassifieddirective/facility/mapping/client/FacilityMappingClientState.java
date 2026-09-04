package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Client copy used by the map tool and SCP-079 map renderer. */
public final class FacilityMappingClientState {
    private static final Map<ResourceLocation, List<FacilityRoomSnapshot>> ROOMS =
            new HashMap<>();
    private static BlockPos selectionStart;

    private FacilityMappingClientState() {
    }

    public static void setSelectionStart(BlockPos pos) {
        selectionStart = pos == null ? null : pos.immutable();
    }

    public static BlockPos selectionStart() {
        return selectionStart;
    }

    public static void sync(ResourceLocation dimension,
            List<FacilityRoomSnapshot> rooms) {
        if (dimension == null) return;
        ROOMS.put(dimension, rooms == null ? List.of() : List.copyOf(rooms));
    }

    public static List<FacilityRoomSnapshot> rooms(ResourceLocation dimension) {
        return dimension == null ? List.of()
                : ROOMS.getOrDefault(dimension, List.of());
    }

    public static FacilityRoomSnapshot roomAt(ResourceLocation dimension,
            BlockPos pos) {
        for (FacilityRoomSnapshot room : rooms(dimension)) {
            if (room.containsColumn(pos)) return room;
        }
        return null;
    }

    public static void clear() {
        selectionStart = null;
        ROOMS.clear();
    }
}
