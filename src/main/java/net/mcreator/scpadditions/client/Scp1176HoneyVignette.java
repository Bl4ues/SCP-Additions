package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;

/**
 * Subtle honey-colored pulse shown while an SCP-1176 outcome is active.
 * It reuses the SCP-173 vignette texture and moves each wave only outward.
 */
public final class Scp1176HoneyVignette {
    private static final ResourceLocation VIGNETTE =
            ResourceLocation.fromNamespaceAndPath("scpinventory", "textures/gui/vignette.png");
    private static final int VIGNETTE_SOURCE_WIDTH = 1920;
    private static final int VIGNETTE_SOURCE_HEIGHT = 1080;
    private static final float PULSE_PERIOD_TICKS = 160.0F;
    private static final float MAX_EXPANSION_RATIO = 0.045F;
    private static final int MAX_VISIBLE_EDGE = 46;
    private static final float BASE_ALPHA = 0.25F;
    private static final int DEEP_HONEY = 0x9A5A14;
    private static final int UNITY_GOLD = 0xE1A704;

    private Scp1176HoneyVignette() {
    }

    public static void render(GuiGraphics graphics, int width, int height, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null
                || !mc.player.hasEffect(ScpAdditionsModMobEffects.SCP_1176_HONEYED.get())) {
            return;
        }

        float age = mc.player.tickCount + partialTick;
        drawPulse(graphics, width, height, phase(age), 1.0F);
        drawPulse(graphics, width, height, phase(age + PULSE_PERIOD_TICKS * 0.5F), 0.68F);
    }

    private static float phase(float ticks) {
        float wrapped = ticks % PULSE_PERIOD_TICKS;
        if (wrapped < 0.0F) wrapped += PULSE_PERIOD_TICKS;
        return wrapped / PULSE_PERIOD_TICKS;
    }

    private static void drawPulse(GuiGraphics graphics, int width, int height,
                                  float progress, float strength) {
        float envelope = (float) Math.sin(Math.PI * progress);
        float alpha = envelope * envelope * BASE_ALPHA * strength;
        if (alpha <= 0.002F) return;

        float easedProgress = smoothStep(progress);
        int expansion = Math.round(Math.min(width, height) * MAX_EXPANSION_RATIO * easedProgress);
        int visibleEdge = Math.max(0, Math.round(MAX_VISIBLE_EDGE * (1.0F - easedProgress)));

        drawExpandedVignette(graphics, width, height, expansion, alpha * 0.22F);
        if (visibleEdge > 0) {
            drawHoneyEdgeGlow(graphics, width, height, visibleEdge, alpha);
        }
    }

    private static void drawExpandedVignette(GuiGraphics graphics, int width, int height,
                                             int expansion, float alpha) {
        int left = -expansion;
        int top = -expansion;
        int drawWidth = width + expansion * 2;
        int drawHeight = height + expansion * 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 0.82F, 0.42F, alpha);
        graphics.blit(VIGNETTE, left, top, drawWidth, drawHeight,
                0.0F, 0.0F, VIGNETTE_SOURCE_WIDTH, VIGNETTE_SOURCE_HEIGHT,
                VIGNETTE_SOURCE_WIDTH, VIGNETTE_SOURCE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawHoneyEdgeGlow(GuiGraphics graphics, int width, int height,
                                           int thickness, float alpha) {
        int steps = Math.max(1, thickness);
        for (int i = 0; i < steps; i++) {
            float t = steps <= 1 ? 1.0F : i / (float) (steps - 1);
            float falloff = (1.0F - t) * (1.0F - t);
            float stripAlpha = alpha * 0.72F * falloff;
            int color = withAlpha(lerpRgb(DEEP_HONEY, UNITY_GOLD, Math.min(1.0F, t * 0.82F)), stripAlpha);

            int left = i;
            int top = i;
            int right = width - i;
            int bottom = height - i;
            if (left >= right || top >= bottom) break;

            graphics.fill(left, top, right, top + 1, color);
            graphics.fill(left, bottom - 1, right, bottom, color);
            graphics.fill(left, top + 1, left + 1, bottom - 1, color);
            graphics.fill(right - 1, top + 1, right, bottom - 1, color);
        }
    }

    private static float smoothStep(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int lerpRgb(int from, int to, float t) {
        int fr = from >> 16 & 255;
        int fg = from >> 8 & 255;
        int fb = from & 255;
        int tr = to >> 16 & 255;
        int tg = to >> 8 & 255;
        int tb = to & 255;
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return r << 16 | g << 8 | b;
    }
}
