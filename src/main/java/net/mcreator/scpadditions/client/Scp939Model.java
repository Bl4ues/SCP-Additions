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
        applyProceduralTurn(animatable);
        applyPounceAirPose(animatable);
    }

    /**
     * Lets the long torso absorb part of a direction change instead of rotating
     * like one rigid Minecraft cuboid. The authored gait remains the base pose;
     * these are deliberately small additive offsets.
     */
    private void applyProceduralTurn(T animatable) {
        if (animatable.getAction() != Scp939Entity.ACTION_NONE
                || animatable.getDeltaMovement().horizontalDistanceSqr()
                < 0.00008D) {
            return;
        }

        float turnDegrees = Mth.wrapDegrees(
                animatable.getYRot() - animatable.yRotO);
        float turn = Mth.clamp(turnDegrees / 12.0F, -1.0F, 1.0F);
        if (Math.abs(turn) < 0.02F) return;

        addRotation("939body", 0.0F, turn * 0.045F, -turn * 0.025F);
        addRotation("torso", 0.0F, turn * 0.070F, -turn * 0.020F);
        addRotation("torso2", 0.0F, turn * 0.060F, -turn * 0.014F);
        addRotation("torso3", 0.0F, turn * 0.045F, 0.0F);
        addRotation("neck", 0.0F, -turn * 0.105F, turn * 0.018F);
        addRotation("head", 0.0F, -turn * 0.045F, 0.0F);
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
