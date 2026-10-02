package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.WallPanelModule;
import com.bl4ues.scpclassifieddirective.facility.WallPanelModule.WallPanelBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.WallPanelModule.WallPanelItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renders the authored Wall Panel when empty and a compressed copy of the
 * selected solid block model when configured.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WallPanelClient {
    private static final ResourceLocation GEO = id(
            "geo/block/wall_panel.geo.json");
    private static final ResourceLocation ITEM_GEO = id(
            "geo/item/wall_panel.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/wall_panel.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/wall_panel.animation.json");

    private WallPanelClient() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                WallPanelModule.BLOCK_ENTITY.get(), BlockRenderer::new);
    }

    private static final class BlockModel
            extends GeoModel<WallPanelBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                WallPanelBlockEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                WallPanelBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                WallPanelBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(WallPanelBlockEntity animatable,
                long instanceId,
                AnimationState<WallPanelBlockEntity> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            CoreGeoBone frame = getAnimationProcessor().getBone("frame");
            if (frame != null) {
                frame.setHidden(animatable.hasMaterial());
            }
        }
    }

    public static final class BlockRenderer
            extends GeoBlockRenderer<WallPanelBlockEntity> {
        public BlockRenderer(BlockEntityRendererProvider.Context context) {
            super(new BlockModel());
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        WallPanelBlockEntity panel, BakedGeoModel bakedModel,
                        RenderType renderType, MultiBufferSource bufferSource,
                        VertexConsumer buffer, float partialTick,
                        int packedLight, int packedOverlay) {
                    if (!panel.hasMaterial()) return;

                    CopycatPanelRenderUtil.render(
                            panel.materialState(), panel.getLevel(),
                            panel.getBlockPos(),
                            panel.getBlockState().getValue(
                                    HorizontalDirectionalBlock.FACING),
                            poseStack, bufferSource, packedOverlay,
                            0.0D, 1.0D / 16.0D, null);
                }
            });
        }

        @Override
        public RenderType getRenderType(WallPanelBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }

    private static final class ItemModel extends GeoModel<WallPanelItem> {
        @Override
        public ResourceLocation getModelResource(WallPanelItem animatable) {
            return ITEM_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(WallPanelItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(WallPanelItem animatable) {
            return ANIMATION;
        }
    }

    public static final class ItemRenderer extends GeoItemRenderer<WallPanelItem> {
        public ItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public RenderType getRenderType(WallPanelItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
