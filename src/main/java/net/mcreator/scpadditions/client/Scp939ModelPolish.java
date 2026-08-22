package net.mcreator.scpadditions.client;

import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.Map;
import java.util.WeakHashMap;

/** Presentation-only SCP-939 model polish. */
public final class Scp939ModelPolish {
    /*
     * A walking dog does not trot. Normal canine walking is a symmetrical
     * four-beat lateral-sequence gait: hind foot, fore foot on the same side,
     * then the opposite hind and fore feet. Each limb spends about 60% of the
     * stride in stance, so two or three paws support the body at all times.
     *
     * The SCP-939 rig has roughly 0.9 blocks between shoulder and hip. A 1.30
     * block full stride gives the support leg enough angular travel to stay under
     * the body without the huge skating produced by the old 3.24-block cycle.
     */
    public static final double WALK_STRIDE_BLOCKS = 1.30D;
    private static final float STANCE_FRACTION = 0.60F;

    // Touchdown order during one stride: RH -> RF -> LH -> LF.
    private static final float RIGHT_HIND_TOUCHDOWN = 0.00F;
    private static final float RIGHT_FORE_TOUCHDOWN = 0.18F;
    private static final float LEFT_HIND_TOUCHDOWN = 0.50F;
    private static final float LEFT_FORE_TOUCHDOWN = 0.68F;

    private static final Map<Scp939Entity, WalkClock> WALK_CLOCKS =
            new WeakHashMap<>();

    private Scp939ModelPolish() {
    }

