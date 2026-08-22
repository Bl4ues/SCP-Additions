package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import com.bl4ues.scpclassifieddirective.client.CustomMainMenuScreen;
import com.bl4ues.scpclassifieddirective.client.MainMenuWhatsNewPanelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the oversized prototype What's New block with the compact scrollable panel. */
@Mixin(value = CustomMainMenuScreen.class, remap = false)
public abstract class CustomMainMenuWhatsNewMixin {
    @Inject(method = "drawWhatsNew", at = @At("HEAD"), cancellable = true,
            remap = false)
    private void scpClassifiedDirective$drawCompactWhatsNew(
            GuiGraphics graphics, CallbackInfo callback) {
        MainMenuWhatsNewPanelClient.render(
                (CustomMainMenuScreen) (Object) this, graphics);
        callback.cancel();
    }
}
