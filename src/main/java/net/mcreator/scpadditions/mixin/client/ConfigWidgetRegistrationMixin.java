package net.mcreator.scpadditions.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.mcreator.scpadditions.config.ui.ClientPreferenceModulesUi;
import net.mcreator.scpadditions.config.ui.CrosshairModulesPlacement;
import net.mcreator.scpadditions.config.ui.VoiceProfileModulesUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers replacement config widgets through a remapped Screen invoker. */
@Mixin(value = {
        ClientPreferenceModulesUi.class,
        CrosshairModulesPlacement.class,
        VoiceProfileModulesUi.class
}, remap = false)
public abstract class ConfigWidgetRegistrationMixin {
    @Inject(method = "addRenderableWidget", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$registerReplacementWidget(
            Screen screen, Button button, CallbackInfo callback) {
        ((ScreenInvoker) (Object) screen)
                .scpAdditions$invokeAddRenderableWidget(button);
        callback.cancel();
    }
}
