package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.ArchivistsChairBlockItem;
import software.bernie.geckolib.model.GeoModel;

public final class ArchivistsChairItemModel extends GeoModel<ArchivistsChairBlockItem> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(ScpAdditionsMod.MODID, "geo/archivists_chair.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ScpAdditionsMod.MODID, "textures/block/archivists_chair.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ScpAdditionsMod.MODID, "animations/archivists_chair.animation.json");

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
