package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A camera mounted one block outside an authored floor still belongs to that room. */
@Mixin(FacilityMappingManager.class)
public abstract class FacilityMappingCameraBorderMixin {
    @Redirect(method = "roomForPosition",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/facility/mapping/FacilityRoom;containsColumn(Lnet/minecraft/core/BlockPos;)Z"),
            remap = false)
    private static boolean scpclassifieddirective$includeRoomBorder(
            FacilityRoom room, BlockPos pos) {
        return room.containsColumn(pos)
                || Scp079RoomInteractionPolicy.withinExpandedFloor(room, pos, 1);
    }
}
