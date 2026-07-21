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
    private static final ResourceLocation ANIMATION = new ResourceLocation(ScpAdditionsMod.MODID, "animations/entity/scp106.animation.json");

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

        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head == null) {
            return;
        }

        EntityModelData modelData = animationState.getData(
                DataTickets.ENTITY_MODEL_DATA);
        float yaw = Mth.clamp(modelData.netHeadYaw(), -30.0F, 30.0F);
        float pitch = Mth.clamp(modelData.headPitch(), -12.0F, 12.0F);

        head.setRotY(head.getRotY() + yaw * Mth.DEG_TO_RAD);
        head.setRotX(head.getRotX() + pitch * Mth.DEG_TO_RAD);
    }
}
