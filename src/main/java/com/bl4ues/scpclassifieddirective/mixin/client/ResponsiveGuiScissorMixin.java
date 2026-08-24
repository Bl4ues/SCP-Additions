package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * GuiGraphics scissor rectangles are screen-coordinate based and do not inherit
 * PoseStack scale. Convert responsive virtual coordinates back to the actual GUI
 * canvas so scroll panels and clipped animations stay aligned with their content.
 */
@Mixin(GuiGraphics.class)
public abstract class ResponsiveGuiScissorMixin {
    @ModifyVariable(method = "enableScissor", at = @At("HEAD"),
            argsOnly = true, ordinal = 0)
    private int scpClassifiedDirective$scaleScissorLeft(int value) {
        return ResponsiveUiScale.scissorFloor(value);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"),
            argsOnly = true, ordinal = 1)
    private int scpClassifiedDirective$scaleScissorTop(int value) {
        return ResponsiveUiScale.scissorFloor(value);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"),
            argsOnly = true, ordinal = 2)
    private int scpClassifiedDirective$scaleScissorRight(int value) {
        return ResponsiveUiScale.scissorCeil(value);
    }

    @ModifyVariable(method = "enableScissor", at = @At("HEAD"),
            argsOnly = true, ordinal = 3)
    private int scpClassifiedDirective$scaleScissorBottom(int value) {
        return ResponsiveUiScale.scissorCeil(value);
    }
}
