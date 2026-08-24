package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp1176BlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
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

/** Client renderer for SCP-1176 and its translucent honey/decal texture areas. */
public final class Scp1176Client {
    private Scp1176Client() {
    }

    @Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    ScpClassifiedDirectiveModBlockEntities.SCP_1176.get(),
                    Renderer::new);
        }
    }

    private static final class Model extends GeoModel<Scp1176BlockEntity> {
        private static final ResourceLocation MODEL = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "geo/block/scp1176.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "textures/block/scp1176.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID,
                "animations/block/scp1176.animation.json");

        @Override
        public ResourceLocation getModelResource(Scp1176BlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(Scp1176BlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(Scp1176BlockEntity animatable) {
            return ANIMATION;
        }
    }

    private static final class Renderer extends GeoBlockRenderer<Scp1176BlockEntity> {
        private Renderer(BlockEntityRendererProvider.Context context) {
            super(new Model());
        }

        @Override
        public RenderType getRenderType(Scp1176BlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            // Full alpha blending is required for the honey surface and the
            // transparent side artwork. Keeping this in the translucent pass
            // prevents transparent texels from behaving like opaque geometry.
            return RenderType.entityTranslucent(texture, true);
        }

        @Override
        public boolean shouldRenderOffScreen(Scp1176BlockEntity blockEntity) {
            return true;
        }
    }
}
