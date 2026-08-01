package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/** Compact oxygen meter rendered beneath the crosshair while air matters. */
public final class CustomOxygenOverlay {
    private static final int BAR_WIDTH = 156;
    private static final int BAR_HEIGHT = 8;
    private static final int CROSSHAIR_GAP = 22;

    private static final int TRACK = 0x7710181B;
    private static final int TRACK_DARK = 0xAA0B1012;
    private static final int BORDER = 0x997A8790;
    private static final int FULL_BLUE = 0xFF9CEBFF;
    private static final int MID_BLUE = 0xFF68BFE0;
    private static final int WARNING_RED = 0xFFED5E55;
    private static final int CRITICAL_RED = 0xFFFF2A2A;

    private CustomOxygenOverlay() {
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight) {
        if (!InventoryModuleRuntimeState.customOxygenBarForClient()) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isCreative() || player.isSpectator()
                || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }

        int maximumAir = Math.max(1, player.getMaxAirSupply());
        int air = Math.max(0, Math.min(maximumAir, player.getAirSupply()));
        if (!player.isUnderWater() && air >= maximumAir) return;

        float ratio = air / (float) maximumAir;
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight / 2 + CROSSHAIR_GAP;
        drawBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, ratio);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y,
            int width, int height, float ratio) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, TRACK);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, TRACK_DARK);

        int fillWidth = Math.max(0,
                Math.min(width - 2, Math.round((width - 2) * ratio)));
        int bright = oxygenColor(ratio);
        int dark = darken(bright, 0.60F);
        if (fillWidth > 0) {
            for (int i = 0; i < fillWidth; i++) {
                float progress = fillWidth <= 1
                        ? 1.0F : i / (float) (fillWidth - 1);
                graphics.fill(x + 1 + i, y + 1, x + 2 + i,
                        bottom - 1, lerpColor(dark, bright, progress));
            }

            int markerX = Math.min(right - 2, x + fillWidth);
            graphics.fill(markerX, y - 2, markerX + 1, bottom + 2,
                    withAlpha(bright, 0.88F));
        }

        graphics.fill(x, y, right, y + 1, BORDER);
        graphics.fill(x, bottom - 1, right, bottom, BORDER);
        graphics.fill(x, y, x + 1, bottom, BORDER);
        graphics.fill(right - 1, y, right, bottom, BORDER);
    }

    private static int oxygenColor(float ratio) {
        float value = Math.max(0.0F, Math.min(1.0F, ratio));
        if (value >= 0.50F) {
            return lerpColor(MID_BLUE, FULL_BLUE,
                    (value - 0.50F) / 0.50F);
        }
        if (value >= 0.20F) {
            return lerpColor(WARNING_RED, MID_BLUE,
                    (value - 0.20F) / 0.30F);
        }
        return lerpColor(CRITICAL_RED, WARNING_RED,
                value / 0.20F);
    }

    private static int darken(int color, float factor) {
        int alpha = color >>> 24;
        int red = Math.round(((color >> 16) & 0xFF) * factor);
        int green = Math.round(((color >> 8) & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int lerpColor(int from, int to, float amount) {
        float value = Math.max(0.0F, Math.min(1.0F, amount));
        int alpha = Math.round(((from >>> 24) & 0xFF)
                + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * value);
        int red = Math.round(((from >> 16) & 0xFF)
                + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * value);
        int green = Math.round(((from >> 8) & 0xFF)
                + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * value);
        int blue = Math.round((from & 0xFF)
                + ((to & 0xFF) - (from & 0xFF)) * value);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int withAlpha(int color, float alpha) {
        int value = Math.max(0, Math.min(255,
                Math.round(alpha * 255.0F)));
        return (value << 24) | (color & 0x00FFFFFF);
    }
}
