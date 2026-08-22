package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.CustomMainMenuScreen;
import com.bl4ues.scpclassifieddirective.client.MainMenuPlayPanelsClient;
import com.bl4ues.scpclassifieddirective.client.MainMenuSettingsPanelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opening Settings must dismiss the play flyout instead of stacking both panels. */
@Mixin(value = MainMenuSettingsPanelClient.class, remap = false)
public abstract class MainMenuSettingsPlayPanelMixin {
    @Inject(method = "closeExtras", at = @At("HEAD"))
    private static void scpClassifiedDirective$closePlayPanelWithSettings(
            CustomMainMenuScreen screen, CallbackInfo callback) {
        MainMenuPlayPanelsClient.close(screen);
    }
}
