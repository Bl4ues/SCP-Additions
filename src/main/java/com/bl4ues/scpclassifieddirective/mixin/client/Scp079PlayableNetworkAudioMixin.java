package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableAudioClient;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/** Immediate listener feedback for choosing an SCP-079 surveillance room. */
@Mixin(Scp079PlayableNetwork.class)
public abstract class Scp079PlayableNetworkAudioMixin {
    @Inject(method = "requestRoom", at = @At("HEAD"), remap = false)
    private static void scpclassifieddirective$roomSwitch(UUID roomId,
            CallbackInfo ci) {
        if (roomId != null) Scp079PlayableAudioClient.playRoomSwitch();
    }
}
