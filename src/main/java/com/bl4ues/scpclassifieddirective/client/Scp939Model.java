package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.scp939.Scp939AwarenessState;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class Scp939Model<T extends Scp939Entity> extends GeoModel<T> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/entity/scp939.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/entities/scp939.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "animations/entity/scp939.animation.json");

    /*
     * The authored walk already contains the correct swing motion. The only
     * missing piece is stance: a paw that is carrying weight must remain nearly
     * fixed in world space while the body travels over it. These thresholds use
     * the authored distal-joint poses to detect which paws are actually in their
     * planted part of the 1.8 second walk cycle instead of inventing a second,
     * unrelated gait clock in Java.
     */
    private static final float FRONT_STANCE_THRESHOLD =
            -8.0F * Mth.DEG_TO_RAD;
    private static final float REAR_STANCE_THRESHOLD =
            -10.0F * Mth.DEG_TO_RAD;
    private static final float FRONT_MAX_LOCK = 12.0F * Mth.DEG_TO_RAD;
    private static final float REAR_MAX_LOCK = 10.0F * Mth.DEG_TO_RAD;
    private static final float MAX_LOCK_STEP = 2.2F * Mth.DEG_TO_RAD;
    private static final float LOCK_GAIN = 1.25F;
    private static final float LOCK_RELEASE = 0.58F;
    private static final float REANCHOR_TURN_DEGREES = 18.0F;
    private static final double REANCHOR_DISTANCE_SQR = 0.65D * 0.65D;
    private static final double WALK_REFERENCE_SPEED = 0.09D;

    private static final int MODE_IDLE = 0;
    private static final int MODE_WALK = 1;
    private static final int MODE_RUN = 2;
    private static final int MODE_SEARCH = 3;
    private static final int MODE_LISTEN = 4;
    private static final int MODE_SPECIAL = -1;
    private static final double LOCOMOTION_BLEND_TICKS = 5.0D;
    private static final String[] LOCOMOTION_BLEND_BONES = {
            "939body", "torso", "torso2", "torso3", "neck", "head", "jaw",
            "left_arm", "left_hand", "right_arm", "right_hand",
            "left_leg", "left_foot", "left_foot2", "left_foot3",
            "right_leg", "right_foot", "right_foot2", "right_foot3"
    };

    private final Map<Scp939Entity, FootLockState> footLocks =
            new WeakHashMap<>();
    private final Map<Scp939Entity, LocomotionBlendState> locomotionBlends =
            new WeakHashMap<>();

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId,
            AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        applyTurnLead(animatable);
        applyPounceAirPose(animatable);
        applyWalkFootLocking(animatable);
        applyLocomotionBlend(animatable, animationState);
    }

    /**
     * Turning is handled primarily by smooth entity rotation. Only the neck and
     * head lead a moving body slightly so the animal does not rotate like a
     * rigid display stand.
     */
    private void applyTurnLead(T animatable) {
        if (animatable.getAction() != Scp939Entity.ACTION_NONE
                || animatable.getDeltaMovement().horizontalDistanceSqr()
                < 0.00008D) {
            return;
        }

        float difference = Mth.clamp(Mth.wrapDegrees(
                animatable.getYHeadRot() - animatable.yBodyRot),
                -22.0F, 22.0F) * Mth.DEG_TO_RAD;
        if (Math.abs(difference) < 0.004F) return;
        addRotation("neck", 0.0F, difference * 0.42F, 0.0F);
        addRotation("head", 0.0F, difference * 0.18F, 0.0F);
    }

    /** Keeps the launch silhouette extended while the entity is genuinely airborne. */
    private void applyPounceAirPose(T animatable) {
        if (animatable.getAction() != Scp939Entity.ACTION_POUNCE
                || animatable.onGround()) {
            return;
        }
        addRotation("939body", -0.055F, 0.0F, 0.0F);
        addRotation("neck", 0.070F, 0.0F, 0.0F);
        addRotation("left_arm", -0.090F, 0.0F, -0.025F);
        addRotation("right_arm", -0.090F, 0.0F, 0.025F);
        addRotation("left_leg", 0.075F, 0.0F, 0.0F);
        addRotation("right_leg", 0.075F, 0.0F, 0.0F);
    }

    /**
     * World-space stance locking layered on top of the authored walk.
     *
     * Swinging paws are deliberately untouched: their motion in the animation
     * is already correct. When a distal joint enters its authored support pose,
     * we remember that paw's world-space X/Z position. On subsequent game ticks
     * a small servo rotates only the connected upper limb enough to cancel the
     * world-space slide produced by the entity moving forward. The paw is then
     * released as soon as the animation enters its swing pose.
     *
     * No bone positions are translated here. That is important: GeckoLib bones
     * are mutable and the old additive position correction accumulated between
     * renders, eventually pulling limbs away from their parents and into the
     * void. Rotation preserves the hierarchy and cannot detach a paw from its
     * limb chain.
     */
    private void applyWalkFootLocking(T animatable) {
        byte action = animatable.getAction();
        boolean compatibleAction = action == Scp939Entity.ACTION_NONE
                || action == Scp939Entity.ACTION_BITE
                || action == Scp939Entity.ACTION_MIMIC;
        double speed = Math.sqrt(animatable.getDeltaMovement()
                .horizontalDistanceSqr());
        Scp939AwarenessState awareness = animatable.getAwarenessState();
        boolean running = awareness == Scp939AwarenessState.CONFIRMED_HUNT
                || awareness == Scp939AwarenessState.LOST_SEARCH;
        boolean walking = compatibleAction && animatable.onGround()
                && speed >= 0.004D && !running;

        FootLockState state = footLocks.get(animatable);
        if (!walking) {
            if (state == null) return;
            if (state.lastTick != animatable.tickCount) {
                state.lastTick = animatable.tickCount;
                state.releaseAll();
            }
            applyFootCorrections(state);
            if (state.isReleased()) footLocks.remove(animatable);
            return;
        }

        state = footLocks.computeIfAbsent(animatable,
                ignored -> new FootLockState());
        if (state.lastTick != animatable.tickCount) {
            state.lastTick = animatable.tickCount;
            float speedStrength = Mth.clamp((float) (speed / WALK_REFERENCE_SPEED),
                    0.35F, 1.0F);
            updateFrontLock(animatable, state.frontLeft,
                    "left_arm", "left_hand", speedStrength);
            updateFrontLock(animatable, state.frontRight,
                    "right_arm", "right_hand", speedStrength);
            updateRearLock(animatable, state.rearLeft,
                    "left_leg", "left_foot", "left_foot2", "left_foot3",
                    speedStrength);
            updateRearLock(animatable, state.rearRight,
                    "right_leg", "right_foot", "right_foot2", "right_foot3",
                    speedStrength);
        }
        applyFootCorrections(state);
    }

    private void updateFrontLock(T animatable, LimbLock lock,
            String rootName, String endName, float strength) {
        GeoBone root = bone(rootName);
        GeoBone end = bone(endName);
        if (root == null || end == null) {
            lock.release();
            return;
        }
        boolean stance = end.getRotX() > FRONT_STANCE_THRESHOLD;
        updateLock(animatable, lock, end, stance,
                FRONT_MAX_LOCK * strength);
    }

    private void updateRearLock(T animatable, LimbLock lock,
            String rootName, String firstJointName, String stanceJointName,
            String endName, float strength) {
        GeoBone root = bone(rootName);
        GeoBone first = bone(firstJointName);
        GeoBone stanceJoint = bone(stanceJointName);
        GeoBone end = bone(endName);
        if (root == null || first == null || stanceJoint == null || end == null) {
            lock.release();
            return;
        }
        boolean stance = stanceJoint.getRotX() > REAR_STANCE_THRESHOLD;
        updateLock(animatable, lock, end, stance,
                REAR_MAX_LOCK * strength);
    }

    private void updateLock(T animatable, LimbLock lock, GeoBone end,
            boolean stance, float maximumCorrection) {
        boolean wasTracking = end.isTrackingMatrices();
        Vector3d world = end.getWorldPosition();
        if (!stance || !wasTracking || !validWorldSample(animatable, world)) {
            lock.release();
            return;
        }

        float bodyYaw = animatable.yBodyRot;
        if (!lock.planted) {
            lock.plant(world, bodyYaw);
            return;
        }

        double dx = world.x - lock.anchorX;
        double dz = world.z - lock.anchorZ;
        if (dx * dx + dz * dz > REANCHOR_DISTANCE_SQR
                || Math.abs(Mth.wrapDegrees(bodyYaw - lock.anchorYaw))
                > REANCHOR_TURN_DEGREES) {
            lock.plant(world, bodyYaw);
            return;
        }

        float yaw = bodyYaw * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yaw);
        double forwardZ = Mth.cos(yaw);
        double forwardSlide = dx * forwardX + dz * forwardZ;
        float adjustment = Mth.clamp((float) forwardSlide * LOCK_GAIN,
                -MAX_LOCK_STEP, MAX_LOCK_STEP);
        lock.correction = Mth.clamp(lock.correction + adjustment,
                -maximumCorrection, maximumCorrection);
    }

    private static boolean validWorldSample(Scp939Entity animatable,
            Vector3d sample) {
        if (sample == null || !Double.isFinite(sample.x)
                || !Double.isFinite(sample.y) || !Double.isFinite(sample.z)) {
            return false;
        }
        double dx = sample.x - animatable.getX();
        double dy = sample.y - animatable.getY();
        double dz = sample.z - animatable.getZ();
        return dx * dx + dy * dy + dz * dz < 64.0D;
    }

    private void applyFootCorrections(FootLockState state) {
        applyFrontCorrection("left_arm", "left_hand",
                state.frontLeft.correction);
        applyFrontCorrection("right_arm", "right_hand",
                state.frontRight.correction);
        applyRearCorrection("left_leg", "left_foot", "left_foot2",
                state.rearLeft.correction);
        applyRearCorrection("right_leg", "right_foot", "right_foot2",
                state.rearRight.correction);
    }

    private void applyFrontCorrection(String root, String hand,
            float correction) {
        if (Math.abs(correction) < 0.0001F) return;
        addRotation(root, correction, 0.0F, 0.0F);
        addRotation(hand, -correction * 0.52F, 0.0F, 0.0F);
    }

    private void applyRearCorrection(String root, String foot,
            String foot2, float correction) {
        if (Math.abs(correction) < 0.0001F) return;
        addRotation(root, correction, 0.0F, 0.0F);
        addRotation(foot, -correction * 0.42F, 0.0F, 0.0F);
        addRotation(foot2, -correction * 0.14F, 0.0F, 0.0F);
    }

    /**
     * GeckoLib's controller transition for the listening action is intentionally
     * short because pounce/hurt actions share the same controller. Add a small
     * render-only blend between locomotion states instead, so walk -> listen,
     * walk -> idle and similar changes ease over roughly a quarter second while
     * combat actions remain immediate.
     */
    private void applyLocomotionBlend(T animatable,
            AnimationState<T> animationState) {
        int mode = locomotionMode(animatable);
        if (mode == MODE_SPECIAL) {
            locomotionBlends.remove(animatable);
            return;
        }

        double now = animatable.tickCount + animationState.getPartialTick();
        LocomotionBlendState state = locomotionBlends.computeIfAbsent(animatable,
                ignored -> new LocomotionBlendState());
        if (!state.initialized) {
            state.initialized = true;
            state.lastMode = mode;
            state.lastPose = capturePose();
            return;
        }

        if (mode != state.lastMode) {
            state.fromPose = copyPose(state.lastPose);
            state.blendStartedAt = now;
            state.lastMode = mode;
        }

        if (state.fromPose != null) {
            float amount = Mth.clamp((float) ((now - state.blendStartedAt)
                    / LOCOMOTION_BLEND_TICKS), 0.0F, 1.0F);
            amount = smoothstep(amount);
            blendFromPose(state.fromPose, amount);
            if (amount >= 0.999F) {
                state.fromPose = null;
            }
        }
        state.lastPose = capturePose();
    }

    private int locomotionMode(T animatable) {
        byte action = animatable.getAction();
        if (action == Scp939Entity.ACTION_POUNCE
                || action == Scp939Entity.ACTION_PIN_LAND
                || action == Scp939Entity.ACTION_MAUL
                || action == Scp939Entity.ACTION_KICKED
                || action == Scp939Entity.ACTION_HURT
                || action == Scp939Entity.ACTION_DEATH) {
            return MODE_SPECIAL;
        }
        if (action == Scp939Entity.ACTION_LISTEN) return MODE_LISTEN;

        boolean moving = animatable.getDeltaMovement()
                .horizontalDistanceSqr() > 0.00008D;
        Scp939AwarenessState awareness = animatable.getAwarenessState();
        if (moving) {
            return awareness == Scp939AwarenessState.CONFIRMED_HUNT
                    || awareness == Scp939AwarenessState.LOST_SEARCH
                    ? MODE_RUN : MODE_WALK;
        }
        if (awareness == Scp939AwarenessState.SEARCH) return MODE_SEARCH;
        if (awareness == Scp939AwarenessState.HEARD_SOUND) return MODE_LISTEN;
        return MODE_IDLE;
    }

    private Map<String, BonePose> capturePose() {
        Map<String, BonePose> poses = new HashMap<>();
        for (String name : LOCOMOTION_BLEND_BONES) {
            GeoBone bone = bone(name);
            if (bone == null) continue;
            poses.put(name, new BonePose(bone.getRotX(), bone.getRotY(),
                    bone.getRotZ(), bone.getPosX(), bone.getPosY(),
                    bone.getPosZ()));
        }
        return poses;
    }

    private static Map<String, BonePose> copyPose(Map<String, BonePose> source) {
        return source == null ? new HashMap<>() : new HashMap<>(source);
    }

    private void blendFromPose(Map<String, BonePose> from, float amount) {
        for (Map.Entry<String, BonePose> entry : from.entrySet()) {
            GeoBone bone = bone(entry.getKey());
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

    private GeoBone bone(String boneName) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        return bone instanceof GeoBone geoBone ? geoBone : null;
    }

    private static float smoothstep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private void addRotation(String boneName, float x, float y, float z) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    private static final class FootLockState {
        private int lastTick = Integer.MIN_VALUE;
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

        private boolean isReleased() {
            return frontLeft.isReleased() && frontRight.isReleased()
                    && rearLeft.isReleased() && rearRight.isReleased();
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
            correction *= LOCK_RELEASE;
            if (Math.abs(correction) < 0.0004F) correction = 0.0F;
        }

        private boolean isReleased() {
            return !planted && correction == 0.0F;
        }
    }

    private static final class LocomotionBlendState {
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
