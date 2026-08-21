package net.mcreator.scpadditions.mixin.client;

import net.minecraft.util.Mth;
import net.mcreator.scpadditions.client.Scp939Model;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Corrects two presentation problems that are easier to solve after GeckoLib
 * has authored the pose: planted paws sliding with the entity and locomotion
 * clips snapping into one another.
 *
 * The old paw servo used the right idea but the wrong correction direction and
 * only allowed about ten degrees of compensation. On this model positive X
 * rotation pushes a limb farther in the direction of travel, so the servo was
 * effectively helping the slide it was meant to cancel. This replacement uses
 * the measured world-space drift, applies the opposite correction, and permits
 * enough angular travel for a long quadruped limb to actually stay planted.
 */
@Mixin(Scp939Model.class)
public abstract class Scp939ModelPolishMixin {
    @Unique
    private static final float SCPADDITIONS_FRONT_STANCE =
            -8.0F * Mth.DEG_TO_RAD;
    @Unique
    private static final float SCPADDITIONS_REAR_STANCE =
            -10.0F * Mth.DEG_TO_RAD;
    @Unique
    private static final float SCPADDITIONS_FRONT_MAX =
            36.0F * Mth.DEG_TO_RAD;
    @Unique
    private static final float SCPADDITIONS_REAR_MAX =
            31.0F * Mth.DEG_TO_RAD;
    @Unique
    private static final float SCPADDITIONS_MAX_STEP =
            4.25F * Mth.DEG_TO_RAD;
    @Unique
    private static final float SCPADDITIONS_LOCK_GAIN = 0.92F;
    @Unique
    private static final float SCPADDITIONS_RELEASE = 0.48F;
    @Unique
    private static final float SCPADDITIONS_REANCHOR_TURN = 24.0F;
    @Unique
    private static final double SCPADDITIONS_REANCHOR_DISTANCE_SQR =
            1.35D * 1.35D;
    @Unique
    private static final double SCPADDITIONS_WALK_REFERENCE_SPEED = 0.09D;
    @Unique
    private static final double SCPADDITIONS_BLEND_TICKS = 9.0D;

    @Unique
    private static final String[] SCPADDITIONS_BLEND_BONES = {
            "939body", "torso", "torso2", "torso3", "neck", "head", "jaw",
            "left_arm", "left_hand", "right_arm", "right_hand",
            "left_leg", "left_foot", "left_foot2", "left_foot3",
            "right_leg", "right_foot", "right_foot2", "right_foot3"
    };

    @Unique
    private final Map<Scp939Entity, Scp939FootState> scpadditions$footStates =
            new WeakHashMap<>();
    @Unique
    private final Map<Scp939Entity, Scp939BlendState> scpadditions$blendStates =
            new WeakHashMap<>();

