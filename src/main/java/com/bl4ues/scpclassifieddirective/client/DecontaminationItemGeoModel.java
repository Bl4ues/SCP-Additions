package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.DecontaminationBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class DecontaminationItemGeoModel
        extends GeoModel<DecontaminationBlockItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "geo/block/decontamination.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/block/decontamination.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/decontamination.animation.json");

    @Override
    public ResourceLocation getModelResource(DecontaminationBlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DecontaminationBlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DecontaminationBlockItem animatable) {
        return ANIMATION;
    }
}
