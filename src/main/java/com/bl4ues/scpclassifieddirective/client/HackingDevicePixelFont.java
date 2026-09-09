package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * Tiny 5x7 raster font used by the Hacking Device CRT.
 *
 * The ordinary Minecraft Font pipeline repeatedly disappeared when the device
 * was rendered from RenderLevelStageEvent, especially under Oculus/Hysteria.
 * This renderer instead submits the characters as the same opaque/cutout world
 * quads already proven by the Diagnostic Terminal. The UI therefore remains a
 * real surface on the authored `screen` plane rather than a HUD or fake GUI.
 */
public final class HackingDevicePixelFont {
    private static final ResourceLocation WHITE_PIXEL = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/screens/white_pixel.png");
    private static final RenderType RENDER_TYPE =
            RenderType.entityCutoutNoCull(WHITE_PIXEL);
    private static final double WORLD_EPSILON = 0.0035D;
    private static final int ADVANCE = 6;
    private static final int GLYPH_HEIGHT = 7;

    private static final ThreadLocal<Canvas> ACTIVE = new ThreadLocal<>();

    private HackingDevicePixelFont() {
    }

    public static void beginWorld(PoseStack poseStack, MultiBufferSource buffers,
            Frame frame, Vec3 camera) {
        if (poseStack == null || buffers == null || frame == null
                || camera == null) {
            ACTIVE.remove();
            return;
        }
        ACTIVE.set(new WorldCanvas(poseStack, buffers, frame, camera));
    }

    public static void beginMatrix(MultiBufferSource buffers,
            Matrix4f logicalTransform) {
        if (buffers == null || logicalTransform == null) {
            ACTIVE.remove();
            return;
        }
        ACTIVE.set(new MatrixCanvas(buffers,
                new Matrix4f(logicalTransform)));
    }

    public static void end() {
        ACTIVE.remove();
    }

    public static boolean active() {
        return ACTIVE.get() != null;
    }

    public static float width(String text) {
        if (text == null || text.isEmpty()) return 0.0F;
        return text.length() * ADVANCE - 1.0F;
    }

    public static void centered(String text, float y, int color) {
        float x = (HackingDeviceScreenTextClient.LOGICAL_WIDTH - width(text))
                * 0.5F;
        draw(text, x, y, color);
    }

