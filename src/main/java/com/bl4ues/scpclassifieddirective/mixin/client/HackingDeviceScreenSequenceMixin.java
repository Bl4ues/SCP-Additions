package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceAttachedRenderer;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceScreenTextClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Owns the final terminal commit/countdown without duplicating the physical CRT. */
@Mixin(value = HackingDeviceAttachedRenderer.class, remap = false)
public abstract class HackingDeviceScreenSequenceMixin {
    @Inject(method = "renderSession", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$renderFinalSequence(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers,
            CallbackInfo ci) {
        HackingDeviceMinigameClient.Phase phase =
                HackingDeviceMinigameClient.phase();
        if (phase == HackingDeviceMinigameClient.Phase.SUCCESS) {
            HackingDeviceScreenTextClient.renderSuccess(font, poseStack, buffers);
            ci.cancel();
        } else if (phase == HackingDeviceMinigameClient.Phase.COOLDOWN) {
            HackingDeviceScreenTextClient.renderAttachedCooldown(font, poseStack,
                    buffers);
            ci.cancel();
        }
    }
}
