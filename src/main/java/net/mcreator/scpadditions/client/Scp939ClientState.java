package net.mcreator.scpadditions.client;

/** Client-only snapshot synchronized by the SCP-939 interaction packets. */
public final class Scp939ClientState {
    private static boolean breathActive;
    private static float breathReserve = 1.0F;
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
        breathActive = breathActiveValue;
        breathReserve = Math.max(0.0F, Math.min(1.0F, reserve));
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
    public static float breathReserve() { return breathReserve; }
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
    }
}
