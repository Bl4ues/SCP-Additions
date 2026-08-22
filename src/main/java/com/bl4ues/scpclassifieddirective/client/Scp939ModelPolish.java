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
     * The gait is advanced by real horizontal distance. The SCP-939 rig faces
     * toward negative local Z, so positive X rotation brings a hanging limb
     * forward and negative X rotation carries it behind the body. The previous
     * pass had that stance sweep backwards: paws were being animated forward
     * while the entity itself moved forward, which visually guaranteed skating.
     *
     * 1.35 blocks is intentionally close to the actual reach of this rig. A
     * longer virtual stride cannot be cancelled by the available joint motion
     * and turns every stance phase into a slide.
     */
    public static final double WALK_STRIDE_BLOCKS = 1.35D;
    private static final float STANCE_END = 0.66F;

    /*
     * Lateral-sequence four-beat walk. Local limb phase zero is touchdown:
     * right hind -> right fore -> left hind -> left fore.
     */
    private static final float RIGHT_REAR_CONTACT = 0.00F;
    private static final float RIGHT_FRONT_CONTACT = 0.25F;
    private static final float LEFT_REAR_CONTACT = 0.50F;
    private static final float LEFT_FRONT_CONTACT = 0.75F;

    private static final Map<Scp939Entity, WalkClock> WALK_CLOCKS =
            new WeakHashMap<>();

    private Scp939ModelPolish() {
    }

    /** The obsolete world-space paw servo remains deliberately disabled. */
    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // The distance-driven joint cycle provides the stance compensation.
    }

    /** Applies a restrained, weight-bearing canine walk. */
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

        applyBodyMotion(model, phase);
        applyRear(model, "right_leg", "right_foot", "right_foot2",
                "right_foot3", phase, RIGHT_REAR_CONTACT);
        applyFront(model, "right_arm", "right_hand",
                phase, RIGHT_FRONT_CONTACT);
        applyRear(model, "left_leg", "left_foot", "left_foot2",
                "left_foot3", phase, LEFT_REAR_CONTACT);
        applyFront(model, "left_arm", "left_hand",
                phase, LEFT_FRONT_CONTACT);
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

    /**
     * Keep the torso almost quiet. The old pass added enough independent roll
     * and yaw to make the already unusual silhouette look rubbery. A walking
     * 939 should read as heavy first, animated second.
     */
    private static void applyBodyMotion(Scp939Model<?> model, float phase) {
        float cycle = phase * (float) (Math.PI * 2.0D);
        float doubleCycle = cycle * 2.0F;
        float side = Mth.sin(cycle);
        float support = Mth.cos(doubleCycle);

        setAnimationRotationDelta(model, "939body",
                0.55F + support * 0.22F,
                side * 0.22F,
                0.0F);
        setAnimationRotationDelta(model, "torso",
                -0.35F - support * 0.15F,
                -side * 0.28F,
                0.0F);
        setAnimationRotationDelta(model, "torso2",
                0.45F,
                side * 0.20F,
                0.0F);
        setAnimationRotationDelta(model, "torso3",
                -0.25F,
                -side * 0.18F,
                0.0F);
        setAnimationRotationDelta(model, "neck",
                0.70F - support * 0.20F,
                -side * 0.10F,
                0.0F);
        setAnimationRotationDelta(model, "head",
                -0.18F + support * 0.08F,
                0.0F,
                0.0F);
    }

    private static void applyFront(Scp939Model<?> model,
            String upperName, String lowerName, float globalPhase,
            float contactPhase) {
        FrontPose pose = frontPose(localPhase(globalPhase, contactPhase));

        // No decorative lateral flaring here. This rig already has wide paws;
        // extra Y/Z rotation only makes the limb arc look like paddling.
        setAnimationRotationDelta(model, upperName,
                pose.upper(), 0.0F, 0.0F);
        setAnimationRotationDelta(model, lowerName,
                pose.lower(), 0.0F, 0.0F);
    }

    private static void applyRear(Scp939Model<?> model,
            String hipName, String kneeName, String hockName, String pawName,
            float globalPhase, float contactPhase) {
        RearPose pose = rearPose(localPhase(globalPhase, contactPhase));

        setAnimationRotationDelta(model, hipName,
                pose.hip(), 0.0F, 0.0F);
        setAnimationRotationDelta(model, kneeName,
                pose.knee(), 0.0F, 0.0F);
        setAnimationRotationDelta(model, hockName,
                pose.hock(), 0.0F, 0.0F);
        setAnimationRotationDelta(model, pawName,
                pose.paw(), 0.0F, 0.0F);
    }

    /**
     * Touchdown starts with the paw in front of the shoulder. Through stance the
     * upper limb sweeps rearward slightly farther than before so the paw better
     * matches ground travel. Recovery deliberately stays shallow: unload the paw,
     * carry it forward, then extend. The forelimb should never tuck into a large
     * circular loop during an ordinary walk.
     */
    private static FrontPose frontPose(float phase) {
        if (phase < STANCE_END) {
            float u = smoothstep(phase / STANCE_END);
            return new FrontPose(
                    Mth.lerp(u, 15.0F, -15.0F),
                    Mth.lerp(u, -3.0F, 2.0F));
        }

        float swing = (phase - STANCE_END) / (1.0F - STANCE_END);
        if (swing < 0.22F) {
            float u = smoothstep(swing / 0.22F);
            return new FrontPose(
                    Mth.lerp(u, -15.0F, -10.0F),
                    Mth.lerp(u, 2.0F, -5.0F));
        }
        if (swing < 0.78F) {
            float u = smoothstep((swing - 0.22F) / 0.56F);
            return new FrontPose(
                    Mth.lerp(u, -10.0F, 12.0F),
                    -5.0F);
        }

        float u = smoothstep((swing - 0.78F) / 0.22F);
        return new FrontPose(
                Mth.lerp(u, 12.0F, 15.0F),
                Mth.lerp(u, -5.0F, -3.0F));
    }

    /**
     * Rear-leg timing follows the same forward-to-rear stance sweep. The support
     * arc now reaches a little farther behind the hip to finish cancelling the
     * small amount of visible ground slip from the previous pass. Most folding
     * still happens only after toe-off.
     */
    private static RearPose rearPose(float phase) {
        if (phase < STANCE_END) {
            float u = smoothstep(phase / STANCE_END);
            return new RearPose(
                    Mth.lerp(u, 12.0F, -12.5F),
                    Mth.lerp(u, 2.0F, -3.5F),
                    Mth.lerp(u, -5.0F, 10.5F),
                    Mth.lerp(u, -1.0F, 3.5F));
        }

        float swing = (phase - STANCE_END) / (1.0F - STANCE_END);
        if (swing < 0.30F) {
            float u = smoothstep(swing / 0.30F);
            return new RearPose(
                    Mth.lerp(u, -12.5F, -14.0F),
                    Mth.lerp(u, -3.5F, -6.0F),
                    Mth.lerp(u, 10.5F, 20.0F),
                    Mth.lerp(u, 3.5F, 6.0F));
        }
        if (swing < 0.72F) {
            float u = smoothstep((swing - 0.30F) / 0.42F);
            return new RearPose(
                    Mth.lerp(u, -14.0F, 9.0F),
                    Mth.lerp(u, -6.0F, -3.0F),
                    Mth.lerp(u, 20.0F, 2.0F),
                    Mth.lerp(u, 6.0F, 1.0F));
        }

        float u = smoothstep((swing - 0.72F) / 0.28F);
        return new RearPose(
                Mth.lerp(u, 9.0F, 12.0F),
                Mth.lerp(u, -3.0F, 2.0F),
                Mth.lerp(u, 2.0F, -5.0F),
                Mth.lerp(u, 1.0F, -1.0F));
    }

    private static float localPhase(float globalPhase, float contactPhase) {
        return Mth.positiveModulo(globalPhase - contactPhase, 1.0F);
    }

    private static float smoothstep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
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

    /** Replaces the authored walk rotation for one bone on all three axes. */
    private static void setAnimationRotationDelta(Scp939Model<?> model,
            String boneName, float xDegrees, float yDegrees, float zDegrees) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null || bone.getInitialSnapshot() == null) return;

        var rest = bone.getInitialSnapshot();
        bone.setRotX(rest.getRotX() + xDegrees * Mth.DEG_TO_RAD);
        bone.setRotY(rest.getRotY() + yDegrees * Mth.DEG_TO_RAD);
        bone.setRotZ(rest.getRotZ() + zDegrees * Mth.DEG_TO_RAD);
    }

    private static void addRotation(Scp939Model<?> model, String boneName,
            float x, float y, float z) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    private record FrontPose(float upper, float lower) {
    }

    private record RearPose(float hip, float knee, float hock, float paw) {
    }

    private static final class WalkClock {
        private int lastTick = Integer.MIN_VALUE;
        private float phase;
        private float phaseStart;
        private float phaseAdvance;
    }
}
