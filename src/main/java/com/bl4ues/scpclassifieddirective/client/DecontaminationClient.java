package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.DecontaminationBlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Top-level client registration for the placed GeckoLib checkpoint. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DecontaminationClient {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int GLOW_PASSES = 3;
    private static final ResourceLocation GLOWMASK = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/block/decontamination_glowmask.png");

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

            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        DecontaminationBlockEntity animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    RenderType emissive = RenderType.eyes(GLOWMASK);
                    VertexConsumer emissiveBuffer = bufferSource.getBuffer(emissive);

                    // The authored mask is intentionally a dark red. One
                    // full-bright pass makes it illumination-independent but
                    // can still remain below shader bloom thresholds. Repeating
                    // the additive eyes pass increases only the masked pixels,
                    // leaving the window and the rest of the body untouched.
                    for (int pass = 0; pass < GLOW_PASSES; pass++) {
                        getRenderer().reRender(bakedModel, poseStack, bufferSource,
                                animatable, emissive, emissiveBuffer, partialTick,
                                FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                                1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            });
        }

        @Override
        public void preRender(PoseStack poseStack,
                DecontaminationBlockEntity animatable,
                BakedGeoModel bakedModel, MultiBufferSource bufferSource,
                VertexConsumer buffer, boolean isReRender, float partialTick,
                int packedLight, int packedOverlay, float red, float green,
                float blue, float alpha) {
            if (!isReRender) {
                poseStack.translate(0.0D, -1.0D, 0.0D);
            }
            super.preRender(poseStack, animatable, bakedModel, bufferSource,
                    buffer, isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }

        @Override
        public RenderType getRenderType(DecontaminationBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            // Cull backfaces on the large zero-thickness window plane. The old
            // no-cull translucent pass rendered both sides and could double the
            // shader/specular response into bright white patches.
            return RenderType.entityTranslucentCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(DecontaminationBlockEntity blockEntity) {
            return true;
        }
    }
}
