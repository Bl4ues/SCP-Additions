package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraNetworkClientState;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapNetworkOverlay;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds SCP-079-only network coverage and the physical host marker to its map. */
@Mixin(Scp079FacilityMapScreen.class)
public abstract class Scp079FacilityMapNetworkMixin {
    @Shadow(remap = false) private int floorIndex;
    @Shadow(remap = false) private boolean leaveConfirmation;
    @Shadow(remap = false) private double mapZoom;
    @Shadow(remap = false) private double panX;
    @Shadow(remap = false) private double panY;
    @Shadow(remap = false) private FacilityRoomSnapshot pressedRoom;

    @Inject(method = "mouseReleased", at = @At("HEAD"), remap = false)
    private void scpclassifieddirective$rejectOfflineRoomClick(
            double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && pressedRoom != null
                && Scp079PlayableClient.active()
                && !Scp079CameraNetworkClientState.hasCamera(pressedRoom.id())) {
            pressedRoom = null;
        }
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void scpclassifieddirective$renderNetworkCoverage(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!Scp079PlayableClient.active() || leaveConfirmation) return;
        Screen screen = (Screen) (Object) this;
        Scp079FacilityMapNetworkOverlay.render(graphics, floorIndex,
                mapZoom, panX, panY, screen.width, screen.height);
    }
}
