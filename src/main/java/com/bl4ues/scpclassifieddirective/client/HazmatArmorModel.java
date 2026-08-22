package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.HazmatArmorItem;
import software.bernie.geckolib.model.GeoModel;

public final class HazmatArmorModel extends GeoModel<HazmatArmorItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/armor/hazmat_suit.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/armor/hazmat_suit.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "animations/armor/hazmat_suit.animation.json");

    @Override
    public ResourceLocation getModelResource(HazmatArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HazmatArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HazmatArmorItem animatable) {
        return ANIMATION;
    }
}