    /** Replace the previous stance servo while leaving the rest of the model pass intact. */
    @Inject(method = "applyWalkFootLocking", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replaceWalkFootLocking(Scp939Entity entity,
            CallbackInfo ci) {
        ci.cancel();
        scpadditions$applyWalkFootLocking(entity);
    }

    /** Replace the five-tick blend with a slower, clearly visible locomotion blend. */
    @Inject(method = "applyLocomotionBlend", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replaceLocomotionBlend(Scp939Entity entity,
            AnimationState<?> animationState, CallbackInfo ci) {
        ci.cancel();
        scpadditions$applyLocomotionBlend(entity, animationState);
    }

    @Unique
    private void scpadditions$applyWalkFootLocking(Scp939Entity entity) {
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

        Scp939FootState state = scpadditions$footStates.get(entity);
        if (!walking) {
            if (state == null) return;
            if (state.lastTick != entity.tickCount) {
                state.lastTick = entity.tickCount;
                state.releaseAll();
            }
            scpadditions$applyCorrections(state);
            if (state.released()) scpadditions$footStates.remove(entity);
            return;
        }

        state = scpadditions$footStates.computeIfAbsent(entity,
                ignored -> new Scp939FootState());
        if (state.lastTick != entity.tickCount) {
            state.lastTick = entity.tickCount;
            float strength = Mth.clamp((float) (speed
                    / SCPADDITIONS_WALK_REFERENCE_SPEED), 0.45F, 1.0F);

            GeoBone leftHand = scpadditions$bone("left_hand");
            GeoBone rightHand = scpadditions$bone("right_hand");
            GeoBone leftFoot2 = scpadditions$bone("left_foot2");
            GeoBone rightFoot2 = scpadditions$bone("right_foot2");
            GeoBone leftFoot3 = scpadditions$bone("left_foot3");
            GeoBone rightFoot3 = scpadditions$bone("right_foot3");

            scpadditions$updateLock(entity, state.frontLeft, leftHand,
                    leftHand != null
                            && leftHand.getRotX() > SCPADDITIONS_FRONT_STANCE,
                    SCPADDITIONS_FRONT_MAX * strength);
            scpadditions$updateLock(entity, state.frontRight, rightHand,
                    rightHand != null
                            && rightHand.getRotX() > SCPADDITIONS_FRONT_STANCE,
                    SCPADDITIONS_FRONT_MAX * strength);
            scpadditions$updateLock(entity, state.rearLeft, leftFoot3,
                    leftFoot2 != null
                            && leftFoot2.getRotX() > SCPADDITIONS_REAR_STANCE,
                    SCPADDITIONS_REAR_MAX * strength);
            scpadditions$updateLock(entity, state.rearRight, rightFoot3,
                    rightFoot2 != null
                            && rightFoot2.getRotX() > SCPADDITIONS_REAR_STANCE,
                    SCPADDITIONS_REAR_MAX * strength);
        }

        scpadditions$applyCorrections(state);
    }

    @Unique
    private void scpadditions$updateLock(Scp939Entity entity,
            Scp939LimbLock lock, GeoBone contactBone, boolean stance,
            float maximumCorrection) {
        if (contactBone == null) {
            lock.release();
            return;
        }

        boolean hadWorldMatrix = contactBone.isTrackingMatrices();
        Vector3d world = contactBone.getWorldPosition();
        if (!stance || !hadWorldMatrix
                || !scpadditions$validWorldSample(entity, world)) {
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
        if (distanceSqr > SCPADDITIONS_REANCHOR_DISTANCE_SQR
                || Math.abs(Mth.wrapDegrees(bodyYaw - lock.anchorYaw))
                > SCPADDITIONS_REANCHOR_TURN) {
            lock.plant(world, bodyYaw);
            return;
        }

        float yaw = bodyYaw * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yaw);
        double forwardZ = Mth.cos(yaw);
        double forwardDrift = dx * forwardX + dz * forwardZ;

        // Negative is intentional. On the 939 rig, positive X rotation sends
        // the paw farther forward, which was the sign error in the old solver.
        float adjustment = Mth.clamp(
                -(float) forwardDrift * SCPADDITIONS_LOCK_GAIN,
                -SCPADDITIONS_MAX_STEP, SCPADDITIONS_MAX_STEP);
        lock.correction = Mth.clamp(lock.correction + adjustment,
                -maximumCorrection, maximumCorrection);
    }

    @Unique
    private static boolean scpadditions$validWorldSample(Scp939Entity entity,
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

    @Unique
    private void scpadditions$applyCorrections(Scp939FootState state) {
        scpadditions$applyFront("left_arm", "left_hand",
                state.frontLeft.correction);
        scpadditions$applyFront("right_arm", "right_hand",
                state.frontRight.correction);
        scpadditions$applyRear("left_leg", "left_foot", "left_foot2",
                state.rearLeft.correction);
        scpadditions$applyRear("right_leg", "right_foot", "right_foot2",
                state.rearRight.correction);
    }

    @Unique
    private void scpadditions$applyFront(String root, String hand,
            float correction) {
        if (Math.abs(correction) < 0.0001F) return;
        scpadditions$addRotation(root, correction, 0.0F, 0.0F);
        scpadditions$addRotation(hand, -correction * 0.38F, 0.0F, 0.0F);
    }

    @Unique
    private void scpadditions$applyRear(String root, String foot,
            String foot2, float correction) {
        if (Math.abs(correction) < 0.0001F) return;
        scpadditions$addRotation(root, correction, 0.0F, 0.0F);
        scpadditions$addRotation(foot, -correction * 0.31F, 0.0F, 0.0F);
        scpadditions$addRotation(foot2, -correction * 0.10F, 0.0F, 0.0F);
    }

    @Unique
    private void scpadditions$applyLocomotionBlend(Scp939Entity entity,
            AnimationState<?> animationState) {
        int mode = scpadditions$locomotionMode(entity);
        if (mode < 0) {
            scpadditions$blendStates.remove(entity);
            return;
        }

        double now = entity.tickCount + animationState.getPartialTick();
        Scp939BlendState state = scpadditions$blendStates.computeIfAbsent(entity,
                ignored -> new Scp939BlendState());
        if (!state.initialized) {
            state.initialized = true;
            state.lastMode = mode;
            state.lastPose = scpadditions$capturePose();
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
                    / SCPADDITIONS_BLEND_TICKS), 0.0F, 1.0F);
            amount = amount * amount * (3.0F - 2.0F * amount);
            scpadditions$blendFromPose(state.fromPose, amount);
            if (amount >= 0.999F) state.fromPose = null;
        }

        state.lastPose = scpadditions$capturePose();
    }

    @Unique
    private static int scpadditions$locomotionMode(Scp939Entity entity) {
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

    @Unique
    private Map<String, Scp939BonePose> scpadditions$capturePose() {
        Map<String, Scp939BonePose> poses = new HashMap<>();
        for (String name : SCPADDITIONS_BLEND_BONES) {
            GeoBone bone = scpadditions$bone(name);
            if (bone == null) continue;
            poses.put(name, new Scp939BonePose(bone.getRotX(), bone.getRotY(),
                    bone.getRotZ(), bone.getPosX(), bone.getPosY(),
                    bone.getPosZ()));
        }
        return poses;
    }

    @Unique
    private void scpadditions$blendFromPose(
            Map<String, Scp939BonePose> from, float amount) {
        for (Map.Entry<String, Scp939BonePose> entry : from.entrySet()) {
            GeoBone bone = scpadditions$bone(entry.getKey());
            if (bone == null) continue;
            Scp939BonePose pose = entry.getValue();
            bone.setRotX(Mth.lerp(amount, pose.rotX, bone.getRotX()));
            bone.setRotY(Mth.lerp(amount, pose.rotY, bone.getRotY()));
            bone.setRotZ(Mth.lerp(amount, pose.rotZ, bone.getRotZ()));
            bone.setPosX(Mth.lerp(amount, pose.posX, bone.getPosX()));
            bone.setPosY(Mth.lerp(amount, pose.posY, bone.getPosY()));
            bone.setPosZ(Mth.lerp(amount, pose.posZ, bone.getPosZ()));
        }
    }

    @Unique
    private GeoBone scpadditions$bone(String name) {
        CoreGeoBone bone = ((Scp939Model<?>) (Object) this)
                .getAnimationProcessor().getBone(name);
        return bone instanceof GeoBone geoBone ? geoBone : null;
    }

    @Unique
    private void scpadditions$addRotation(String name, float x, float y,
            float z) {
        CoreGeoBone bone = ((Scp939Model<?>) (Object) this)
                .getAnimationProcessor().getBone(name);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    @Unique
    private static final class Scp939FootState {
        private int lastTick = Integer.MIN_VALUE;
        private final Scp939LimbLock frontLeft = new Scp939LimbLock();
        private final Scp939LimbLock frontRight = new Scp939LimbLock();
        private final Scp939LimbLock rearLeft = new Scp939LimbLock();
        private final Scp939LimbLock rearRight = new Scp939LimbLock();

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

    @Unique
    private static final class Scp939LimbLock {
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
            correction *= SCPADDITIONS_RELEASE;
            if (Math.abs(correction) < 0.0004F) correction = 0.0F;
        }

        private boolean released() {
            return !planted && correction == 0.0F;
        }
    }

    @Unique
    private static final class Scp939BlendState {
        private boolean initialized;
        private int lastMode;
        private double blendStartedAt;
        private Map<String, Scp939BonePose> fromPose;
        private Map<String, Scp939BonePose> lastPose;
    }

    @Unique
    private record Scp939BonePose(float rotX, float rotY, float rotZ,
            float posX, float posY, float posZ) {
    }
}
