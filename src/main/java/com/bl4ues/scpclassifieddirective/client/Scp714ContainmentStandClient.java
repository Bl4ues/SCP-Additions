package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp714ContainmentStandModule;
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
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** GeckoLib rendering for the SCP-714 Containment Stand. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp714ContainmentStandClient {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final ResourceLocation GEO = id(
            "geo/block/scp_714_containment_stand.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/714_stand.png");
    private static final ResourceLocation GLOWMASK = id(
            "textures/block/714_stand_glowmask.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/scp_714_containment_stand.animation.json");

    private Scp714ContainmentStandClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                Scp714ContainmentStandModule.BLOCK_ENTITY.get(),
                BlockRenderer::new);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private static void forceRing(CoreGeoBone bone, boolean visible) {
        if (bone == null) return;
        bone.setHidden(!visible);
        float scale = visible ? 1.0F : 0.0F;
        bone.setScaleX(scale);
        bone.setScaleY(scale);
        bone.setScaleZ(scale);
    }

    private static final class BlockModel extends
            GeoModel<Scp714ContainmentStandModule.StandBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                Scp714ContainmentStandModule.StandBlockEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                Scp714ContainmentStandModule.StandBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                Scp714ContainmentStandModule.StandBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(
                Scp714ContainmentStandModule.StandBlockEntity animatable,
                long instanceId,
                AnimationState<Scp714ContainmentStandModule.StandBlockEntity> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            // GeoModels are shared between placed instances. Reassert this bone
            // after animation evaluation so a nearby stand cannot inherit the
            // previous stand's scale and flicker between holding/removed poses.
            forceRing(getAnimationProcessor().getBone("714"),
                    animatable.hasRing());
        }
    }

    public static final class BlockRenderer extends
            GeoBlockRenderer<Scp714ContainmentStandModule.StandBlockEntity> {
        public BlockRenderer(BlockEntityRendererProvider.Context context) {
            super(new BlockModel());
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        Scp714ContainmentStandModule.StandBlockEntity animatable,
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
                Scp714ContainmentStandModule.StandBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(
                Scp714ContainmentStandModule.StandBlockEntity blockEntity) {
            return true;
        }
    }

    private static final class ItemModel extends
            GeoModel<Scp714ContainmentStandModule.StandItem> {
        @Override
        public ResourceLocation getModelResource(
                Scp714ContainmentStandModule.StandItem animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                Scp714ContainmentStandModule.StandItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                Scp714ContainmentStandModule.StandItem animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(
                Scp714ContainmentStandModule.StandItem animatable,
                long instanceId,
                AnimationState<Scp714ContainmentStandModule.StandItem> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            forceRing(getAnimationProcessor().getBone("714"), false);
        }
    }

    public static final class ItemRenderer extends
            GeoItemRenderer<Scp714ContainmentStandModule.StandItem> {
        public ItemRenderer() {
            super(new ItemModel());
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        Scp714ContainmentStandModule.StandItem animatable,
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
                Scp714ContainmentStandModule.StandItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
