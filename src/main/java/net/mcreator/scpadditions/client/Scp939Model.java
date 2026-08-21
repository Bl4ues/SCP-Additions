package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp939Entity;
import software.bernie.geckolib.model.GeoModel;

public class Scp939Model<T extends Scp939Entity> extends GeoModel<T> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpAdditionsMod.MODID, "geo/entity/scp939.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/entities/scp939.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpAdditionsMod.MODID, "animations/entity/scp939.animation.json");

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        return ANIMATION;
    }
}
