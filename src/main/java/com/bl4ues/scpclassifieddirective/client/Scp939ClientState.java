package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.util.Mth;

/** Client-only snapshot synchronized by the SCP-939 interaction packets. */
public final class Scp939ClientState {
    private static boolean breathActive;
    private static float breathReserveTarget = 1.0F;
    private static float breathReserveVisual = 1.0F;
    private static long breathVisualNanos;
    private static boolean holdingBreath;
    private static boolean pinned;
    private static int pinProgress;
    private static int pinFailures;
    private static int expectedKey;
    private static int pinWindowTicks;
    private static long pinStartedNanos;

    private Scp939ClientState() {
    }

    public static void update(boolean breathActiveValue, float reserve,
            boolean holding, boolean pinnedValue, int progress, int failures,
            int expected, int windowTicks) {
        boolean wasPinned = pinned;
        boolean wasBreathActive = breathActive;
        breathActive = breathActiveValue;
        breathReserveTarget = Mth.clamp(reserve, 0.0F, 1.0F);
        if (!wasBreathActive && breathActive) {
            // Opening the encounter HUD should begin at the authoritative value,
            // not animate in from an unrelated reserve left by an older encounter.
            breathReserveVisual = breathReserveTarget;
            breathVisualNanos = System.nanoTime();
        }
        holdingBreath = holding;
        pinned = pinnedValue;
        pinProgress = Math.max(0, progress);
        pinFailures = Math.max(0, failures);
        expectedKey = expected;
        pinWindowTicks = Math.max(0, windowTicks);
        if (pinned && !wasPinned) pinStartedNanos = System.nanoTime();
        if (!pinned) pinStartedNanos = 0L;
    }

    public static boolean breathActive() { return breathActive; }

    /**
     * Render-time exponential smoothing hides the five-tick network cadence.
     * The gameplay reserve remains fully server-authoritative; only its visual
     * presentation interpolates between snapshots.
     */
    public static float breathReserve() {
        long now = System.nanoTime();
        if (breathVisualNanos == 0L) {
            breathVisualNanos = now;
            breathReserveVisual = breathReserveTarget;
            return breathReserveVisual;
        }
        float seconds = Mth.clamp((now - breathVisualNanos)
                / 1_000_000_000.0F, 0.0F, 0.10F);
        breathVisualNanos = now;
        float response = 1.0F - (float) Math.exp(-seconds * 11.0F);
        breathReserveVisual += (breathReserveTarget - breathReserveVisual)
                * response;
        if (Math.abs(breathReserveTarget - breathReserveVisual) < 0.0005F) {
            breathReserveVisual = breathReserveTarget;
        }
        return Mth.clamp(breathReserveVisual, 0.0F, 1.0F);
    }

    public static boolean holdingBreath() { return holdingBreath; }
    public static boolean pinned() { return pinned; }
    public static int pinProgress() { return pinProgress; }
    public static int pinFailures() { return pinFailures; }
    public static int expectedKey() { return expectedKey; }
    public static int pinWindowTicks() { return pinWindowTicks; }

    public static float pinElapsedSeconds() {
        if (!pinned || pinStartedNanos == 0L) return 0.0F;
        return Math.max(0.0F,
                (System.nanoTime() - pinStartedNanos) / 1_000_000_000.0F);
    }

    public static void clear() {
        update(false, 1.0F, false, false, 0, 0, 0, 0);
        breathReserveTarget = 1.0F;
        breathReserveVisual = 1.0F;
        breathVisualNanos = 0L;
    }
}
