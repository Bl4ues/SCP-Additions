package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class Scp939Model<T extends Scp939Entity> extends GeoModel<T> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpAdditionsMod.MODID, "geo/entity/scp939.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/entities/scp939.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpAdditionsMod.MODID, "animations/entity/scp939.animation.json");

    private static final float WALK_GAIT_PHASE_SCALE = 0.6662F;
    private static final float WALK_SUPPORT_FRACTION = 0.64F;
    private static final float WALK_FRONT_PLANT_RADIANS =
            22.0F * Mth.DEG_TO_RAD;
    private static final float WALK_REAR_PLANT_RADIANS =
            18.0F * Mth.DEG_TO_RAD;
    private static final double WALK_REFERENCE_SPEED = 0.09D;

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
        applyWalkFootPlanting(animatable, animationState);
    }

    /**
     * The old turn pass twisted every torso segment from frame-to-frame yaw and
     * produced a rubber-spine wobble. Turning is handled by smooth entity
     * rotation; only the neck/head lead the body by a small amount.
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
     * Adds a planted stance pass to the authored walking clip without moving
     * bones away from their parents. The previous terrain solver translated the
     * distal bones every render frame; GeckoLib keeps those offsets as mutable
     * bone state, so the corrections accumulated and eventually sent paws below
     * the world while also separating them from the limb chain.
     *
     * This pass instead uses the entity's real limb-swing phase. During the
     * support portion of each step the limb sweeps backward almost linearly as
     * the body advances, then returns forward quickly during the airborne swing
     * portion. That is the visual foot-locking trick used by ordinary procedural
     * quadruped gaits: the paw appears planted instead of sliding with the body,
     * while every joint remains connected because only rotations are changed.
     *
     * The authored run is a bounding gait rather than the walk's left/right
     * pacing gait, so it is intentionally left alone here instead of forcing a
     * walk-style phase correction over it.
     */
    private void applyWalkFootPlanting(T animatable,
            AnimationState<T> animationState) {
        byte action = animatable.getAction();
        boolean locomotionCompatible = action == Scp939Entity.ACTION_NONE
                || action == Scp939Entity.ACTION_BITE
                || action == Scp939Entity.ACTION_MIMIC;
        if (!locomotionCompatible || !animatable.onGround()) return;

        double speed = Math.sqrt(animatable.getDeltaMovement()
                .horizontalDistanceSqr());
        if (speed < 0.004D) return;

        Scp939AwarenessState awareness = animatable.getAwarenessState();
        if (awareness == Scp939AwarenessState.CONFIRMED_HUNT
                || awareness == Scp939AwarenessState.LOST_SEARCH) {
            return;
        }

        float strength = Mth.clamp((float) (speed / WALK_REFERENCE_SPEED),
                0.0F, 1.0F);
        if (strength <= 0.01F) return;

        float phase = animationState.getLimbSwing()
                * WALK_GAIT_PHASE_SCALE;
        float leftSweep = plantedSweep(phase);
        float rightSweep = plantedSweep(phase + Mth.PI);

        float front = WALK_FRONT_PLANT_RADIANS * strength;
        float rear = WALK_REAR_PLANT_RADIANS * strength;

        // The authored walk moves both limbs on a side together. Keep that
        // pacing rhythm, but extend the support sweep enough to counter the
        // entity's actual translation through the world.
        addRotation("left_arm", leftSweep * front, 0.0F, 0.0F);
        addRotation("right_arm", rightSweep * front, 0.0F, 0.0F);
        addRotation("left_leg", leftSweep * rear, 0.0F, 0.0F);
        addRotation("right_leg", rightSweep * rear, 0.0F, 0.0F);

        // Counter-rotate the next joint a little so the distal paw does not
        // point straight into the floor while the upper limb performs the
        // larger anti-slide sweep.
        addRotation("left_hand", -leftSweep * front * 0.34F,
                0.0F, 0.0F);
        addRotation("right_hand", -rightSweep * front * 0.34F,
                0.0F, 0.0F);
        addRotation("left_foot", -leftSweep * rear * 0.26F,
                0.0F, 0.0F);
        addRotation("right_foot", -rightSweep * rear * 0.26F,
                0.0F, 0.0F);
    }

    private static float plantedSweep(float phase) {
        float normalized = Mth.positiveModulo(
                phase / (Mth.PI * 2.0F), 1.0F);
        if (normalized < WALK_SUPPORT_FRACTION) {
            float support = normalized / WALK_SUPPORT_FRACTION;
            return 1.0F - support * 2.0F;
        }

        float swing = (normalized - WALK_SUPPORT_FRACTION)
                / (1.0F - WALK_SUPPORT_FRACTION);
        swing = smoothstep(swing);
        return -1.0F + swing * 2.0F;
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
}
