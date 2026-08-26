package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.TeslaGateBlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Client registration and renderer for the replacement Tesla Gate. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TeslaGateClient {
    private TeslaGateClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ScpClassifiedDirectiveModBlockEntities.TESLA_GATE.get(),
                context -> new Renderer());
    }

    private static final class Model extends GeoModel<TeslaGateBlockEntity> {
        private static final ResourceLocation MODEL = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "geo/block/tesla_gate.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "textures/block/tesla_gate.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID,
                "animations/block/tesla_gate.animation.json");

        @Override
        public ResourceLocation getModelResource(TeslaGateBlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(TeslaGateBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(TeslaGateBlockEntity animatable) {
            return ANIMATION;
        }
    }

    private static final class Renderer extends GeoBlockRenderer<TeslaGateBlockEntity> {
        private Renderer() {
            super(new Model());
        }

        @Override
        public RenderType getRenderType(TeslaGateBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(TeslaGateBlockEntity blockEntity) {
            return true;
        }
    }
}
