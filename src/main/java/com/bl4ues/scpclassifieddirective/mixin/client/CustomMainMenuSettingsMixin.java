package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import com.bl4ues.scpclassifieddirective.client.CustomMainMenuScreen;
import com.bl4ues.scpclassifieddirective.client.MainMenuSettingsPanelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Integrates the compact Settings flyout into the custom title screen. */
@Mixin(value = CustomMainMenuScreen.class, remap = false)
public abstract class CustomMainMenuSettingsMixin {
    @Inject(method = "buildPrimaryButtons", at = @At("TAIL"))
    private void scpClassifiedDirective$attachSettings(CallbackInfo callback) {
        MainMenuSettingsPanelClient.attach(
                (CustomMainMenuScreen) (Object) this);
    }

    /*
     * CustomMainMenuScreen#render overrides a Minecraft method. Forge reobfuscates
     * that override to its SRG name in production jars, while development runs
     * retain the mapped name. Because this mixin intentionally does not remap
     * our custom class/methods, support both runtime names explicitly.
     */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/bl4ues/scpclassifieddirective/client/CustomMainMenuScreen;drawTransition(Lnet/minecraft/client/gui/GuiGraphics;J)V"),
            require = 0)
    private void scpClassifiedDirective$renderSettingsMapped(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        scpClassifiedDirective$renderSettings(graphics, mouseX, mouseY);
    }

    @Inject(method = "m_88315_", at = @At(value = "INVOKE",
            target = "Lcom/bl4ues/scpclassifieddirective/client/CustomMainMenuScreen;drawTransition(Lnet/minecraft/client/gui/GuiGraphics;J)V"),
            require = 0)
    private void scpClassifiedDirective$renderSettingsSrg(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        scpClassifiedDirective$renderSettings(graphics, mouseX, mouseY);
    }

    private void scpClassifiedDirective$renderSettings(GuiGraphics graphics,
            int mouseX, int mouseY) {
        MainMenuSettingsPanelClient.render(
                (CustomMainMenuScreen) (Object) this,
                graphics, mouseX, mouseY);
    }
}
