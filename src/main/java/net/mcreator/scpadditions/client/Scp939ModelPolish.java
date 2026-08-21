package net.mcreator.scpadditions.client;

import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import org.joml.Vector3d;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Render-only locomotion polish for SCP-939.
 *
 * Kept outside the mixin package deliberately. Mixin helper/nested classes may
 * be renamed while being merged into the target class, and direct references to
 * their original mixin-package names are forbidden at runtime. Keeping all
 * state and helper types here lets the mixin remain a tiny delegation shim.
 */
public final class Scp939ModelPolish {
    private static final float FRONT_STANCE = -8.0F * Mth.DEG_TO_RAD;
    private static final float REAR_STANCE = -10.0F * Mth.DEG_TO_RAD;
    private static final float FRONT_MAX = 36.0F * Mth.DEG_TO_RAD;
    private static final float REAR_MAX = 31.0F * Mth.DEG_TO_RAD;
    private static final float MAX_STEP = 2.4F * Mth.DEG_TO_RAD;
    private static final float LOCK_GAIN = 0.82F;
    private static final float RELEASE = 0.72F;
    private static final float REANCHOR_TURN = 24.0F;
    private static final double REANCHOR_DISTANCE_SQR = 1.35D * 1.35D;
    private static final double WALK_REFERENCE_SPEED = 0.09D;
    private static final double BLEND_TICKS = 9.0D;

    // Offsets from each tracked distal-bone pivot, in authored model pixels.
    private static final float FRONT_CONTACT_Y = -10.6109F;
    private static final float FRONT_CONTACT_Z = -10.4604F;
    private static final float REAR_CONTACT_Y = -0.3289F;
    private static final float REAR_CONTACT_Z = -4.3225F;

    private static final String[] BLEND_BONES = {
            "939body", "torso", "torso2", "torso3", "neck", "head", "jaw",
            "left_arm", "left_hand", "right_arm", "right_hand",
            "left_leg", "left_foot", "left_foot2", "left_foot3",
            "right_leg", "right_foot", "right_foot2", "right_foot3"
    };

    private static final Map<Scp939Entity, FootState> FOOT_STATES =
            new WeakHashMap<>();
    private static final Map<Scp939Entity, BlendState> BLEND_STATES =
            new WeakHashMap<>();

    private Scp939ModelPolish() {
    }

    public static void applyWalkFootLocking(Scp939Model<?> model,
            Scp939Entity entity) {
        byte action = entity.getAction();
        boolean compatibleAction = action == Scp939Entity.ACTION_NONE
                || action == Scp939Entity.ACTION_BITE
                || action == Scp939Entity.ACTION_MIMIC;
        double speed = Math.sqrt(entity.getDeltaMovement()
                .horizontalDistanceSqr());
        Scp939AwarenessState awareness = entity.getAwarenessState();
        boolean running = awareness == Scp939AwarenessState.CONFIRMED_HUNT
                || awareness == Scp939AwarenessState.LOST_SEARCH;
        boolean walking = compatibleAction && entity.onGround()
                && speed >= 0.004D && !running;

        FootState state = FOOT_STATES.get(entity);
        if (!walking) {
            if (state == null) return;
            state.releaseAll();
            applyCorrections(model, state);
            if (state.released()) FOOT_STATES.remove(entity);
            return;
        }

        state = FOOT_STATES.computeIfAbsent(entity, ignored -> new FootState());
        float strength = Mth.clamp((float) (speed / WALK_REFERENCE_SPEED),
                0.45F, 1.0F);

        GeoBone leftHand = bone(model, "left_hand");
        GeoBone rightHand = bone(model, "right_hand");
        GeoBone leftFoot2 = bone(model, "left_foot2");
        GeoBone rightFoot2 = bone(model, "right_foot2");
        GeoBone leftFoot3 = bone(model, "left_foot3");
        GeoBone rightFoot3 = bone(model, "right_foot3");

        // Update every rendered frame. Entity movement is interpolated between
        // ticks, so tick-only feedback still permits visible inter-frame skate.
        updateLock(entity, state.frontLeft,
                contactPoint(leftHand, FRONT_CONTACT_Y, FRONT_CONTACT_Z),
                leftHand != null && leftHand.getRotX() > FRONT_STANCE,
                FRONT_MAX * strength);
        updateLock(entity, state.frontRight,
                contactPoint(rightHand, FRONT_CONTACT_Y, FRONT_CONTACT_Z),
                rightHand != null && rightHand.getRotX() > FRONT_STANCE,
                FRONT_MAX * strength);
        updateLock(entity, state.rearLeft,
                contactPoint(leftFoot3, REAR_CONTACT_Y, REAR_CONTACT_Z),
                leftFoot2 != null && leftFoot2.getRotX() > REAR_STANCE,
                REAR_MAX * strength);
        updateLock(entity, state.rearRight,
                contactPoint(rightFoot3, REAR_CONTACT_Y, REAR_CONTACT_Z),
                rightFoot2 != null && rightFoot2.getRotX() > REAR_STANCE,
                REAR_MAX * strength);

        applyCorrections(model, state);
    }

