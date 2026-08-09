package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.mcreator.scpadditions.client.CustomMainMenuScreen;
import net.mcreator.scpadditions.client.MainMenuWhatsNewPanelClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the oversized prototype What's New block with the compact scrollable panel. */
@Mixin(CustomMainMenuScreen.class)
public abstract class CustomMainMenuWhatsNewMixin {
    @Inject(method = "drawWhatsNew", at = @At("HEAD"), cancellable = true)
    private void scpAdditions$drawCompactWhatsNew(
            GuiGraphics graphics, CallbackInfo callback) {
        MainMenuWhatsNewPanelClient.render(
                (CustomMainMenuScreen) (Object) this, graphics);
        callback.cancel();
    }
}
