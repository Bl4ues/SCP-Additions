package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.ScpRoleSelectorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Fits authored 16:9 role images inside cards instead of cropping into the SCP. */
@Mixin(ScpRoleSelectorScreen.class)
public abstract class ScpRoleSelectorPreviewMixin {
    private static final float PREVIEW_ASPECT = 1920.0F / 1080.0F;

    @Redirect(method = "drawCenteredPreview",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
    private void scpclassifieddirective$fitFullPreview(GuiGraphics graphics,
            ResourceLocation texture, int x, int y, int width, int height,
            float sourceX, float sourceY, int sourceWidth, int sourceHeight,
            int textureWidth, int textureHeight) {
        int drawWidth = width;
        int drawHeight = Math.max(1, Math.round(drawWidth / PREVIEW_ASPECT));
        if (drawHeight > height) {
            drawHeight = height;
            drawWidth = Math.max(1, Math.round(drawHeight * PREVIEW_ASPECT));
        }
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        graphics.blit(texture, drawX, drawY, drawWidth, drawHeight,
                0.0F, 0.0F, textureWidth, textureHeight,
                textureWidth, textureHeight);
    }
}
