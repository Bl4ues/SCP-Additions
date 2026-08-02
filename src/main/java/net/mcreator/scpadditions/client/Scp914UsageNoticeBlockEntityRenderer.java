package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.AbstractFramedSignBlock;
import net.mcreator.scpadditions.facility.Scp914UsageNoticeBlock;
import net.mcreator.scpadditions.facility.Scp914UsageNoticeBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the completed SCP-914 notice directly behind the support glass. */
public final class Scp914UsageNoticeBlockEntityRenderer
        implements BlockEntityRenderer<Scp914UsageNoticeBlockEntity> {
    private static final ResourceLocation NOTICE = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/screens/scpsign/914-notice.png");
    private static final float PANEL_MIN_X = 0.2F / 16.0F;
    private static final float PANEL_MAX_X = 15.7F / 16.0F;
    private static final float PANEL_MIN_Y = 3.15F / 16.0F;
    private static final float PANEL_MAX_Y = 12.85F / 16.0F;
    private static final float IMAGE_Z = FramedSignFrameRenderer.ARTWORK_Z;

    public Scp914UsageNoticeBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(Scp914UsageNoticeBlockEntity notice, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        BlockState state = notice.getBlockState();
        if (!(state.getBlock() instanceof Scp914UsageNoticeBlock)) return;

        FramedSignFrameRenderer.render(state, poseStack, buffer,
                packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(
                state.getValue(AbstractFramedSignBlock.FACING))));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        poseStack.translate(state.getValue(AbstractFramedSignBlock.POSITION)
                .modelOffsetBlocks(), 0.0D, 0.0D);

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityTranslucent(NOTICE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, PANEL_MAX_X, PANEL_MAX_Y, 0.0F, 0.0F,
                packedLight);
        vertex(consumer, matrix, normal, PANEL_MAX_X, PANEL_MIN_Y, 0.0F, 1.0F,
                packedLight);
        vertex(consumer, matrix, normal, PANEL_MIN_X, PANEL_MIN_Y, 1.0F, 1.0F,
                packedLight);
        vertex(consumer, matrix, normal, PANEL_MIN_X, PANEL_MAX_Y, 1.0F, 0.0F,
                packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float y, float u, float v,
            int packedLight) {
        consumer.vertex(matrix, x, y, IMAGE_Z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F)
                .endVertex();
    }

    private static float rotationDegrees(Direction direction) {
        return switch (direction) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
