package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.item.HazmatArmorItem;
import software.bernie.geckolib.model.GeoModel;

public final class HazmatArmorModel extends GeoModel<HazmatArmorItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            ScpAdditionsMod.MODID, "geo/armor/hazmat_suit.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ScpAdditionsMod.MODID, "textures/armor/hazmat_suit.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
            ScpAdditionsMod.MODID, "animations/armor/hazmat_suit.animation.json");

    @Override
    public ResourceLocation getModelResource(HazmatArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(HazmatArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(HazmatArmorItem animatable) {
        return ANIMATION;
    }
}
