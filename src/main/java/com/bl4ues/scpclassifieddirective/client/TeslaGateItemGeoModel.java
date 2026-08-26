package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.TeslaGateBlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class TeslaGateItemGeoModel extends GeoModel<TeslaGateBlockItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/block/tesla_gate.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/block/tesla_gate.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/tesla_gate.animation.json");

    @Override
    public ResourceLocation getModelResource(TeslaGateBlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TeslaGateBlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(TeslaGateBlockItem animatable) {
        return ANIMATION;
    }
}
