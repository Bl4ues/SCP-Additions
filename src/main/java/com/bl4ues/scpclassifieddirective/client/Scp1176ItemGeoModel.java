package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.Scp1176BlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class Scp1176ItemGeoModel extends GeoModel<Scp1176BlockItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/block/scp1176.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/block/scp1176.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/scp1176.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp1176BlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Scp1176BlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp1176BlockItem animatable) {
        return ANIMATION;
    }
}
