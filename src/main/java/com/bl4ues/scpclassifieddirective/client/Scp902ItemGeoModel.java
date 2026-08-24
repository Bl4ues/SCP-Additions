package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.Scp902BlockItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Item-form binding to the same SCP-902 authored geometry and animation. */
public final class Scp902ItemGeoModel extends GeoModel<Scp902BlockItem> {
    @Override
    public ResourceLocation getModelResource(Scp902BlockItem animatable) {
        return Scp902GeoModel.MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(Scp902BlockItem animatable) {
        return Scp902GeoModel.TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp902BlockItem animatable) {
        return Scp902GeoModel.ANIMATION;
    }
}
