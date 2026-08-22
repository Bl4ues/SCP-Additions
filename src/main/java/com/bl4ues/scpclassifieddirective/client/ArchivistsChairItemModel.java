package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ArchivistsChairBlockItem;
import software.bernie.geckolib.model.GeoModel;

public final class ArchivistsChairItemModel extends GeoModel<ArchivistsChairBlockItem> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "geo/archivists_chair.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "textures/block/archivists_chair.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "animations/archivists_chair.animation.json");

    @Override
    public ResourceLocation getModelResource(ArchivistsChairBlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ArchivistsChairBlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ArchivistsChairBlockItem animatable) {
        return ANIMATION;
    }
}
