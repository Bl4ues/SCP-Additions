
package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.WetFloorBlock;
import com.bl4ues.scpclassifieddirective.facility.WetFloorBlockEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class WetFloorGeoModel extends GeoModel<WetFloorBlockEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/block/wet_floor.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/block/wet_floor.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "animations/block/wet_floor.animation.json");

    @Override
    public ResourceLocation getModelResource(WetFloorBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(WetFloorBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(WetFloorBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(WetFloorBlockEntity animatable, long instanceId,
            AnimationState<WetFloorBlockEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        CoreGeoBone root = getAnimationProcessor().getBone("root");
        if (root == null || !animatable.getBlockState().hasProperty(WetFloorBlock.ROTATION)) {
            return;
        }
        int rotation = animatable.getBlockState().getValue(WetFloorBlock.ROTATION);
        root.setRotY((float) (-rotation * Math.PI / 4.0D));
    }
}
