package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import software.bernie.geckolib.model.GeoModel;

public class Scp173Model<T extends Scp173Entity> extends GeoModel<T> {
    private static final ResourceLocation MODEL = new ResourceLocation("scp_classified_directive", "geo/entity/scp_173.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation("scp_classified_directive", "textures/entities/scp_173.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation("scp_classified_directive", "animations/entity/scp_173.animation.json");

    @Override public ResourceLocation getModelResource(T animatable) { return MODEL; }
    @Override public ResourceLocation getTextureResource(T animatable) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(T animatable) { return ANIMATION; }
}
