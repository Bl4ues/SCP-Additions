package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp106Model<T extends Scp106Entity> extends GeoModel<T> {
    private static final ResourceLocation MODEL = new ResourceLocation(ScpAdditionsMod.MODID, "geo/entity/scp106.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ScpAdditionsMod.MODID, "textures/entities/scp106.png");
    private static final ResourceLocation BASE_ANIMATION = new ResourceLocation(ScpAdditionsMod.MODID, "animations/entity/scp106.animation.json");
    private static final ResourceLocation PHASE_GROUND_ANIMATION = new ResourceLocation(ScpAdditionsMod.MODID, "animations/entity/scp106_phase_ground.animation.json");
    private static final ResourceLocation EMERGE_GROUND_ANIMATION = new ResourceLocation(ScpAdditionsMod.MODID, "animations/entity/scp106_emerge_ground.animation.json");
    private static final ResourceLocation EMERGE_WALL_ANIMATION = new ResourceLocation(ScpAdditionsMod.MODID, "animations/entity/scp106_emerge_wall.animation.json");

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
        return switch (animatable.getEncounterState()) {
            case 1 -> EMERGE_GROUND_ANIMATION;
            case 2 -> EMERGE_WALL_ANIMATION;
            case 4 -> PHASE_GROUND_ANIMATION;
            default -> BASE_ANIMATION;
        };
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId,
            AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        applyAttackWalkCycle(animatable, animationState);

        if (!animatable.allowsHeadTracking()) return;
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head == null) return;

        EntityModelData modelData = animationState.getData(
                DataTickets.ENTITY_MODEL_DATA);
        float yaw = Mth.clamp(modelData.netHeadYaw(), -30.0F, 30.0F);
        float pitch = Mth.clamp(modelData.headPitch(), -12.0F, 12.0F);

        head.setRotY(head.getRotY() + yaw * Mth.DEG_TO_RAD);
        head.setRotX(head.getRotX() + pitch * Mth.DEG_TO_RAD);
    }

    private void applyAttackWalkCycle(T animatable,
            AnimationState<T> animationState) {
        if (!animatable.isAttacking()
                || animatable.getDeltaMovement().horizontalDistanceSqr()
                < 0.00008D) {
            return;
        }

        CoreGeoBone leftLeg = getAnimationProcessor().getBone("left_leg");
        CoreGeoBone rightLeg = getAnimationProcessor().getBone("right_leg");
        CoreGeoBone leftFoot = getAnimationProcessor().getBone("left_feet");
        CoreGeoBone rightFoot = getAnimationProcessor().getBone("right_feet");
        if (leftLeg == null || rightLeg == null
                || leftFoot == null || rightFoot == null) {
            return;
        }

        float time = animatable.tickCount
                + animationState.getPartialTick();
        float gait = time * 0.46F;
        float movement = (float) Math.sqrt(animatable.getDeltaMovement()
                .horizontalDistanceSqr());
        float strength = Mth.clamp(movement * 9.0F, 0.25F, 0.82F);
        float legSwing = Mth.cos(gait) * 0.50F * strength;
        float footSwing = Mth.sin(gait) * 0.20F * strength;

        leftLeg.setRotX(leftLeg.getRotX() + legSwing);
        rightLeg.setRotX(rightLeg.getRotX() - legSwing);
        leftFoot.setRotX(leftFoot.getRotX()
                - legSwing * 0.44F + footSwing);
        rightFoot.setRotX(rightFoot.getRotX()
                + legSwing * 0.44F - footSwing);
    }
}
