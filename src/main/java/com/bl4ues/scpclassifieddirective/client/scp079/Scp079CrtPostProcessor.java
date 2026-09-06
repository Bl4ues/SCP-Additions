package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Final framebuffer pass for the complete playable SCP-079 display. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CrtPostProcessor {
    public static final float WARP_QUADRATIC = 0.055F;
    public static final float WARP_QUARTIC = 0.012F;

    private static ShaderInstance shader;
    private static TextureTarget copyTarget;

    private Scp079CrtPostProcessor() { }

    static void setShader(ShaderInstance value) {
        shader = value;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp079PlayableClient.active() || minecraft.screen != null) return;
        apply(minecraft, event.getGuiGraphics());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!Scp079PlayableClient.active()) return;
        // The CRT is SCP-079's display surface, not merely a world-HUD effect.
        // Keep the final pass over every GUI, including chat and pause/options,
        // so opening a vanilla screen cannot momentarily reveal a flat display.
        apply(Minecraft.getInstance(), event.getGuiGraphics());
    }

    public static double logicalX(double screenX, double screenY,
            int width, int height) {
        return logical(screenX, screenY, width, height)[0];
    }

    public static double logicalY(double screenX, double screenY,
            int width, int height) {
        return logical(screenX, screenY, width, height)[1];
    }

    private static double[] logical(double x, double y, int width, int height) {
        if (width <= 0 || height <= 0) return new double[] {x, y};
        double px = x / width * 2.0D - 1.0D;
        double py = y / height * 2.0D - 1.0D;
        double r2 = px * px + py * py;
        double factor = 1.0D + WARP_QUADRATIC * r2
                + WARP_QUARTIC * r2 * r2;
        return new double[] {
                (px * factor * 0.5D + 0.5D) * width,
                (py * factor * 0.5D + 0.5D) * height
        };
    }

    private static void apply(Minecraft minecraft, GuiGraphics graphics) {
        if (shader == null) return;
        RenderTarget main = minecraft.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) return;
        ensureTarget(main.width, main.height);
        if (copyTarget == null) return;

        graphics.flush();
        RenderSystem.disableScissor();

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                copyTarget.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height,
                0, 0, copyTarget.width, copyTarget.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, main.frameBufferId);

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, copyTarget.getColorTextureId());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (shader.getUniform("Time") != null) {
            shader.getUniform("Time").set((System.nanoTime() % 60_000_000_000L)
                    / 1_000_000_000.0F);
        }

        Matrix4f identity = new Matrix4f();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        // FBO textures use a bottom-left UV origin. The quad itself is clip-space,
        // so no GUI matrix can leave unfiltered strips around a Screen.
        buffer.vertex(identity, -1.0F, -1.0F, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(identity, 1.0F, -1.0F, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(identity, 1.0F, 1.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(identity, -1.0F, 1.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void ensureTarget(int width, int height) {
        if (copyTarget == null) {
            copyTarget = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            copyTarget.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            copyTarget.setFilterMode(GL11.GL_LINEAR);
        } else if (copyTarget.width != width || copyTarget.height != height) {
            copyTarget.resize(width, height, Minecraft.ON_OSX);
        }
    }
}
