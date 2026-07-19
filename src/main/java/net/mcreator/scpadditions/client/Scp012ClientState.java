package net.mcreator.scpadditions.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/** Client-only SCP-012 target, sound state and subliminal intensity. */
public final class Scp012ClientState {
    private static boolean active;
    private static boolean damageActive;
    private static BlockPos target = BlockPos.ZERO;
    private static float contactProgress;
    private static float targetContactProgress;

    private Scp012ClientState() {
    }

    public static void update(boolean nextActive, BlockPos nextTarget,
                              float nextContactProgress,
                              boolean nextDamageActive) {
        active = nextActive;
        damageActive = nextActive && nextDamageActive;
        target = nextTarget == null ? BlockPos.ZERO : nextTarget.immutable();
        targetContactProgress = Mth.clamp(nextContactProgress, 0.0F, 1.0F);
        if (!active) targetContactProgress = 0.0F;
    }

    public static void tick() {
        contactProgress += (targetContactProgress - contactProgress) * 0.18F;
        if (Math.abs(contactProgress - targetContactProgress) < 0.002F) {
            contactProgress = targetContactProgress;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isDamageActive() {
        return damageActive;
    }

    public static BlockPos target() {
        return target;
    }

    public static float contactProgress() {
        return contactProgress;
    }

    public static boolean shouldRenderOverlay() {
        return contactProgress > 0.01F || targetContactProgress > 0.01F;
    }

    public static void clear() {
        active = false;
        damageActive = false;
        target = BlockPos.ZERO;
        contactProgress = 0.0F;
        targetContactProgress = 0.0F;
    }
}
