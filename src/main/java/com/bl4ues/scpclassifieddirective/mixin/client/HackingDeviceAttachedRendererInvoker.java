package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceAttachedRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow bridge so the corrected physical screen transform can reuse all UI content. */
@Mixin(value = HackingDeviceAttachedRenderer.class, remap = false)
public interface HackingDeviceAttachedRendererInvoker {
    @Invoker("renderSession")
    static void scpclassifieddirective$renderSession(Font font,
            PoseStack poseStack, MultiBufferSource.BufferSource buffers) {
        throw new AssertionError();
    }
}
