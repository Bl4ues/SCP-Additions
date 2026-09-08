package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.TeslaTerminalBlockEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Gives dynamic terminal overlays enough depth separation for shader pipelines. */
@Mixin(value = TeslaTerminalBlockEntityRenderer.class, remap = false)
public abstract class TeslaTerminalOverlayDepthMixin {
    @ModifyConstant(method = "render",
            constant = @Constant(doubleValue = 0.0030D), remap = false)
    private double scpclassifieddirective$separateOverlay(double original) {
        return 0.0080D;
    }
}
