package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.pda.InventoryPdaPresentationRenderer;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the physical PDA's private depth pass from erasing shader-pack world
 * depth. Deferred packs such as BSL still need that depth later in the frame to
 * reject sky/cloud composition behind already-rendered walls.
 */
@Mixin(InventoryPdaPresentationRenderer.class)
public abstract class InventoryPdaDepthPreservationMixin {
    @Unique
    private TextureTarget scpclassifieddirective$pdaDepthBackup;
    @Unique
    private int scpclassifieddirective$savedReadFramebuffer;
    @Unique
    private int scpclassifieddirective$savedDrawFramebuffer;
    @Unique
    private int scpclassifieddirective$depthWidth;
    @Unique
    private int scpclassifieddirective$depthHeight;
    @Unique
    private boolean scpclassifieddirective$depthSaved;

    @Inject(method = "renderPhysical", at = @At("HEAD"), remap = false)
    private void scpclassifieddirective$saveWorldDepth(
            InventoryPdaPresentationRenderer.Pose pose, int packedLight,
            int guiWidth, int guiHeight, boolean renderHands,
            CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (width <= 0 || height <= 0) return;

        scpclassifieddirective$savedReadFramebuffer = GL11.glGetInteger(
                GL30.GL_READ_FRAMEBUFFER_BINDING);
        scpclassifieddirective$savedDrawFramebuffer = GL11.glGetInteger(
                GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        if (scpclassifieddirective$savedDrawFramebuffer <= 0) return;

        scpclassifieddirective$ensureDepthTarget(width, height);
        if (scpclassifieddirective$pdaDepthBackup == null) return;

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                scpclassifieddirective$savedDrawFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                scpclassifieddirective$pdaDepthBackup.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, width, height,
                0, 0, scpclassifieddirective$pdaDepthBackup.width,
                scpclassifieddirective$pdaDepthBackup.height,
                GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                scpclassifieddirective$savedReadFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                scpclassifieddirective$savedDrawFramebuffer);
        scpclassifieddirective$depthSaved = true;
    }

    @Inject(method = "renderPhysical", at = @At("RETURN"), remap = false)
    private void scpclassifieddirective$restoreWorldDepth(
            InventoryPdaPresentationRenderer.Pose pose, int packedLight,
            int guiWidth, int guiHeight, boolean renderHands,
            CallbackInfo ci) {
        if (!scpclassifieddirective$depthSaved
                || scpclassifieddirective$pdaDepthBackup == null) return;
        scpclassifieddirective$depthSaved = false;

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                scpclassifieddirective$pdaDepthBackup.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                scpclassifieddirective$savedDrawFramebuffer);
        GL30.glBlitFramebuffer(0, 0,
                scpclassifieddirective$pdaDepthBackup.width,
                scpclassifieddirective$pdaDepthBackup.height,
                0, 0, scpclassifieddirective$depthWidth,
                scpclassifieddirective$depthHeight,
                GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                scpclassifieddirective$savedReadFramebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                scpclassifieddirective$savedDrawFramebuffer);
    }

    /**
     * The shell's temporary camera-space depth is intentionally gone by the GUI
     * composite. The display is already constrained to the authored bezel, so
     * it must not test against the restored world depth.
     */
    @Redirect(method = "renderDisplayQuad",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V"),
            remap = false)
    private void scpclassifieddirective$keepDisplayOffWorldDepth() {
        RenderSystem.disableDepthTest();
    }

    @Inject(method = "close", at = @At("RETURN"), remap = false)
    private void scpclassifieddirective$releaseDepthBackup(CallbackInfo ci) {
        if (scpclassifieddirective$pdaDepthBackup != null) {
            scpclassifieddirective$pdaDepthBackup.destroyBuffers();
            scpclassifieddirective$pdaDepthBackup = null;
        }
        scpclassifieddirective$depthWidth = -1;
        scpclassifieddirective$depthHeight = -1;
        scpclassifieddirective$depthSaved = false;
    }

    @Unique
    private void scpclassifieddirective$ensureDepthTarget(int width, int height) {
        if (scpclassifieddirective$pdaDepthBackup == null) {
            scpclassifieddirective$pdaDepthBackup = new TextureTarget(
                    width, height, true, Minecraft.ON_OSX);
            scpclassifieddirective$depthWidth = width;
            scpclassifieddirective$depthHeight = height;
        } else if (scpclassifieddirective$depthWidth != width
                || scpclassifieddirective$depthHeight != height) {
            scpclassifieddirective$pdaDepthBackup.resize(width, height,
                    Minecraft.ON_OSX);
            scpclassifieddirective$depthWidth = width;
            scpclassifieddirective$depthHeight = height;
        }
    }
}
