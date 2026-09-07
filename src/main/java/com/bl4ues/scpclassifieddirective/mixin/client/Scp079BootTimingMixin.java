package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079BootSequenceScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retimes the authored boot choreography without changing its staged events. */
@Mixin(value = Scp079BootSequenceScreen.class, remap = false)
public abstract class Scp079BootTimingMixin {
    // The previous 2/3 speed pass overshot the requested slowdown. 13/15 is
    // roughly 30% faster than that revision while remaining slower than the
    // original boot sequence.
    private static final double TIMELINE_SPEED = 13.0D / 15.0D;

    @Inject(method = "elapsedMs", at = @At("RETURN"),
            cancellable = true, remap = false)
    private void scpclassifieddirective$retimeBootTimeline(
            CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(Math.round(cir.getReturnValue() * TIMELINE_SPEED));
    }
}
