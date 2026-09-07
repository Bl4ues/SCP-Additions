package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079BootSequenceScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Slows the authored boot timeline uniformly without changing its choreography. */
@Mixin(value = Scp079BootSequenceScreen.class, remap = false)
public abstract class Scp079BootTimingMixin {
    private static final double TIMELINE_SPEED = 2.0D / 3.0D;

    @Inject(method = "elapsedMs", at = @At("RETURN"),
            cancellable = true, remap = false)
    private void scpclassifieddirective$slowBootTimeline(
            CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(Math.round(cir.getReturnValue() * TIMELINE_SPEED));
    }
}
