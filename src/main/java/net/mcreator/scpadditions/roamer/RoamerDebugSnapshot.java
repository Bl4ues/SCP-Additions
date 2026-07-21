package net.mcreator.scpadditions.roamer;

/** Immutable server snapshot used for low-frequency debug synchronization. */
public record RoamerDebugSnapshot(RoamerType type, RoamerState state,
        RoamerResult result, int nextCheckTick) {
    public int remainingTicks(int currentTick) {
        return state == RoamerState.COUNTDOWN && nextCheckTick >= 0
                ? Math.max(0, nextCheckTick - currentTick) : -1;
    }
}
