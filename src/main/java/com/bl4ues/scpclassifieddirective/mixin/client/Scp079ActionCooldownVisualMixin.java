package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079ActionCooldownClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Visually marks device actions that are still inside the 079 debounce window. */
@Mixin(Scp079PlayableVisualsV2.class)
public abstract class Scp079ActionCooldownVisualMixin {
    @Redirect(method = "renderPrompts",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079UiTheme;blitIcon64(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;IIIFFFF)V"),
            remap = false)
    private static void scpclassifieddirective$dimCoolingAction(
            GuiGraphics graphics, ResourceLocation icon, int x, int y, int size,
            float r, float g, float b, float a) {
        float brightness = Scp079ActionCooldownClient.iconBrightness(icon);
        Scp079UiTheme.blitIcon64(graphics, icon, x, y, size,
                r * brightness, g * brightness, b * brightness,
                brightness < 1.0F ? a * 0.72F : a);
    }
}
