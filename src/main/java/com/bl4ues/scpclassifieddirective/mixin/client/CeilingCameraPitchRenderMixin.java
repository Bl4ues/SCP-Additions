package com.bl4ues.scpclassifieddirective.mixin.client;

import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Blockbench/Gecko's authored dome X axis is opposite the vanilla pitch axis.
 * Gameplay pitch is already correct; mirror only the physical bone rotation.
 */
@Mixin(targets = "com.bl4ues.scpclassifieddirective.client.CeilingCameraClient$BlockModel",
        remap = false)
public abstract class CeilingCameraPitchRenderMixin {
    @Redirect(method = "setCustomAnimations",
            at = @At(value = "INVOKE",
                    target = "Lsoftware/bernie/geckolib/core/animatable/model/CoreGeoBone;setRotX(F)V"),
            remap = false)
    private void scpclassifieddirective$correctDomePitch(CoreGeoBone bone,
            float radians) {
        bone.setRotX(-radians);
    }
}
