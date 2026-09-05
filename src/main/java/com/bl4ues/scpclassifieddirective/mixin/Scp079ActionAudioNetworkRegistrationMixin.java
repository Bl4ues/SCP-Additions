package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.network.Scp079ActionAudioNetwork;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the SCP-079 feedback packet beside the playable network registration. */
@Mixin(Scp079PlayableNetwork.class)
public abstract class Scp079ActionAudioNetworkRegistrationMixin {
    @Inject(method = "register", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$registerActionAudio(
            CallbackInfo ci) {
        Scp079ActionAudioNetwork.register();
    }
}
