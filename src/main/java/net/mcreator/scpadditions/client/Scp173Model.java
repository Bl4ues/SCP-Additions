package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.entity.Scp173Entity;
import software.bernie.geckolib.model.GeoModel;

public class Scp173Model<T extends Scp173Entity> extends GeoModel<T> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("scpinventory", "geo/entity/scp_173.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("scpinventory", "textures/entities/scp_173.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath("scpinventory", "animations/entity/scp_173.animation.json");

    @Override public ResourceLocation getModelResource(T animatable) { return MODEL; }
    @Override public ResourceLocation getTextureResource(T animatable) { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(T animatable) { return ANIMATION; }
}
