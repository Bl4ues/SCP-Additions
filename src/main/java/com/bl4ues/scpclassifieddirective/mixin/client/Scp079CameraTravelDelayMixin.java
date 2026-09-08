package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraTravelDelayClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Defers only camera-to-camera state application for authored travel time. */
@Mixin(value = Scp079PlayableClient.class, remap = false)
public abstract class Scp079CameraTravelDelayMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpclassifieddirective$delayCameraTravel(
            Scp079PlayableNetwork.State state, CallbackInfo ci) {
        if (Scp079CameraTravelDelayClient.intercept(state)) ci.cancel();
    }
}
