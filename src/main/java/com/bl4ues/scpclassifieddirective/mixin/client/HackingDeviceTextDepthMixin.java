package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceAttachedRenderer;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceScreenTextClient;
import com.bl4ues.scpclassifieddirective.client.ScpFonts;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The CRT layer already has a real world-space depth offset. Do not ask
 * POLYGON_OFFSET to solve a second time what shaders routinely make worse.
 */
@Mixin(value = HackingDeviceAttachedRenderer.class, remap = false)
public abstract class HackingDeviceTextDepthMixin {
    @Inject(method = "centered", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$centeredNormalDepth(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            String text, float y, int color, CallbackInfo ci) {
        ci.cancel();
        var sequence = ScpFonts.anonymousPro(text).getVisualOrderText();
        float x = (HackingDeviceScreenTextClient.LOGICAL_WIDTH
                - font.width(sequence)) * 0.5F;
        font.drawInBatch(sequence, x, y, color, false,
                poseStack.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$drawNormalDepth(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            String text, float x, float y, int color, CallbackInfo ci) {
        ci.cancel();
        var sequence = ScpFonts.anonymousPro(text).getVisualOrderText();
        font.drawInBatch(sequence, x, y, color, false,
                poseStack.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
    }
}