    public static void draw(String text, float x, float y, int color) {
        Canvas canvas = ACTIVE.get();
        if (canvas == null || text == null || text.isEmpty()) return;

        String normalized = text.toUpperCase(Locale.ROOT);
        float cursor = x;
        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);
            int[] rows = glyph(character);
            if (rows != null) {
                for (int row = 0; row < GLYPH_HEIGHT; row++) {
                    int bits = rows[row];
                    int column = 0;
                    while (column < 5) {
                        while (column < 5 && (bits & 1 << (4 - column)) == 0) {
                            column++;
                        }
                        if (column >= 5) break;
                        int start = column;
                        while (column < 5
                                && (bits & 1 << (4 - column)) != 0) {
                            column++;
                        }
                        canvas.rect(cursor + start, y + row,
                                column - start, 1.0F, color);
                    }
                }
            }
            cursor += ADVANCE;
        }
    }

    public static void flush(MultiBufferSource buffers) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(RENDER_TYPE);
        }
    }

    private interface Canvas {
        void rect(float x, float y, float width, float height, int color);
    }

    private static final class WorldCanvas implements Canvas {
        private final PoseStack poseStack;
        private final MultiBufferSource buffers;
        private final Frame frame;
        private final Vec3 camera;

        private WorldCanvas(PoseStack poseStack, MultiBufferSource buffers,
                Frame frame, Vec3 camera) {
            this.poseStack = poseStack;
            this.buffers = buffers;
            this.frame = frame;
            this.camera = camera;
        }

        @Override
        public void rect(float x, float y, float width, float height,
                int color) {
            double left = x / HackingDeviceScreenTextClient.LOGICAL_WIDTH - 0.5D;
            double right = (x + width)
                    / HackingDeviceScreenTextClient.LOGICAL_WIDTH - 0.5D;
            double top = 0.5D
                    - y / HackingDeviceScreenTextClient.LOGICAL_HEIGHT;
            double bottom = 0.5D
                    - (y + height) / HackingDeviceScreenTextClient.LOGICAL_HEIGHT;

            Vec3 topLeft = frame.point(left, top, WORLD_EPSILON)
                    .subtract(camera);
            Vec3 topRight = frame.point(right, top, WORLD_EPSILON)
                    .subtract(camera);
            Vec3 bottomRight = frame.point(right, bottom, WORLD_EPSILON)
                    .subtract(camera);
            Vec3 bottomLeft = frame.point(left, bottom, WORLD_EPSILON)
                    .subtract(camera);
            Vec3 normal = frame.outward();
            VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);

            vertex(consumer, poseStack, topLeft, 0.0F, 0.0F, normal, color);
            vertex(consumer, poseStack, topRight, 1.0F, 0.0F, normal, color);
            vertex(consumer, poseStack, bottomRight, 1.0F, 1.0F, normal, color);
            vertex(consumer, poseStack, bottomLeft, 0.0F, 1.0F, normal, color);
        }
    }

    private static final class MatrixCanvas implements Canvas {
        private final MultiBufferSource buffers;
        private final Matrix4f transform;

        private MatrixCanvas(MultiBufferSource buffers, Matrix4f transform) {
            this.buffers = buffers;
            this.transform = transform;
        }

        @Override
        public void rect(float x, float y, float width, float height,
                int color) {
            VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);
            matrixVertex(consumer, transform, x, y, 0.0F, 0.0F, color);
            matrixVertex(consumer, transform, x + width, y,
                    1.0F, 0.0F, color);
            matrixVertex(consumer, transform, x + width, y + height,
                    1.0F, 1.0F, color);
            matrixVertex(consumer, transform, x, y + height,
                    0.0F, 1.0F, color);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack,
            Vec3 point, float u, float v, Vec3 normal, int color) {
        consumer.vertex(poseStack.last().pose(), (float) point.x,
                        (float) point.y, (float) point.z)
                .color(red(color), green(color), blue(color), alpha(color))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(poseStack.last().normal(), (float) normal.x,
                        (float) normal.y, (float) normal.z)
                .endVertex();
    }

    private static void matrixVertex(VertexConsumer consumer, Matrix4f matrix,
            float x, float y, float u, float v, int color) {
        consumer.vertex(matrix, x, y, 0.0F)
                .color(red(color), green(color), blue(color), alpha(color))
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    private static int red(int color) {
        return color >> 16 & 0xFF;
    }

    private static int green(int color) {
        return color >> 8 & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }

    private static int alpha(int color) {
        int value = color >>> 24 & 0xFF;
        return value == 0 ? 255 : value;
    }

    private static int[] glyph(char character) {
        return switch (character) {
            case 'A' -> g(14, 17, 17, 31, 17, 17, 17);
            case 'B' -> g(30, 17, 17, 30, 17, 17, 30);
            case 'C' -> g(14, 17, 16, 16, 16, 17, 14);
            case 'D' -> g(30, 17, 17, 17, 17, 17, 30);
            case 'E' -> g(31, 16, 16, 30, 16, 16, 31);
            case 'F' -> g(31, 16, 16, 30, 16, 16, 16);
            case 'G' -> g(14, 17, 16, 23, 17, 17, 15);
            case 'H' -> g(17, 17, 17, 31, 17, 17, 17);
            case 'I' -> g(31, 4, 4, 4, 4, 4, 31);
            case 'J' -> g(7, 2, 2, 2, 18, 18, 12);
            case 'K' -> g(17, 18, 20, 24, 20, 18, 17);
            case 'L' -> g(16, 16, 16, 16, 16, 16, 31);
            case 'M' -> g(17, 27, 21, 21, 17, 17, 17);
            case 'N' -> g(17, 25, 21, 19, 17, 17, 17);
            case 'O' -> g(14, 17, 17, 17, 17, 17, 14);
            case 'P' -> g(30, 17, 17, 30, 16, 16, 16);
            case 'Q' -> g(14, 17, 17, 17, 21, 18, 13);
            case 'R' -> g(30, 17, 17, 30, 20, 18, 17);
            case 'S' -> g(15, 16, 16, 14, 1, 1, 30);
            case 'T' -> g(31, 4, 4, 4, 4, 4, 4);
            case 'U' -> g(17, 17, 17, 17, 17, 17, 14);
            case 'V' -> g(17, 17, 17, 17, 17, 10, 4);
            case 'W' -> g(17, 17, 17, 21, 21, 21, 10);
            case 'X' -> g(17, 17, 10, 4, 10, 17, 17);
            case 'Y' -> g(17, 17, 10, 4, 4, 4, 4);
            case 'Z' -> g(31, 1, 2, 4, 8, 16, 31);
            case '0' -> g(14, 17, 19, 21, 25, 17, 14);
            case '1' -> g(4, 12, 4, 4, 4, 4, 14);
            case '2' -> g(14, 17, 1, 2, 4, 8, 31);
            case '3' -> g(30, 1, 1, 14, 1, 1, 30);
            case '4' -> g(2, 6, 10, 18, 31, 2, 2);
            case '5' -> g(31, 16, 16, 30, 1, 1, 30);
            case '6' -> g(14, 16, 16, 30, 17, 17, 14);
            case '7' -> g(31, 1, 2, 4, 8, 8, 8);
            case '8' -> g(14, 17, 17, 14, 17, 17, 14);
            case '9' -> g(14, 17, 17, 15, 1, 1, 14);
            case ':' -> g(0, 4, 4, 0, 4, 4, 0);
            case '.' -> g(0, 0, 0, 0, 0, 6, 6);
            case ',' -> g(0, 0, 0, 0, 6, 6, 4);
            case '-' -> g(0, 0, 0, 31, 0, 0, 0);
            case '_' -> g(0, 0, 0, 0, 0, 0, 31);
            case '/' -> g(1, 2, 2, 4, 8, 8, 16);
            case '\\' -> g(16, 8, 8, 4, 2, 2, 1);
            case '[' -> g(14, 8, 8, 8, 8, 8, 14);
            case ']' -> g(14, 2, 2, 2, 2, 2, 14);
            case '(' -> g(2, 4, 8, 8, 8, 4, 2);
            case ')' -> g(8, 4, 2, 2, 2, 4, 8);
            case '<' -> g(1, 2, 4, 8, 4, 2, 1);
            case '>' -> g(16, 8, 4, 2, 4, 8, 16);
            case '=' -> g(0, 31, 0, 31, 0, 0, 0);
            case '^' -> g(4, 10, 17, 0, 0, 0, 0);
            case '%' -> g(17, 2, 4, 8, 16, 17, 0);
            case '#' -> g(10, 31, 10, 10, 31, 10, 0);
            case '!' -> g(4, 4, 4, 4, 4, 0, 4);
            case '?' -> g(14, 17, 1, 2, 4, 0, 4);
            case '|' -> g(4, 4, 4, 4, 4, 4, 4);
            case '+' -> g(0, 4, 4, 31, 4, 4, 0);
            case '*' -> g(0, 21, 14, 31, 14, 21, 0);
            case '\'' -> g(4, 4, 2, 0, 0, 0, 0);
            case '"' -> g(10, 10, 5, 0, 0, 0, 0);
            case ' ' -> null;
            default -> g(14, 17, 1, 2, 4, 0, 4);
        };
    }

    private static int[] g(int a, int b, int c, int d, int e, int f, int g) {
        return new int[]{a, b, c, d, e, f, g};
    }
}
