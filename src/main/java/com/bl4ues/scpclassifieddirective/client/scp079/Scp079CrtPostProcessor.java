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

/**
 * Final framebuffer pass for the complete playable SCP-079 display.
 *
 * The already-composited framebuffer is sampled only after 079's HUD or screen
 * has been drawn. That makes the world, text, scanlines, brackets and prompts
 * live on the same curved CRT glass instead of warping the world underneath a
 * perfectly flat modern GUI. The shaderpack framebuffer itself is never
 * replaced or injected into.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CrtPostProcessor {
    private static ShaderInstance shader;
    private static TextureTarget copyTarget;

    private Scp079CrtPostProcessor() { }

    static void setShader(ShaderInstance value) {
        shader = value;
    }

    /** Curves the local-host and surveillance-camera presentation after HUD. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp079PlayableClient.active() || minecraft.screen != null) return;
        apply(minecraft, event.getGuiGraphics());
    }

    /** Curves SCP-079's actual screens, including the facility map and exit UI. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!Scp079PlayableClient.active()
                || !(event.getScreen() instanceof Scp079FacilityMapScreen
                || event.getScreen() instanceof Scp079LeaveRoleScreen)) {
            return;
        }
        apply(Minecraft.getInstance(), event.getGuiGraphics());
    }

    private static void apply(Minecraft minecraft, GuiGraphics graphics) {
        if (shader == null) return;
        RenderTarget main = minecraft.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) return;
        ensureTarget(main.width, main.height);
        if (copyTarget == null) return;

        // GUI draws are batched. Flush them before copying so the post pass sees
        // the same complete frame that the player would otherwise see.
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

        int guiW = minecraft.getWindow().getGuiScaledWidth();
        int guiH = minecraft.getWindow().getGuiScaledHeight();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        // Framebuffer colour textures use a bottom-left origin.
        buffer.vertex(matrix, 0.0F, guiH, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(matrix, guiW, guiH, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, guiW, 0.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
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
