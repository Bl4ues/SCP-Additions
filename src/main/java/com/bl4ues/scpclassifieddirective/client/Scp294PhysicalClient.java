package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.Scp294Block;
import com.bl4ues.scpclassifieddirective.block.entity.Scp294BlockEntity;
import com.bl4ues.scpclassifieddirective.client.gui.Scp294GuiScreen;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * Physical presentation for SCP-294. The large coffee display remains the
 * authored decorative image; only the tiny payment readout and temporary cup
 * are added in world space. Context prompts target the real payment panel and
 * keyboard instead of treating the whole machine as one button.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp294PhysicalClient {
    /** Close framing around the upper console rather than the full 2-block body. */
    public static final double FOCUS_DISTANCE = 0.72D;

    private static final double FOCUS_CENTER_X = 8.0D / 16.0D;
    private static final double FOCUS_CENTER_Y = 26.0D / 16.0D;
    private static final double FOCUS_CENTER_Z = 0.0D;
    private static final double FOCUS_WIDTH = 14.0D / 16.0D;
    private static final double FOCUS_HEIGHT = 12.0D / 16.0D;

    // Exact authored readout area measured from the SCP-294 model:
    // X 2.15..3.15, Y 24.60..25.10, Z -0.12. The supplied Z is already
    // offset from the model surface, so do not add another depth fudge here.
    private static final double STATUS_CENTER_X = 2.65D / 16.0D;
    private static final double STATUS_CENTER_Y = 24.85D / 16.0D;
    private static final double STATUS_CENTER_Z = -0.12D / 16.0D;
    private static final double STATUS_WIDTH = 1.00D / 16.0D;
    private static final double STATUS_HEIGHT = 0.50D / 16.0D;
    private static final double STATUS_PADDING_X = 0.06D / 16.0D;
    private static final double STATUS_PADDING_Y = 0.04D / 16.0D;
    private static final int STATUS_TEXT = 0xFF071109;
    private static final ResourceLocation STATUS_FONT = ScpFonts.PF_VIDEOTEXT;

    // The dispensing opening is only painted into the authored model, so the
    // temporary cup is rendered as its own upright cutout exactly on that slot
    // instead of using item transforms that push it forward/down unpredictably.
    private static final double CUP_CENTER_X = 11.50D / 16.0D;
    private static final double CUP_CENTER_Y = 7.45D / 16.0D;
    private static final double CUP_CENTER_Z = -0.0010D;
    private static final double CUP_WIDTH = 2.10D / 16.0D;
    private static final double CUP_HEIGHT = 2.90D / 16.0D;
    private static final double CUP_EPSILON = 0.00045D;
    private static final ResourceLocation CUP_TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/item/emptycup.png");

    private static final double MODEL_UNIT = 1.0D / 16.0D;
    private static final ResourceLocation OUTLINE_MASK_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/white_concrete.png");

    private Scp294PhysicalClient() { }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityType<Scp294BlockEntity> normal = (BlockEntityType)
                ScpClassifiedDirectiveModBlockEntities.SCP_294.get();
        event.registerBlockEntityRenderer(normal, Renderer::new);
    }

    public static Frame focusFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, FOCUS_CENTER_X, FOCUS_CENTER_Y,
                FOCUS_CENTER_Z, FOCUS_WIDTH, FOCUS_HEIGHT);
    }

    private static Frame statusFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, STATUS_CENTER_X, STATUS_CENTER_Y,
                STATUS_CENTER_Z, STATUS_WIDTH, STATUS_HEIGHT);
    }

    private static Frame cupFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, CUP_CENTER_X, CUP_CENTER_Y,
                CUP_CENTER_Z, CUP_WIDTH, CUP_HEIGHT);
    }

    private static Frame frame(BlockPos pos, Direction facing,
            double centerX, double centerY, double centerZ,
            double width, double height) {
        return PhysicalBlockScreenGeometry.fromNorthFacing(pos, facing,
                centerX, centerY, centerZ, 0.0D, width, height);
    }

    public static boolean isContextControl(String interactionKey) {
        return Scp294Block.COIN_INTERACTION_KEY.equals(interactionKey)
                || Scp294Block.KEYBOARD_INTERACTION_KEY.equals(interactionKey);
    }

    /** Draw only the authored physical control selected by Context Interaction. */
    public static void renderContextOutline(BlockPos pos, BlockState state,
            String interactionKey, PoseStack poseStack, Camera camera,
            MultiBufferSource buffers) {
        if (pos == null || state == null || camera == null || buffers == null) return;
        Direction facing = state.hasProperty(Scp294Block.FACING)
                ? state.getValue(Scp294Block.FACING) : Direction.NORTH;
        Vec3 cameraPosition = camera.getPosition();
        VertexConsumer consumer = buffers.getBuffer(
                RenderType.entityCutoutNoCull(OUTLINE_MASK_TEXTURE));

        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX() - cameraPosition.x,
                    pos.getY() - cameraPosition.y,
                    pos.getZ() - cameraPosition.z);
            poseStack.translate(0.5D, 0.0D, 0.5D);
            rotateForFacing(poseStack, facing);
            poseStack.translate(-0.5D, 0.0D, -0.5D);

            if (Scp294Block.COIN_INTERACTION_KEY.equals(interactionKey)) {
                // Exact measured payment-panel bounds from the model.
                emitModelBox(consumer, poseStack.last(),
                        1.40D, 21.60D, -0.10D,
                        3.90D, 25.60D, 0.00D);
            } else if (Scp294Block.KEYBOARD_INTERACTION_KEY.equals(interactionKey)) {
                // The keyboard backing plate is tilted 22.5 degrees. Render only
                // its visible front plane, not the full one-pixel-thick cuboid.
                poseStack.pushPose();
                try {
                    double pivotX = 8.0D * MODEL_UNIT;
                    double pivotY = 22.0D * MODEL_UNIT;
                    double pivotZ = 0.5D * MODEL_UNIT;
                    poseStack.translate(pivotX, pivotY, pivotZ);
                    poseStack.mulPose(Axis.XP.rotationDegrees(22.5F));
                    poseStack.translate(-pivotX, -pivotY, -pivotZ);
                    emitModelFrontPlane(consumer, poseStack.last(),
                            5.0D, 19.70D, 0.96D,
                            14.0D, 25.70D);
                } finally {
                    poseStack.popPose();
                }
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static final class Renderer
            implements BlockEntityRenderer<Scp294BlockEntity> {
        private final Font font;

        private Renderer(BlockEntityRendererProvider.Context context) {
            this.font = context.getFont();
        }

        @Override
        public void render(Scp294BlockEntity machine, float partialTick,
                PoseStack poseStack, MultiBufferSource buffers,
                int packedLight, int packedOverlay) {
            Direction facing = facing(machine.getBlockState());
            BlockPos pos = machine.getBlockPos();
            Scp294GuiScreen screen = activeScreen(pos);

            String status;
            boolean reserveCursor = false;
            boolean cursorVisible = false;
            if (machine.isPouring()) {
                status = "POURING";
            } else if (machine.isOutOfRange()) {
                status = "OUT OF RANGE";
            } else if (screen != null) {
                String order = screen.physicalOrder();
                if (order.isBlank()) {
                    status = "ENTER ORDER";
                } else {
                    status = order;
                    reserveCursor = screen.physicalInputFocused();
                    cursorVisible = reserveCursor
                            && (System.currentTimeMillis() / 400L) % 2L == 0L;
                }
            } else if (machine.getItem(0).is(ScpClassifiedDirectiveModItems.COIN.get())) {
                status = "ENTER ORDER";
            } else {
                status = "INSERT $0.50";
            }

            renderStatus(font, poseStack, buffers, pos, facing, status,
                    reserveCursor, cursorVisible);
            if (machine.isPouring()) {
                renderEmptyCup(poseStack, buffers, pos, facing);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(Scp294BlockEntity blockEntity) {
            return true;
        }
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }

    /**
     * Left-anchored status rendering. Long orders shrink to the exact physical
     * readout bounds; reserving cursor width keeps the text stationary while the
     * underscore blinks. Padding is applied on every edge of the measured area.
     */
    private static void renderStatus(Font font, PoseStack poseStack,
            MultiBufferSource buffers, BlockPos pos, Direction facing,
            String rawStatus, boolean reserveCursor, boolean cursorVisible) {
        Frame frame = statusFrame(pos, facing);
        String status = rawStatus == null ? ""
                : rawStatus.toUpperCase(Locale.ROOT);
        String measured = status + (reserveCursor ? "_" : "");
        String visible = status + (cursorVisible ? "_" : "");
        Component measuredText = Component.literal(measured).withStyle(
                style -> style.withFont(STATUS_FONT));
        Component display = Component.literal(visible).withStyle(
                style -> style.withFont(STATUS_FONT));

        double usableWidth = Math.max(0.001D,
                frame.width() - STATUS_PADDING_X * 2.0D);
        double usableHeight = Math.max(0.001D,
                frame.height() - STATUS_PADDING_Y * 2.0D);
        float measuredWidth = Math.max(1.0F, font.width(measuredText));
        float scaleByWidth = (float) (usableWidth / measuredWidth);
        float scaleByHeight = (float) (usableHeight / 9.0D);
        float textScale = Math.max(0.0001F,
                Math.min(scaleByWidth, scaleByHeight));

        double normalizedInset = STATUS_PADDING_X / frame.width();
        Vec3 topLeft = local(frame.point(-0.5D + normalizedInset,
                0.5D, 0.0D), pos);
        float logicalHeight = (float) (frame.height() / textScale);
        float y = Math.max(0.0F, (logicalHeight - 9.0F) * 0.5F);

        poseStack.pushPose();
        poseStack.translate(topLeft.x, topLeft.y, topLeft.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(textYaw(facing)));
        poseStack.scale(textScale, -textScale, textScale);
        font.drawInBatch(display, 0.0F, y, STATUS_TEXT, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void renderEmptyCup(PoseStack poseStack,
            MultiBufferSource buffers, BlockPos pos, Direction facing) {
        Frame frame = cupFrame(pos, facing);
        Vec3 topLeft = local(frame.point(-0.5D, 0.5D, CUP_EPSILON), pos);
        Vec3 topRight = local(frame.point(0.5D, 0.5D, CUP_EPSILON), pos);
        Vec3 bottomRight = local(frame.point(0.5D, -0.5D, CUP_EPSILON), pos);
        Vec3 bottomLeft = local(frame.point(-0.5D, -0.5D, CUP_EPSILON), pos);
        VertexConsumer consumer = buffers.getBuffer(
                RenderType.entityCutoutNoCull(CUP_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        Vec3 outward = frame.outward();

        texturedQuad(consumer, matrix, normal,
                topLeft, topRight, bottomRight, bottomLeft,
                (float) outward.x, (float) outward.y, (float) outward.z);
    }

    private static Scp294GuiScreen activeScreen(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Scp294GuiScreen screen
                && screen.isFor(pos)) {
            return screen;
        }
        return null;
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

    private static void rotateForFacing(PoseStack poseStack,
            Direction facing) {
        float degrees = switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
        if (degrees != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        }
    }

    private static void emitModelFrontPlane(VertexConsumer consumer,
            PoseStack.Pose pose, double fromX, double fromY, double z,
            double toX, double toY) {
        float minX = (float) (Math.min(fromX, toX) * MODEL_UNIT);
        float minY = (float) (Math.min(fromY, toY) * MODEL_UNIT);
        float maxX = (float) (Math.max(fromX, toX) * MODEL_UNIT);
        float maxY = (float) (Math.max(fromY, toY) * MODEL_UNIT);
        float planeZ = (float) (z * MODEL_UNIT);
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        quad(consumer, matrix, normal,
                maxX, minY, planeZ,
                minX, minY, planeZ,
                minX, maxY, planeZ,
                maxX, maxY, planeZ,
                0, 0, -1);
    }

    private static void emitModelBox(VertexConsumer consumer,
            PoseStack.Pose pose, double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ) {
        float minX = (float) (Math.min(fromX, toX) * MODEL_UNIT);
        float minY = (float) (Math.min(fromY, toY) * MODEL_UNIT);
        float minZ = (float) (Math.min(fromZ, toZ) * MODEL_UNIT);
        float maxX = (float) (Math.max(fromX, toX) * MODEL_UNIT);
        float maxY = (float) (Math.max(fromY, toY) * MODEL_UNIT);
        float maxZ = (float) (Math.max(fromZ, toZ) * MODEL_UNIT);
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        quad(consumer, matrix, normal,
                maxX, minY, minZ, minX, minY, minZ,
                minX, maxY, minZ, maxX, maxY, minZ, 0, 0, -1);
        quad(consumer, matrix, normal,
                minX, minY, maxZ, maxX, minY, maxZ,
                maxX, maxY, maxZ, minX, maxY, maxZ, 0, 0, 1);
        quad(consumer, matrix, normal,
                minX, minY, minZ, minX, minY, maxZ,
                minX, maxY, maxZ, minX, maxY, minZ, -1, 0, 0);
        quad(consumer, matrix, normal,
                maxX, minY, maxZ, maxX, minY, minZ,
                maxX, maxY, minZ, maxX, maxY, maxZ, 1, 0, 0);
        quad(consumer, matrix, normal,
                minX, maxY, minZ, minX, maxY, maxZ,
                maxX, maxY, maxZ, maxX, maxY, minZ, 0, 1, 0);
        quad(consumer, matrix, normal,
                minX, minY, maxZ, minX, minY, minZ,
                maxX, minY, minZ, maxX, minY, maxZ, 0, -1, 0);
    }

    private static void texturedQuad(VertexConsumer consumer,
            Matrix4f matrix, Matrix3f normal, Vec3 topLeft, Vec3 topRight,
            Vec3 bottomRight, Vec3 bottomLeft, float nx, float ny, float nz) {
        vertex(consumer, matrix, normal,
                (float) topLeft.x, (float) topLeft.y, (float) topLeft.z,
                0, 0, nx, ny, nz);
        vertex(consumer, matrix, normal,
                (float) topRight.x, (float) topRight.y, (float) topRight.z,
                1, 0, nx, ny, nz);
        vertex(consumer, matrix, normal,
                (float) bottomRight.x, (float) bottomRight.y, (float) bottomRight.z,
                1, 1, nx, ny, nz);
        vertex(consumer, matrix, normal,
                (float) bottomLeft.x, (float) bottomLeft.y, (float) bottomLeft.z,
                0, 1, nx, ny, nz);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x0, float y0, float z0,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float nx, float ny, float nz) {
        vertex(consumer, matrix, normal, x0, y0, z0, 0, 0, nx, ny, nz);
        vertex(consumer, matrix, normal, x1, y1, z1, 1, 0, nx, ny, nz);
        vertex(consumer, matrix, normal, x2, y2, z2, 1, 1, nx, ny, nz);
        vertex(consumer, matrix, normal, x3, y3, z3, 0, 1, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float y, float z, float u, float v,
            float nx, float ny, float nz) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }

    private static Vec3 local(Vec3 world, BlockPos pos) {
        return world.subtract(pos.getX(), pos.getY(), pos.getZ());
    }
}
