package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.HazardSignBlock;
import com.bl4ues.scpclassifieddirective.facility.HazardSignBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.ScpSignHazards;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders the portrait warning artwork and its selected anomaly pictogram. */
public final class HazardSignBlockEntityRenderer
        implements BlockEntityRenderer<HazardSignBlockEntity> {
    private static final ResourceLocation BASE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/screens/scpsign/hazard_warning.png");

    private static final float IMAGE_WIDTH = 640.0F;
    private static final float IMAGE_HEIGHT = 1024.0F;
    private static final float PANEL_MIN_X = 3.15F / 16.0F;
    private static final float PANEL_MAX_X = 12.85F / 16.0F;
    private static final float PANEL_MIN_Y = 0.2F / 16.0F;
    private static final float PANEL_MAX_Y = 15.7F / 16.0F;
    private static final float PANEL_WIDTH = PANEL_MAX_X - PANEL_MIN_X;
    private static final float PANEL_HEIGHT = PANEL_MAX_Y - PANEL_MIN_Y;
    private static final ImageArea PICTOGRAM =
            new ImageArea(96.0F, 365.0F, 448.0F, 448.0F);

    public HazardSignBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HazardSignBlockEntity sign, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        BlockState state = sign.getBlockState();
        if (!(state.getBlock() instanceof HazardSignBlock)) return;
        Direction facing = state.getValue(HazardSignBlock.FACING);

        FramedSignFrameRenderer.renderPortrait(facing, poseStack, buffer,
                packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(facing)));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        renderImage(BASE, new ImageArea(0.0F, 0.0F,
                        IMAGE_WIDTH, IMAGE_HEIGHT),
                FramedSignFrameRenderer.ARTWORK_Z,
                poseStack, buffer, packedLight);

        ScpSignHazards.Option option = ScpSignHazards.option(sign.hazardId());
        ResourceLocation texture = option.texture();
        if (!option.isNone() && resourceExists(texture)) {
            renderImage(texture, PICTOGRAM,
                    FramedSignFrameRenderer.ARTWORK_DETAIL_Z,
                    poseStack, buffer, packedLight);
        }
        poseStack.popPose();
    }

    private static void renderImage(ResourceLocation texture, ImageArea area,
            float z, PoseStack poseStack, MultiBufferSource buffer,
            int packedLight) {
        float left = panelX(area.x());
        float right = panelX(area.x() + area.width());
        float top = panelY(area.y());
        float bottom = panelY(area.y() + area.height());

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        vertex(consumer, matrix, normal, left, top, z,
                0.0F, 0.0F, packedLight);
        vertex(consumer, matrix, normal, left, bottom, z,
                0.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, right, bottom, z,
                1.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, right, top, z,
                1.0F, 0.0F, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float y, float z, float u, float v,
            int packedLight) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F)
                .endVertex();
    }

    private static boolean resourceExists(ResourceLocation texture) {
        return texture != null && Minecraft.getInstance().getResourceManager()
                .getResource(texture).isPresent();
    }

    private static float panelX(float imageX) {
        return PANEL_MAX_X - imageX / IMAGE_WIDTH * PANEL_WIDTH;
    }

    private static float panelY(float imageY) {
        return PANEL_MAX_Y - imageY / IMAGE_HEIGHT * PANEL_HEIGHT;
    }

    private static float rotationDegrees(Direction direction) {
        return switch (direction) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private record ImageArea(float x, float y, float width, float height) {
    }
}
