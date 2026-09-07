package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.block.TeslaTerminalBlockBlock;
import com.bl4ues.scpclassifieddirective.block.entity.TeslaTerminalBlockEntity;
import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Full-bright physical CRT surface drawn a hair above the authored monitor. */
public final class TeslaTerminalBlockEntityRenderer
        implements BlockEntityRenderer<TeslaTerminalBlockEntity> {
    private static final ResourceLocation ROBOTO_FONT = new ResourceLocation(
            "scp_classified_directive", "roboto");
    private static final ResourceLocation SCREEN_ON = screen("1");
    private static final ResourceLocation SCREEN_OFF = screen("3");
    private static final ResourceLocation SCREEN_ON_OVERRIDE = screen("11");
    private static final ResourceLocation SCREEN_AUXILIARY_OFFLINE = screen("12");
    private static final double BASE_EPSILON = 0.0022D;
    private static final double TEXT_EPSILON = 0.0026D;
    private static final double OVERLAY_EPSILON = 0.0030D;
    private static final float PERMISSION_TEXT_SCALE = 2.6F;
    private static final float PERMISSION_X = 1278.0F;
    private static final float PERMISSION_Y = 79.0F;

    private final Font font;

    public TeslaTerminalBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
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
        boolean authenticated = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TeslaTerminalScreen screen
                && screen.isFor(pos)) {
            base = screen.physicalBaseTexture();
            overlay = screen.physicalOverlayTexture();
            authenticated = screen.physicalAuthenticated();
        } else {
            base = terminal.manualOverride() ? SCREEN_ON_OVERRIDE
                    : terminal.teslaGatesEnabled() ? SCREEN_ON : SCREEN_OFF;
            if (!terminal.auxiliaryPowerOnline()) {
                overlay = SCREEN_AUXILIARY_OFFLINE;
            }
        }

        renderQuad(poseStack, buffers, frame, pos, base, BASE_EPSILON);
        renderPermissionText(poseStack, buffers, frame, pos, facing,
                authenticated);
        if (overlay != null) {
            renderQuad(poseStack, buffers, frame, pos, overlay,
                    OVERLAY_EPSILON);
        }
    }

    private void renderPermissionText(PoseStack poseStack,
            MultiBufferSource buffers, Frame frame, BlockPos pos,
            Direction facing, boolean authenticated) {
        Vec3 topLeft = local(frame.point(-0.5D, 0.5D, TEXT_EPSILON), pos);
        float pixelScaleX = (float) (frame.width() / TeslaTerminalScreen.TEX_W);
        float pixelScaleY = (float) (frame.height() / TeslaTerminalScreen.TEX_H);
        float depthScale = Math.min(pixelScaleX, pixelScaleY);
        int color = authenticated ? 0x608952 : 0xAC384A;
        Component text = Component.literal(authenticated ? "GRANTED" : "DENIED")
                .withStyle(style -> style.withFont(ROBOTO_FONT));

        poseStack.pushPose();
        poseStack.translate(topLeft.x, topLeft.y, topLeft.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(textYaw(facing)));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (float) -TeslaTerminalFocusClient.SCREEN_TILT_DEGREES));
        // The authored CRT is not the same aspect ratio as the old 1410x1080
        // fullscreen GUI. Map texture pixels independently on X/Y so dynamic
        // permission text remains on the same pixel as the stretched image.
        poseStack.scale(pixelScaleX, -pixelScaleY, depthScale);
        poseStack.translate(PERMISSION_X, PERMISSION_Y, 0.0F);
        poseStack.scale(PERMISSION_TEXT_SCALE, PERMISSION_TEXT_SCALE,
                PERMISSION_TEXT_SCALE);
        font.drawInBatch(text, 0.0F, 0.0F, color, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL,
                0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static float textYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> -90.0F;
            default -> 180.0F;
        };
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
