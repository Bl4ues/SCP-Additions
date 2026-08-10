package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.mcreator.scpadditions.client.CustomMainMenuScreen;
import net.mcreator.scpadditions.client.MainMenuPlayPanelsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CustomMainMenuScreen.class, remap = false)
public abstract class CustomMainMenuPlayPanelsMixin {
    @Inject(method = "buildPrimaryButtons", at = @At("TAIL"))
    private void scpAdditions$attachPlayPanels(CallbackInfo callback) {
        MainMenuPlayPanelsClient.attach((CustomMainMenuScreen) (Object) this);
    }

    @Inject(method = "sourceAction", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$replacePlayActions(String key,
            CallbackInfoReturnable<Runnable> callback) {
        CustomMainMenuScreen screen = (CustomMainMenuScreen) (Object) this;
        if ("menu.singleplayer".equals(key)) {
            callback.setReturnValue(() ->
                    MainMenuPlayPanelsClient.toggleSingleplayer(screen));
        } else if ("menu.multiplayer".equals(key)) {
            callback.setReturnValue(() ->
                    MainMenuPlayPanelsClient.toggleMultiplayer(screen));
        }
    }

    @Inject(method = "toggleExtras", at = @At("HEAD"))
    private void scpAdditions$closePlayPanelsForExtras(CallbackInfo callback) {
        MainMenuPlayPanelsClient.close((CustomMainMenuScreen) (Object) this);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/scpadditions/client/CustomMainMenuScreen;drawTransition(Lnet/minecraft/client/gui/GuiGraphics;J)V"))
    private void scpAdditions$renderPlayPanels(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        MainMenuPlayPanelsClient.render(
                (CustomMainMenuScreen) (Object) this,
                graphics, mouseX, mouseY);
    }
}
