package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps drag speed consistent after responsive screen scaling. */
@Mixin(value = ScreenEvent.MouseDragged.class, remap = false)
public abstract class ResponsiveScreenMouseDragMixin {
    @Shadow @Final @Mutable private double dragX;
    @Shadow @Final @Mutable private double dragY;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void scpClassifiedDirective$normalizeDrag(Screen screen,
            double mouseX, double mouseY, int mouseButton,
            double dragX, double dragY, CallbackInfo callback) {
        if (!ResponsiveUiScale.manages(screen)) return;
        ResponsiveUiScale.Context context = ResponsiveUiScale.current();
        this.dragX = context.virtualDelta(dragX);
        this.dragY = context.virtualDelta(dragY);
    }
}
