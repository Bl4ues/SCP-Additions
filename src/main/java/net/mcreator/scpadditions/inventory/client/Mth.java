package net.mcreator.scpadditions.inventory.client;

/** Small package-local clamp helper used by the integrated screen. */
final class Mth {
    private Mth() {
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
