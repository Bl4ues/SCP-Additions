package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/** Uses precise authored polygons for SCP-079 map occupancy and hover tests. */
@Mixin(value = Scp079FacilityMapScreen.class, remap = false)
public abstract class Scp079FacilityMapFineGeometryMixin {
    @Inject(method = "roomCells", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpClassifiedDirective$preciseRoomCells(
            FacilityRoomSnapshot room, CallbackInfoReturnable<Set<Long>> cir) {
        if (room == null || room.patches().stream().noneMatch(
                FacilityFloorPatch::isPolygon)) return;
        Set<Long> cells = new HashSet<>();
        for (FacilityFloorPatch patch : room.patches()) {
            for (int x = patch.minX(); x <= patch.maxX(); x++) {
                for (int z = patch.minZ(); z <= patch.maxZ(); z++) {
                    if (patch.containsXZ(x + 0.5D, z + 0.5D)) {
                        cells.add(pack(x, z));
                    }
                }
            }
        }
        cir.setReturnValue(cells);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
