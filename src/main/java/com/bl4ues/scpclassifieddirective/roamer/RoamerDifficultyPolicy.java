package com.bl4ues.scpclassifieddirective.roamer;

import net.minecraft.world.Difficulty;

/** Difficulty-specific timing rules for natural roamer encounters. */
public final class RoamerDifficultyPolicy {
    private static final int TICKS_PER_MINUTE = 20 * 60;
    private static final double PLAYER_REDUCTION_PER_PLAYER = 0.10D;
    private static final double MAX_PLAYER_REDUCTION = 0.50D;

    private RoamerDifficultyPolicy() {
    }

    /** Thaumiel/Peaceful never runs natural roamer spawn checks. */
    public static boolean schedulesEnabled(Difficulty difficulty) {
        return difficulty != null && difficulty != Difficulty.PEACEFUL;
    }

    public static int initialDelayTicks(RoamerType type, Difficulty difficulty) {
        if (type == null || !schedulesEnabled(difficulty)) return -1;
        return minutes(switch (difficulty) {
            case EASY -> switch (type) {
                case SCP_106 -> 5;
                case SCP_173 -> 8;
                case SCP_939 -> 10;
            };
            case NORMAL -> switch (type) {
                case SCP_106 -> 2;
                case SCP_173 -> 5;
                case SCP_939 -> 8;
            };
            case HARD -> switch (type) {
                case SCP_106 -> 2;
                case SCP_173 -> 4;
                case SCP_939 -> 5;
            };
            case PEACEFUL -> 0;
        });
    }

    public static int recurringDelayTicks(RoamerType type,
            Difficulty difficulty) {
        if (type == null || !schedulesEnabled(difficulty)) return -1;
        return minutes(switch (difficulty) {
            case EASY -> switch (type) {
                case SCP_106 -> 10;
                case SCP_173 -> 8;
                case SCP_939 -> 12;
            };
            case NORMAL -> switch (type) {
                case SCP_106 -> 8;
                case SCP_173 -> 5;
                case SCP_939 -> 10;
            };
            case HARD -> switch (type) {
                case SCP_106 -> 5;
                case SCP_173 -> 4;
                case SCP_939 -> 8;
            };
            case PEACEFUL -> 0;
        });
    }

    /**
     * Each valid survival player shortens the global encounter interval by 10%,
     * capped at 50% for five or more players.
     */
    public static double playerIntervalMultiplier(int validPlayerCount) {
        int players = Math.max(0, validPlayerCount);
        double reduction = Math.min(MAX_PLAYER_REDUCTION,
                players * PLAYER_REDUCTION_PER_PLAYER);
        return 1.0D - reduction;
    }

    public static int scaleDelayForPlayers(int baseDelayTicks,
            int validPlayerCount) {
        if (baseDelayTicks < 0) return -1;
        return Math.max(1, (int) Math.round(baseDelayTicks
                * playerIntervalMultiplier(validPlayerCount)));
    }

    private static int minutes(int minutes) {
        return Math.max(1, minutes * TICKS_PER_MINUTE);
    }
}
