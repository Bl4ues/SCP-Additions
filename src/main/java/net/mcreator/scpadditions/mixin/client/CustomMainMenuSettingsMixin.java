package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.mcreator.scpadditions.client.CustomMainMenuScreen;
import net.mcreator.scpadditions.client.MainMenuSettingsPanelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Integrates the compact Settings flyout into the custom title screen. */
@Mixin(value = CustomMainMenuScreen.class, remap = false)
public abstract class CustomMainMenuSettingsMixin {
    @Inject(method = "buildPrimaryButtons", at = @At("TAIL"))
    private void scpAdditions$attachSettings(CallbackInfo callback) {
        MainMenuSettingsPanelClient.attach(
                (CustomMainMenuScreen) (Object) this);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/scpadditions/client/CustomMainMenuScreen;drawTransition(Lnet/minecraft/client/gui/GuiGraphics;J)V"))
    private void scpAdditions$renderSettings(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        MainMenuSettingsPanelClient.render(
                (CustomMainMenuScreen) (Object) this,
                graphics, mouseX, mouseY);
    }
}
