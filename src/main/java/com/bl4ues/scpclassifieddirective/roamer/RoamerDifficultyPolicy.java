package com.bl4ues.scpclassifieddirective.roamer;

import net.minecraft.world.Difficulty;

/** Difficulty-specific timing rules for natural roamer encounters. */
public final class RoamerDifficultyPolicy {
    private static final int TICKS_PER_MINUTE = 20 * 60;

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

    private static int minutes(int minutes) {
        return Math.max(1, minutes * TICKS_PER_MINUTE);
    }
}
