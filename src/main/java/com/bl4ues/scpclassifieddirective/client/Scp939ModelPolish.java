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
     * The walk is advanced by actual horizontal distance rather than elapsed
     * animation time. A complete four-beat stride deliberately covers a long
     * distance: SCP-939 is built like a large, heavy canine, not a small animal
     * taking nervous little steps. This also makes the stance sweep naturally
     * cancel most of the entity's forward travel in world space.
     */
    public static final double WALK_STRIDE_BLOCKS = 2.10D;
    private static final float STANCE_END = 0.68F;

    /*
     * Lateral-sequence four-beat walk. Local limb phase zero is touchdown:
     * right hind -> right fore -> left hind -> left fore. More than half of
     * every limb cycle is stance, so multiple paws are always carrying weight.
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
        // Grounding is built into the spatial stance portion of the gait.
    }

    /** Applies a grounded, weight-bearing large-canine walk. */
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
                "right_foot3", phase, RIGHT_REAR_CONTACT, -1.0F);
        applyFront(model, "right_arm", "right_hand",
                phase, RIGHT_FRONT_CONTACT, -1.0F);
        applyRear(model, "left_leg", "left_foot", "left_foot2",
                "left_foot3", phase, LEFT_REAR_CONTACT, 1.0F);
        applyFront(model, "left_arm", "left_hand",
                phase, LEFT_FRONT_CONTACT, 1.0F);
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
     * The shoulder girdle and pelvis counter-rotate through the step while the
     * spine distributes that motion. The neck then cancels most of it, keeping
     * the head comparatively stable instead of making the whole creature sway as
     * one rigid object.
     */
    private static void applyBodyMotion(Scp939Model<?> model, float phase) {
        float cycle = phase * (float) (Math.PI * 2.0D);
        float doubleCycle = cycle * 2.0F;
        float side = Mth.sin(cycle);
        float sideLead = Mth.sin(cycle + 0.55F);
        float support = Mth.cos(doubleCycle);

        setAnimationRotationDelta(model, "939body",
                0.9F + support * 0.45F,
                side * 0.80F,
                sideLead * 0.55F);
        setAnimationRotationDelta(model, "torso",
                -0.55F - support * 0.30F,
                -side * 1.05F,
                -sideLead * 0.62F);
        setAnimationRotationDelta(model, "torso2",
                0.75F + Mth.sin(cycle + 0.35F) * 0.30F,
                sideLead * 0.90F,
                side * 0.35F);
        setAnimationRotationDelta(model, "torso3",
                -0.35F + Mth.sin(cycle + 0.85F) * 0.28F,
                Mth.sin(cycle + 0.60F) * 1.35F,
                Mth.sin(cycle + 0.50F) * 0.70F);
        setAnimationRotationDelta(model, "neck",
                1.00F - support * 0.42F,
                -side * 0.40F,
                -sideLead * 0.24F);
        setAnimationRotationDelta(model, "head",
                -0.25F + support * 0.18F,
                -side * 0.17F,
                -sideLead * 0.10F);
    }

    private static void applyFront(Scp939Model<?> model,
            String upperName, String lowerName, float globalPhase,
            float contactPhase, float side) {
        float phase = localPhase(globalPhase, contactPhase);
        FrontPose pose = frontPose(phase);
        float lift = swingLift(phase);

        // Clearance is intentionally restrained. A walking animal does not throw
        // its forelimbs sideways; it merely unloads and folds them enough to pass.
        setAnimationRotationDelta(model, upperName,
                pose.upper(), side * 0.28F * lift,
                side * (0.62F + 0.52F * lift));
        setAnimationRotationDelta(model, lowerName,
                pose.lower(), side * 0.09F * lift,
                side * (0.16F + 0.16F * lift));
    }

    private static void applyRear(Scp939Model<?> model,
            String hipName, String kneeName, String hockName, String pawName,
            float globalPhase, float contactPhase, float side) {
        float phase = localPhase(globalPhase, contactPhase);
        RearPose pose = rearPose(phase);
        float lift = swingLift(phase);

        setAnimationRotationDelta(model, hipName,
                pose.hip(), side * 0.20F * lift,
                side * (0.52F + 0.48F * lift));
        setAnimationRotationDelta(model, kneeName,
                pose.knee(), side * 0.08F * lift, side * 0.14F);
        setAnimationRotationDelta(model, hockName,
                pose.hock(), 0.0F, side * 0.10F);
        setAnimationRotationDelta(model, pawName,
                pose.paw(), 0.0F, side * 0.08F);
    }

    /**
     * During stance the shoulder sweeps almost linearly from in front of the
     * body to behind it, approximating a planted paw while the entity advances.
     * Recovery is shorter: unload, fold, carry forward, extend, touch down.
     */
    private static FrontPose frontPose(float phase) {
        if (phase < STANCE_END) {
            float u = phase / STANCE_END;
            return new FrontPose(
                    Mth.lerp(u, -14.0F, 16.0F),
                    Mth.lerp(u, 5.0F, -4.0F));
        }

        float swing = (phase - STANCE_END) / (1.0F - STANCE_END);
        if (swing < 0.30F) {
            float u = smoothstep(swing / 0.30F);
            return new FrontPose(
                    Mth.lerp(u, 16.0F, 22.0F),
                    Mth.lerp(u, -4.0F, -28.0F));
        }
        if (swing < 0.72F) {
            float u = smoothstep((swing - 0.30F) / 0.42F);
            return new FrontPose(
                    Mth.lerp(u, 22.0F, -10.0F),
                    Mth.lerp(u, -28.0F, -9.0F));
        }

        float u = smoothstep((swing - 0.72F) / 0.28F);
        return new FrontPose(
                Mth.lerp(u, -10.0F, -14.0F),
                Mth.lerp(u, -9.0F, 5.0F));
    }

    /** Same support/recovery logic, distributed through hip, knee, hock and paw. */
    private static RearPose rearPose(float phase) {
        if (phase < STANCE_END) {
            float u = phase / STANCE_END;
            return new RearPose(
                    Mth.lerp(u, -11.0F, 11.0F),
                    Mth.lerp(u, -3.5F, 2.5F),
                    Mth.lerp(u, 11.0F, -7.0F),
                    Mth.lerp(u, 4.0F, -2.0F));
        }

        float swing = (phase - STANCE_END) / (1.0F - STANCE_END);
        if (swing < 0.30F) {
            float u = smoothstep(swing / 0.30F);
            return new RearPose(
                    Mth.lerp(u, 11.0F, 16.0F),
                    Mth.lerp(u, 2.5F, -5.0F),
                    Mth.lerp(u, -7.0F, -24.0F),
                    Mth.lerp(u, -2.0F, -7.0F));
        }
        if (swing < 0.74F) {
            float u = smoothstep((swing - 0.30F) / 0.44F);
            return new RearPose(
                    Mth.lerp(u, 16.0F, -8.0F),
                    Mth.lerp(u, -5.0F, -4.0F),
                    Mth.lerp(u, -24.0F, 6.0F),
                    Mth.lerp(u, -7.0F, 3.0F));
        }

        float u = smoothstep((swing - 0.74F) / 0.26F);
        return new RearPose(
                Mth.lerp(u, -8.0F, -11.0F),
                Mth.lerp(u, -4.0F, -3.5F),
                Mth.lerp(u, 6.0F, 11.0F),
                Mth.lerp(u, 3.0F, 4.0F));
    }

    private static float localPhase(float globalPhase, float contactPhase) {
        return Mth.positiveModulo(globalPhase - contactPhase, 1.0F);
    }

    private static float swingLift(float phase) {
        if (phase < STANCE_END) return 0.0F;
        float swing = (phase - STANCE_END) / (1.0F - STANCE_END);
        return Mth.sin(Mth.clamp(swing, 0.0F, 1.0F) * (float) Math.PI);
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
