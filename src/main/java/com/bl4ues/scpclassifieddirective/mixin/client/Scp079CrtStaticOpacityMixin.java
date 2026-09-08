package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraEffectsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps authored alpha-static extremely subtle at rest and prevents short
 * automatic interference bursts from truncating a longer authored camera
 * travel mask that is already covering a cross-floor/cross-zone hand-off.
 */
@Mixin(value = Scp079CameraEffectsClient.class, remap = false)
public abstract class Scp079CrtStaticOpacityMixin {
    private static final long MIN_TRANSITION_NANOS = 300_000_000L;

    @Shadow
    private static long interferenceStartedAt;

    @Shadow
    private static long interferenceUntil;

    @Inject(method = "startTransition", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$neverShortenActiveMask(
            long durationNanos, CallbackInfo ci) {
        if (Scp079CameraEffectsClient.signalEffectActive()) return;
        long now = System.nanoTime();
        if (interferenceStartedAt > 0L && now < interferenceUntil) {
            long requestedUntil = now + Math.max(MIN_TRANSITION_NANOS,
                    durationNanos);
            if (requestedUntil > interferenceUntil) {
                interferenceUntil = requestedUntil;
            }
            // Most importantly, do not let the original method reset a long
            // cross-zone carrier to a later 300 ms mode/feed-change burst.
            ci.cancel();
        }
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(doubleValue = 0.25D), require = 1)
    private static double scpclassifieddirective$idleBase(double original) {
        return 0.015D;
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(doubleValue = 0.032D), require = 1)
    private static double scpclassifieddirective$idleSlow(double original) {
        return 0.0024D;
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(doubleValue = 0.018D), require = 1)
    private static double scpclassifieddirective$idleDrift(double original) {
        return 0.0012D;
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(floatValue = 0.20F), require = 1)
    private static float scpclassifieddirective$idleMinimum(float original) {
        return 0.01F;
    }

    @ModifyConstant(method = "renderCrtStatic",
            constant = @Constant(floatValue = 0.30F), require = 1)
    private static float scpclassifieddirective$idleMaximum(float original) {
        return 0.02F;
    }
}
