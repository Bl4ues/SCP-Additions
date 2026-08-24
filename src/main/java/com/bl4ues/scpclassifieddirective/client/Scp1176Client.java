package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp1176BlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Client renderer for SCP-1176.
 *
 * Solid geometry, translucent wall glyphs and the honey surface are deliberately
 * kept in separate render passes. The solid pass writes normal depth so the
 * corpse and sarcophagus walls remain stable behind the honey. The glyph pass
 * preserves their authored alpha instead of forcing them through cutout, and
 * the honey remains the final translucent layer.
 */
public final class Scp1176Client {
    private static final float HONEY_ALPHA = 0.72F;

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

        @Override
        public void setCustomAnimations(Scp1176BlockEntity animatable,
                long instanceId,
                AnimationState<Scp1176BlockEntity> animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);
            prepareOpaquePass();
        }

        private void prepareOpaquePass() {
            setHidden("bb_main", true);
            setHidden("glyphs", true);
            setHidden("sarc", false);
            setHidden("1176", false);
            setHidden("lid", false);
            setHidden("2", false);
            setHidden("faucet", false);
            setHidden("bone", false);
        }

        private void prepareGlyphPass() {
            setHidden("bb_main", true);
            setHidden("glyphs", false);
            setHidden("sarc", true);
            setHidden("1176", true);
            setHidden("lid", true);
            setHidden("2", true);
            setHidden("faucet", true);
            setHidden("bone", true);
        }

        private void prepareHoneyPass() {
            setHidden("bb_main", false);
            setHidden("glyphs", true);
            setHidden("sarc", true);
            setHidden("1176", true);
            setHidden("lid", true);
            setHidden("2", true);
            setHidden("faucet", true);
            setHidden("bone", true);
        }

        private void setHidden(String name, boolean hidden) {
            CoreGeoBone bone = getAnimationProcessor().getBone(name);
            if (bone != null) bone.setHidden(hidden);
        }
    }

    private static final class Renderer extends GeoBlockRenderer<Scp1176BlockEntity> {
        private final Model scpModel;

        private Renderer(BlockEntityRendererProvider.Context context) {
            this(new Model());
        }

        private Renderer(Model model) {
            super(model);
            this.scpModel = model;

            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        Scp1176BlockEntity animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    scpModel.prepareGlyphPass();
                    try {
                        RenderType glyphs = RenderType.entityTranslucent(
                                Model.TEXTURE, true);
                        getRenderer().reRender(bakedModel, poseStack, bufferSource,
                                animatable, glyphs,
                                bufferSource.getBuffer(glyphs), partialTick,
                                packedLight, packedOverlay,
                                1.0F, 1.0F, 1.0F, 1.0F);
                    } finally {
                        scpModel.prepareOpaquePass();
                    }
                }
            });

            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        Scp1176BlockEntity animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    scpModel.prepareHoneyPass();
                    try {
                        RenderType honey = RenderType.entityTranslucent(
                                Model.TEXTURE, true);
                        getRenderer().reRender(bakedModel, poseStack, bufferSource,
                                animatable, honey,
                                bufferSource.getBuffer(honey), partialTick,
                                packedLight, packedOverlay,
                                1.0F, 1.0F, 1.0F, HONEY_ALPHA);
                    } finally {
                        scpModel.prepareOpaquePass();
                    }
                }
            });
        }

        @Override
        public RenderType getRenderType(Scp1176BlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(Scp1176BlockEntity blockEntity) {
            return true;
        }
    }
}
