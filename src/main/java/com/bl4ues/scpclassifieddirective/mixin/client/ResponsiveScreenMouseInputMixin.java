package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Converts all Forge screen mouse events into the mod's virtual canvas. */
@Mixin(targets = "net.minecraftforge.client.event.ScreenEvent$MouseInput",
        remap = false)
public abstract class ResponsiveScreenMouseInputMixin {
    @Shadow @Final @Mutable private double mouseX;
    @Shadow @Final @Mutable private double mouseY;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void scpClassifiedDirective$normalizeMouse(Screen screen,
            double mouseX, double mouseY, CallbackInfo callback) {
        if (!ResponsiveUiScale.manages(screen)) return;
        ResponsiveUiScale.Context context = ResponsiveUiScale.current();
        this.mouseX = context.virtualX(mouseX);
        this.mouseY = context.virtualY(mouseY);
    }
}
