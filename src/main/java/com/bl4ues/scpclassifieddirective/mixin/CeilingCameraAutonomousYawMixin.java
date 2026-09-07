package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Corrects the dome model's horizontal handedness for autonomous tracking. */
@Mixin(value = CeilingCameraModule.CeilingCameraBlockEntity.class, remap = false)
public abstract class CeilingCameraAutonomousYawMixin {
    @Redirect(method = "serverTick",
            at = @At(value = "INVOKE",
                    target = "Ljava/lang/Math;atan2(DD)D",
                    ordinal = 0),
            remap = false)
    private static double scpclassifieddirective$mirrorAutonomousYaw(
            double y, double x) {
        return -Math.atan2(y, x);
    }
}
