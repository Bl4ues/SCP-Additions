package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.scp939.Scp939AwarenessState;
import net.minecraft.util.Mth;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.Map;
import java.util.WeakHashMap;

/** Presentation-only SCP-939 model polish. */
public final class Scp939ModelPolish {
    /*
     * The previous procedural walk tried to invent a new limb motion on top of
     * an already-authored GeckoLib walk. That produced the conspicuous folding
     * and skating seen in game. These curves are the actual X-axis animation
     * deltas sampled from the SCP-939 walk clip every 0.15 seconds across its
     * 1.8 second loop. We keep the rig-specific arcs and only change their phase
     * and distance calibration.
     *
     * A normal canine walk is a lateral-sequence four-beat gait. The gait clock
     * below advances from real horizontal displacement, so a paw in stance moves
     * backward through model space at the same rate the entity moves forward in
     * world space. It therefore appears planted instead of sliding along the
     * floor, without a fragile world-space IK/servo fighting GeckoLib.
     */
    public static final double WALK_STRIDE_BLOCKS = 0.78D;

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

    /*
     * Approximate contact points in the original authored curves are front=0.25
     * and rear=0.50. Offsetting each chain from those contacts gives the desired
     * RH -> RF -> LH -> LF touchdown order without changing the limb shapes.
     */
    private static final float RIGHT_REAR_SAMPLE_OFFSET = 0.50F;
    private static final float RIGHT_FRONT_SAMPLE_OFFSET = 0.07F;
    private static final float LEFT_REAR_SAMPLE_OFFSET = 0.00F;
    private static final float LEFT_FRONT_SAMPLE_OFFSET = 0.57F;

    // Small root-only amplification increases physical reach while preserving
    // the authored elbow/stifle/hock relationships instead of over-flexing them.
    private static final float FRONT_ROOT_SCALE = 1.25F;
    private static final float REAR_ROOT_SCALE = 1.18F;

    private static final Map<Scp939Entity, WalkClock> WALK_CLOCKS =
            new WeakHashMap<>();

    private Scp939ModelPolish() {
    }

    /** The obsolete world-space paw servo remains deliberately disabled. */
    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // Distance-locked gait timing provides stance anchoring without mutating
        // bone positions or depending on GeckoLib matrix tracking.
    }

    /** Applies a distance-driven lateral-sequence canine walk. */
    public static void applyWalkGait(Scp939Model<?> model,
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

        float phase = walkPhase(entity, animationState.getPartialTick());

        applyRear(model, "right_leg", "right_foot", "right_foot2",
                "right_foot3", phase + RIGHT_REAR_SAMPLE_OFFSET);
        applyFront(model, "right_arm", "right_hand",
                phase + RIGHT_FRONT_SAMPLE_OFFSET);
        applyRear(model, "left_leg", "left_foot", "left_foot2",
                "left_foot3", phase + LEFT_REAR_SAMPLE_OFFSET);
        applyFront(model, "left_arm", "left_hand",
                phase + LEFT_FRONT_SAMPLE_OFFSET);
    }

    private static float walkPhase(Scp939Entity entity, float partialTick) {
        WalkClock clock = WALK_CLOCKS.computeIfAbsent(entity,
                ignored -> new WalkClock());
        int tick = entity.tickCount;

        if (clock.lastTick != tick) {
            boolean stale = clock.lastTick == Integer.MIN_VALUE
                    || tick - clock.lastTick > 2;
            if (stale) {
                clock.phase = 0.0F;
            }

            double dx = entity.getX() - entity.xo;
            double dz = entity.getZ() - entity.zo;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (!Double.isFinite(distance) || distance > 0.65D) {
                distance = 0.0D;
            }

            clock.phaseStart = clock.phase;
            clock.phaseAdvance = (float) (distance / WALK_STRIDE_BLOCKS);
            clock.phase = Mth.positiveModulo(
                    clock.phase + clock.phaseAdvance, 1.0F);
            clock.lastTick = tick;
        }

        float partial = Mth.clamp(partialTick, 0.0F, 1.0F);
        return Mth.positiveModulo(
                clock.phaseStart + clock.phaseAdvance * partial, 1.0F);
    }

    private static void applyFront(Scp939Model<?> model,
            String upperName, String lowerName, float phase) {
        float local = Mth.positiveModulo(phase, 1.0F);
        setAnimationXDelta(model, upperName,
                sampleCyclic(FRONT_UPPER_X, local) * FRONT_ROOT_SCALE);
        setAnimationXDelta(model, lowerName,
                sampleCyclic(FRONT_LOWER_X, local));
    }

    private static void applyRear(Scp939Model<?> model,
            String hipName, String kneeName, String hockName, String pawName,
            float phase) {
        float local = Mth.positiveModulo(phase, 1.0F);
        setAnimationXDelta(model, hipName,
                sampleCyclic(REAR_HIP_X, local) * REAR_ROOT_SCALE);
        setAnimationXDelta(model, kneeName,
                sampleCyclic(REAR_KNEE_X, local));
        setAnimationXDelta(model, hockName,
                sampleCyclic(REAR_HOCK_X, local));
        setAnimationXDelta(model, pawName,
                sampleCyclic(REAR_PAW_X, local));
    }

    /**
     * Cyclic Catmull-Rom interpolation preserves the rounded motion of the source
     * animation while allowing the phase to be driven continuously by distance.
     */
    private static float sampleCyclic(float[] samples, float phase) {
        float scaled = Mth.positiveModulo(phase, 1.0F) * samples.length;
        int i1 = Mth.floor(scaled) % samples.length;
        float t = scaled - Mth.floor(scaled);
        int i0 = (i1 - 1 + samples.length) % samples.length;
        int i2 = (i1 + 1) % samples.length;
        int i3 = (i1 + 2) % samples.length;

        float p0 = samples[i0];
        float p1 = samples[i1];
        float p2 = samples[i2];
        float p3 = samples[i3];
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * ((2.0F * p1)
                + (-p0 + p2) * t
                + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * t2
                + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * t3);
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

    /**
     * Replaces only the X animation delta. Y/Z remain exactly as authored by the
     * GeckoLib clip; zeroing them was one source of the previous robotic gait.
     */
    private static void setAnimationXDelta(Scp939Model<?> model,
            String boneName, float xDegrees) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null || bone.getInitialSnapshot() == null) return;

        var rest = bone.getInitialSnapshot();
        bone.setRotX(rest.getRotX() + xDegrees * Mth.DEG_TO_RAD);
    }

    private static void addRotation(Scp939Model<?> model, String boneName,
            float x, float y, float z) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    private static final class WalkClock {
        private int lastTick = Integer.MIN_VALUE;
        private float phase;
        private float phaseStart;
        private float phaseAdvance;
    }
}
