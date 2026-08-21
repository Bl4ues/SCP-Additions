package net.mcreator.scpadditions.client;

import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.Scp939Entity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

/** Presentation-only SCP-939 model polish. */
public final class Scp939ModelPolish {
    private Scp939ModelPolish() {
    }

    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // Do not servo individual limbs here. The authored gait already contains
        // the complete swing/stance cycle; the locomotion controller synchronizes
        // that cycle to filtered world displacement without mutating bone chains.
    }

    public static void applyLocomotionBlend(Scp939Model<?> model,
            Scp939Entity entity, AnimationState<?> animationState) {
        // GeckoLib's locomotion controller owns the eight-tick crossfade. Keeping
        // a second pose blender here caused visible double-easing at transitions.
    }

    /**
     * Adds only a restrained head/neck lead during turns.
     *
     * <p>Older polish twisted every torso segment in opposite directions and
     * added roll on top of entity yaw. That made the quadruped look crooked and
     * occasionally twitch when pathfinding adjusted its heading. The body now
     * follows the entity normally while the sensory end of the animal leads the
     * turn by a few degrees.</p>
     */
    public static void applyTurnMotion(Scp939Model<?> model,
            Scp939Entity entity) {
        if (model == null || entity == null) return;
        byte action = entity.getAction();
        if (action == Scp939Entity.ACTION_POUNCE
                || action == Scp939Entity.ACTION_PIN_LAND
                || action == Scp939Entity.ACTION_MAUL
                || action == Scp939Entity.ACTION_KICKED
                || action == Scp939Entity.ACTION_HURT
                || action == Scp939Entity.ACTION_DEATH) {
            return;
        }

        float difference = Mth.clamp(Mth.wrapDegrees(
                entity.getYHeadRot() - entity.yBodyRot), -18.0F, 18.0F)
                * Mth.DEG_TO_RAD;
        if (Math.abs(difference) < 0.003F) return;

        addRotation(model, "torso3", 0.0F, difference * 0.07F, 0.0F);
        addRotation(model, "neck", 0.0F, difference * 0.30F, 0.0F);
        addRotation(model, "head", 0.0F, difference * 0.16F, 0.0F);
    }

    private static void addRotation(Scp939Model<?> model, String boneName,
            float x, float y, float z) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }
}
