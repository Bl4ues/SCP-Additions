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

    @Inject(method = "sourceAction", at = @At("RETURN"), cancellable = true)
    private void scpAdditions$closePlayPanelsForOtherActions(String key,
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
    private void scpAdditions$closePlayPanelsForExtras(CallbackInfo callback) {
        MainMenuPlayPanelsClient.close((CustomMainMenuScreen) (Object) this);
    }

    /* See CustomMainMenuSettingsMixin for why both mapped and SRG names are used. */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/scpadditions/client/CustomMainMenuScreen;drawTransition(Lnet/minecraft/client/gui/GuiGraphics;J)V"),
            require = 0)
    private void scpAdditions$renderPlayPanelsMapped(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        scpAdditions$renderPlayPanels(graphics, mouseX, mouseY);
    }

    @Inject(method = "m_88315_", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/scpadditions/client/CustomMainMenuScreen;drawTransition(Lnet/minecraft/client/gui/GuiGraphics;J)V"),
            require = 0)
    private void scpAdditions$renderPlayPanelsSrg(GuiGraphics graphics,
            int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        scpAdditions$renderPlayPanels(graphics, mouseX, mouseY);
    }

    private void scpAdditions$renderPlayPanels(GuiGraphics graphics,
            int mouseX, int mouseY) {
        MainMenuPlayPanelsClient.render(
                (CustomMainMenuScreen) (Object) this,
                graphics, mouseX, mouseY);
    }
}
