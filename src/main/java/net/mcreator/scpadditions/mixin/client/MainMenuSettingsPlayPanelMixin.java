package net.mcreator.scpadditions.mixin.client;

import net.mcreator.scpadditions.client.CustomMainMenuScreen;
import net.mcreator.scpadditions.client.MainMenuPlayPanelsClient;
import net.mcreator.scpadditions.client.MainMenuSettingsPanelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opening Settings must dismiss the play flyout instead of stacking both panels. */
@Mixin(value = MainMenuSettingsPanelClient.class, remap = false)
public abstract class MainMenuSettingsPlayPanelMixin {
    @Inject(method = "closeExtras", at = @At("HEAD"))
    private static void scpAdditions$closePlayPanelWithSettings(
            CustomMainMenuScreen screen, CallbackInfo callback) {
        MainMenuPlayPanelsClient.close(screen);
    }
}
