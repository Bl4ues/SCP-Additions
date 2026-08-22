package net.mcreator.scpadditions.client;

import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

/** Presentation-only SCP-939 model polish. */
public final class Scp939ModelPolish {
    /*
     * These are the actual animation-space X rotations authored in the walk clip,
     * sampled every 0.15 seconds across its 1.8 second loop. They are deliberately
     * not values from the .geo model. Geometry rotations already define the rest
     * pose and must never be applied again as animation deltas.
     */
    private static final float[] FRONT_UPPER_X = {
            9.8374F, 17.0119F, 9.5990F, -12.0239F,
            -8.9736F, -6.0630F, -3.3023F, -0.7005F,
            1.7361F, 4.0048F, 6.1069F, 8.0481F
    };
    private static final float[] FRONT_LOWER_X = {
            -2.5549F, -22.0952F, -20.8063F, 6.4158F,
            4.2688F, 2.4021F, 0.8267F, -0.4504F,
            -1.4282F, -2.1122F, -2.5149F, -2.6549F
    };
    private static final float[] REAR_HIP_X = {
            3.2570F, 5.3630F, 7.3956F, 9.3541F,
            10.0844F, 2.5828F, -10.4605F, -8.1139F,
            -5.7691F, -3.4441F, -1.1559F, 1.0815F
    };
    private static final float[] REAR_KNEE_X = {
            0.9733F, 1.6666F, 2.3656F, 3.0799F,
            -2.2622F, -3.5834F, -4.2636F, -3.1126F,
            -2.1199F, -1.2477F, -0.4610F, 0.2708F
    };
    private static final float[] REAR_HOCK_X = {
            -3.1695F, -4.6353F, -5.8476F, -6.8328F,
            -19.7043F, -15.1942F, 12.0059F, 8.6578F,
            5.6475F, 2.9692F, 0.6150F, -1.4260F
    };
    private static final float[] REAR_PAW_X = {
            -1.0784F, -1.6101F, -2.0686F, -2.4620F,
            -5.8916F, -4.3021F, 3.9498F, 2.8808F,
            1.9042F, 1.0216F, 0.2323F, -0.4662F
    };

    private Scp939ModelPolish() {
    }

    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // Intentionally empty. Positional foot servos previously accumulated on
        // mutable GeckoLib bones and detached paws from their chains.
    }

    /**
     * Re-phases the existing walk into a diagonal canine gait without changing
     * the rig's rest pose.
     *
     * <p>The authored clip has good individual limb arcs, but plays them as four
     * conspicuously isolated beats in game. Rather than inventing new joint angles,
     * this method reuses those exact authored curves and only changes when each
     * chain plays them: right-front with left-rear, then left-front with right-rear.
     * Y/Z rotations remain entirely owned by the GeckoLib clip.</p>
     */
    public static void applyLocomotionBlend(Scp939Model<?> model,
            Scp939Entity entity, AnimationState<?> animationState) {
        if (model == null || entity == null || animationState == null) return;

        byte action = entity.getAction();
        boolean compatibleAction = action == Scp939Entity.ACTION_NONE
                || action == Scp939Entity.ACTION_BITE
                || action == Scp939Entity.ACTION_MIMIC;
        if (!compatibleAction || !entity.onGround()) return;

        Scp939AwarenessState awareness = entity.getAwarenessState();
        if (awareness == Scp939AwarenessState.CONFIRMED_HUNT
                || awareness == Scp939AwarenessState.LOST_SEARCH) {
            return;
        }

        double speed = Math.sqrt(entity.getDeltaMovement()
                .horizontalDistanceSqr());
        if (speed < 0.004D) return;

        float blend = Mth.clamp((float) (speed / 0.055D), 0.30F, 1.0F);
        float cycle = Mth.positiveModulo(
                animationState.getLimbSwing() * 0.6662F
                        / (Mth.PI * 2.0F),
                1.0F);

        // Pair A: right front + left rear.
        applyFront(model, "right_arm", "right_hand", cycle, blend);
        applyRear(model, "left_leg", "left_foot", "left_foot2",
                "left_foot3", cycle + 0.25F, blend);

        // Pair B: left front + right rear, half a stride later.
        applyFront(model, "left_arm", "left_hand", cycle + 0.50F, blend);
        applyRear(model, "right_leg", "right_foot", "right_foot2",
                "right_foot3", cycle + 0.75F, blend);
    }

    private static void applyFront(Scp939Model<?> model,
            String upperName, String lowerName, float phase, float blend) {
        blendX(model, upperName, sampleDegrees(FRONT_UPPER_X, phase), blend);
        blendX(model, lowerName, sampleDegrees(FRONT_LOWER_X, phase), blend);
    }

    private static void applyRear(Scp939Model<?> model,
            String hipName, String kneeName, String hockName, String pawName,
            float phase, float blend) {
        blendX(model, hipName, sampleDegrees(REAR_HIP_X, phase), blend);
        blendX(model, kneeName, sampleDegrees(REAR_KNEE_X, phase), blend);
        blendX(model, hockName, sampleDegrees(REAR_HOCK_X, phase), blend);
        blendX(model, pawName, sampleDegrees(REAR_PAW_X, phase), blend);
    }

    private static float sampleDegrees(float[] values, float phase) {
        float wrapped = Mth.positiveModulo(phase, 1.0F);
        float scaled = wrapped * values.length;
        int base = Mth.floor(scaled);
        int index = base % values.length;
        int next = (index + 1) % values.length;
        float alpha = scaled - base;
        alpha = alpha * alpha * (3.0F - 2.0F * alpha);
        return Mth.lerp(alpha, values[index], values[next]) * Mth.DEG_TO_RAD;
    }

    /** Adds only a restrained head/neck lead during turns. */
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

    private static void blendX(Scp939Model<?> model, String boneName,
            float targetX, float blend) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        float amount = Mth.clamp(blend, 0.0F, 1.0F);
        bone.setRotX(Mth.lerp(amount, bone.getRotX(), targetX));
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
