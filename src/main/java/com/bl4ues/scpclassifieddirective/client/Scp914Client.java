package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Top-level client subscriber for the placed SCP-914 GeckoLib renderer. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp914Client {
    private Scp914Client() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Scp914Module.SCP_914_BLOCK_ENTITY.get(),
                context -> new Renderer());
    }

    private static final class Model extends GeoModel<Scp914BlockEntity> {
        private static final ResourceLocation GEO = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "geo/block/scp914.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "textures/block/scp914.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "animations/block/scp914.animation.json");

        @Override public ResourceLocation getModelResource(Scp914BlockEntity animatable) { return GEO; }
        @Override public ResourceLocation getTextureResource(Scp914BlockEntity animatable) { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(Scp914BlockEntity animatable) { return ANIMATION; }
    }

    private static final class Renderer extends GeoBlockRenderer<Scp914BlockEntity> {
        private Renderer() { super(new Model()); }

        @Override
        public void preRender(PoseStack poseStack, Scp914BlockEntity animatable,
                BakedGeoModel bakedModel, MultiBufferSource bufferSource,
                VertexConsumer buffer, boolean isReRender, float partialTick,
                int packedLight, int packedOverlay, float red, float green,
                float blue, float alpha) {
            float angle = (float) Math.toRadians(animatable.getDialAngle());
            getGeoModel().getBone("grab_dial").ifPresent(bone -> bone.setRotZ(angle));
            getGeoModel().getBone("triangle_dial").ifPresent(bone -> bone.setRotZ(-angle));
            super.preRender(poseStack, animatable, bakedModel, bufferSource, buffer,
                    isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha);
        }

        @Override
        public RenderType getRenderType(Scp914BlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(Scp914BlockEntity blockEntity) { return true; }
    }
}
