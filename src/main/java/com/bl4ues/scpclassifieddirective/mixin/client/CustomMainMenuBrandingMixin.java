package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.CustomMainMenuScreen;
import com.bl4ues.scpclassifieddirective.client.MainMenuBrandingClient;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the compact legacy branding block with the enlarged 4.0 layout. */
@Mixin(value = CustomMainMenuScreen.class, remap = false)
public abstract class CustomMainMenuBrandingMixin {
    @Inject(method = "drawBranding", at = @At("HEAD"), cancellable = true,
            remap = false)
    private void scpClassifiedDirective$drawBranding(
            GuiGraphics graphics, CallbackInfo callback) {
        MainMenuBrandingClient.render(
                (CustomMainMenuScreen) (Object) this, graphics);
        callback.cancel();
    }
}
