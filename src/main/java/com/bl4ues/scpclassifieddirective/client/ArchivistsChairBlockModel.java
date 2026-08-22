package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ArchivistsChairBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public final class ArchivistsChairBlockModel extends GeoModel<ArchivistsChairBlockEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "geo/archivists_chair.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "textures/block/archivists_chair.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ScpClassifiedDirectiveMod.MODID, "animations/archivists_chair.animation.json");

    @Override
    public ResourceLocation getModelResource(ArchivistsChairBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ArchivistsChairBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ArchivistsChairBlockEntity animatable) {
        return ANIMATION;
    }
}
