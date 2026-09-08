package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraEffectsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the CRT surface after no-signal/static content has been composed. */
@Mixin(value = Scp079CameraEffectsClient.class, remap = false)
public abstract class Scp079SignalCrtComposeMixin {
    @Inject(method = "renderComposite", at = @At("TAIL"))
    private static void scpclassifieddirective$curveSignalLoss(
            GuiGraphics graphics, int width, int height, CallbackInfo ci) {
        if (!Scp079CameraEffectsClient.signalEffectActive()) return;
        Scp079CrtPostProcessorInvoker.scpclassifieddirective$apply(
                Minecraft.getInstance(), graphics, 0.0F, false);
    }
}
