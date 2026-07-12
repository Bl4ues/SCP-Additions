package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.entity.Scp173Entity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class Scp173Model<T extends Scp173Entity> extends GeoModel<T> {
    private static final ResourceLocation MODEL_RESOURCE = new ResourceLocation(ScpInventoryMod.MODID, "geo/entity/scp_173.geo.json");
    private static final ResourceLocation TEXTURE_RESOURCE = new ResourceLocation(ScpInventoryMod.MODID, "textures/entities/scp_173.png");
    private static final ResourceLocation ANIMATION_RESOURCE = new ResourceLocation(ScpInventoryMod.MODID, "animations/entity/scp_173.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION_RESOURCE;
    }
}
