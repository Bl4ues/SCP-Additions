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
            new ResourceLocation("scpinventory", "textures/gui/vignette.png");
    private static final int VIGNETTE_SOURCE_WIDTH = 1920;
    private static final int VIGNETTE_SOURCE_HEIGHT = 1080;
    private static final float PULSE_PERIOD_TICKS = 120.0F;
    private static final float MAX_EXPANSION_RATIO = 0.075F;
    private static final float BASE_ALPHA = 0.18F;
    private static final int EDGE_STEPS = 24;
    private static final int DEEP_HONEY = 0x9A5A14;
    private static final int LIGHT_HONEY = 0xE7B84D;

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
        drawPulse(graphics, width, height, phase(age + PULSE_PERIOD_TICKS * 0.5F), 0.72F);
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

        float easedProgress = 1.0F - (1.0F - progress) * (1.0F - progress);
        int expansion = Math.round(Math.min(width, height) * MAX_EXPANSION_RATIO * easedProgress);
        int left = -expansion;
        int top = -expansion;
        int right = width + expansion;
        int bottom = height + expansion;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 0.82F, 0.42F, alpha * 0.30F);
        graphics.blit(VIGNETTE, left, top, right - left, bottom - top,
                0.0F, 0.0F, VIGNETTE_SOURCE_WIDTH, VIGNETTE_SOURCE_HEIGHT,
                VIGNETTE_SOURCE_WIDTH, VIGNETTE_SOURCE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        drawHoneyEdgeGlow(graphics, left, top, right, bottom, alpha);
    }

    private static void drawHoneyEdgeGlow(GuiGraphics graphics, int left, int top,
                                           int right, int bottom, float alpha) {
        for (int i = 0; i < EDGE_STEPS; i++) {
            float t = i / (float) (EDGE_STEPS - 1);
            float falloff = (1.0F - t) * (1.0F - t);
            float stripAlpha = alpha * 0.58F * falloff;
            int color = withAlpha(lerpRgb(DEEP_HONEY, LIGHT_HONEY, t * 0.72F), stripAlpha);

            int x1 = left + i;
            int y1 = top + i;
            int x2 = right - i;
            int y2 = bottom - i;
            if (x1 >= x2 || y1 >= y2) break;

            graphics.fill(x1, y1, x2, y1 + 1, color);
            graphics.fill(x1, y2 - 1, x2, y2, color);
            graphics.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
            graphics.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
        }
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
