package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.Scp914BlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Inventory/hand renderer for the rebuilt SCP-914. */
public final class Scp914ItemRenderer extends GeoItemRenderer<Scp914BlockItem> {
    public Scp914ItemRenderer() {
        super(new Model());
    }

    @Override
    public RenderType getRenderType(Scp914BlockItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    private static final class Model extends GeoModel<Scp914BlockItem> {
        private static final ResourceLocation GEO = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "geo/item/scp914.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "textures/block/scp914.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "animations/block/scp914.animation.json");

        @Override
        public ResourceLocation getModelResource(Scp914BlockItem animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(Scp914BlockItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(Scp914BlockItem animatable) {
            return ANIMATION;
        }
    }
}
