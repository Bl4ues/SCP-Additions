package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

/**
 * Owns the two genuinely different passes used by the physical inventory PDA.
 * The existing inventory is first drawn into an off-screen color target. The
 * authored GeckoLib model and that target are then rendered in camera space
 * under a perspective projection, so depth, normals and shader-provided PBR
 * remain real instead of being imitated with a transformed 2D GUI.
 */
public final class InventoryPdaPresentationRenderer implements AutoCloseable {
    private static final float FOV_DEGREES = 36.0F;
    private static final float NEAR_PLANE = 0.05F;
    private static final float FAR_PLANE = 32.0F;

    private static final float SCREEN_MIN_X = -10.0F / 16.0F;
    private static final float SCREEN_MAX_X = 10.0F / 16.0F;
    private static final float SCREEN_MIN_Y = 30.0F / 16.0F;
    private static final float SCREEN_MAX_Y = 56.0F / 16.0F;
    private static final float SCREEN_Z = -8.055F / 16.0F;
    private static final float GEO_CENTER_Y = 43.0F / 16.0F;

    private final Minecraft minecraft = Minecraft.getInstance();
    private TextureTarget interfaceTarget;
    private int framebufferWidth = -1;
    private int framebufferHeight = -1;

    /** Render the normal GUI into a texture without touching the world color. */
    public void captureInterface(GuiGraphics graphics, Runnable renderContents) {
        ensureInterfaceTarget();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        graphics.flush();
        RenderSystem.disableScissor();
        interfaceTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        // RenderTarget.clear binds and then unbinds its framebuffer. Binding
        // before clear therefore sent the inventory pass back to the default
        // target, leaving the PDA texture transparent.
        interfaceTarget.clear(Minecraft.ON_OSX);
        interfaceTarget.bindWrite(true);

        try {
            renderContents.run();
            graphics.flush();
        } finally {
            mainTarget.bindWrite(true);
            RenderSystem.disableScissor();
        }
    }

    /** Draw the body and its live emissive display through a perspective lens. */
    public void render(Pose pose, int packedLight, int guiWidth, int guiHeight,
            int rootX, int rootY, int rootWidth, int rootHeight) {
        if (interfaceTarget == null || guiWidth <= 0 || guiHeight <= 0) return;

        Matrix4f projection = projection(guiWidth, guiHeight);
        PoseStack modelPose = modelPose(pose);
        PoseStack screenPose = screenPose(pose);
        PoseStack modelView = RenderSystem.getModelViewStack();

        RenderSystem.backupProjectionMatrix();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection,
                VertexSorting.DISTANCE_TO_ORIGIN);

