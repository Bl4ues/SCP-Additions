package net.mcreator.scpadditions.facility.elevator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Client renderers for the authored elevator assets and procedural cables. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CoreRoomElevatorClient {
    private static final ResourceLocation CABLE_TEXTURE = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/block/core_room_elevator_beams.png");

    private CoreRoomElevatorClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(CoreRoomElevatorModule.CARRIAGE.get(),
                CarriageRenderer::new);
        event.registerBlockEntityRenderer(CoreRoomElevatorModule.STATION_BE.get(),
                StationRenderer::new);
        event.registerBlockEntityRenderer(CoreRoomElevatorModule.PULLEY_BE.get(),
                PulleyRenderer::new);
    }

    private static float rotationFor(Direction facing) {
        return switch (facing) {
            case EAST -> (float) (-Math.PI / 2.0D);
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) (Math.PI / 2.0D);
            default -> 0.0F;
        };
    }

    public static final class StationModel extends
            GeoModel<CoreRoomElevatorModule.StationBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                CoreRoomElevatorModule.StationBlockEntity animatable) {
            return ElevatorAssets.FLOOR_STATION_MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                CoreRoomElevatorModule.StationBlockEntity animatable) {
            return ElevatorAssets.FLOOR_STATION_TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                CoreRoomElevatorModule.StationBlockEntity animatable) {
            return ElevatorAssets.FLOOR_STATION_ANIMATION;
        }

    }

    public static final class StationRenderer extends
            GeoBlockRenderer<CoreRoomElevatorModule.StationBlockEntity> {
        public StationRenderer(BlockEntityRendererProvider.Context context) {
            super(new StationModel());
        }

        @Override
        public boolean shouldRenderOffScreen(
                CoreRoomElevatorModule.StationBlockEntity blockEntity) {
            return true;
        }

        @Override
        public RenderType getRenderType(
                CoreRoomElevatorModule.StationBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }

    public static final class PulleyModel extends
            GeoModel<CoreRoomElevatorModule.PulleyBlockEntity> {
        private static final ResourceLocation EMPTY_ANIMATION =
                new ResourceLocation(ScpAdditionsMod.MODID,
                        "animations/block/core_room_elevator_pulley.animation.json");

        @Override
        public ResourceLocation getModelResource(
                CoreRoomElevatorModule.PulleyBlockEntity animatable) {
            return ElevatorAssets.PULLEY_MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                CoreRoomElevatorModule.PulleyBlockEntity animatable) {
            return ElevatorAssets.PULLEY_TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                CoreRoomElevatorModule.PulleyBlockEntity animatable) {
            return EMPTY_ANIMATION;
        }

    }

    public static final class PulleyRenderer extends
            GeoBlockRenderer<CoreRoomElevatorModule.PulleyBlockEntity> {
        public PulleyRenderer(BlockEntityRendererProvider.Context context) {
            super(new PulleyModel());
        }

        @Override
        public boolean shouldRenderOffScreen(
                CoreRoomElevatorModule.PulleyBlockEntity blockEntity) {
            return true;
        }

        @Override
        public RenderType getRenderType(
                CoreRoomElevatorModule.PulleyBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }

    public static final class CarriageModel extends
            GeoModel<CoreRoomElevatorCarriageEntity> {
        @Override
        public ResourceLocation getModelResource(
                CoreRoomElevatorCarriageEntity animatable) {
            return ElevatorAssets.CARRIAGE_MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(
                CoreRoomElevatorCarriageEntity animatable) {
            return ElevatorAssets.CARRIAGE_TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                CoreRoomElevatorCarriageEntity animatable) {
            return ElevatorAssets.CARRIAGE_ANIMATION;
        }

    }

    public static final class CarriageRenderer extends
            GeoEntityRenderer<CoreRoomElevatorCarriageEntity> {
        public CarriageRenderer(EntityRendererProvider.Context context) {
            super(context, new CarriageModel());
            shadowRadius = 0.0F;
        }

        @Override
        public boolean shouldRender(CoreRoomElevatorCarriageEntity entity,
                Frustum frustum, double camX, double camY, double camZ) {
            return true;
        }

        @Override
        public RenderType getRenderType(
                CoreRoomElevatorCarriageEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityTranslucent(texture);
        }

        @Override
        public void render(CoreRoomElevatorCarriageEntity entity,
                float entityYaw, float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight) {
            super.render(entity, entityYaw, partialTick, poseStack,
                    bufferSource, packedLight);
            renderCable(entity, true, partialTick, poseStack, bufferSource,
                    packedLight);
            renderCable(entity, false, partialTick, poseStack, bufferSource,
                    packedLight);
        }

        private static void renderCable(CoreRoomElevatorCarriageEntity entity,
                boolean front, float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight) {
            Vec3 attachment = entity.cableAttachment(front, partialTick);
            Vec3 origin = entity.cableOrigin(front, partialTick);
            double bottom = attachment.y;
            double top = origin.y;
            if (top <= bottom + 0.02D) return;

            Vec3 renderOrigin = entity.getPosition(partialTick);
            float x = (float) ((attachment.x + origin.x) * 0.5D
                    - renderOrigin.x);
            float z = (float) ((attachment.z + origin.z) * 0.5D
                    - renderOrigin.z);
            float y0 = (float) (bottom - renderOrigin.y);
            float y1 = (float) (top - renderOrigin.y);
            float radius = 0.028F;
            float vMax = Math.max(1.0F, (y1 - y0) * 4.0F);

            VertexConsumer consumer = bufferSource.getBuffer(
                    RenderType.entityCutoutNoCull(CABLE_TEXTURE));
            PoseStack.Pose pose = poseStack.last();
            Matrix4f matrix = pose.pose();
            Matrix3f normal = pose.normal();
            quad(consumer, matrix, normal,
                    x - radius, y0, z - radius,
                    x - radius, y1, z - radius,
                    x + radius, y1, z - radius,
                    x + radius, y0, z - radius,
                    0.0F, vMax, packedLight, 0.0F, 0.0F, -1.0F);
            quad(consumer, matrix, normal,
                    x + radius, y0, z + radius,
                    x + radius, y1, z + radius,
                    x - radius, y1, z + radius,
                    x - radius, y0, z + radius,
                    0.0F, vMax, packedLight, 0.0F, 0.0F, 1.0F);
            quad(consumer, matrix, normal,
                    x - radius, y0, z + radius,
                    x - radius, y1, z + radius,
                    x - radius, y1, z - radius,
                    x - radius, y0, z - radius,
                    0.0F, vMax, packedLight, -1.0F, 0.0F, 0.0F);
            quad(consumer, matrix, normal,
                    x + radius, y0, z - radius,
                    x + radius, y1, z - radius,
                    x + radius, y1, z + radius,
                    x + radius, y0, z + radius,
                    0.0F, vMax, packedLight, 1.0F, 0.0F, 0.0F);
        }

        private static void quad(VertexConsumer consumer, Matrix4f matrix,
                Matrix3f normal, float x0, float y0, float z0,
                float x1, float y1, float z1,
                float x2, float y2, float z2,
                float x3, float y3, float z3,
                float u0, float v1, int light,
                float normalX, float normalY, float normalZ) {
            vertex(consumer, matrix, normal, x0, y0, z0,
                    0.0F, 0.0F, light, normalX, normalY, normalZ);
            vertex(consumer, matrix, normal, x1, y1, z1,
                    0.0F, v1, light, normalX, normalY, normalZ);
            vertex(consumer, matrix, normal, x2, y2, z2,
                    1.0F, v1, light, normalX, normalY, normalZ);
            vertex(consumer, matrix, normal, x3, y3, z3,
                    1.0F, 0.0F, light, normalX, normalY, normalZ);
        }

        private static void vertex(VertexConsumer consumer, Matrix4f matrix,
                Matrix3f normal, float x, float y, float z, float u, float v,
                int light, float normalX, float normalY, float normalZ) {
            consumer.vertex(matrix, x, y, z)
                    .color(255, 255, 255, 255)
                    .uv(u, v)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normal, normalX, normalY, normalZ)
                    .endVertex();
        }
    }
}
