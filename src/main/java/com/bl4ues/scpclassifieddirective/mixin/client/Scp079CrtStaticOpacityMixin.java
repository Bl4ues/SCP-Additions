package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraEffectsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Keeps the authored alpha-static subtle during normal SCP-079 operation. */
@Mixin(value = Scp079CameraEffectsClient.class, remap = false)
public abstract class Scp079CrtStaticOpacityMixin {
    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(doubleValue = 0.25D), require = 1)
    private static double scpclassifieddirective$idleBase(double original) {
        return 0.075D;
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(doubleValue = 0.032D), require = 1)
    private static double scpclassifieddirective$idleSlow(double original) {
        return 0.012D;
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(doubleValue = 0.018D), require = 1)
    private static double scpclassifieddirective$idleDrift(double original) {
        return 0.006D;
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(floatValue = 0.20F), require = 1)
    private static float scpclassifieddirective$idleMinimum(float original) {
        return 0.05F;
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(floatValue = 0.30F), require = 1)
    private static float scpclassifieddirective$idleMaximum(float original) {
        return 0.10F;
    }
}
