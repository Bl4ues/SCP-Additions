package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.gui.ScpInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps closing an expanded PDA document silent without muting the PDA power-on cue. */
@Mixin(ScpInventoryScreen.class)
public abstract class InventoryPdaDocumentCloseAudioMixin {
    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/inventory/client/pda/InventoryPdaAudioClient;playPowerBeep()V"),
            remap = false)
    private void scpclassifieddirective$keepDocumentCloseSilent() {
        // The same pda_on cue still belongs to InventoryPdaAudioClient.powerOn().
    }
}
