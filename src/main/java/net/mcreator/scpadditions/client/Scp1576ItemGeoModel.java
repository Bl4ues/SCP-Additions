package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.scp1576.Scp1576Item;
import software.bernie.geckolib.model.GeoModel;

public final class Scp1576ItemGeoModel extends GeoModel<Scp1576Item> {
    @Override
    public ResourceLocation getModelResource(Scp1576Item animatable) {
        return new ResourceLocation(ScpAdditionsMod.MODID,
                "geo/item/scp1576.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Scp1576Item animatable) {
        return new ResourceLocation(ScpAdditionsMod.MODID,
                "textures/item/scp1576.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Scp1576Item animatable) {
        return new ResourceLocation(ScpAdditionsMod.MODID,
                "animations/item/scp1576.animation.json");
    }
}
