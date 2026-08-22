package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.CustomMainMenuScreen;
import com.bl4ues.scpclassifieddirective.client.MainMenuPlayPanelsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CustomMainMenuScreen.class, remap = false)
public abstract class CustomMainMenuPlayPanelsMixin {
    @Inject(method = "buildPrimaryButtons", at = @At("TAIL"))
    private void scpClassifiedDirective$attachPlayPanels(CallbackInfo callback) {
        MainMenuPlayPanelsClient.attach((CustomMainMenuScreen) (Object) this);
    }

    @Inject(method = "sourceAction", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$replacePlayActions(String key,
            CallbackInfoReturnable<Runnable> callback) {
        CustomMainMenuScreen screen = (CustomMainMenuScreen) (Object) this;
        if ("menu.singleplayer".equals(key)) {
            callback.setReturnValue(() ->
                    MainMenuPlayPanelsClient.toggleSingleplayer(screen));
        } else if ("menu.multiplayer".equals(key)) {
            callback.setReturnValue(() -> {
                try {
                    MainMenuPlayPanelsClient.toggleMultiplayer(screen);
                } catch (Exception exception) {
                    ScpClassifiedDirectiveMod.LOGGER.warn(
                            "Could not initialize the custom multiplayer panel; falling back to vanilla",
                            exception);
                    MainMenuPlayPanelsClient.close(screen);
                    Minecraft.getInstance().setScreen(
                            new JoinMultiplayerScreen(screen));
                }
            });
        }
    }

    @Inject(method = "sourceAction", at = @At("RETURN"), cancellable = true)
    private void scpClassifiedDirective$closePlayPanelsForOtherActions(String key,
            CallbackInfoReturnable<Runnable> callback) {
        if ("menu.singleplayer".equals(key) || "menu.multiplayer".equals(key)) {
            return;
        }
        Runnable action = callback.getReturnValue();
        if (action == null) return;
        CustomMainMenuScreen screen = (CustomMainMenuScreen) (Object) this;
        callback.setReturnValue(() -> {
            MainMenuPlayPanelsClient.close(screen);
            action.run();
        });
    }

    @Inject(method = "toggleExtras", at = @At("HEAD"))
    private void scpClassifiedDirective$closePlayPanelsForExtras(CallbackInfo callback) {
        MainMenuPlayPanelsClient.close((CustomMainMenuScreen) (Object) this);
    }

    /* See CustomMainMenuSettingsMixin for why both mapped and SRG names are used. */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/bl4ues/scpclassifieddirective/client/CustomMainMenuScreen;drawTransition(Lnet/minecraft/client/gui/GuiGraphics;J)V"),
            require = 0)
    private void scpClassifiedDirective$renderPlayPanelsMapped(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        scpClassifiedDirective$renderPlayPanels(graphics, mouseX, mouseY);
    }

    @Inject(method = "m_88315_", at = @At(value = "INVOKE",
            target = "Lcom/bl4ues/scpclassifieddirective/client/CustomMainMenuScreen;drawTransition(Lnet/minecraft/client/gui/GuiGraphics;J)V"),
            require = 0)
    private void scpClassifiedDirective$renderPlayPanelsSrg(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        scpClassifiedDirective$renderPlayPanels(graphics, mouseX, mouseY);
    }

    private void scpClassifiedDirective$renderPlayPanels(GuiGraphics graphics,
            int mouseX, int mouseY) {
        MainMenuPlayPanelsClient.render(
                (CustomMainMenuScreen) (Object) this,
                graphics, mouseX, mouseY);
    }
}