        try {
            // The world has already been fully drawn. Its depth values belong
            // to a different projection, so reset depth while preserving color.
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            InventoryPdaRenderer.INSTANCE.render(modelPose, packedLight);
            renderDisplay(screenPose.last().pose(), guiWidth, guiHeight,
                    rootX, rootY, rootWidth, rootHeight);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.restoreProjectionMatrix();
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    /**
     * Convert a real cursor coordinate to the original inventory coordinate
     * system by ray-casting against the authored screen plane.
     */
    public MappedMouse mapMouse(Pose pose, double mouseX, double mouseY,
            int guiWidth, int guiHeight, int rootX, int rootY,
            int rootWidth, int rootHeight) {
        if (guiWidth <= 0 || guiHeight <= 0 || rootWidth <= 0
                || rootHeight <= 0) {
            return new MappedMouse(mouseX, mouseY, false);
        }

        Matrix4f inverse = new Matrix4f(projection(guiWidth, guiHeight))
                .mul(screenPose(pose).last().pose())
                .invert();
        float ndcX = (float) (mouseX / guiWidth * 2.0D - 1.0D);
        float ndcY = (float) (1.0D - mouseY / guiHeight * 2.0D);
        Vector4f near = inverse.transform(new Vector4f(
                ndcX, ndcY, -1.0F, 1.0F));
        Vector4f far = inverse.transform(new Vector4f(
                ndcX, ndcY, 1.0F, 1.0F));
        if (Math.abs(near.w()) < 1.0E-6F || Math.abs(far.w()) < 1.0E-6F) {
            return new MappedMouse(mouseX, mouseY, false);
        }
        near.div(near.w());
        far.div(far.w());

        float dz = far.z() - near.z();
        if (Math.abs(dz) < 1.0E-6F) {
            return new MappedMouse(mouseX, mouseY, false);
        }
        float distance = (SCREEN_Z - near.z()) / dz;
        float localX = near.x() + (far.x() - near.x()) * distance;
        float localY = near.y() + (far.y() - near.y()) * distance;

        // The device settles at +90 degrees: authored Y runs right-to-left
        // and authored X runs bottom-to-top on the visible display.
        double u = (SCREEN_MAX_Y - localY)
                / (SCREEN_MAX_Y - SCREEN_MIN_Y);
        double v = (SCREEN_MAX_X - localX)
                / (SCREEN_MAX_X - SCREEN_MIN_X);
        double mappedX = rootX + u * rootWidth;
        double mappedY = rootY + v * rootHeight;
        boolean over = distance >= 0.0F && distance <= 1.0F
                && u >= 0.0D && u <= 1.0D
                && v >= 0.0D && v <= 1.0D;
        return new MappedMouse(mappedX, mappedY, over);
    }

    private void renderDisplay(Matrix4f matrix, int guiWidth, int guiHeight,
            int rootX, int rootY, int rootWidth, int rootHeight) {
        float u0 = rootX / (float) guiWidth;
        float u1 = (rootX + rootWidth) / (float) guiWidth;
        // Framebuffer color textures use an OpenGL bottom-left origin.
        float vTop = 1.0F - rootY / (float) guiHeight;
        float vBottom = 1.0F - (rootY + rootHeight) / (float) guiHeight;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0,
                interfaceTarget.getColorTextureId());
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix, SCREEN_MAX_X, SCREEN_MAX_Y, SCREEN_Z)
                .uv(u0, vTop).endVertex();
        builder.vertex(matrix, SCREEN_MAX_X, SCREEN_MIN_Y, SCREEN_Z)
                .uv(u1, vTop).endVertex();
        builder.vertex(matrix, SCREEN_MIN_X, SCREEN_MIN_Y, SCREEN_Z)
                .uv(u1, vBottom).endVertex();
        builder.vertex(matrix, SCREEN_MIN_X, SCREEN_MAX_Y, SCREEN_Z)
                .uv(u0, vBottom).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    private PoseStack modelPose(Pose pose) {
        PoseStack stack = new PoseStack();
        stack.translate(pose.x(), pose.y(), pose.depth());
        stack.mulPose(Axis.YP.rotationDegrees(pose.yaw()));
        stack.mulPose(Axis.XP.rotationDegrees(pose.pitch()));
        stack.mulPose(Axis.ZP.rotationDegrees(pose.roll()));
        stack.scale(pose.scale(), -pose.scale(), pose.scale());
        stack.translate(-0.5F, -(0.51F + GEO_CENTER_Y), 0.0F);
        return stack;
    }

    private PoseStack screenPose(Pose pose) {
        PoseStack stack = modelPose(pose);
        // This is the pre-translation performed by GeoObjectRenderer.
        stack.translate(0.5F, 0.51F, 0.5F);
        return stack;
    }

    private static Matrix4f projection(int width, int height) {
        float aspect = width / (float) Math.max(1, height);
        return new Matrix4f().setPerspective(
                (float) Math.toRadians(FOV_DEGREES), aspect,
                NEAR_PLANE, FAR_PLANE);
    }

    private void ensureInterfaceTarget() {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (interfaceTarget == null) {
            interfaceTarget = new TextureTarget(width, height, true,
                    Minecraft.ON_OSX);
            interfaceTarget.setFilterMode(GL11.GL_LINEAR);
            framebufferWidth = width;
            framebufferHeight = height;
        } else if (width != framebufferWidth || height != framebufferHeight) {
            interfaceTarget.resize(width, height, Minecraft.ON_OSX);
            framebufferWidth = width;
            framebufferHeight = height;
        }
    }

    @Override
    public void close() {
        if (interfaceTarget != null) {
            interfaceTarget.destroyBuffers();
            interfaceTarget = null;
        }
        framebufferWidth = -1;
        framebufferHeight = -1;
    }

    public record Pose(float x, float y, float depth, float pitch,
            float yaw, float roll, float scale) {
    }

    public record MappedMouse(double x, double y, boolean overSurface) {
    }
}
