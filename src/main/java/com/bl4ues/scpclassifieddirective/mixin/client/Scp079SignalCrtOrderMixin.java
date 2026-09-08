package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraEffectsClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CrtPostProcessor;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Modal no-signal content is composed after the normal SCP-079 HUD. Defer the
 * final CRT pass in that state so the no-signal image itself receives curvature
 * instead of being painted flat on top of an already-curved framebuffer.
 */
@Mixin(value = Scp079CrtPostProcessor.class, remap = false)
public abstract class Scp079SignalCrtOrderMixin {
    @Inject(method = "onRenderGui", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$deferHudCrt(
            RenderGuiEvent.Post event, CallbackInfo ci) {
        if (Scp079CameraEffectsClient.signalEffectActive()) ci.cancel();
    }

    @Inject(method = "onScreenRenderPost", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$deferScreenCrt(
            ScreenEvent.Render.Post event, CallbackInfo ci) {
        if (Scp079CameraEffectsClient.signalEffectActive()) ci.cancel();
    }
}
