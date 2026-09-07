package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.block.TeslaTerminalBlockBlock;
import com.bl4ues.scpclassifieddirective.block.entity.TeslaTerminalBlockEntity;
import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Full-bright physical CRT surface drawn a hair above the authored monitor. */
public final class TeslaTerminalBlockEntityRenderer
        implements BlockEntityRenderer<TeslaTerminalBlockEntity> {
    private static final ResourceLocation SCREEN_ON = screen("1");
    private static final ResourceLocation SCREEN_OFF = screen("3");
    private static final ResourceLocation SCREEN_ON_OVERRIDE = screen("11");
    private static final ResourceLocation SCREEN_AUXILIARY_OFFLINE = screen("12");
    private static final double BASE_EPSILON = 0.0022D;
    private static final double OVERLAY_EPSILON = 0.0030D;

    public TeslaTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TeslaTerminalBlockEntity terminal, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int packedLight,
            int packedOverlay) {
        BlockState state = terminal.getBlockState();
        Direction facing = state.hasProperty(TeslaTerminalBlockBlock.FACING)
                ? state.getValue(TeslaTerminalBlockBlock.FACING)
                : Direction.NORTH;
        BlockPos pos = terminal.getBlockPos();
        Frame frame = TeslaTerminalFocusClient.frame(pos, facing);

        ResourceLocation base;
        ResourceLocation overlay = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TeslaTerminalScreen screen
                && screen.isFor(pos)) {
            base = screen.physicalBaseTexture();
            overlay = screen.physicalOverlayTexture();
        } else {
            base = terminal.manualOverride() ? SCREEN_ON_OVERRIDE
                    : terminal.teslaGatesEnabled() ? SCREEN_ON : SCREEN_OFF;
            if (!terminal.auxiliaryPowerOnline()) {
                overlay = SCREEN_AUXILIARY_OFFLINE;
            }
        }

        renderQuad(poseStack, buffers, frame, pos, base, BASE_EPSILON);
        if (overlay != null) {
            renderQuad(poseStack, buffers, frame, pos, overlay,
                    OVERLAY_EPSILON);
        }
    }

    private static void renderQuad(PoseStack poseStack,
            MultiBufferSource buffers, Frame frame, BlockPos pos,
            ResourceLocation texture, double normalOffset) {
        VertexConsumer consumer = buffers.getBuffer(
                RenderType.entityTranslucentEmissive(texture));
        Vec3 topLeft = local(frame.point(-0.5D, 0.5D, normalOffset), pos);
        Vec3 topRight = local(frame.point(0.5D, 0.5D, normalOffset), pos);
        Vec3 bottomRight = local(frame.point(0.5D, -0.5D, normalOffset), pos);
        Vec3 bottomLeft = local(frame.point(-0.5D, -0.5D, normalOffset), pos);
        Vec3 normal = frame.outward();

        vertex(consumer, poseStack, topLeft, 0.0F, 0.0F, normal);
        vertex(consumer, poseStack, topRight, 1.0F, 0.0F, normal);
        vertex(consumer, poseStack, bottomRight, 1.0F, 1.0F, normal);
        vertex(consumer, poseStack, bottomLeft, 0.0F, 1.0F, normal);
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack,
            Vec3 point, float u, float v, Vec3 normal) {
        consumer.vertex(poseStack.last().pose(), (float) point.x,
                        (float) point.y, (float) point.z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(poseStack.last().normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static Vec3 local(Vec3 world, BlockPos pos) {
        return world.subtract(pos.getX(), pos.getY(), pos.getZ());
    }

    private static ResourceLocation screen(String id) {
        return new ResourceLocation("scp_classified_directive",
                "textures/screens/" + id + ".png");
    }
}
