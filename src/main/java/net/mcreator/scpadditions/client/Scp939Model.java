package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp939Entity;
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

    private void addRotation(String boneName, float x, float y, float z) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) return;
        bone.setRotX(bone.getRotX() + x);
        bone.setRotY(bone.getRotY() + y);
        bone.setRotZ(bone.getRotZ() + z);
    }
}
