package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.Scp294Block;
import com.bl4ues.scpclassifieddirective.block.entity.Scp294BlockEntity;
import com.bl4ues.scpclassifieddirective.client.gui.Scp294GuiScreen;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Physical screen and controls for SCP-294. The vanilla block model remains
 * responsible for the vending-machine body; this renderer paints only the CRT
 * contents and exposes real model-space control surfaces to the immersive UI. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp294PhysicalClient {
    public static final double FOCUS_DISTANCE = 1.30D;

    private static final double FOCUS_CENTER_X = 8.0D / 16.0D;
    private static final double FOCUS_CENTER_Y = 18.6D / 16.0D;
    private static final double FOCUS_CENTER_Z = 0.0D;
    private static final double FOCUS_WIDTH = 16.0D / 16.0D;
    private static final double FOCUS_HEIGHT = 23.0D / 16.0D;

    // The authored CRT housing occupies roughly x=5..14.1, y=23..30.6 and
    // begins at z=1.7. Keep the live phosphor plane just in front of that face.
    private static final double CRT_CENTER_X = 9.55D / 16.0D;
    private static final double CRT_CENTER_Y = 26.72D / 16.0D;
    private static final double CRT_CENTER_Z = 1.60D / 16.0D;
    private static final double CRT_WIDTH = 8.30D / 16.0D;
    private static final double CRT_HEIGHT = 6.35D / 16.0D;

    // Physical input regions follow the keyboard and coin-panel geometry rather
    // than the polygons from the removed fullscreen GUI.
    private static final double KEYBOARD_CENTER_X = 9.50D / 16.0D;
    private static final double KEYBOARD_CENTER_Y = 13.80D / 16.0D;
    private static final double KEYBOARD_CENTER_Z = 0.035D / 16.0D;
    private static final double KEYBOARD_WIDTH = 8.70D / 16.0D;
    private static final double KEYBOARD_HEIGHT = 10.60D / 16.0D;

    private static final double COIN_CENTER_X = 2.45D / 16.0D;
    private static final double COIN_CENTER_Y = 15.55D / 16.0D;
    private static final double COIN_CENTER_Z = 0.035D / 16.0D;
    private static final double COIN_WIDTH = 4.10D / 16.0D;
    private static final double COIN_HEIGHT = 10.10D / 16.0D;

    private static final ResourceLocation WHITE_PIXEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/screens/white_pixel.png");
    private static final int CRT_BG = 0xFFD5DFDC;
    private static final int CRT_SCANLINE = 0xFFC8D4D2;
    private static final int CRT_TEXT = 0xFF101B20;
    private static final int CRT_DIM = 0xFF52666A;
    private static final int CRT_ACCENT = 0xFF304B50;
    private static final int CRT_W = 256;
    private static final int CRT_H = 192;
    private static final double BASE_EPSILON = 0.0020D;
    private static final double DETAIL_EPSILON = 0.0023D;
    private static final double TEXT_EPSILON = 0.0027D;

    private Scp294PhysicalClient() { }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityType<Scp294BlockEntity> type = (BlockEntityType)
                ScpClassifiedDirectiveModBlockEntities.SCP_294.get();
        event.registerBlockEntityRenderer(type, Renderer::new);
    }

    public static Frame focusFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, FOCUS_CENTER_X, FOCUS_CENTER_Y,
                FOCUS_CENTER_Z, FOCUS_WIDTH, FOCUS_HEIGHT);
    }

    public static Frame crtFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, CRT_CENTER_X, CRT_CENTER_Y, CRT_CENTER_Z,
                CRT_WIDTH, CRT_HEIGHT);
    }

    private static Frame keyboardFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, KEYBOARD_CENTER_X, KEYBOARD_CENTER_Y,
                KEYBOARD_CENTER_Z, KEYBOARD_WIDTH, KEYBOARD_HEIGHT);
    }

    private static Frame coinFrame(BlockPos pos, Direction facing) {
        return frame(pos, facing, COIN_CENTER_X, COIN_CENTER_Y,
                COIN_CENTER_Z, COIN_WIDTH, COIN_HEIGHT);
    }

    private static Frame frame(BlockPos pos, Direction facing,
            double centerX, double centerY, double centerZ,
            double width, double height) {
        return PhysicalBlockScreenGeometry.fromNorthFacing(pos, facing,
                centerX, centerY, centerZ, 0.0D, width, height);
    }

    /** Resolve the actual piece of SCP-294 underneath the OS cursor. */
    public static Control controlAt(BlockPos pos, Direction facing,
            double mouseX, double mouseY, int viewportWidth, int viewportHeight) {
        if (pos == null || viewportWidth <= 0 || viewportHeight <= 0) {
            return Control.NONE;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 origin = camera.getPosition();
        Vec3 forward = Vec3.directionFromRotation(camera.getXRot(),
                camera.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-8D) return Control.NONE;
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();

        double nx = mouseX / viewportWidth * 2.0D - 1.0D;
        double ny = 1.0D - mouseY / viewportHeight * 2.0D;
        double tan = Math.tan(Math.toRadians(
                TeslaTerminalFocusClient.currentFovDegrees()) * 0.5D);
        double aspect = viewportWidth / (double) viewportHeight;
        Vec3 ray = forward.add(right.scale(nx * tan * aspect))
                .add(up.scale(ny * tan)).normalize();

        Control best = Control.NONE;
        double bestDistance = Double.POSITIVE_INFINITY;
        double hit = hitDistance(origin, ray, crtFrame(pos, facing));
        if (hit < bestDistance) {
            bestDistance = hit;
            best = Control.INPUT;
        }
        hit = hitDistance(origin, ray, keyboardFrame(pos, facing));
        if (hit < bestDistance) {
            bestDistance = hit;
            best = Control.ENTER;
        }
        hit = hitDistance(origin, ray, coinFrame(pos, facing));
        if (hit < bestDistance) {
            best = Control.COIN;
        }
        return best;
    }

    private static double hitDistance(Vec3 origin, Vec3 ray, Frame frame) {
        double denominator = ray.dot(frame.outward());
        if (Math.abs(denominator) < 1.0E-6D) {
            return Double.POSITIVE_INFINITY;
        }
        double distance = frame.center().subtract(origin)
                .dot(frame.outward()) / denominator;
        if (distance <= 0.0D) return Double.POSITIVE_INFINITY;
        Vec3 delta = origin.add(ray.scale(distance)).subtract(frame.center());
        double localX = delta.dot(frame.right());
        double localY = delta.dot(frame.up());
        if (Math.abs(localX) > frame.width() * 0.5D
                || Math.abs(localY) > frame.height() * 0.5D) {
            return Double.POSITIVE_INFINITY;
        }
        return distance;
    }

    public enum Control {
        NONE,
        INPUT,
        ENTER,
        COIN
    }

    private static final class Renderer
            implements BlockEntityRenderer<Scp294BlockEntity> {
        private final Font font;

        private Renderer(BlockEntityRendererProvider.Context context) {
            this.font = context.getFont();
        }

        @Override
        public void render(Scp294BlockEntity terminal, float partialTick,
                PoseStack poseStack, MultiBufferSource buffers,
                int packedLight, int packedOverlay) {
            BlockState state = terminal.getBlockState();
            if (!state.hasProperty(Scp294Block.FACING)) return;
            Direction facing = state.getValue(Scp294Block.FACING);
            BlockPos pos = terminal.getBlockPos();
            Frame frame = crtFrame(pos, facing);

            drawRect(poseStack, buffers, frame, pos, 0, 0, CRT_W, CRT_H,
                    CRT_BG, BASE_EPSILON);
            for (int y = 3; y < CRT_H; y += 6) {
                drawRect(poseStack, buffers, frame, pos, 0, y, CRT_W, 1,
                        CRT_SCANLINE, DETAIL_EPSILON);
            }
            flush(buffers);

            Scp294GuiScreen screen = activeScreen(pos);
            boolean hasCoin = screen != null && screen.physicalHasCoinInserted();
            String order = screen == null ? "" : screen.physicalOrder();
            boolean typing = screen != null && screen.physicalInputFocused()
                    && hasCoin;
            renderText(poseStack, buffers, frame, pos, facing,
                    hasCoin, order, typing);
        }

        private void renderText(PoseStack poseStack, MultiBufferSource buffers,
                Frame frame, BlockPos pos, Direction facing, boolean hasCoin,
                String order, boolean typing) {
            Vec3 topLeft = local(frame.point(-0.5D, 0.5D, TEXT_EPSILON), pos);
            float pixelScaleX = (float) (frame.width() / CRT_W);
            float pixelScaleY = (float) (frame.height() / CRT_H);
            float depthScale = Math.min(pixelScaleX, pixelScaleY);

            poseStack.pushPose();
            poseStack.translate(topLeft.x, topLeft.y, topLeft.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(textYaw(facing)));
            poseStack.scale(pixelScaleX, -pixelScaleY, depthScale);

            draw(poseStack, buffers, Component.literal("SCP-294"),
                    12, 12, CRT_ACCENT);
            draw(poseStack, buffers, Component.literal(
                    hasCoin ? "LIQUID REQUEST" : "PAYMENT REQUIRED"),
                    12, 30, CRT_DIM);

            String main;
            if (!hasCoin) {
                main = "INSERT COIN";
            } else if (order.isBlank()) {
                main = "ENTER LIQUID";
            } else {
                main = trimToWidth(order, 220);
            }
            if (typing && (System.currentTimeMillis() / 400L) % 2L == 0L) {
                main += "_";
            }

            poseStack.pushPose();
            poseStack.translate(14.0F, 82.0F, 0.0F);
            poseStack.scale(1.45F, 1.45F, 1.0F);
            draw(poseStack, buffers, Component.literal(main), 0, 0, CRT_TEXT);
            poseStack.popPose();

            String hint = !hasCoin ? "ONE SCP-294 COIN"
                    : order.isBlank() ? "TYPE REQUEST"
                    : "PRESS KEYBOARD TO DISPENSE";
            draw(poseStack, buffers, Component.literal(hint),
                    14, 137, CRT_DIM);
            poseStack.popPose();
        }

        private String trimToWidth(String text, int width) {
            String value = text == null ? "" : text;
            while (!value.isEmpty() && font.width(value) * 1.45F > width) {
                value = value.substring(1);
            }
            return value;
        }

        private void draw(PoseStack poseStack, MultiBufferSource buffers,
                Component text, float x, float y, int color) {
            font.drawInBatch(text, x, y, color, false,
                    poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL,
                    0, LightTexture.FULL_BRIGHT);
        }

        @Override
        public boolean shouldRenderOffScreen(Scp294BlockEntity blockEntity) {
            return true;
        }
    }

    private static Scp294GuiScreen activeScreen(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Scp294GuiScreen screen
                && screen.isFor(pos)) {
            return screen;
        }
        return null;
    }

    private static void drawRect(PoseStack poseStack,
            MultiBufferSource buffers, Frame frame, BlockPos pos,
            int x, int y, int width, int height, int color,
            double normalOffset) {
        double left = x / (double) CRT_W - 0.5D;
        double right = (x + width) / (double) CRT_W - 0.5D;
        double top = 0.5D - y / (double) CRT_H;
        double bottom = 0.5D - (y + height) / (double) CRT_H;
        RenderType type = RenderType.entityCutoutNoCull(WHITE_PIXEL);
        VertexConsumer consumer = buffers.getBuffer(type);
        Vec3 topLeft = local(frame.point(left, top, normalOffset), pos);
        Vec3 topRight = local(frame.point(right, top, normalOffset), pos);
        Vec3 bottomRight = local(frame.point(right, bottom, normalOffset), pos);
        Vec3 bottomLeft = local(frame.point(left, bottom, normalOffset), pos);
        Vec3 normal = frame.outward();
        vertex(consumer, poseStack, topLeft, 0, 0, normal, color);
        vertex(consumer, poseStack, topRight, 1, 0, normal, color);
        vertex(consumer, poseStack, bottomRight, 1, 1, normal, color);
        vertex(consumer, poseStack, bottomLeft, 0, 1, normal, color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack,
            Vec3 point, float u, float v, Vec3 normal, int color) {
        consumer.vertex(poseStack.last().pose(), (float) point.x,
                        (float) point.y, (float) point.z)
                .color(red(color), green(color), blue(color), 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(poseStack.last().normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static void flush(MultiBufferSource buffers) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(RenderType.entityCutoutNoCull(WHITE_PIXEL));
        }
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

    private static Vec3 local(Vec3 world, BlockPos pos) {
        return world.subtract(pos.getX(), pos.getY(), pos.getZ());
    }

    private static int red(int color) { return color >> 16 & 0xFF; }
    private static int green(int color) { return color >> 8 & 0xFF; }
    private static int blue(int color) { return color & 0xFF; }
}
