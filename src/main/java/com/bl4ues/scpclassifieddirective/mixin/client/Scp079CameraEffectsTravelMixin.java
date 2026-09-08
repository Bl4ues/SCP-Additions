package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraEffectsClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraTravelDelayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses the automatic short glitch immediately after timed travel ends. */
@Mixin(value = Scp079CameraEffectsClient.class, remap = false)
public abstract class Scp079CameraEffectsTravelMixin {
    @Inject(method = "startTransition", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpclassifieddirective$avoidArrivalRetrigger(
            long durationNanos, CallbackInfo ci) {
        if (Scp079CameraTravelDelayClient.suppressAutomaticTransition()) {
            ci.cancel();
        }
    }
}
