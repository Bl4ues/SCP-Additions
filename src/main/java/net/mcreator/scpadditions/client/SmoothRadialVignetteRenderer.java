package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Shared low-resolution gradient renderer for smooth code-generated vignettes. */
public final class SmoothRadialVignetteRenderer {
    private static final int GRID_COLUMNS = 48;
    private static final int MIN_GRID_ROWS = 20;
    private static final int MAX_GRID_ROWS = 36;

    private SmoothRadialVignetteRenderer() {
    }

    public static void render(GuiGraphics graphics, int width, int height,
                              ColorSampler sampler) {
        if (width <= 0 || height <= 0 || sampler == null) return;

        int rows = Mth.clamp(Math.round(GRID_COLUMNS
                * height / (float) width), MIN_GRID_ROWS, MAX_GRID_ROWS);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);

        for (int row = 0; row < rows; row++) {
            float y0 = height * row / (float) rows;
            float y1 = height * (row + 1) / (float) rows;
            for (int column = 0; column < GRID_COLUMNS; column++) {
                float x0 = width * column / (float) GRID_COLUMNS;
                float x1 = width * (column + 1) / (float) GRID_COLUMNS;

                VertexColor topLeft = sampler.colorAt(x0, y0);
                VertexColor bottomLeft = sampler.colorAt(x0, y1);
                VertexColor bottomRight = sampler.colorAt(x1, y1);
                VertexColor topRight = sampler.colorAt(x1, y0);

                vertex(buffer, matrix, x0, y0, topLeft);
                vertex(buffer, matrix, x0, y1, bottomLeft);
                vertex(buffer, matrix, x1, y1, bottomRight);
                vertex(buffer, matrix, x1, y0, topRight);
            }
        }

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static float smoothStep(float edge0, float edge1, float value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0F : 0.0F;
        }
        float normalized = Mth.clamp((value - edge0) / (edge1 - edge0),
                0.0F, 1.0F);
        return normalized * normalized * (3.0F - 2.0F * normalized);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix,
                               float x, float y, VertexColor color) {
        buffer.vertex(matrix, x, y, 0.0F)
                .color(color.red(), color.green(), color.blue(), color.alpha())
                .endVertex();
    }

    @FunctionalInterface
    public interface ColorSampler {
        VertexColor colorAt(float x, float y);
    }

    public record VertexColor(int red, int green, int blue, int alpha) {
    }
}
