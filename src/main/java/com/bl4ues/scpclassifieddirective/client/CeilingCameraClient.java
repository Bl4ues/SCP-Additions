package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraViewGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Map;
import java.util.WeakHashMap;

/** Client renderer for the ceiling-mounted dome surveillance camera. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CeilingCameraClient {
    private static final ResourceLocation MODEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "geo/block/ceiling_camera.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/block/dome_camera.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "animations/block/surveillance_camera.animation.json");

    private CeilingCameraClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CeilingCameraModule.BLOCK_ENTITY.get(),
                context -> new BlockRenderer());
    }

    private static final class BlockModel extends GeoModel<
            CeilingCameraModule.CeilingCameraBlockEntity> {
        private static final float RELEASE_YAW_SPEED = 58.0F;
        private static final float RELEASE_PITCH_SPEED = 45.0F;
        private final Map<CeilingCameraModule.CeilingCameraBlockEntity,
                ReleasePose> releasePoses = new WeakHashMap<>();

        @Override
        public ResourceLocation getModelResource(
                CeilingCameraModule.CeilingCameraBlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                CeilingCameraModule.CeilingCameraBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                CeilingCameraModule.CeilingCameraBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(
                CeilingCameraModule.CeilingCameraBlockEntity animatable,
                long instanceId,
                AnimationState<CeilingCameraModule.CeilingCameraBlockEntity>
                        animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);
            CoreGeoBone dome = getAnimationProcessor().getBone("dome");

            float yawDegrees = animatable.visualYaw(animationState.getPartialTick());
            float pitchDegrees = animatable.visualPitch(
                    animationState.getPartialTick());

            Minecraft minecraft = Minecraft.getInstance();
            long now = System.nanoTime();
            if (isLocallyControlled(animatable, minecraft)) {
                yawDegrees = Mth.clamp(Mth.wrapDegrees(
                                minecraft.player.getYRot()
                                        - CeilingCameraModule.BASE_YAW),
                        -CeilingCameraModule.MANUAL_YAW_LIMIT,
                        CeilingCameraModule.MANUAL_YAW_LIMIT);
                pitchDegrees = Mth.clamp(minecraft.player.getXRot(),
                        CeilingCameraModule.MANUAL_MIN_PITCH,
                        CeilingCameraModule.MANUAL_MAX_PITCH);
                ReleasePose pose = releasePoses.computeIfAbsent(animatable,
                        ignored -> new ReleasePose());
                pose.yaw = yawDegrees;
                pose.pitch = pitchDegrees;
                pose.lastNanos = now;
            } else {
                ReleasePose pose = releasePoses.get(animatable);
                if (pose != null) {
                    float dt = Mth.clamp((now - pose.lastNanos)
                            / 1_000_000_000.0F, 0.0F, 0.05F);
                    pose.lastNanos = now;
                    pose.yaw = Mth.approachDegrees(pose.yaw, yawDegrees,
                            RELEASE_YAW_SPEED * dt);
                    pose.pitch = Mth.approach(pose.pitch, pitchDegrees,
                            RELEASE_PITCH_SPEED * dt);
                    yawDegrees = pose.yaw;
                    pitchDegrees = pose.pitch;
                    if (Math.abs(Mth.wrapDegrees(pose.yaw
                            - animatable.visualYaw(
                                    animationState.getPartialTick()))) <= 0.08F
                            && Math.abs(pose.pitch - animatable.visualPitch(
                                    animationState.getPartialTick())) <= 0.08F) {
                        releasePoses.remove(animatable);
                    }
                }
            }

            if (dome != null) {
                // The original model authors the eye at +90 degrees under the
                // dome. Rotate the complete dome assembly, never the eye alone:
                // 90 world pitch = neutral/down, 40 = the 50-degree hard stop.
                dome.setRotY(-yawDegrees * Mth.DEG_TO_RAD);
                float relativePitch = pitchDegrees
                        - CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH;
                dome.setRotX(relativePitch * Mth.DEG_TO_RAD);
            }
        }

        private static boolean isLocallyControlled(
                CeilingCameraModule.CeilingCameraBlockEntity camera,
                Minecraft minecraft) {
            if (!Scp079PlayableClient.cameraMode() || minecraft.player == null
                    || minecraft.level == null
                    || camera.getLevel() != minecraft.level) {
                return false;
            }
            Vec3 baseEye = CeilingCameraModule.eyePosition(
                    camera.getBlockPos(), camera.getBlockState());
            return Scp079PlayableClient.viewPosition().distanceToSqr(baseEye)
                    <= 0.64D;
        }

        private static final class ReleasePose {
            private float yaw;
            private float pitch;
            private long lastNanos;
        }
    }

    private static final class ItemModel extends GeoModel<
            CeilingCameraModule.CeilingCameraItem> {
        @Override
        public ResourceLocation getModelResource(
                CeilingCameraModule.CeilingCameraItem animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                CeilingCameraModule.CeilingCameraItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                CeilingCameraModule.CeilingCameraItem animatable) {
            return ANIMATION;
        }
    }

    private static final class BlockRenderer extends GeoBlockRenderer<
            CeilingCameraModule.CeilingCameraBlockEntity> {
        private BlockRenderer() {
            super(new BlockModel());
        }

        @Override
        public RenderType getRenderType(
                CeilingCameraModule.CeilingCameraBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            CeilingCameraAudioClient.observe(animatable);
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(
                CeilingCameraModule.CeilingCameraBlockEntity blockEntity) {
            return true;
        }
    }

    public static final class ItemRenderer extends GeoItemRenderer<
            CeilingCameraModule.CeilingCameraItem> {
        public ItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public RenderType getRenderType(
                CeilingCameraModule.CeilingCameraItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
