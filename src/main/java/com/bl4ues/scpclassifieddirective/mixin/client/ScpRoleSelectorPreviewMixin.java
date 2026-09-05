package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.ScpRoleSelectorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Standard playable-SCP card preview policy: use each role's authored crop/zoom,
 * render it prominently and permit controlled overflow beyond the preview pane.
 */
@Mixin(ScpRoleSelectorScreen.class)
public abstract class ScpRoleSelectorPreviewMixin {
    private static final float DESTINATION_SCALE = 1.16F;

    @Redirect(method = "renderCard",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;enableScissor(IIII)V"))
    private void scpclassifieddirective$allowPreviewOverflow(GuiGraphics graphics,
            int x1, int y1, int x2, int y2) {
        // Intentionally un-clipped. SCP artwork may extend beyond the nominal
        // preview rectangle while the card border/text remain the visual frame.
    }

    @Redirect(method = "renderCard",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;disableScissor()V"))
    private void scpclassifieddirective$leavePreviewOverflowEnabled(
            GuiGraphics graphics) {
        // Paired with the no-op enableScissor redirect above.
    }

    @Redirect(method = "drawCenteredPreview",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
    private void scpclassifieddirective$prominentPreview(GuiGraphics graphics,
            ResourceLocation texture, int x, int y, int width, int height,
            float sourceX, float sourceY, int sourceWidth, int sourceHeight,
            int textureWidth, int textureHeight) {
        int drawWidth = Math.max(1, Math.round(width * DESTINATION_SCALE));
        int drawHeight = Math.max(1, Math.round(height * DESTINATION_SCALE));
        int drawX = x - (drawWidth - width) / 2;
        int drawY = y - (drawHeight - height) / 2;
        graphics.blit(texture, drawX, drawY, drawWidth, drawHeight,
                sourceX, sourceY, sourceWidth, sourceHeight,
                textureWidth, textureHeight);
    }
}
