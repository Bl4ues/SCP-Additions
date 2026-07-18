package net.mcreator.scpadditions.client;

import net.minecraft.util.Mth;

/** Client-only smoothed exposure state used by the code-generated vignette. */
public final class Scp714ClientState {
    private static boolean active;
    private static boolean immobilized;
    private static float currentProgress;
    private static float targetProgress;

    private Scp714ClientState() {
    }

    public static void update(boolean isActive, float progress,
            boolean isImmobilized) {
        active = isActive;
        immobilized = isImmobilized;
        targetProgress = Mth.clamp(progress, 0.0F, 1.0F);
        if (!active) {
            currentProgress = 0.0F;
            targetProgress = 0.0F;
            immobilized = false;
        }
    }

    public static float getSmoothedProgress(float partialTick) {
        if (!active) {
            return 0.0F;
        }
        float smoothing = Mth.clamp(0.18F + partialTick * 0.08F,
                0.18F, 0.30F);
        currentProgress = Mth.lerp(smoothing, currentProgress,
                targetProgress);
        if (Math.abs(currentProgress - targetProgress) < 0.001F) {
            currentProgress = targetProgress;
        }
        return Mth.clamp(currentProgress, 0.0F, 1.0F);
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isImmobilized() {
        return immobilized;
    }

    public static void clear() {
        update(false, 0.0F, false);
    }
}
