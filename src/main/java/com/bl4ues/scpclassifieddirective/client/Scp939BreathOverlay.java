package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;

/**
 * Breath-holding presentation matched to the custom oxygen HUD: smooth entry
 * and exit, the same compact meter language, and a progressive edge vignette as
 * the player's breath reserve approaches exhaustion.
 */
public final class Scp939BreathOverlay {
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

    private static final float HUD_IN_RESPONSE = 9.5F;
    private static final float HUD_OUT_RESPONSE = 7.0F;
    private static final float VIGNETTE_START_RATIO = 0.85F;
    private static final float MAX_VIGNETTE_STRENGTH = 0.62F;
    private static final float VIGNETTE_RESPONSE = 6.0F;
    private static final int VIGNETTE_BANDS = 28;

    private static float hudAlpha;
    private static float vignetteStrength;
    private static float lastReserve = 1.0F;
    private static long lastFrameNanos;

    private Scp939BreathOverlay() {
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive() || player.isCreative()
                || player.isSpectator()) {
            reset();
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds = frameDelta(now);
        boolean active = Scp939ClientState.breathActive()
                && !Scp939ClientState.pinned();
        if (active) lastReserve = Scp939ClientState.breathReserve();

        boolean presentationAllowed = !minecraft.options.hideGui
                && minecraft.screen == null && !Scp939ClientState.pinned();
        float targetAlpha = active && presentationAllowed ? 1.0F : 0.0F;
        float hudResponse = targetAlpha > hudAlpha
                ? HUD_IN_RESPONSE : HUD_OUT_RESPONSE;
        hudAlpha = approachExp(hudAlpha, targetAlpha,
                hudResponse, deltaSeconds);

        float depletion = active
                ? clamp01((VIGNETTE_START_RATIO - lastReserve)
                        / VIGNETTE_START_RATIO)
                : 0.0F;
        float targetVignette = smoothstep(depletion)
                * MAX_VIGNETTE_STRENGTH * targetAlpha;
        vignetteStrength = approachExp(vignetteStrength, targetVignette,
                VIGNETTE_RESPONSE, deltaSeconds);

        if (!presentationAllowed) return;
        if (vignetteStrength > 0.001F) {
            drawVignette(graphics, width, height, vignetteStrength);
        }
        if (hudAlpha <= 0.002F) return;

        int x = (width - BAR_WIDTH) / 2;
        int y = height / 2 + CROSSHAIR_GAP;
        drawBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT,
                lastReserve, hudAlpha);
        drawPrompt(graphics, minecraft, width, y, hudAlpha);
    }

    private static void drawPrompt(GuiGraphics graphics, Minecraft minecraft,
            int width, int barY, float alpha) {
        Component key = ScpFonts.roboto(
                Scp939Keybinds.HOLD_BREATH.getTranslatedKeyMessage());
        String action = Scp939ClientState.holdingBreath()
                ? "RELEASE" : "HOLD";
        Component actionText = ScpFonts.roboto(action);
        int keyWidth = minecraft.font.width(key) + 10;
        int actionWidth = minecraft.font.width(actionText);
        int totalWidth = actionWidth + 5 + keyWidth;
        int promptX = width / 2 - totalWidth / 2;
        int promptY = barY + BAR_HEIGHT + 6;
        int keyX = promptX + actionWidth + 5;
        int keyColor = Scp939ClientState.holdingBreath()
                ? ConfigCenterVisuals.ACCENT_BRIGHT
                : ConfigCenterVisuals.TEXT;

        graphics.drawString(minecraft.font, actionText, promptX, promptY,
                withAlpha(ConfigCenterVisuals.MUTED, alpha), false);
        graphics.fill(keyX, promptY - 3, keyX + keyWidth,
                promptY + minecraft.font.lineHeight + 3,
                withAlpha(0xB50B0E12, alpha));
        graphics.fill(keyX, promptY - 3, keyX + keyWidth, promptY - 2,
                withAlpha(keyColor, alpha));
        graphics.drawString(minecraft.font, key, keyX + 5, promptY,
                withAlpha(keyColor, alpha), false);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y,
            int width, int height, float ratio, float alpha) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, withAlpha(TRACK, alpha));
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1,
                withAlpha(TRACK_DARK, alpha));

        int fillWidth = Math.max(0,
                Math.min(width - 2, Math.round((width - 2) * ratio)));
        int bright = oxygenColor(ratio);
        int dark = darken(bright, 0.60F);
        if (fillWidth > 0) {
            for (int i = 0; i < fillWidth; i++) {
                float progress = fillWidth <= 1
                        ? 1.0F : i / (float) (fillWidth - 1);
                graphics.fill(x + 1 + i, y + 1, x + 2 + i,
                        bottom - 1,
                        withAlpha(lerpColor(dark, bright, progress), alpha));
            }

            int markerX = Math.min(right - 2, x + fillWidth);
            graphics.fill(markerX, y - 2, markerX + 1, bottom + 2,
                    withAlpha(bright, alpha * 0.88F));
        }

        graphics.fill(x, y, right, y + 1, withAlpha(BORDER, alpha));
        graphics.fill(x, bottom - 1, right, bottom,
                withAlpha(BORDER, alpha));
        graphics.fill(x, y, x + 1, bottom, withAlpha(BORDER, alpha));
        graphics.fill(right - 1, y, right, bottom,
                withAlpha(BORDER, alpha));
    }

    private static void drawVignette(GuiGraphics graphics, int width,
            int height, float strength) {
        int maximumInset = Math.max(24, Math.min(width, height) / 4);
        for (int band = 0; band < VIGNETTE_BANDS; band++) {
            int outer = Math.round(maximumInset
                    * band / (float) VIGNETTE_BANDS);
            int inner = Math.round(maximumInset
                    * (band + 1) / (float) VIGNETTE_BANDS);
            if (inner <= outer) continue;

            float edgeAmount = 1.0F
                    - band / (float) (VIGNETTE_BANDS - 1);
            int color = alphaBlack(strength * edgeAmount * edgeAmount);
            graphics.fill(outer, outer, width - outer, inner, color);
            graphics.fill(outer, height - inner,
                    width - outer, height - outer, color);
            graphics.fill(outer, inner, inner, height - inner, color);
            graphics.fill(width - inner, inner,
                    width - outer, height - inner, color);
        }
    }

    private static int oxygenColor(float ratio) {
        float value = clamp01(ratio);
        if (value >= 0.50F) {
            return lerpColor(MID_BLUE, FULL_BLUE,
                    (value - 0.50F) / 0.50F);
        }
        if (value >= 0.20F) {
            return lerpColor(WARNING_RED, MID_BLUE,
                    (value - 0.20F) / 0.30F);
        }
        return lerpColor(CRITICAL_RED, WARNING_RED, value / 0.20F);
    }

    private static float frameDelta(long now) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return 1.0F / 60.0F;
        }
        float delta = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;
        return Mth.clamp(delta, 0.0F, 0.10F);
    }

    private static float approachExp(float current, float target,
            float response, float deltaSeconds) {
        float amount = 1.0F - (float) Math.exp(-response * deltaSeconds);
        float value = current + (target - current) * amount;
        return Math.abs(target - value) < 0.0005F ? target : value;
    }

    private static int darken(int color, float factor) {
        int alpha = color >>> 24;
        int red = Math.round(((color >> 16) & 0xFF) * factor);
        int green = Math.round(((color >> 8) & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int lerpColor(int from, int to, float amount) {
        float value = clamp01(amount);
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

    private static int withAlpha(int color, float opacity) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        int alpha = Mth.clamp(Math.round(sourceAlpha * clamp01(opacity)),
                0, 255);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int alphaBlack(float alpha) {
        return Mth.clamp(Math.round(clamp01(alpha) * 255.0F), 0, 255) << 24;
    }

    private static float smoothstep(float value) {
        float clamped = clamp01(value);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float clamp01(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }

    private static void reset() {
        hudAlpha = 0.0F;
        vignetteStrength = 0.0F;
        lastReserve = 1.0F;
        lastFrameNanos = 0L;
    }
}
