package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.ArchivistsChairBlockEntity;
import software.bernie.geckolib.model.GeoModel;

public final class ArchivistsChairBlockModel extends GeoModel<ArchivistsChairBlockEntity> {
    private static final ResourceLocation MODEL =
            new ResourceLocation(ScpAdditionsMod.MODID, "geo/archivists_chair.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ScpAdditionsMod.MODID, "textures/block/archivists_chair.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(ScpAdditionsMod.MODID, "animations/archivists_chair.animation.json");

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
