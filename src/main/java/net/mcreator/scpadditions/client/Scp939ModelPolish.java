package net.mcreator.scpadditions.client;

import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

/** Presentation-only SCP-939 model polish. */
public final class Scp939ModelPolish {
    private static final float FRONT_BASE = 0.0F * Mth.DEG_TO_RAD;
    private static final float FRONT_HAND_BASE = 0.0F * Mth.DEG_TO_RAD;
    private static final float REAR_HIP_BASE = 32.5F * Mth.DEG_TO_RAD;
    private static final float REAR_KNEE_BASE = 60.0F * Mth.DEG_TO_RAD;
    private static final float REAR_HOCK_BASE = -102.5F * Mth.DEG_TO_RAD;
    private static final float REAR_PAW_BASE = 54.0F * Mth.DEG_TO_RAD;

    private static final float FRONT_STRIDE = 25.0F * Mth.DEG_TO_RAD;
    private static final float FRONT_COUNTER = 10.0F * Mth.DEG_TO_RAD;
    private static final float FRONT_LIFT_FLEX = 18.0F * Mth.DEG_TO_RAD;
    private static final float REAR_STRIDE = 21.0F * Mth.DEG_TO_RAD;
    private static final float REAR_KNEE_COUNTER = 11.0F * Mth.DEG_TO_RAD;
    private static final float REAR_SWING_FLEX = 25.0F * Mth.DEG_TO_RAD;
    private static final float REAR_PAW_FLEX = 12.0F * Mth.DEG_TO_RAD;
    private static final float SUPPORT_FRACTION = 0.61F;
    private static final double WALK_REFERENCE_SPEED = 0.09D;

    private Scp939ModelPolish() {
    }

    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // The old independent per-paw servo fought the authored animation and
        // produced detached or skating feet. The gait below owns stance/swing
        // timing as one coherent quadruped cycle instead.
    }

    /**
     * Replaces the authored four-beat-looking walk with a canine diagonal gait.
     *
     * <p>The Blockbench clip moved the four paws in visibly isolated turns. That
     * reads acceptably in the editor but, once the body actually travels through
     * the world, it looks like the creature is carefully operating four separate
     * levers. A dog-like trot is much clearer in motion: left-front moves with
     * right-rear, then right-front with left-rear. The support part of each cycle
     * is deliberately longer than the recovery swing so a loaded paw sweeps back
     * beneath the body rather than moonwalking forward across the floor.</p>
     *
     * <p>This runs after GeckoLib has evaluated the authored clip. Only the limb
     * chains are replaced; body, spine, neck and head motion from the animation
     * remain intact. Running keeps its separate authored gallop.</p>
     */
    public static void applyLocomotionBlend(Scp939Model<?> model,
            Scp939Entity entity, AnimationState<?> animationState) {
        if (model == null || entity == null || animationState == null) return;
        if (entity.getAction() != Scp939Entity.ACTION_NONE || !entity.onGround()) {
            return;
        }

        Scp939AwarenessState awareness = entity.getAwarenessState();
        if (awareness == Scp939AwarenessState.CONFIRMED_HUNT
                || awareness == Scp939AwarenessState.LOST_SEARCH) {
            return;
        }

        double speed = Math.sqrt(entity.getDeltaMovement()
                .horizontalDistanceSqr());
        if (speed < 0.004D) return;

        float gaitStrength = Mth.clamp((float) (speed / WALK_REFERENCE_SPEED),
                0.38F, 1.0F);
        float replaceBlend = Mth.clamp((float) (speed / 0.055D),
                0.08F, 1.0F);
        float phase = animationState.getLimbSwing() * 0.6662F;

        GaitSample diagonalA = gaitSample(phase);
        GaitSample diagonalB = gaitSample(phase + Mth.PI);

        // Diagonal pair A: left front + right rear.
        applyFrontLeg(model, "left_arm", "left_hand",
                diagonalA, gaitStrength, replaceBlend);
        applyRearLeg(model, "right_leg", "right_foot", "right_foot2",
                "right_foot3", diagonalA, gaitStrength, replaceBlend);

        // Diagonal pair B: right front + left rear.
        applyFrontLeg(model, "right_arm", "right_hand",
                diagonalB, gaitStrength, replaceBlend);
        applyRearLeg(model, "left_leg", "left_foot", "left_foot2",
                "left_foot3", diagonalB, gaitStrength, replaceBlend);

        // A tiny weight transfer keeps the torso from reading as a rigid table.
        float transfer = Mth.sin(phase * 2.0F) * gaitStrength;
        addRotation(model, "939body",
                transfer * 0.55F * Mth.DEG_TO_RAD,
                0.0F,
                transfer * 0.35F * Mth.DEG_TO_RAD);
    }

    private static void applyFrontLeg(Scp939Model<?> model,
            String upperName, String lowerName, GaitSample sample,
            float strength, float replaceBlend) {
        float upper = FRONT_BASE + sample.sweep * FRONT_STRIDE * strength;
        float lower = FRONT_HAND_BASE
                - sample.sweep * FRONT_COUNTER * strength
                - sample.lift * FRONT_LIFT_FLEX * strength;
        blendRotation(model, upperName, upper, 0.0F, 0.0F, replaceBlend);
        blendRotation(model, lowerName, lower, 0.0F, 0.0F, replaceBlend);
    }

    private static void applyRearLeg(Scp939Model<?> model,
            String hipName, String kneeName, String hockName, String pawName,
            GaitSample sample, float strength, float replaceBlend) {
        float hip = REAR_HIP_BASE + sample.sweep * REAR_STRIDE * strength;
        float knee = REAR_KNEE_BASE
                - sample.sweep * REAR_KNEE_COUNTER * strength
                - sample.lift * 7.0F * Mth.DEG_TO_RAD * strength;
        float hock = REAR_HOCK_BASE
                + sample.lift * REAR_SWING_FLEX * strength;
        float paw = REAR_PAW_BASE
                - sample.lift * REAR_PAW_FLEX * strength;

        blendRotation(model, hipName, hip, 0.0F, 0.0F, replaceBlend);
        blendRotation(model, kneeName, knee, 0.0F, 0.0F, replaceBlend);
        blendRotation(model, hockName, hock, 0.0F, 0.0F, replaceBlend);
        blendRotation(model, pawName, paw, 0.0F, 0.0F, replaceBlend);
    }

    private static GaitSample gaitSample(float phase) {
        float cycle = Mth.positiveModulo(phase / (Mth.PI * 2.0F), 1.0F);
        if (cycle < SUPPORT_FRACTION) {
            float support = cycle / SUPPORT_FRACTION;
            return new GaitSample(1.0F - support * 2.0F, 0.0F);
        }

        float recovery = (cycle - SUPPORT_FRACTION)
                / (1.0F - SUPPORT_FRACTION);
        float smooth = smoothstep(recovery);
        float sweep = -1.0F + smooth * 2.0F;
        float lift = Mth.sin(Mth.PI * recovery);
        return new GaitSample(sweep, Math.max(0.0F, lift));
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
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

    private static void blendRotation(Scp939Model<?> model, String boneName,
            float targetX, float targetY, float targetZ, float blend) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        float amount = Mth.clamp(blend, 0.0F, 1.0F);
        bone.setRotX(Mth.lerp(amount, bone.getRotX(), targetX));
        bone.setRotY(Mth.lerp(amount, bone.getRotY(), targetY));
        bone.setRotZ(Mth.lerp(amount, bone.getRotZ(), targetZ));
    }

    private static void addRotation(Scp939Model<?> model, String boneName,
            float x, float y, float z) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    private record GaitSample(float sweep, float lift) {
    }
}
