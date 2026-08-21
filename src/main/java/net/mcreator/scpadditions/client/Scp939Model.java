package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp939Entity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.WeakHashMap;

public class Scp939Model<T extends Scp939Entity> extends GeoModel<T> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpAdditionsMod.MODID, "geo/entity/scp939.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/entities/scp939.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpAdditionsMod.MODID, "animations/entity/scp939.animation.json");

    // Approximate model-space paw anchors, expressed in world blocks after the
    // entity facing is applied. They deliberately sit under the distal limbs,
    // not at the body origin, so stairs and uneven floors can affect each leg.
    private static final double PAW_LATERAL = 0.23D;
    private static final double FRONT_PAW_FORWARD = 0.72D;
    private static final double REAR_PAW_FORWARD = -0.58D;
    private static final float GROUND_BIAS_PIXELS = -1.05F;
    private static final float MAX_GROUND_CORRECTION_PIXELS = 3.20F;

    private final Map<Scp939Entity, float[]> pawGrounding =
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
        applyPawGrounding(animatable);
    }

    /**
     * The old turn pass twisted every torso segment from frame-to-frame yaw and
     * produced a rubber-spine wobble. Turning is now handled by smooth entity
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
     * Lightweight quadruped IK approximation layered over the authored gait.
     * Minecraft/GeckoLib does not provide an analytic IK solver here, so each
     * paw samples the collision floor beneath its expected world-space anchor.
     * The body takes a small common correction and the distal limb absorbs the
     * remainder. This keeps paws visually attached to flat/stepped floors while
     * preserving the original walk/run animation instead of replacing it.
     */
    private void applyPawGrounding(T animatable) {
        byte action = animatable.getAction();
        boolean compatibleAction = action == Scp939Entity.ACTION_NONE
                || action == Scp939Entity.ACTION_BITE
                || action == Scp939Entity.ACTION_MIMIC;
        if (!animatable.onGround() || !compatibleAction) {
            pawGrounding.remove(animatable);
            return;
        }

        Vec3 forward = horizontal(animatable.getLookAngle());
        if (forward.lengthSqr() < 0.0001D) return;
        forward = forward.normalize();
        Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);

        float[] target = new float[] {
                sampleGroundOffsetPixels(animatable,
                        animatable.position().add(right.scale(-PAW_LATERAL))
                                .add(forward.scale(FRONT_PAW_FORWARD))),
                sampleGroundOffsetPixels(animatable,
                        animatable.position().add(right.scale(PAW_LATERAL))
                                .add(forward.scale(FRONT_PAW_FORWARD))),
                sampleGroundOffsetPixels(animatable,
                        animatable.position().add(right.scale(-PAW_LATERAL))
                                .add(forward.scale(REAR_PAW_FORWARD))),
                sampleGroundOffsetPixels(animatable,
                        animatable.position().add(right.scale(PAW_LATERAL))
                                .add(forward.scale(REAR_PAW_FORWARD)))
        };

        float[] current = pawGrounding.computeIfAbsent(animatable,
                ignored -> target.clone());
        double speed = Math.sqrt(animatable.getDeltaMovement()
                .horizontalDistanceSqr());
        float response = speed > 0.16D ? 0.48F : 0.34F;
        for (int i = 0; i < current.length; i++) {
            current[i] += (target[i] - current[i]) * response;
        }

        float average = (current[0] + current[1] + current[2] + current[3])
                * 0.25F;
        float bodyCorrection = average * 0.20F;
        addPosition("939body", 0.0F, bodyCorrection, 0.0F);
        addPosition("left_hand", 0.0F, current[0] - bodyCorrection, 0.0F);
        addPosition("right_hand", 0.0F, current[1] - bodyCorrection, 0.0F);
        addPosition("left_foot", 0.0F, current[2] - bodyCorrection, 0.0F);
        addPosition("right_foot", 0.0F, current[3] - bodyCorrection, 0.0F);
    }

    private float sampleGroundOffsetPixels(T animatable, Vec3 anchor) {
        Vec3 from = anchor.add(0.0D, 1.15D, 0.0D);
        Vec3 to = anchor.add(0.0D, -1.05D, 0.0D);
        BlockHitResult hit = animatable.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, animatable));
        if (hit.getType() == HitResult.Type.MISS) {
            return GROUND_BIAS_PIXELS;
        }

        float terrainPixels = (float) ((hit.getLocation().y - animatable.getY())
                * 16.0D);
        return Mth.clamp(terrainPixels + GROUND_BIAS_PIXELS,
                -MAX_GROUND_CORRECTION_PIXELS,
                MAX_GROUND_CORRECTION_PIXELS);
    }

    private static Vec3 horizontal(Vec3 value) {
        return new Vec3(value.x, 0.0D, value.z);
    }

    private void addRotation(String boneName, float x, float y, float z) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }

    private void addPosition(String boneName, float x, float y, float z) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setPosX(bone.getPosX() + x);
        bone.setPosY(bone.getPosY() + y);
        bone.setPosZ(bone.getPosZ() + z);
    }
}
