package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Converts Forge screen-render mouse coordinates into the mod's virtual canvas. */
@Mixin(value = ScreenEvent.Render.class, remap = false)
public abstract class ResponsiveScreenRenderEventMixin {
    @Shadow @Final @Mutable private int mouseX;
    @Shadow @Final @Mutable private int mouseY;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void scpClassifiedDirective$normalizeRenderMouse(Screen screen,
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            CallbackInfo callback) {
        if (!ResponsiveUiScale.manages(screen)) return;
        ResponsiveUiScale.Context context = ResponsiveUiScale.current();
        this.mouseX = context.virtualX(mouseX);
        this.mouseY = context.virtualY(mouseY);
    }
}
