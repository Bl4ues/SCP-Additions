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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Monochrome low-light camera pass for playable SCP-079.
 *
 * <p>The world framebuffer is processed before the SCP-079 HUD is drawn. This
 * keeps the camera image black-and-white while the interface remains readable,
 * and lets one transition value drive both desaturation and sensor gain.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079NightVisionPostProcessor {
    private static final float TRANSITION_PER_SECOND = 2.85F;
    private static final int LOW_LIGHT_THRESHOLD = 5;
    private static final long NIGHT_START = 13_000L;
    private static final long NIGHT_END = 23_000L;

    private static ShaderInstance shader;
    private static TextureTarget copyTarget;
    private static float strength;
    private static long lastFrameNanos;

    private Scp079NightVisionPostProcessor() { }

    static void setShader(ShaderInstance value) {
        shader = value;
    }

    public static float strength() {
        return strength;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.0F
                : Mth.clamp((now - lastFrameNanos) / 1_000_000_000.0F,
                        0.0F, 0.10F);
        lastFrameNanos = now;

        if (!Scp079PlayableClient.active()
                || !Scp079PlayableClient.cameraMode()
                || minecraft.level == null) {
            strength = 0.0F;
            return;
        }

        // Sample the camera Minecraft is actually rendering from, not the
        // surveillance registration anchor. Wall-mounted cameras can have an
        // eye point offset from their block, and that difference matters when
        // the block sits on the boundary between a lit and an enclosed room.
        BlockPos cameraPos = BlockPos.containing(
                minecraft.gameRenderer.getMainCamera().getPosition());
        float target = shouldEnhance(minecraft.level, cameraPos) ? 1.0F : 0.0F;
        strength = approach(strength, target, dt * TRANSITION_PER_SECOND);
        if (strength <= 0.001F) {
            strength = 0.0F;
            return;
        }

        apply(minecraft, event, now);
    }

    private static boolean shouldEnhance(ClientLevel level, BlockPos pos) {
        int localBrightness = level.getMaxLocalRawBrightness(pos);
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        long timeOfDay = Math.floorMod(level.getDayTime(), 24_000L);
        boolean night = timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;

        // Enclosed darkness is detected from the local Minecraft light value.
        // Outdoors and around doors, raw skylight can remain deceptively high
        // through the night, so night-time surveillance instead stays in colour
        // only when a real block-light source stronger than level five exists.
        return localBrightness <= LOW_LIGHT_THRESHOLD
                || (night && blockLight <= LOW_LIGHT_THRESHOLD);
    }

    private static float approach(float current, float target, float amount) {
        if (current < target) return Math.min(target, current + amount);
        if (current > target) return Math.max(target, current - amount);
        return current;
    }

    private static void apply(Minecraft minecraft, RenderGuiEvent.Pre event,
            long now) {
        if (shader == null) return;
        RenderTarget main = minecraft.getMainRenderTarget();
        if (main == null || main.width <= 0 || main.height <= 0) return;
        ensureTarget(main.width, main.height);
        if (copyTarget == null) return;

        event.getGuiGraphics().flush();
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
        if (shader.getUniform("Strength") != null) {
            shader.getUniform("Strength").set(strength);
        }
        if (shader.getUniform("Time") != null) {
            shader.getUniform("Time").set((now % 60_000_000_000L)
                    / 1_000_000_000.0F);
        }

        Matrix4f identity = new Matrix4f();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(identity, -1.0F, -1.0F, 0.0F)
                .uv(0.0F, 0.0F).endVertex();
        buffer.vertex(identity, 1.0F, -1.0F, 0.0F)
                .uv(1.0F, 0.0F).endVertex();
        buffer.vertex(identity, 1.0F, 1.0F, 0.0F)
                .uv(1.0F, 1.0F).endVertex();
        buffer.vertex(identity, -1.0F, 1.0F, 0.0F)
                .uv(0.0F, 1.0F).endVertex();
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
