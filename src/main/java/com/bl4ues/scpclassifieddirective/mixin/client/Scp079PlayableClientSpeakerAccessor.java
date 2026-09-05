package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the synchronized SCP-079 Speaker state to the replacement camera HUD. */
@Mixin(Scp079PlayableClient.class)
public interface Scp079PlayableClientSpeakerAccessor {
    @Accessor(value = "speakerAvailable", remap = false)
    static boolean scpclassifieddirective$speakerAvailable() {
        throw new AssertionError();
    }

    @Accessor(value = "speakerActive", remap = false)
    static boolean scpclassifieddirective$speakerActive() {
        throw new AssertionError();
    }
}