    /**
     * Uses the previous rendered frame's tracked world matrix and transforms the
     * actual toe contact point instead of the distal bone origin.
     */
    private static Vector3d contactPoint(GeoBone bone, float localYPixels,
            float localZPixels) {
        if (bone == null) return null;
        boolean hadMatrix = bone.isTrackingMatrices();
        var matrix = bone.getWorldSpaceMatrix();
        if (!hadMatrix) return null;

        Vector4f point = matrix.transform(new Vector4f(0.0F,
                localYPixels / 16.0F, localZPixels / 16.0F, 1.0F));
        return new Vector3d(point.x(), point.y(), point.z());
    }

    private static void updateLock(Scp939Entity entity, LimbLock lock,
            Vector3d world, boolean stance, float maximumCorrection) {
        if (!stance || !validWorldSample(entity, world)) {
            lock.release();
            return;
        }

        float bodyYaw = entity.yBodyRot;
        if (!lock.planted) {
            lock.plant(world, bodyYaw);
            return;
        }

        double dx = world.x - lock.anchorX;
        double dz = world.z - lock.anchorZ;
        double distanceSqr = dx * dx + dz * dz;
        if (distanceSqr > REANCHOR_DISTANCE_SQR
                || Math.abs(Mth.wrapDegrees(bodyYaw - lock.anchorYaw))
                > REANCHOR_TURN) {
            lock.plant(world, bodyYaw);
            return;
        }

        float yaw = bodyYaw * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yaw);
        double forwardZ = Mth.cos(yaw);
        double forwardDrift = dx * forwardX + dz * forwardZ;