    /**
     * The old world-space paw servo is intentionally disabled. It corrected
     * individual paws after the fact and repeatedly fought both the animation
     * controller and the bone hierarchy. The gait itself now owns stance timing.
     */
    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        // Intentionally empty.
    }

    /**
     * Applies a distance-driven, four-beat canine walk on top of the walk clip.
     *
     * <p>The phase advances from actual horizontal distance travelled rather than
     * elapsed render time. A slower SCP-939 therefore takes the same physical
     * stride more slowly instead of moonwalking over a time-based loop. During
     * stance the limb retracts steadily underneath the travelling body; during
     * swing it flexes, clears the floor, reaches forward and extends for the next
     * touchdown.</p>
     *
     * <p>All targets below are animation deltas. They are added to GeckoLib's
     * initial bone snapshot, preserving the SCP-939 rig's authored 32.5/60/
     * -102.5/54 degree rear-leg rest chain instead of accidentally replacing it.
     * That was the cause of the collapsed, broken-looking hind legs in the
     * previous implementation.</p>
     */
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

        // Lateral-sequence canine walk. A hind paw lands shortly before the fore
        // paw on the same side; the opposite side repeats half a stride later.
        applyRear(model, "right_leg", "right_foot", "right_foot2",
                "right_foot3", localPhase(phase, RIGHT_HIND_TOUCHDOWN));
        applyFront(model, "right_arm", "right_hand",
                localPhase(phase, RIGHT_FORE_TOUCHDOWN));
        applyRear(model, "left_leg", "left_foot", "left_foot2",
                "left_foot3", localPhase(phase, LEFT_HIND_TOUCHDOWN));
        applyFront(model, "left_arm", "left_hand",
                localPhase(phase, LEFT_FORE_TOUCHDOWN));
    }

    private static float walkPhase(Scp939Entity entity, float partialTick) {
        WalkClock clock = WALK_CLOCKS.computeIfAbsent(entity,
                ignored -> new WalkClock());
        int tick = entity.tickCount;

        if (clock.lastTick != tick) {
            boolean stale = clock.lastTick == Integer.MIN_VALUE
                    || tick - clock.lastTick > 2;
            if (stale) {
                // Phase zero is a stable three-leg support pose and also matches
                // the start of the underlying GeckoLib walk body's cycle.
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

    private static float localPhase(float globalPhase, float touchdown) {
        return Mth.positiveModulo(globalPhase - touchdown, 1.0F);
    }

    private static void applyFront(Scp939Model<?> model,
            String upperName, String lowerName, float phase) {
        FrontPose pose = frontPose(phase);
        setAnimationDelta(model, upperName, pose.upperX, 0.0F, 0.0F);
        setAnimationDelta(model, lowerName, pose.lowerX, 0.0F, 0.0F);
    }

    private static FrontPose frontPose(float phase) {
        if (phase < STANCE_FRACTION) {
            // Forelimbs are primarily weight-bearing struts. Keep the paw on the
            // floor while the shoulder travels over it, with only mild elbow give.
            float stance = phase / STANCE_FRACTION;
            float upper = Mth.lerp(stance, -17.0F, 15.0F);
            float lower = Mth.lerp(stance, 5.0F, -4.0F)
                    - Mth.sin(Mth.PI * stance) * 1.8F;
            return new FrontPose(upper, lower);
        }

        float swing = (phase - STANCE_FRACTION)
                / (1.0F - STANCE_FRACTION);
        if (swing < 0.32F) {
            // Toe-off: fold the distal segment quickly so the paw clears ground.
            float t = smoothstep(swing / 0.32F);
            return new FrontPose(
                    Mth.lerp(t, 15.0F, 20.0F),
                    Mth.lerp(t, -4.0F, -24.0F));
        }
        if (swing < 0.72F) {
            // Recovery: bring the whole forelimb forward while it remains folded.
            float t = smoothstep((swing - 0.32F) / 0.40F);
            return new FrontPose(
                    Mth.lerp(t, 20.0F, -8.0F),
                    Mth.lerp(t, -24.0F, -13.0F));
        }

        // Reach: extend just before touchdown instead of snapping the paw down.
        float t = smoothstep((swing - 0.72F) / 0.28F);
        return new FrontPose(
                Mth.lerp(t, -8.0F, -17.0F),
                Mth.lerp(t, -13.0F, 5.0F));
    }

    private static void applyRear(Scp939Model<?> model,
            String hipName, String kneeName, String hockName, String pawName,
            float phase) {
        RearPose pose = rearPose(phase);
        setAnimationDelta(model, hipName, pose.hipX, 0.0F, 0.0F);
        setAnimationDelta(model, kneeName, pose.kneeX, 0.0F, 0.0F);
        setAnimationDelta(model, hockName, pose.hockX, 0.0F, 0.0F);
        setAnimationDelta(model, pawName, pose.pawX, 0.0F, 0.0F);
    }

    private static RearPose rearPose(float phase) {
        if (phase < STANCE_FRACTION) {
            // Hind limbs are the propulsive pair. The hip continuously retracts
            // through stance while stifle/hock compress slightly under load and
            // extend into toe-off.
            float stance = phase / STANCE_FRACTION;
            float load = Mth.sin(Mth.PI * stance);
            float hip = Mth.lerp(stance, -12.0F, 14.0F);
            float knee = Mth.lerp(stance, -2.0F, 2.0F) - load * 3.2F;
            float hock = Mth.lerp(stance, 8.0F, -9.0F) - load * 2.0F;
            float paw = Mth.lerp(stance, 3.0F, -2.0F);
            return new RearPose(hip, knee, hock, paw);
        }

        float swing = (phase - STANCE_FRACTION)
                / (1.0F - STANCE_FRACTION);
        if (swing < 0.30F) {
            // Toe-off and early recovery: fold the long rear chain compactly.
            float t = smoothstep(swing / 0.30F);
            return new RearPose(
                    Mth.lerp(t, 14.0F, 11.0F),
                    Mth.lerp(t, 2.0F, -7.0F),
                    Mth.lerp(t, -9.0F, -23.0F),
                    Mth.lerp(t, -2.0F, -7.0F));
        }
        if (swing < 0.72F) {
            // Protract the folded limb underneath the pelvis.
            float t = smoothstep((swing - 0.30F) / 0.42F);
            return new RearPose(
                    Mth.lerp(t, 11.0F, -7.0F),
                    Mth.lerp(t, -7.0F, -6.0F),
                    Mth.lerp(t, -23.0F, -11.0F),
                    Mth.lerp(t, -7.0F, -3.0F));
        }

        // Late swing: extend the hock and paw for a quiet, forward touchdown.
        float t = smoothstep((swing - 0.72F) / 0.28F);
        return new RearPose(
                Mth.lerp(t, -7.0F, -12.0F),
                Mth.lerp(t, -6.0F, -2.0F),
                Mth.lerp(t, -11.0F, 8.0F),
                Mth.lerp(t, -3.0F, 3.0F));
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

    private static void setAnimationDelta(Scp939Model<?> model,
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

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private record FrontPose(float upperX, float lowerX) {
    }

    private record RearPose(float hipX, float kneeX, float hockX, float pawX) {
    }

    private static final class WalkClock {
        private int lastTick = Integer.MIN_VALUE;
        private float phase;
        private float phaseStart;
        private float phaseAdvance;
    }
}
