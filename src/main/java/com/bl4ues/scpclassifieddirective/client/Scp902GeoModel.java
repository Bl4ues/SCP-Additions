package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp902BlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Shared authored asset binding for the animated SCP-902 block. */
public final class Scp902GeoModel extends GeoModel<Scp902BlockEntity> {
    public static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "geo/block/scp902.geo.json");
    public static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/block/scp902.png");
    public static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/scp902.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp902BlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Scp902BlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp902BlockEntity animatable) {
        return ANIMATION;
    }
}