        // Positive X rotation pushes the limb model-forward, so feedback must
        // oppose the measured forward drift rather than reinforce it.
        float adjustment = Mth.clamp(-(float) forwardDrift * LOCK_GAIN,
                -MAX_STEP, MAX_STEP);
        lock.correction = Mth.clamp(lock.correction + adjustment,
                -maximumCorrection, maximumCorrection);
    }

    private static boolean validWorldSample(Scp939Entity entity,
            Vector3d sample) {
        if (sample == null || !Double.isFinite(sample.x)
                || !Double.isFinite(sample.y) || !Double.isFinite(sample.z)) {
            return false;
        }
        double dx = sample.x - entity.getX();
        double dy = sample.y - entity.getY();
        double dz = sample.z - entity.getZ();
        return dx * dx + dy * dy + dz * dz < 64.0D;
    }

    private static void applyCorrections(Scp939Model<?> model,
            FootState state) {
        applyFront(model, "left_arm", "left_hand", state.frontLeft.correction);
        applyFront(model, "right_arm", "right_hand", state.frontRight.correction);
        applyRear(model, "left_leg", "left_foot", "left_foot2",
                state.rearLeft.correction);
        applyRear(model, "right_leg", "right_foot", "right_foot2",
                state.rearRight.correction);
    }

    private static void applyFront(Scp939Model<?> model, String root,
            String hand, float correction) {
        if (Math.abs(correction) < 0.0001F) return;
        addRotation(model, root, correction, 0.0F, 0.0F);
        addRotation(model, hand, -correction * 0.38F, 0.0F, 0.0F);
    }

    private static void applyRear(Scp939Model<?> model, String root,
            String foot, String foot2, float correction) {
        if (Math.abs(correction) < 0.0001F) return;
        addRotation(model, root, correction, 0.0F, 0.0F);
        addRotation(model, foot, -correction * 0.31F, 0.0F, 0.0F);
        addRotation(model, foot2, -correction * 0.10F, 0.0F, 0.0F);
    }

    public static void applyLocomotionBlend(Scp939Model<?> model,
            Scp939Entity entity, AnimationState<?> animationState) {
        int mode = locomotionMode(entity);
        if (mode < 0) {
            BLEND_STATES.remove(entity);
            return;
        }

        double now = entity.tickCount + animationState.getPartialTick();
        BlendState state = BLEND_STATES.computeIfAbsent(entity,
                ignored -> new BlendState());
        if (!state.initialized) {
            state.initialized = true;
            state.lastMode = mode;
            state.lastPose = capturePose(model);
            return;
        }

        if (mode != state.lastMode) {
            state.fromPose = state.lastPose == null
                    ? new HashMap<>() : new HashMap<>(state.lastPose);
            state.blendStartedAt = now;
            state.lastMode = mode;
        }

        if (state.fromPose != null) {
            float amount = Mth.clamp((float) ((now - state.blendStartedAt)
                    / BLEND_TICKS), 0.0F, 1.0F);
            amount = amount * amount * (3.0F - 2.0F * amount);
            blendFromPose(model, state.fromPose, amount);
            if (amount >= 0.999F) state.fromPose = null;
        }

        state.lastPose = capturePose(model);
    }

    private static int locomotionMode(Scp939Entity entity) {
        byte action = entity.getAction();
        if (action == Scp939Entity.ACTION_POUNCE
                || action == Scp939Entity.ACTION_PIN_LAND
                || action == Scp939Entity.ACTION_MAUL
                || action == Scp939Entity.ACTION_KICKED
                || action == Scp939Entity.ACTION_HURT
                || action == Scp939Entity.ACTION_DEATH) {
            return -1;
        }
        if (action == Scp939Entity.ACTION_LISTEN) return 4;

        boolean moving = entity.getDeltaMovement()
                .horizontalDistanceSqr() > 0.00008D;
        Scp939AwarenessState awareness = entity.getAwarenessState();
        if (moving) {
            return awareness == Scp939AwarenessState.CONFIRMED_HUNT
                    || awareness == Scp939AwarenessState.LOST_SEARCH ? 2 : 1;
        }
        if (awareness == Scp939AwarenessState.SEARCH) return 3;
        if (awareness == Scp939AwarenessState.HEARD_SOUND) return 4;
        return 0;
    }

    private static Map<String, BonePose> capturePose(Scp939Model<?> model) {
        Map<String, BonePose> poses = new HashMap<>();
        for (String name : BLEND_BONES) {
            GeoBone bone = bone(model, name);
            if (bone == null) continue;
            poses.put(name, new BonePose(bone.getRotX(), bone.getRotY(),
                    bone.getRotZ(), bone.getPosX(), bone.getPosY(),
                    bone.getPosZ()));
        }
        return poses;
    }

    private static void blendFromPose(Scp939Model<?> model,
            Map<String, BonePose> from, float amount) {
        for (Map.Entry<String, BonePose> entry : from.entrySet()) {
            GeoBone bone = bone(model, entry.getKey());
            if (bone == null) continue;
            BonePose pose = entry.getValue();
            bone.setRotX(Mth.lerp(amount, pose.rotX, bone.getRotX()));
            bone.setRotY(Mth.lerp(amount, pose.rotY, bone.getRotY()));
            bone.setRotZ(Mth.lerp(amount, pose.rotZ, bone.getRotZ()));
            bone.setPosX(Mth.lerp(amount, pose.posX, bone.getPosX()));
            bone.setPosY(Mth.lerp(amount, pose.posY, bone.getPosY()));
            bone.setPosZ(Mth.lerp(amount, pose.posZ, bone.getPosZ()));
        }
    }

    private static GeoBone bone(Scp939Model<?> model, String name) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(name);
        return bone instanceof GeoBone geoBone ? geoBone : null;
    }

    private static void addRotation(Scp939Model<?> model, String name,
            float x, float y, float z) {
        CoreGeoBone bone = model.getAnimationProcessor().getBone(name);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    private static final class FootState {
        private final LimbLock frontLeft = new LimbLock();
        private final LimbLock frontRight = new LimbLock();
        private final LimbLock rearLeft = new LimbLock();
        private final LimbLock rearRight = new LimbLock();

        private void releaseAll() {
            frontLeft.release();
            frontRight.release();
            rearLeft.release();
            rearRight.release();
        }

        private boolean released() {
            return frontLeft.released() && frontRight.released()
                    && rearLeft.released() && rearRight.released();
        }
    }

    private static final class LimbLock {
        private boolean planted;
        private double anchorX;
        private double anchorZ;
        private float anchorYaw;
        private float correction;

        private void plant(Vector3d world, float yaw) {
            planted = true;
            anchorX = world.x;
            anchorZ = world.z;
            anchorYaw = yaw;
        }

        private void release() {
            planted = false;
            correction *= RELEASE;
            if (Math.abs(correction) < 0.0004F) correction = 0.0F;
        }

        private boolean released() {
            return !planted && correction == 0.0F;
        }
    }

    private static final class BlendState {
        private boolean initialized;
        private int lastMode;
        private double blendStartedAt;
        private Map<String, BonePose> fromPose;
        private Map<String, BonePose> lastPose;
    }

    private record BonePose(float rotX, float rotY, float rotZ,
            float posX, float posY, float posZ) {
    }
}
