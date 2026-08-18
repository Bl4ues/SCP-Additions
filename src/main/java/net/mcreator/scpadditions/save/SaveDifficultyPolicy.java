package net.mcreator.scpadditions.save;

import net.minecraft.world.Difficulty;

/** Maps vanilla difficulty to SCP Additions save rules and presentation names. */
public final class SaveDifficultyPolicy {
    private SaveDifficultyPolicy() {
    }

    public static boolean allowsQuickSave(Difficulty difficulty) {
        return difficulty == Difficulty.PEACEFUL || difficulty == Difficulty.EASY;
    }

    public static boolean allowsCheckpoint(Difficulty difficulty) {
        return difficulty != Difficulty.HARD;
    }

    public static String displayName(Difficulty difficulty) {
        if (difficulty == null) return "Euclid";
        return switch (difficulty) {
            case PEACEFUL -> "Thaumiel";
            case EASY -> "Safe";
            case NORMAL -> "Euclid";
            case HARD -> "Keter";
        };
    }
}
