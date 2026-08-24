package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScaleEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets Forge's complete screen render dispatch, including every Render.Pre
 * and Render.Post listener, inside the responsive virtual-canvas transform.
 */
@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class ResponsiveScreenForgeHooksMixin {
    @Inject(method = "drawScreenInternal", at = @At("HEAD"))
    private static void scpClassifiedDirective$beginResponsiveScreen(
            Screen screen, GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo callback) {
        ResponsiveUiScaleEvents.beginRender(screen, graphics);
    }

    @Inject(method = "drawScreenInternal", at = @At("RETURN"))
    private static void scpClassifiedDirective$endResponsiveScreen(
            Screen screen, GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo callback) {
        ResponsiveUiScaleEvents.endRender(screen, graphics);
    }
}
