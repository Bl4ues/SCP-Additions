package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only state needed by custom SCP-079 map overlays and button audio. */
@Mixin(Scp079FacilityMapScreen.class)
public interface Scp079FacilityMapScreenAccessor {
    @Accessor(value = "leaveConfirmation", remap = false)
    boolean scpclassifieddirective$isLeaveConfirmationOpen();

    @Accessor(value = "floorMenuOpen", remap = false)
    boolean scpclassifieddirective$isFloorMenuOpen();

    @Accessor(value = "hoveredRoom", remap = false)
    FacilityRoomSnapshot scpclassifieddirective$hoveredRoom();
}
