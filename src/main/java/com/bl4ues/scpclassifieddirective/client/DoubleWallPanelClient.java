package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.DoubleWallPanelModule;
import com.bl4ues.scpclassifieddirective.facility.DoubleWallPanelModule.DoubleWallPanelBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.DoubleWallPanelModule.DoubleWallPanelItem;
import com.bl4ues.scpclassifieddirective.facility.DoubleWallPanelModule.Side;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DoubleWallPanelClient {
    private static final ResourceLocation GEO = id(
            "geo/block/double_wall_panel.geo.json");
    private static final ResourceLocation ITEM_GEO = id(
            "geo/item/double_wall_panel.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/wall_panel.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/wall_panel.animation.json");
    private static final double LATERAL_INSET = 1.0D / 1024.0D;

    private DoubleWallPanelClient() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                DoubleWallPanelModule.BLOCK_ENTITY.get(), BlockRenderer::new);
    }

    private static final class BlockModel
            extends GeoModel<DoubleWallPanelBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                DoubleWallPanelBlockEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                DoubleWallPanelBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                DoubleWallPanelBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(DoubleWallPanelBlockEntity animatable,
                long instanceId,
                AnimationState<DoubleWallPanelBlockEntity> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            CoreGeoBone frame = getAnimationProcessor().getBone("frame");
            CoreGeoBone frame2 = getAnimationProcessor().getBone("frame2");
            if (frame != null) {
                frame.setHidden(animatable.hasMaterial(Side.FRONT));
            }
            if (frame2 != null) {
                // Rear fallback is rendered in a separate darker pass.
                frame2.setHidden(true);
            }
        }
    }

    public static final class BlockRenderer
            extends GeoBlockRenderer<DoubleWallPanelBlockEntity> {
        private final BlockModel panelModel;

        public BlockRenderer(BlockEntityRendererProvider.Context context) {
            this(new BlockModel());
        }

        private BlockRenderer(BlockModel model) {
            super(model);
            this.panelModel = model;
            addRearFallbackLayer();
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        DoubleWallPanelBlockEntity panel,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight,
                        int packedOverlay) {
                    Direction facing = panel.getBlockState().getValue(
                            HorizontalDirectionalBlock.FACING);
                    if (panel.hasMaterial(Side.FRONT)) {
                        CopycatPanelRenderUtil.render(
                                panel.material(Side.FRONT),
                                panel.getLevel(), panel.getBlockPos(),
                                facing, poseStack, bufferSource,
                                packedOverlay, 0.0D, 0.5D,
                                Direction.SOUTH, 1.0F, LATERAL_INSET);
                    }
                    if (panel.hasMaterial(Side.BACK)) {
                        CopycatPanelRenderUtil.render(
                                panel.material(Side.BACK),
                                panel.getLevel(), panel.getBlockPos(),
                                facing, poseStack, bufferSource,
                                packedOverlay, 0.5D, 1.0D,
                                Direction.NORTH, 0.82F, LATERAL_INSET);
                    }
                }
            });
        }

        private void addRearFallbackLayer() {
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        DoubleWallPanelBlockEntity panel,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight,
                        int packedOverlay) {
                    if (panel.hasMaterial(Side.BACK)) return;

                    CoreGeoBone front = panelModel.getAnimationProcessor()
                            .getBone("frame");
                    CoreGeoBone rear = panelModel.getAnimationProcessor()
                            .getBone("frame2");
                    if (front != null) front.setHidden(true);
                    if (rear != null) rear.setHidden(false);
                    try {
                        RenderType tinted = RenderType.entityCutoutNoCull(
                                TEXTURE);
                        getRenderer().reRender(bakedModel, poseStack,
                                bufferSource, panel, tinted,
                                bufferSource.getBuffer(tinted), partialTick,
                                packedLight, packedOverlay,
                                0.82F, 0.82F, 0.82F, 1.0F);
                    } finally {
                        if (front != null) {
                            front.setHidden(panel.hasMaterial(Side.FRONT));
                        }
                        if (rear != null) rear.setHidden(true);
                    }
                }
            });
        }

        @Override
        public RenderType getRenderType(DoubleWallPanelBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }

    private static final class ItemModel
            extends GeoModel<DoubleWallPanelItem> {
        @Override
        public ResourceLocation getModelResource(
                DoubleWallPanelItem animatable) {
            return ITEM_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                DoubleWallPanelItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                DoubleWallPanelItem animatable) {
            return ANIMATION;
        }
    }

    public static final class ItemRenderer
            extends GeoItemRenderer<DoubleWallPanelItem> {
        public ItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public RenderType getRenderType(DoubleWallPanelItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
