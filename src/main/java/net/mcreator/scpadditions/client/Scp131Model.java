package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp131Model<T extends AbstractScp131Entity> extends GeoModel<T> {
    private static final String RESOURCE_NAMESPACE = "scpinventory";
    private static final ResourceLocation MODEL_RESOURCE = new ResourceLocation(RESOURCE_NAMESPACE, "geo/entity/scp_131_a.geo.json");
    private static final ResourceLocation ANIMATION_RESOURCE = new ResourceLocation(RESOURCE_NAMESPACE, "animations/entity/scp_131_a.animation.json");
    private static final float EYE_YAW_SCALE = 0.45F;
    private static final float EYE_PITCH_SCALE = 0.45F;
    private static final float EYE_MAX_YAW = 28.0F;
    private static final float EYE_MAX_PITCH = 18.0F;
    private static final float EYE_DEAD_ZONE = 3.0F;
    private static final float EYE_SNAP_DEGREES = 1.5F;

    private final ResourceLocation textureResource;

    public Scp131Model(ResourceLocation textureResource) {
        this.textureResource = textureResource;
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return textureResource;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION_RESOURCE;
    }

    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        getBone("eye").ifPresent(eye -> rotateEyeBone(eye, animationState));
    }

    private void rotateEyeBone(GeoBone eye, AnimationState<T> animationState) {
        EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        float pitch = stabilize(soften(entityData.headPitch(), EYE_MAX_PITCH)) * EYE_PITCH_SCALE;
        float yaw = stabilize(soften(entityData.netHeadYaw(), EYE_MAX_YAW)) * EYE_YAW_SCALE;
        eye.setRotX(pitch * Mth.DEG_TO_RAD);
        eye.setRotY(yaw * Mth.DEG_TO_RAD);
    }

    private float soften(float value, float limit) {
        if (Math.abs(value) < EYE_DEAD_ZONE) {
            return 0.0F;
        }
        return Mth.clamp(value, -limit, limit);
    }

    private float stabilize(float value) {
        if (EYE_SNAP_DEGREES <= 0.0F) {
            return value;
        }
        return Math.round(value / EYE_SNAP_DEGREES) * EYE_SNAP_DEGREES;
    }
}
