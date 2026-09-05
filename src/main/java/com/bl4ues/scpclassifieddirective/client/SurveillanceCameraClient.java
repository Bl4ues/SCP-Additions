package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraViewGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
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
                context -> new BlockRenderer());
    }

    private static final class BlockModel extends GeoModel<
            SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity> {
        private static final float RELEASE_YAW_SPEED = 58.0F;
        private static final float RELEASE_PITCH_SPEED = 45.0F;
        private final Map<SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity,
                ReleasePose> releasePoses = new WeakHashMap<>();

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

            float yawDegrees = animatable.visualYaw(animationState.getPartialTick());
            float pitchDegrees = animatable.visualPitch(animationState.getPartialTick());

            Minecraft minecraft = Minecraft.getInstance();
            long now = System.nanoTime();
            if (isLocallyControlled(animatable, minecraft)) {
                BlockState state = animatable.getBlockState();
                Direction facing = state.hasProperty(
                        SurveillanceCameraPlaceholderModule.FACING)
                        ? state.getValue(SurveillanceCameraPlaceholderModule.FACING)
                        : Direction.NORTH;
                yawDegrees = Mth.clamp(Mth.wrapDegrees(
                                minecraft.player.getYRot() - facing.toYRot()),
                        -SurveillanceCameraPlaceholderModule.MANUAL_YAW_LIMIT,
                        SurveillanceCameraPlaceholderModule.MANUAL_YAW_LIMIT);
                pitchDegrees = Mth.clamp(minecraft.player.getXRot(),
                        SurveillanceCameraPlaceholderModule.MANUAL_MIN_PITCH,
                        SurveillanceCameraPlaceholderModule.MANUAL_MAX_PITCH);
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
                            - animatable.visualYaw(animationState.getPartialTick())))
                            <= 0.08F
                            && Math.abs(pose.pitch
                            - animatable.visualPitch(animationState.getPartialTick()))
                            <= 0.08F) {
                        releasePoses.remove(animatable);
                    }
                }
            }

            if (yaw != null) {
                yaw.setRotY(-yawDegrees * Mth.DEG_TO_RAD);
            }
            if (pitch != null) {
                float physicalPitch = pitchDegrees
                        + SurveillanceCameraViewGeometry.DEFAULT_DOWN_PITCH;
                pitch.setRotX(-physicalPitch * Mth.DEG_TO_RAD);
            }
        }

        private static boolean isLocallyControlled(
                SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity camera,
                Minecraft minecraft) {
            if (!Scp079PlayableClient.cameraMode() || minecraft.player == null
                    || minecraft.level == null || camera.getLevel() != minecraft.level) {
                return false;
            }
            Vec3 baseEye = SurveillanceCameraPlaceholderModule.eyePosition(
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
        private BlockRenderer() {
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
            return true;
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
