package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.intercom.IntercomModule;
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

/** Top-level client registration for the placed Intercom renderer. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class IntercomClient {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final ResourceLocation GEO = id(
            "geo/block/intercom.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/intercom.png");
    private static final ResourceLocation GLOWMASK = id(
            "textures/block/intercom_glowmask.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/intercom.animation.json");

    private IntercomClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(IntercomModule.BLOCK_ENTITY.get(),
                BlockRenderer::new);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private static final class BlockModel extends
            GeoModel<IntercomModule.IntercomBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                IntercomModule.IntercomBlockEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                IntercomModule.IntercomBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                IntercomModule.IntercomBlockEntity animatable) {
            return ANIMATION;
        }
    }

    public static final class BlockRenderer extends
            GeoBlockRenderer<IntercomModule.IntercomBlockEntity> {
        public BlockRenderer(BlockEntityRendererProvider.Context context) {
            super(new BlockModel());
            // Keep the authored _glowmask explicit for block entities. This is
            // intentionally the same full-bright path used by other facility
            // GeckoLib equipment so shader/PBR wrappers cannot swallow it.
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        IntercomModule.IntercomBlockEntity animatable,
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
                IntercomModule.IntercomBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(
                IntercomModule.IntercomBlockEntity blockEntity) {
            return true;
        }
    }

    private static final class ItemModel extends
            GeoModel<IntercomModule.IntercomItem> {
        @Override
        public ResourceLocation getModelResource(
                IntercomModule.IntercomItem animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                IntercomModule.IntercomItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                IntercomModule.IntercomItem animatable) {
            return ANIMATION;
        }
    }

    public static final class ItemRenderer extends
            GeoItemRenderer<IntercomModule.IntercomItem> {
        public ItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public RenderType getRenderType(IntercomModule.IntercomItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
