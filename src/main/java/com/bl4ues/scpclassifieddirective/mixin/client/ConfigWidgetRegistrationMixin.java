package com.bl4ues.scpclassifieddirective.mixin.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import com.bl4ues.scpclassifieddirective.config.ui.ClientPreferenceModulesUi;
import com.bl4ues.scpclassifieddirective.config.ui.VoiceProfileModulesUi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Registers Button-based replacement config widgets without mapped-name reflection. */
@Mixin(value = {
        ClientPreferenceModulesUi.class,
        VoiceProfileModulesUi.class
}, remap = false)
public abstract class ConfigWidgetRegistrationMixin {
    @SuppressWarnings("unchecked")
    @Inject(
            method = "addRenderableWidget(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/components/Button;)V",
            at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$registerButtonWidget(
            Screen screen, Button button, CallbackInfo callback) {
        List<GuiEventListener> children =
                (List<GuiEventListener>) (List<?>) screen.children();
        if (!children.contains(button)) children.add(button);
        callback.cancel();
    }
}
