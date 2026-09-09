package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceAttachedRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compatibility bridge for the Hacking Device UI mixins.
 *
 * Font.drawInBatch is intentionally no longer used here. All characters are
 * emitted by HackingDevicePixelFont as opaque full-bright quads on the physical
 * CRT, so mixin-injected boot/puzzle lines cannot accidentally fall back to the
 * shader-sensitive vanilla font pass.
 */
@Mixin(value = HackingDeviceAttachedRenderer.class, remap = false)
public abstract class HackingDeviceTextDepthMixin {
    @Inject(method = "centered", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$centeredPhysical(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            String text, float y, int color, CallbackInfo ci) {
        HackingDeviceAttachedRenderer.pixelCentered(text, y, color);
        ci.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$drawPhysical(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            String text, float x, float y, int color, CallbackInfo ci) {
        HackingDeviceAttachedRenderer.pixelDraw(text, x, y, color);
        ci.cancel();
    }
}
