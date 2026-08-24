package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GuiGraphics scissor rectangles are screen-coordinate based and do not inherit
 * PoseStack scale. Convert responsive virtual coordinates back to the actual GUI
 * canvas so scroll panels and clipped animations stay aligned with their content.
 * Tooltip/layout helpers also need to see the virtual bounds while that canvas is
 * active, otherwise they clamp themselves against the smaller raw GUI surface.
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

    @Inject(method = "guiWidth", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$virtualGuiWidth(
            CallbackInfoReturnable<Integer> cir) {
        ResponsiveUiScale.Context context = ResponsiveUiScale.activeContext();
        if (context != null) cir.setReturnValue(context.virtualWidth());
    }

    @Inject(method = "guiHeight", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$virtualGuiHeight(
            CallbackInfoReturnable<Integer> cir) {
        ResponsiveUiScale.Context context = ResponsiveUiScale.activeContext();
        if (context != null) cir.setReturnValue(context.virtualHeight());
    }
}
