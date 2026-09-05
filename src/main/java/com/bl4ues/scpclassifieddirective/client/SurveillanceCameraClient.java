package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Client renderer for the authored wall-mounted surveillance camera. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SurveillanceCameraClient {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "geo/block/surveillance_camera.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/block/surveillance_camera.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/surveillance_camera.animation.json");

    private SurveillanceCameraClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                SurveillanceCameraPlaceholderModule.BLOCK_ENTITY.get(),
                BlockRenderer::new);
    }

    private static final class BlockModel extends GeoModel<
            SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity animatable,
                long instanceId,
                AnimationState<SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity>
                        animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);
            CoreGeoBone yaw = getAnimationProcessor().getBone("camera_yaw");
            CoreGeoBone pitch = getAnimationProcessor().getBone("camera_pitch");
            if (yaw != null) {
                // Minecraft yaw increases opposite to the authored GeckoLib Y axis.
                yaw.setRotY(-animatable.visualYaw(animationState.getPartialTick())
                        * Mth.DEG_TO_RAD);
            }
            if (pitch != null) {
                // Positive Minecraft pitch looks down, while positive model X
                // rotation raises the lens, so invert this axis as well.
                pitch.setRotX(-animatable.visualPitch(animationState.getPartialTick())
                        * Mth.DEG_TO_RAD);
            }
        }
    }

    private static final class ItemModel extends GeoModel<
            SurveillanceCameraPlaceholderModule.SurveillanceCameraItem> {
        @Override
        public ResourceLocation getModelResource(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraItem animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraItem animatable) {
            return ANIMATION;
        }
    }

    private static final class BlockRenderer extends GeoBlockRenderer<
            SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity> {
        private BlockRenderer(BlockEntityRendererProvider.Context context) {
            super(new BlockModel());
        }

        @Override
        public RenderType getRenderType(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            SurveillanceCameraAudioClient.observe(animatable);
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity blockEntity) {
            return false;
        }
    }

    public static final class ItemRenderer extends GeoItemRenderer<
            SurveillanceCameraPlaceholderModule.SurveillanceCameraItem> {
        public ItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public RenderType getRenderType(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
