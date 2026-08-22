package com.bl4ues.scpclassifieddirective.client;

/** Keeps upper-right debug panels clear of the larger custom advancement card. */
public final class AdvancementToastHudCoordination {
    private static final long FRAME_GRACE_NANOS = 250_000_000L;
    private static final int GAP = 5;
    private static volatile long activeUntilNanos;

    private AdvancementToastHudCoordination() {
    }

    public static void markRendered() {
        activeUntilNanos = System.nanoTime() + FRAME_GRACE_NANOS;
    }

    public static int topRightClearance() {
        return System.nanoTime() <= activeUntilNanos
                ? CustomAdvancementToastClient.HEIGHT + GAP : 0;
    }
}
