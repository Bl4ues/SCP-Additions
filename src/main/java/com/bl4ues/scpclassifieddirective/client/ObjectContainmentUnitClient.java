package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Client-side GeckoLib rendering for the Object Containment Unit. */
public final class ObjectContainmentUnitClient {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "geo/block/containment_stand.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/block/containment_stand.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/containment_stand.animation.json");

    private ObjectContainmentUnitClient() {
    }

    @Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void registerRenderers(
                EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    ObjectContainmentUnitModule.BLOCK_ENTITY.get(),
                    BlockRenderer::new);
        }
    }

    private static final class BlockModel
            extends GeoModel<ObjectContainmentUnitModule.UnitBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                ObjectContainmentUnitModule.UnitBlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                ObjectContainmentUnitModule.UnitBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                ObjectContainmentUnitModule.UnitBlockEntity animatable) {
            return ANIMATION;
        }
    }

    private static final class ItemModel
            extends GeoModel<ObjectContainmentUnitModule.UnitItem> {
        @Override
        public ResourceLocation getModelResource(
                ObjectContainmentUnitModule.UnitItem animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                ObjectContainmentUnitModule.UnitItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                ObjectContainmentUnitModule.UnitItem animatable) {
            return ANIMATION;
        }
    }

    private static final class BlockRenderer
            extends GeoBlockRenderer<ObjectContainmentUnitModule.UnitBlockEntity> {
        private BlockRenderer(BlockEntityRendererProvider.Context context) {
            super(new BlockModel());
        }

        @Override
        public RenderType getRenderType(
                ObjectContainmentUnitModule.UnitBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityTranslucent(texture, true);
        }

        @Override
        public boolean shouldRenderOffScreen(
                ObjectContainmentUnitModule.UnitBlockEntity blockEntity) {
            return true;
        }
    }

    public static final class ItemRenderer
            extends GeoItemRenderer<ObjectContainmentUnitModule.UnitItem> {
        public ItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public RenderType getRenderType(
                ObjectContainmentUnitModule.UnitItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityTranslucent(texture, true);
        }
    }
}
