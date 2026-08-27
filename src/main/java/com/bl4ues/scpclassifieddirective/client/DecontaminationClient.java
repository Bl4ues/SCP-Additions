package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.DecontaminationBlockEntity;
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

/** Top-level client registration for the placed GeckoLib checkpoint. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DecontaminationClient {
    private DecontaminationClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ScpClassifiedDirectiveModBlockEntities.DECONTAMINATION.get(),
                context -> new Renderer());
    }

    private static final class Model extends GeoModel<DecontaminationBlockEntity> {
        private static final ResourceLocation MODEL = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID,
                "geo/block/decontamination.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID,
                "textures/block/decontamination.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID,
                "animations/block/decontamination.animation.json");

        @Override
        public ResourceLocation getModelResource(DecontaminationBlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(DecontaminationBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(DecontaminationBlockEntity animatable) {
            return ANIMATION;
        }
    }

    private static final class Renderer
            extends GeoBlockRenderer<DecontaminationBlockEntity> {
        private Renderer() {
            super(new Model());
        }

        @Override
        public RenderType getRenderType(DecontaminationBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityTranslucent(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(DecontaminationBlockEntity blockEntity) {
            return true;
        }
    }
}
