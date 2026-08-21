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

    /**
     * The authored gait owns paw motion. A post-animation foot servo was tested
     * here previously, but it fought GeckoLib's evaluated pose and produced
     * planted-looking limbs attached to a body that still slid. Playback is now
     * synchronized from actual entity displacement instead.
     */
    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // Intentionally empty.
    }

    public static void applyLocomotionBlend(Scp939Model<?> model,
            Scp939Entity entity, AnimationState<?> animationState) {
        // Intentionally empty. GeckoLib controller transitions own this.
    }

    /**
     * Adds the bit a quadruped needs when it changes heading: the head leads,
     * the spine bends through the turn, and the rear half lags slightly instead
     * of the entire animal rotating like one rigid display model.
     *
     * This is deliberately small and layered after the authored animation. It
     * reacts to real per-tick yaw change, so straight locomotion is untouched.
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
                    entity.getYRot() - entity.yRotO), -10.0F, 10.0F);
            state.smoothedTurn = Mth.lerp(0.42F, state.smoothedTurn, yawStep);
            if (Math.abs(yawStep) < 0.15F) {
                state.smoothedTurn *= 0.78F;
            }
        }

        float turnDegrees = Mth.clamp(state.smoothedTurn, -8.5F, 8.5F);
        if (Math.abs(turnDegrees) < 0.08F) return;
        float turn = turnDegrees * Mth.DEG_TO_RAD;

        // Hindquarters lag, the middle of the spine follows, then neck/head lead.
        addRotation(model, "939body", 0.0F, -turn * 0.16F,
                turn * 0.035F);
        addRotation(model, "torso", 0.0F, -turn * 0.10F,
                turn * 0.018F);
        addRotation(model, "torso2", 0.0F, turn * 0.09F,
                -turn * 0.012F);
        addRotation(model, "torso3", 0.0F, turn * 0.13F,
                -turn * 0.016F);
        addRotation(model, "neck", 0.0F, turn * 0.34F,
                -turn * 0.030F);
        addRotation(model, "head", 0.0F, turn * 0.20F,
                -turn * 0.018F);
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
