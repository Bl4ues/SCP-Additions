package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.UUID;

/** Compact oxygen meter and progressive suffocation presentation. */
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

    private static final float VIGNETTE_START_RATIO = 0.85F;
    private static final float MAX_VIGNETTE_STRENGTH = 0.72F;
    private static final float VIGNETTE_RESPONSE = 6.0F;
    private static final int VIGNETTE_BANDS = 28;
    private static final float DROWNING_DARKNESS_STEP = 0.14F;
    private static final float MAX_DROWNING_DARKNESS = 0.86F;
    private static final float DARKNESS_RECOVERY_PER_SECOND = 0.36F;
    private static final float HEALTH_EPSILON = 1.0E-3F;

    private static UUID trackedPlayerId;
    private static float previousEffectiveHealth = Float.NaN;
    private static int previousHurtTime;
    private static int previousAirSupply = -1;
    private static float vignetteStrength;
    private static float drowningDarkness;
    private static long lastFrameNanos;

    private CustomOxygenOverlay() {
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight) {
        if (!InventoryModuleRuntimeState.customOxygenBarForClient()) {
            resetVisualState();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isCreative() || player.isSpectator()
                || minecraft.screen != null) {
            resetVisualState();
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds = frameDelta(now);
        initializePlayerState(player);

        int maximumAir = Math.max(1, player.getMaxAirSupply());
        int air = Math.max(0, Math.min(maximumAir, player.getAirSupply()));
        float ratio = air / (float) maximumAir;

        updateSuffocationState(player, air, ratio, deltaSeconds);
        drawSuffocationEffects(graphics, screenWidth, screenHeight);

        if (!minecraft.options.hideGui
                && (player.isUnderWater() || air < maximumAir)) {
            int x = (screenWidth - BAR_WIDTH) / 2;
            int y = screenHeight / 2 + CROSSHAIR_GAP;
            drawBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, ratio);
        }
    }

    private static void initializePlayerState(LocalPlayer player) {
        UUID playerId = player.getUUID();
        if (playerId.equals(trackedPlayerId)
                && !Float.isNaN(previousEffectiveHealth)) {
            return;
        }

        trackedPlayerId = playerId;
        previousEffectiveHealth = player.getHealth()
                + player.getAbsorptionAmount();
        previousHurtTime = player.hurtTime;
        previousAirSupply = player.getAirSupply();
        vignetteStrength = 0.0F;
        drowningDarkness = 0.0F;
    }

    private static void updateSuffocationState(LocalPlayer player, int air,
            float ratio, float deltaSeconds) {
        float depletion = clamp01((VIGNETTE_START_RATIO - ratio)
                / VIGNETTE_START_RATIO);
        float targetVignette = smoothstep(depletion)
                * MAX_VIGNETTE_STRENGTH;
        float response = 1.0F - (float) Math.exp(
                -VIGNETTE_RESPONSE * deltaSeconds);
        vignetteStrength += (targetVignette - vignetteStrength) * response;

        float effectiveHealth = player.getHealth()
                + player.getAbsorptionAmount();
        boolean drowningDamage = player.isUnderWater() && air <= 0
                && isDrowningDamage(player);
        boolean newDrowningPulse = drowningDamage
                && (player.hurtTime > previousHurtTime
                || effectiveHealth + HEALTH_EPSILON
                < previousEffectiveHealth);
        if (newDrowningPulse) {
            drowningDarkness = Math.min(MAX_DROWNING_DARKNESS,
                    drowningDarkness + DROWNING_DARKNESS_STEP);
        }

        boolean airRecovering = previousAirSupply >= 0
                && air > previousAirSupply;
        if (!player.isUnderWater() || airRecovering || air > 0) {
            float recoveryMultiplier = airRecovering ? 1.35F : 1.0F;
            drowningDarkness = Math.max(0.0F,
                    drowningDarkness
                            - DARKNESS_RECOVERY_PER_SECOND
                            * recoveryMultiplier * deltaSeconds);
        }

        previousEffectiveHealth = effectiveHealth;
        previousHurtTime = player.hurtTime;
        previousAirSupply = air;
    }

    private static boolean isDrowningDamage(LocalPlayer player) {
        DamageSource source = player.getLastDamageSource();
        return source != null && source.is(DamageTypes.DROWN);
    }

    private static void drawSuffocationEffects(GuiGraphics graphics,
            int width, int height) {
        if (drowningDarkness > 0.001F) {
            graphics.fill(0, 0, width, height,
                    alphaBlack(drowningDarkness));
        }
        if (vignetteStrength > 0.001F) {
            drawVignette(graphics, width, height, vignetteStrength);
        }
    }

    private static void drawVignette(GuiGraphics graphics, int width,
            int height, float strength) {
        int maximumInset = Math.max(24,
                Math.min(width, height) / 4);
        for (int band = 0; band < VIGNETTE_BANDS; band++) {
            int outer = Math.round(maximumInset
                    * band / (float) VIGNETTE_BANDS);
            int inner = Math.round(maximumInset
                    * (band + 1) / (float) VIGNETTE_BANDS);
            if (inner <= outer) continue;

            float edgeAmount = 1.0F
                    - band / (float) (VIGNETTE_BANDS - 1);
            float alpha = strength * edgeAmount * edgeAmount;
            int color = alphaBlack(alpha);

            graphics.fill(outer, outer, width - outer, inner, color);
            graphics.fill(outer, height - inner,
                    width - outer, height - outer, color);
            graphics.fill(outer, inner, inner, height - inner, color);
            graphics.fill(width - inner, inner,
                    width - outer, height - inner, color);
        }
    }

    private static float frameDelta(long now) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return 1.0F / 60.0F;
        }
        float delta = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;
        return Math.max(0.0F, Math.min(0.10F, delta));
    }

    private static void resetVisualState() {
        trackedPlayerId = null;
        previousEffectiveHealth = Float.NaN;
        previousHurtTime = 0;
        previousAirSupply = -1;
        vignetteStrength = 0.0F;
        drowningDarkness = 0.0F;
        lastFrameNanos = 0L;
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
        float value = clamp01(ratio);
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

    private static int withAlpha(int color, float alpha) {
        int value = Math.max(0, Math.min(255,
                Math.round(alpha * 255.0F)));
        return (value << 24) | (color & 0x00FFFFFF);
    }

    private static int alphaBlack(float alpha) {
        int value = Math.max(0, Math.min(255,
                Math.round(clamp01(alpha) * 255.0F)));
        return value << 24;
    }

    private static float smoothstep(float value) {
        float clamped = clamp01(value);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
