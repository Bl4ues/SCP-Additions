package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraEffectsClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079HostFailureAudioClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Arms the silent death-audio window together with the host no-signal effect. */
@Mixin(value = Scp079CameraEffectsClient.class, remap = false)
public abstract class Scp079HostFailureAudioMixin {
    @Inject(method = "beginSignalInterruption", at = @At("HEAD"))
    private static void scpclassifieddirective$armHostFailureAudioGuard(
            int kind, int durationTicks, CallbackInfo ci) {
        if (kind == Scp079CameraEffectsClient.SIGNAL_HOST_DESTROYED) {
            Scp079HostFailureAudioClient.arm(durationTicks);
        }
    }
}
