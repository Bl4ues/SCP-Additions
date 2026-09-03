package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.config.ui.CrosshairModulesPlacement;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Registers AbstractButton-based Crosshair replacement widgets. */
@Mixin(value = CrosshairModulesPlacement.class, remap = false)
public abstract class CrosshairConfigWidgetRegistrationMixin {
    @SuppressWarnings("unchecked")
    @Inject(
            method = "addRenderableWidget(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/components/AbstractButton;)V",
            at = @At("HEAD"), cancellable = true)
    private static void scpClassifiedDirective$registerAbstractButtonWidget(
            Screen screen, AbstractButton button, CallbackInfo callback) {
        List<GuiEventListener> children =
                (List<GuiEventListener>) (List<?>) screen.children();
        if (!children.contains(button)) children.add(button);
        callback.cancel();
    }
}
