package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.pda.InventoryPdaPresentationRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Preserves shader-pack world depth while drawing the camera-space PDA.
 *
 * Clearing depth made deferred packs forget that walls had already occluded the
 * sky, which is why BSL clouds appeared through solid geometry as soon as the
 * inventory opened. Restoring a saved copy afterwards fixed the walls but also
 * erased the PDA's own depth, allowing late shader composition to paint clouds
 * over the device. Keep the real depth buffer instead and reserve a tiny near
 * slice for the first-person PDA so both the world and the device remain valid
 * occluders for the rest of the frame.
 */
@Mixin(InventoryPdaPresentationRenderer.class)
public abstract class InventoryPdaDepthPreservationMixin {
    private static final double PDA_DEPTH_FAR = 0.08D;

    @Redirect(method = "renderPhysical",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"),
            remap = false)
    private void scpclassifieddirective$preserveWorldDepth(
            int mask, boolean getError) {
        // Do not clear the shader pack's world depth. The PDA's fixed camera
        // projection is mapped into the nearest 8% of the depth range instead.
        GL11.glDepthRange(0.0D, PDA_DEPTH_FAR);
    }

    @Inject(method = "renderPhysical", at = @At("RETURN"), remap = false)
    private void scpclassifieddirective$restoreDepthRange(
            InventoryPdaPresentationRenderer.Pose pose, int packedLight,
            int guiWidth, int guiHeight, boolean renderHands,
            CallbackInfo ci) {
        GL11.glDepthRange(0.0D, 1.0D);
    }

    /**
     * The unlit GUI texture is already constrained to the authored screen quad.
     * It is composited after the physical shell and must not test against the
     * ordinary world depth that the PDA now deliberately preserves.
     */
    @Redirect(method = "renderDisplayQuad",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V"),
            remap = false)
    private void scpclassifieddirective$keepDisplayOffWorldDepth() {
        RenderSystem.disableDepthTest();
    }
}
