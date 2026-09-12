package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityDecorativePropsModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Dedicated GeckoLib renderers for Ceiling Ventilation.
 *
 * <p>Block and item geometry deliberately use separate resources. Several
 * animated facility props previously flickered when GeckoLib shared mutable
 * model state between a placed block and its inventory/hand renderer.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FacilityDecorativePropsClient {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final ResourceLocation BLOCK_GEO = id(
            "geo/block/ceiling_ventilation.geo.json");
    private static final ResourceLocation ITEM_GEO = id(
            "geo/item/ceiling_ventilation.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/ceiling_ventilation.png");
    private static final ResourceLocation GLOWMASK = id(
            "textures/block/ceiling_ventilation_glowmask.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/ceiling_ventilation.animation.json");

    private FacilityDecorativePropsClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                FacilityDecorativePropsModule.CEILING_VENTILATION_BLOCK_ENTITY.get(),
                CeilingVentilationBlockRenderer::new);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private static final class BlockModel extends
            GeoModel<FacilityDecorativePropsModule.CeilingVentilationBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                FacilityDecorativePropsModule.CeilingVentilationBlockEntity animatable) {
            return BLOCK_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                FacilityDecorativePropsModule.CeilingVentilationBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                FacilityDecorativePropsModule.CeilingVentilationBlockEntity animatable) {
            return ANIMATION;
        }
    }

    public static final class CeilingVentilationBlockRenderer extends
            GeoBlockRenderer<FacilityDecorativePropsModule.CeilingVentilationBlockEntity> {
        public CeilingVentilationBlockRenderer(
                BlockEntityRendererProvider.Context context) {
            super(new BlockModel());
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        FacilityDecorativePropsModule.CeilingVentilationBlockEntity animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    RenderType emissive = RenderType.eyes(GLOWMASK);
                    getRenderer().reRender(bakedModel, poseStack, bufferSource,
                            animatable, emissive,
                            bufferSource.getBuffer(emissive), partialTick,
                            FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                            1.0F, 1.0F, 1.0F, 1.0F);
                }
            });
        }

        @Override
        public RenderType getRenderType(
                FacilityDecorativePropsModule.CeilingVentilationBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(
                FacilityDecorativePropsModule.CeilingVentilationBlockEntity blockEntity) {
            return true;
        }
    }

    private static final class ItemModel extends
            GeoModel<FacilityDecorativePropsModule.CeilingVentilationItem> {
        @Override
        public ResourceLocation getModelResource(
                FacilityDecorativePropsModule.CeilingVentilationItem animatable) {
            return ITEM_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                FacilityDecorativePropsModule.CeilingVentilationItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                FacilityDecorativePropsModule.CeilingVentilationItem animatable) {
            return ANIMATION;
        }
    }

    public static final class CeilingVentilationItemRenderer extends
            GeoItemRenderer<FacilityDecorativePropsModule.CeilingVentilationItem> {
        public CeilingVentilationItemRenderer() {
            super(new ItemModel());
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        FacilityDecorativePropsModule.CeilingVentilationItem animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    RenderType emissive = RenderType.eyes(GLOWMASK);
                    getRenderer().reRender(bakedModel, poseStack, bufferSource,
                            animatable, emissive,
                            bufferSource.getBuffer(emissive), partialTick,
                            FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                            1.0F, 1.0F, 1.0F, 1.0F);
                }
            });
        }

        @Override
        public RenderType getRenderType(
                FacilityDecorativePropsModule.CeilingVentilationItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
