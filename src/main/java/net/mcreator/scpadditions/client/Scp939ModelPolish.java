package net.mcreator.scpadditions.client;

import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.Scp939Entity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.Map;
import java.util.WeakHashMap;

/** Presentation-only SCP-939 model polish. */
public final class Scp939ModelPolish {
    private static final Map<Scp939Entity, TurnState> TURN_STATES =
            new WeakHashMap<>();

    private Scp939ModelPolish() {
    }

    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // The authored gait owns paw motion. Playback timing is handled by the
        // locomotion controller so this hook must not mutate the limb hierarchy.
    }

    public static void applyLocomotionBlend(Scp939Model<?> model,
            Scp939Entity entity, AnimationState<?> animationState) {
        // GeckoLib's locomotion controller owns transitions.
    }

    /**
     * Gives turns a flexible quadruped silhouette. Rotation is distributed down
     * the spine with the head leading and the hips lagging instead of bending
     * every segment by the same amount.
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
            TURN_STATES.remove(entity);
            return;
        }

        TurnState state = TURN_STATES.computeIfAbsent(entity,
                ignored -> new TurnState());
        if (state.lastTick != entity.tickCount) {
            state.lastTick = entity.tickCount;
            float yawStep = Mth.clamp(Mth.wrapDegrees(
                    entity.getYRot() - entity.yRotO), -8.0F, 8.0F);
            state.smoothedTurn += (yawStep - state.smoothedTurn) * 0.24F;
            if (Math.abs(yawStep) < 0.10F) state.smoothedTurn *= 0.84F;
        }

        float turnDegrees = Mth.clamp(state.smoothedTurn, -7.5F, 7.5F);
        if (Math.abs(turnDegrees) < 0.06F) return;
        float turn = turnDegrees * Mth.DEG_TO_RAD;

        addRotation(model, "939body", 0.0F, -turn * 0.22F, turn * 0.030F);
        addRotation(model, "torso", 0.0F, -turn * 0.15F, turn * 0.016F);
        addRotation(model, "torso2", 0.0F, turn * 0.10F, -turn * 0.010F);
        addRotation(model, "torso3", 0.0F, turn * 0.17F, -turn * 0.014F);
        addRotation(model, "neck", 0.0F, turn * 0.40F, -turn * 0.024F);
        addRotation(model, "head", 0.0F, turn * 0.24F, -turn * 0.014F);
    }

    private static void addRotation(Scp939Model<?> model, String boneName,
            float x, float y, float z) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    private static final class TurnState {
        private int lastTick = Integer.MIN_VALUE;
        private float smoothedTurn;
    }
}
