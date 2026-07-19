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
    private static float influenceProgress;
    private static float targetInfluenceProgress;

    private Scp012ClientState() {
    }

    public static void update(boolean nextActive, BlockPos nextTarget,
                              float nextContactProgress,
                              boolean nextDamageActive) {
        active = nextActive;
        damageActive = nextActive && nextDamageActive;
        target = nextTarget == null ? BlockPos.ZERO : nextTarget.immutable();
        targetContactProgress = Mth.clamp(nextContactProgress, 0.0F, 1.0F);
        if (!active) {
            targetContactProgress = 0.0F;
            targetInfluenceProgress = 0.0F;
        }
    }

    /**
     * Updates the pre-contact psychosis strength from the player's proximity to
     * the active composition. A faint baseline begins at the edge of the
     * influence radius and rises to 80% near SCP-012, leaving the final contact
     * sequence to intensify the effect to its maximum.
     */
    public static void setInfluenceProximity(float proximity) {
        if (!active) {
            targetInfluenceProgress = 0.0F;
            return;
        }
        float normalized = Mth.clamp(proximity, 0.0F, 1.0F);
        float curved = (float) Math.pow(normalized, 1.10D);
        targetInfluenceProgress = Mth.clamp(0.08F + curved * 0.72F,
                0.0F, 0.80F);
    }

    public static void tick() {
        contactProgress += (targetContactProgress - contactProgress) * 0.18F;
        if (Math.abs(contactProgress - targetContactProgress) < 0.002F) {
            contactProgress = targetContactProgress;
        }

        influenceProgress += (targetInfluenceProgress - influenceProgress) * 0.16F;
        if (Math.abs(influenceProgress - targetInfluenceProgress) < 0.002F) {
            influenceProgress = targetInfluenceProgress;
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

    public static float psychosisProgress() {
        return Math.max(influenceProgress, contactProgress);
    }

    public static boolean shouldRenderOverlay() {
        return psychosisProgress() > 0.01F
                || targetInfluenceProgress > 0.01F
                || targetContactProgress > 0.01F;
    }

    public static void clear() {
        active = false;
        damageActive = false;
        target = BlockPos.ZERO;
        contactProgress = 0.0F;
        targetContactProgress = 0.0F;
        influenceProgress = 0.0F;
        targetInfluenceProgress = 0.0F;
    }
}
