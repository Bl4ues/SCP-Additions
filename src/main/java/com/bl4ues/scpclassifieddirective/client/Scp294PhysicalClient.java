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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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

    // Tiny horizontal readout at the top of the payment panel. The panel lives
    // on the viewer-right black column of the authored NORTH-facing model.
    private static final double STATUS_CENTER_X = 2.45D / 16.0D;
    private static final double STATUS_CENTER_Y = 24.05D / 16.0D;
    private static final double STATUS_CENTER_Z = -0.012D;
    private static final double STATUS_WIDTH = 2.25D / 16.0D;
    private static final double STATUS_HEIGHT = 0.50D / 16.0D;
    private static final int STATUS_W = 128;
    private static final int STATUS_H = 18;
    private static final int STATUS_TEXT = 0xFF071109;
    private static final double STATUS_TEXT_EPSILON = 0.0015D;
    private static final ResourceLocation STATUS_FONT = ScpFonts.PF_VIDEOTEXT;

    // Dispensing recess on the lower front. Keep the temporary empty cup inside
    // the cubby, not floating in front of the machine.
    private static final double CUP_CENTER_X = 11.50D / 16.0D;
    private static final double CUP_CENTER_Y = 10.10D / 16.0D;
    private static final double CUP_CENTER_Z = 2.10D / 16.0D;
    private static final float CUP_SCALE = 0.16F;

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
                CUP_CENTER_Z, 0.01D, 0.01D);
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

    /** Draw only the visible face of the selected physical control. */
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
                // Viewer-right payment panel. Only its front surface enters the
                // outline mask, preventing a thick cuboid silhouette.
                emitModelFrontPlane(consumer, poseStack.last(),
                        0.75D, 19.0D, -0.03D,
                        4.15D, 25.15D);
            } else if (Scp294Block.KEYBOARD_INTERACTION_KEY.equals(interactionKey)) {
                // The keyboard backing plate is tilted 22.5 degrees. Render only
                // its visible front plane, not the full one-pixel-thick cuboid,
                // so the outline cannot continue backwards behind the CRT.
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
                    if (screen.physicalInputFocused()
                            && (System.currentTimeMillis() / 400L) % 2L == 0L) {
                        status += "_";
                    }
                }
            } else if (machine.getItem(0).is(ScpClassifiedDirectiveModItems.COIN.get())) {
                status = "ENTER ORDER";
            } else {
                status = "INSERT $0.50";
            }

            renderStatus(font, poseStack, buffers, pos, facing, status);
            if (machine.isPouring()) {
                renderEmptyCup(machine, poseStack, buffers, pos, facing);
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

    private static void renderStatus(Font font, PoseStack poseStack,
            MultiBufferSource buffers, BlockPos pos, Direction facing,
            String rawStatus) {
        Frame frame = statusFrame(pos, facing);
        Vec3 topLeft = local(frame.point(-0.5D, 0.5D,
                STATUS_TEXT_EPSILON), pos);
        float pixelScaleX = (float) (frame.width() / STATUS_W);
        float pixelScaleY = (float) (frame.height() / STATUS_H);
        float depthScale = Math.min(pixelScaleX, pixelScaleY);
        String status = fitToWidth(font,
                rawStatus == null ? "" : rawStatus.toUpperCase(Locale.ROOT),
                STATUS_W - 6);
        Component display = Component.literal(status).withStyle(
                style -> style.withFont(STATUS_FONT));

        poseStack.pushPose();
        poseStack.translate(topLeft.x, topLeft.y, topLeft.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(textYaw(facing)));
        poseStack.scale(pixelScaleX, -pixelScaleY, depthScale);
        float x = Math.max(3.0F, (STATUS_W - font.width(display)) * 0.5F);
        float y = (STATUS_H - 9.0F) * 0.5F - 0.5F;
        font.drawInBatch(display, x, y, STATUS_TEXT, false,
                poseStack.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET,
                0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static String fitToWidth(Font font, String raw, int width) {
        String value = raw;
        while (!value.isEmpty() && font.width(Component.literal(value)
                .withStyle(style -> style.withFont(STATUS_FONT))) > width) {
            value = value.substring(1);
        }
        return value;
    }

    private static void renderEmptyCup(Scp294BlockEntity machine,
            PoseStack poseStack, MultiBufferSource buffers, BlockPos pos,
            Direction facing) {
        if (machine.getLevel() == null) return;
        Frame anchor = cupFrame(pos, facing);
        Vec3 center = local(anchor.center(), pos);
        ItemStack cup = new ItemStack(ScpClassifiedDirectiveModItems.EMPTY_CUP.get());

        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(textYaw(facing)));
        poseStack.scale(CUP_SCALE, CUP_SCALE, CUP_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(cup,
                ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, buffers,
                machine.getLevel(), 0);
        poseStack.popPose();
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
