package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Shared visual language for every playable SCP-079 screen. */
public final class Scp079UiTheme {
    public static final int FRAME_MARGIN = 20;
    public static final int FRAME_CORNER = 29;
    public static final int FRAME_COLOR = 0xE6D9F7FF;
    public static final int TEXT = 0xFFE8F8FF;
    public static final int MUTED = 0xFF8BAAB6;
    public static final int ACCENT = 0xFFBDEEFF;
    public static final int DIM_ACCENT = 0xFF618999;

    private Scp079UiTheme() {
    }

    public static Component text(String value) {
        return ScpFonts.anonymousPro(value);
    }

    public static void renderFrame(GuiGraphics graphics, int width, int height) {
        int m = FRAME_MARGIN;
        int l = FRAME_CORNER;
        int t = 2;
        int c = FRAME_COLOR;
        graphics.fill(m, m, m + l, m + t, c);
        graphics.fill(m, m, m + t, m + l, c);
        graphics.fill(width - m - l, m, width - m, m + t, c);
        graphics.fill(width - m - t, m, width - m, m + l, c);
        graphics.fill(m, height - m - t, m + l, height - m, c);
        graphics.fill(m, height - m - l, m + t, height - m, c);
        graphics.fill(width - m - l, height - m - t,
                width - m, height - m, c);
        graphics.fill(width - m - t, height - m - l,
                width - m, height - m, c);
    }

    public static void draw(GuiGraphics graphics, Font font, String value,
            float x, float y, float scale, int color) {
        Component text = text(value);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    public static void drawCentered(GuiGraphics graphics, Font font,
            String value, float centerX, float y, float scale, int color) {
        Component text = text(value);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2, 0,
                color, false);
        graphics.pose().popPose();
    }

    public static int scaledWidth(Font font, String value, float scale) {
        return Math.round(font.width(text(value)) * scale);
    }

    public static void renderPower(GuiGraphics graphics, Minecraft minecraft,
            int power) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int barW = Math.min(214, Math.max(154, width / 7));
        int barH = 12;
        int x = width - FRAME_MARGIN - barW - 8;
        int y = height - FRAME_MARGIN - 24;

        draw(graphics, minecraft.font, "AUXILIARY POWER",
                x, y - 16, 1.13F, TEXT);
        String amount = Mth.clamp(power, 0, 100) + " / 100";
        int amountW = scaledWidth(minecraft.font, amount, 1.13F);
        draw(graphics, minecraft.font, amount,
                x + barW - amountW, y - 16, 1.13F, TEXT);

        graphics.fill(x, y, x + barW, y + barH, 0xE20A151A);
        graphics.fill(x, y, x + barW, y + 1, DIM_ACCENT);
        graphics.fill(x, y + barH - 1, x + barW, y + barH, DIM_ACCENT);
        graphics.fill(x, y, x + 1, y + barH, DIM_ACCENT);
        graphics.fill(x + barW - 1, y, x + barW, y + barH, DIM_ACCENT);

        int innerW = barW - 4;
        int fill = Math.round(innerW * Mth.clamp(power, 0, 100) / 100.0F);
        if (fill > 0) {
            graphics.fill(x + 2, y + 2, x + 2 + fill,
                    y + barH - 2, ACCENT);
        }
        for (int i = 1; i < 10; i++) {
            int tick = x + 2 + Math.round(innerW * i / 10.0F);
            graphics.fill(tick, y + 2, tick + 1, y + barH - 2,
                    0x88061318);
        }
    }

    /** Draw an entire 64x64 authored icon into a scaled destination. */
    public static void blitIcon64(GuiGraphics graphics, ResourceLocation icon,
            int x, int y, int size, float r, float g, float b, float a) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);
        graphics.blit(icon, x, y, size, size,
                0.0F, 0.0F, 64, 64, 64, 64);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
