package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraEffectsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes the existing surveillance interference without duplicating it. */
@Mixin(value = Scp079CameraEffectsClient.class, remap = false)
public interface Scp079CameraEffectsClientInvoker {
    @Invoker(value = "startTransition", remap = false)
    static void scpclassifieddirective$startTransition(long durationNanos) {
        throw new AssertionError();
    }
}
