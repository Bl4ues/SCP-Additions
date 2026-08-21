package net.mcreator.scpadditions.client;

import net.mcreator.scpadditions.entity.Scp939Entity;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * Hook target for presentation-only SCP-939 model polish.
 *
 * Paw contact is intentionally not corrected here anymore. The authored walk
 * already contains the stance/swing motion; trying to servo individual GeckoLib
 * bones after animation evaluation fought that motion and still could not make
 * a delayed world-matrix sample behave like true IK. Ground contact is instead
 * preserved by matching locomotion playback speed to real entity displacement.
 *
 * Locomotion transitions are likewise handled by GeckoLib's locomotion
 * controller so both source and destination clips participate in the same
 * transition rather than being blended again after controller evaluation.
 */
public final class Scp939ModelPolish {
    private Scp939ModelPolish() {
    }

    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // Intentionally empty. See class documentation.
    }

    public static void applyLocomotionBlend(Scp939Model<?> model,
            Scp939Entity entity, AnimationState<?> animationState) {
        // Intentionally empty. GeckoLib controller transition owns this now.
    }
}
