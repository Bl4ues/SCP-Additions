package com.bl4ues.scpclassifieddirective.client.photomode;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shader-independent alpha matte generator for Photo Mode.
 *
 * <p>The selected object's normal renderer is allowed to emit its geometry, but
 * every requested RenderType is replaced by a CPU VertexConsumer. No draw call
 * or framebuffer is involved. This avoids Oculus/shader packs redirecting the
 * isolation pass into their own g-buffers while still reusing the exact block,
 * entity and block-entity model transforms used by Minecraft.</p>
 */
final class GeometryMatteRasterizer {
    private GeometryMatteRasterizer() {
    }

    static NativeImage render(PhotoModeCapture.PhotoTarget target,
                              Matrix4f viewPose,
                              Matrix3f viewNormal,
                              Matrix4f projection,
                              Vec3 cameraPosition,
                              float partialTick,
                              int width,
                              int height) throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IOException("invalid framebuffer size for geometry matte");
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            throw new IOException("the client world is no longer available");
        }

        CoverageRasterizer rasterizer = new CoverageRasterizer(projection, width, height);
        MultiBufferSource buffers = renderType ->
                new PrimitiveConsumer(renderType.mode(), rasterizer);

        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(viewPose);
        poseStack.last().normal().set(viewNormal);

        if (target instanceof PhotoModeCapture.EntityTarget entityTarget) {
            renderEntity(entityTarget.entity(), poseStack, buffers,
                    cameraPosition, partialTick, rasterizer);
        } else if (target instanceof PhotoModeCapture.BlockTarget blockTarget) {
            renderBlock(blockTarget.pos(), poseStack, buffers,
                    cameraPosition, partialTick, rasterizer);
        }

        if (rasterizer.emittedVertices == 0) {
            throw new IOException("the selected object's renderer emitted no geometry");
        }
        if (!rasterizer.hasCoverage()) {
            throw new IOException("the selected object's geometry produced no on-screen coverage");
        }
        return rasterizer.toImage();
    }

    private static void renderEntity(Entity entity,
                                     PoseStack poseStack,
                                     MultiBufferSource buffers,
                                     Vec3 cameraPosition,
                                     float partialTick,
                                     CoverageRasterizer rasterizer) throws IOException {
        if (entity == null || entity.isRemoved()) {
            throw new IOException("the selected entity is no longer available");
        }

        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        try {
            double x = Mth.lerp(partialTick, entity.xOld, entity.getX())
                    - cameraPosition.x;
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY())
                    - cameraPosition.y;
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ())
                    - cameraPosition.z;
            float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            int packedLight = dispatcher.getPackedLightCoords(entity, partialTick);
            dispatcher.render(entity, x, y, z, yaw, partialTick,
                    poseStack, buffers, packedLight);
        } catch (RuntimeException ex) {
            throw new IOException("entity geometry capture failed: " + ex.getMessage(), ex);
        } finally {
            dispatcher.setRenderShadow(true);
        }
    }

    private static void renderBlock(BlockPos pos,
                                    PoseStack poseStack,
                                    MultiBufferSource buffers,
                                    Vec3 cameraPosition,
                                    float partialTick,
                                    CoverageRasterizer rasterizer) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.isAir()) {
            throw new IOException("the selected block is no longer available");
        }

        try {
            poseStack.pushPose();
            poseStack.translate(pos.getX() - cameraPosition.x,
                    pos.getY() - cameraPosition.y,
                    pos.getZ() - cameraPosition.z);

            BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
            int packedLight = LevelRenderer.getLightColor(minecraft.level, state, pos);
            blockRenderer.renderSingleBlock(state, poseStack, buffers,
                    packedLight, OverlayTexture.NO_OVERLAY);

            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (blockEntity != null) {
                minecraft.getBlockEntityRenderDispatcher().render(
                        blockEntity, partialTick, poseStack, buffers);
            }
            poseStack.popPose();
        } catch (RuntimeException ex) {
            throw new IOException("block geometry capture failed: " + ex.getMessage(), ex);
        }
    }

    /** Receives already-transformed model/view vertices from Minecraft renderers. */
    private static final class PrimitiveConsumer implements VertexConsumer {
        private final VertexFormat.Mode mode;
        private final CoverageRasterizer rasterizer;
        private final List<ProjectedVertex> vertices = new ArrayList<>(4);
        private ProjectedVertex current;

        PrimitiveConsumer(VertexFormat.Mode mode, CoverageRasterizer rasterizer) {
            this.mode = mode;
            this.rasterizer = rasterizer;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            current = rasterizer.project(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
            rasterizer.emittedVertices++;
            if (current == null) {
                return;
            }
            vertices.add(current);
            emitCompletedPrimitives();
            current = null;
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }

        private void emitCompletedPrimitives() {
            switch (mode) {
                case QUADS -> {
                    if (vertices.size() == 4) {
                        rasterizer.triangle(vertices.get(0), vertices.get(1), vertices.get(2));
                        rasterizer.triangle(vertices.get(0), vertices.get(2), vertices.get(3));
                        vertices.clear();
                    }
                }
                case TRIANGLES -> {
                    if (vertices.size() == 3) {
                        rasterizer.triangle(vertices.get(0), vertices.get(1), vertices.get(2));
                        vertices.clear();
                    }
                }
                case TRIANGLE_STRIP -> {
                    if (vertices.size() >= 3) {
                        int size = vertices.size();
                        ProjectedVertex a = vertices.get(size - 3);
                        ProjectedVertex b = vertices.get(size - 2);
                        ProjectedVertex c = vertices.get(size - 1);
                        rasterizer.triangle(a, b, c);
                        if (vertices.size() > 3) {
                            vertices.remove(0);
                        }
                    }
                }
                case TRIANGLE_FAN -> {
                    if (vertices.size() >= 3) {
                        int size = vertices.size();
                        rasterizer.triangle(vertices.get(0),
                                vertices.get(size - 2), vertices.get(size - 1));
                        if (vertices.size() > 3) {
                            ProjectedVertex first = vertices.get(0);
                            ProjectedVertex last = vertices.get(vertices.size() - 1);
                            vertices.clear();
                            vertices.add(first);
                            vertices.add(last);
                        }
                    }
                }
                default -> {
                    // Lines/debug primitives are not part of the object's silhouette.
                    if (vertices.size() > 8) {
                        vertices.clear();
                    }
                }
            }
        }
    }

    private static final class CoverageRasterizer {
        private static final float[][] SAMPLE_OFFSETS = {
                {0.25F, 0.25F}, {0.75F, 0.25F},
                {0.25F, 0.75F}, {0.75F, 0.75F}
        };

        private final Matrix4f projection;
        private final int width;
        private final int height;
        private final byte[] sampleCoverage;
        private int emittedVertices;

        CoverageRasterizer(Matrix4f projection, int width, int height) {
            this.projection = new Matrix4f(projection);
            this.width = width;
            this.height = height;
            this.sampleCoverage = new byte[width * height];
        }

        ProjectedVertex project(double x, double y, double z) {
            Vector4f clip = projection.transform(new Vector4f(
                    (float) x, (float) y, (float) z, 1.0F));
            float w = clip.w();
            if (!Float.isFinite(w) || w <= 1.0E-5F) {
                return ProjectedVertex.INVALID;
            }

            float ndcX = clip.x() / w;
            float ndcY = clip.y() / w;
            if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY)) {
                return ProjectedVertex.INVALID;
            }

            float screenX = (ndcX * 0.5F + 0.5F) * width;
            float screenY = (0.5F - ndcY * 0.5F) * height;
            return new ProjectedVertex(screenX, screenY, true);
        }

        void triangle(ProjectedVertex a, ProjectedVertex b, ProjectedVertex c) {
            if (!a.valid || !b.valid || !c.valid) {
                return;
            }

            float area = edge(a.x, a.y, b.x, b.y, c.x, c.y);
            if (!Float.isFinite(area) || Math.abs(area) < 1.0E-5F) {
                return;
            }

            float minXf = Math.min(a.x, Math.min(b.x, c.x));
            float maxXf = Math.max(a.x, Math.max(b.x, c.x));
            float minYf = Math.min(a.y, Math.min(b.y, c.y));
            float maxYf = Math.max(a.y, Math.max(b.y, c.y));
            if (maxXf < 0.0F || maxYf < 0.0F
                    || minXf >= width || minYf >= height) {
                return;
            }

            int minX = Math.max(0, (int) Math.floor(minXf));
            int maxX = Math.min(width - 1, (int) Math.ceil(maxXf));
            int minY = Math.max(0, (int) Math.floor(minYf));
            int maxY = Math.min(height - 1, (int) Math.ceil(maxYf));
            boolean positive = area > 0.0F;

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    int bits = sampleCoverage[y * width + x] & 0x0F;
                    if (bits == 0x0F) {
                        continue;
                    }
                    for (int sample = 0; sample < SAMPLE_OFFSETS.length; sample++) {
                        int bit = 1 << sample;
                        if ((bits & bit) != 0) {
                            continue;
                        }
                        float px = x + SAMPLE_OFFSETS[sample][0];
                        float py = y + SAMPLE_OFFSETS[sample][1];
                        float e0 = edge(a.x, a.y, b.x, b.y, px, py);
                        float e1 = edge(b.x, b.y, c.x, c.y, px, py);
                        float e2 = edge(c.x, c.y, a.x, a.y, px, py);
                        boolean inside = positive
                                ? e0 >= -1.0E-4F && e1 >= -1.0E-4F && e2 >= -1.0E-4F
                                : e0 <= 1.0E-4F && e1 <= 1.0E-4F && e2 <= 1.0E-4F;
                        if (inside) {
                            bits |= bit;
                        }
                    }
                    sampleCoverage[y * width + x] = (byte) bits;
                }
            }
        }

        boolean hasCoverage() {
            for (byte value : sampleCoverage) {
                if ((value & 0x0F) != 0) {
                    return true;
                }
            }
            return false;
        }

        NativeImage toImage() {
            NativeImage image = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int samples = Integer.bitCount(sampleCoverage[y * width + x] & 0x0F);
                    int alpha = switch (samples) {
                        case 1 -> 64;
                        case 2 -> 128;
                        case 3 -> 191;
                        case 4 -> 255;
                        default -> 0;
                    };
                    image.setPixelRGBA(x, y, (alpha << 24) | 0x00FFFFFF);
                }
            }
            return image;
        }

        private static float edge(float ax, float ay, float bx, float by,
                                  float px, float py) {
            return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
        }
    }

    private record ProjectedVertex(float x, float y, boolean valid) {
        private static final ProjectedVertex INVALID =
                new ProjectedVertex(0.0F, 0.0F, false);
    }
}
