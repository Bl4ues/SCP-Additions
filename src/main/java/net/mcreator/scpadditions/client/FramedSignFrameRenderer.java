package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.facility.AbstractFramedSignBlock;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders the shared glass-backed frame as block-entity geometry. Unlike baked
 * block models, this remains intact when half of the frame crosses a block
 * boundary for the left and right placement variants.
 */
public final class FramedSignFrameRenderer {
    private static final ResourceLocation GLASS = new ResourceLocation(
            "scp_unity_extra_blocks", "textures/block/glass.png");
    private static final ResourceLocation METAL = new ResourceLocation(
            "scp_unity_extra_blocks", "textures/block/metal.png");

    private FramedSignFrameRenderer() {
    }

    public static void render(BlockState state, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(state.getBlock() instanceof AbstractFramedSignBlock)) return;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees(
                state.getValue(AbstractFramedSignBlock.FACING))));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        poseStack.translate(state.getValue(AbstractFramedSignBlock.POSITION)
                .modelOffsetBlocks(), 0.0D, 0.0D);

        VertexConsumer glass = buffer.getBuffer(
                RenderType.entityTranslucent(GLASS));
        renderPanel(glass, poseStack, packedLight, packedOverlay,
                px(0.2F), px(3.15F), px(15.7F), px(12.85F), px(15.8F));
        renderBox(glass, poseStack, packedLight, packedOverlay,
                px(0.2F), px(2.65F), px(15.7F), px(3.15F),
                px(15.7F), px(15.9F));
        renderBox(glass, poseStack, packedLight, packedOverlay,
                px(0.2F), px(12.85F), px(15.7F), px(13.35F),
                px(15.7F), px(15.9F));

        VertexConsumer metal = buffer.getBuffer(
                RenderType.entityTranslucent(METAL));
        renderRotatedCorner(metal, poseStack, packedLight, packedOverlay,
                0.2F, 12.25F, 0.7F, 12.75F, 0.7F, 12.75F);
        renderRotatedCorner(metal, poseStack, packedLight, packedOverlay,
                14.8F, 12.25F, 15.3F, 12.75F, 15.3F, 12.75F);
        renderRotatedCorner(metal, poseStack, packedLight, packedOverlay,
                14.8F, 3.45F, 15.3F, 3.95F, 15.3F, 3.95F);
        renderRotatedCorner(metal, poseStack, packedLight, packedOverlay,
                0.2F, 3.45F, 0.7F, 3.95F, 0.7F, 3.95F);

        poseStack.popPose();
    }

    private static void renderRotatedCorner(VertexConsumer consumer,
            PoseStack poseStack, int packedLight, int packedOverlay,
            float minX, float minY, float maxX, float maxY,
            float pivotX, float pivotY) {
        poseStack.pushPose();
        poseStack.translate(px(pivotX), px(pivotY), px(16.175F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        poseStack.translate(-px(pivotX), -px(pivotY), -px(16.175F));
        renderBox(consumer, poseStack, packedLight, packedOverlay,
                px(minX), px(minY), px(maxX), px(maxY),
                px(15.55F), px(16.8F));
        poseStack.popPose();
    }

    private static void renderPanel(VertexConsumer consumer,
            PoseStack poseStack, int packedLight, int packedOverlay,
            float minX, float minY, float maxX, float maxY, float z) {
        quad(consumer, poseStack, packedLight, packedOverlay,
                maxX, maxY, z, maxX, minY, z,
                minX, minY, z, minX, maxY, z,
                0.0F, 0.0F, -1.0F);
        quad(consumer, poseStack, packedLight, packedOverlay,
                minX, maxY, z, minX, minY, z,
                maxX, minY, z, maxX, maxY, z,
                0.0F, 0.0F, 1.0F);
    }

    private static void renderBox(VertexConsumer consumer,
            PoseStack poseStack, int packedLight, int packedOverlay,
            float minX, float minY, float maxX, float maxY,
            float minZ, float maxZ) {
        quad(consumer, poseStack, packedLight, packedOverlay,
                maxX, maxY, minZ, maxX, minY, minZ,
                minX, minY, minZ, minX, maxY, minZ,
                0.0F, 0.0F, -1.0F);
        quad(consumer, poseStack, packedLight, packedOverlay,
                minX, maxY, maxZ, minX, minY, maxZ,
                maxX, minY, maxZ, maxX, maxY, maxZ,
                0.0F, 0.0F, 1.0F);
        quad(consumer, poseStack, packedLight, packedOverlay,
                minX, maxY, minZ, minX, minY, minZ,
                minX, minY, maxZ, minX, maxY, maxZ,
                -1.0F, 0.0F, 0.0F);
        quad(consumer, poseStack, packedLight, packedOverlay,
                maxX, maxY, maxZ, maxX, minY, maxZ,
                maxX, minY, minZ, maxX, maxY, minZ,
                1.0F, 0.0F, 0.0F);
        quad(consumer, poseStack, packedLight, packedOverlay,
                minX, maxY, maxZ, maxX, maxY, maxZ,
                maxX, maxY, minZ, minX, maxY, minZ,
                0.0F, 1.0F, 0.0F);
        quad(consumer, poseStack, packedLight, packedOverlay,
                minX, minY, minZ, maxX, minY, minZ,
                maxX, minY, maxZ, minX, minY, maxZ,
                0.0F, -1.0F, 0.0F);
    }

    private static void quad(VertexConsumer consumer, PoseStack poseStack,
            int packedLight, int packedOverlay,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float normalX, float normalY, float normalZ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertex(consumer, matrix, normal, x1, y1, z1,
                0.0F, 0.0F, normalX, normalY, normalZ,
                packedLight, packedOverlay);
        vertex(consumer, matrix, normal, x2, y2, z2,
                0.0F, 1.0F, normalX, normalY, normalZ,
                packedLight, packedOverlay);
        vertex(consumer, matrix, normal, x3, y3, z3,
                1.0F, 1.0F, normalX, normalY, normalZ,
                packedLight, packedOverlay);
        vertex(consumer, matrix, normal, x4, y4, z4,
                1.0F, 0.0F, normalX, normalY, normalZ,
                packedLight, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float y, float z, float u, float v,
            float normalX, float normalY, float normalZ,
            int packedLight, int packedOverlay) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private static float px(float pixels) {
        return pixels / 16.0F;
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
