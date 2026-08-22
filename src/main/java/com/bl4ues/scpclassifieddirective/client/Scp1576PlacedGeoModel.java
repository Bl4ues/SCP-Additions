package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.scp1576.Scp1576PlacedBlockEntity;
import software.bernie.geckolib.model.GeoModel;

/** Reuses the exact authored SCP-1576 geometry in world space. */
public final class Scp1576PlacedGeoModel extends GeoModel<Scp1576PlacedBlockEntity> {
    @Override
    public ResourceLocation getModelResource(Scp1576PlacedBlockEntity animatable) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                "geo/item/scp1576.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Scp1576PlacedBlockEntity animatable) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                "textures/item/scp1576.png");
    }

    @Override
    public ResourceLocation getAnimationResource(Scp1576PlacedBlockEntity animatable) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                "animations/item/scp1576.animation.json");
    }
}
