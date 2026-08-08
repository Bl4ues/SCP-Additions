package net.mcreator.scpadditions.client;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.DocumentHolderBlockItem;
import software.bernie.geckolib.model.GeoModel;

/** GeckoLib resource binding for the Document Holder item. */
public final class DocumentHolderItemGeoModel
        extends GeoModel<DocumentHolderBlockItem> {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpAdditionsMod.MODID, "geo/block/document_holder.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/block/document_holder.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "animations/block/document_holder.animation.json");

    @Override
    public ResourceLocation getModelResource(
            DocumentHolderBlockItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(
            DocumentHolderBlockItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(
            DocumentHolderBlockItem animatable) {
        return ANIMATION;
    }
}
