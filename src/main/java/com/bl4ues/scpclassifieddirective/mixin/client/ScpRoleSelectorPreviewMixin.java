package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.ScpRoleSelectorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps playable-SCP artwork inside the authored preview pane. SCP-079's source
 * image is already framed for the selector, so it should not receive the older
 * aggressive 2.18x crop/zoom or spill across the card border.
 */
@Mixin(ScpRoleSelectorScreen.class)
public abstract class ScpRoleSelectorPreviewMixin {
    @ModifyConstant(method = "<clinit>",
            constant = @Constant(floatValue = 2.18F), remap = false)
    private static float scpclassifieddirective$fitScp079Preview(float original) {
        return 1.0F;
    }

    @Redirect(method = "renderCard",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;enableScissor(IIII)V"))
    private void scpclassifieddirective$clipPreviewToPane(GuiGraphics graphics,
            int x1, int y1, int x2, int y2) {
        graphics.enableScissor(x1, y1, x2, y2);
    }

    @Redirect(method = "renderCard",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;disableScissor()V"))
    private void scpclassifieddirective$restorePreviewScissor(
            GuiGraphics graphics) {
        graphics.disableScissor();
    }

    @Redirect(method = "drawCenteredPreview",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"))
    private void scpclassifieddirective$drawExactPreview(GuiGraphics graphics,
            ResourceLocation texture, int x, int y, int width, int height,
            float sourceX, float sourceY, int sourceWidth, int sourceHeight,
            int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, width, height,
                sourceX, sourceY, sourceWidth, sourceHeight,
                textureWidth, textureHeight);
    }
}
