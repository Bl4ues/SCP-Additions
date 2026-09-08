package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Keeps timed travel from being followed by the old fixed switch glitch. */
@Mixin(value = Scp079PlayableClient.class, remap = false)
public interface Scp079PlayableClientTravelAccessor {
    @Accessor(value = "interferenceUntil", remap = false)
    static void scpclassifieddirective$setInterferenceUntil(long value) {
        throw new AssertionError();
    }
}
