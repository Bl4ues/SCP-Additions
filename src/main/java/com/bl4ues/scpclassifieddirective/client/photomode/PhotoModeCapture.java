package com.bl4ues.scpclassifieddirective.client.photomode;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures the exact shader-composited world frame and combines it with a
 * shader-independent geometry matte for the selected block/entity.
 */
public final class PhotoModeCapture {
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS");

    private static FrameSnapshot lastWorldFrame;
    private static Session session;

    private PhotoModeCapture() {
    }

    public static boolean isActive() {
        return session != null;
    }

    public static boolean hasFrozenFrame() {
        return session != null && session.source != null
                && session.frozenTexture != null;
    }

    public static net.minecraft.resources.ResourceLocation frozenTexture() {
        return session == null ? null : session.frozenTexture;
    }

    public static int frozenWidth() {
        return session == null || session.source == null
                ? 0 : session.source.getWidth();
    }

    public static int frozenHeight() {
        return session == null || session.source == null
                ? 0 : session.source.getHeight();
    }

    /** Called at the end of world rendering so picker and matte share a camera. */
    public static void recordWorldFrame(RenderLevelStageEvent event) {
        if (!PhotoModeFeature.isEnabled()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getCameraEntity() == null) {
            return;
        }

        Matrix4f pose = new Matrix4f(event.getPoseStack());
        Matrix3f normal = new Matrix3f(pose).invert().transpose();
        var camera = event.getCamera();
        lastWorldFrame = new FrameSnapshot(
                pose,
                normal,
                new Matrix4f(event.getProjectionMatrix()),
                camera.getPosition(),
                new Vec3(camera.getLookVector()),
                new Vec3(camera.getUpVector()),
                new Vec3(camera.getLeftVector()),
                event.getPartialTick());
    }

    public static void open() {
        if (!PhotoModeFeature.isEnabled() || session != null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || minecraft.getCameraEntity() == null || minecraft.screen != null) {
            return;
        }

        session = new Session();
        minecraft.setScreen(new PhotoModeSelectionScreen());
    }

    /**
     * Runs immediately before vanilla HUD rendering. This is late enough for
     * Oculus/shader composition but early enough that the source contains no HUD.
     */
    public static void beforeGui(RenderGuiEvent.Pre event) {
        Session current = session;
        if (!PhotoModeFeature.isEnabled() || current == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (current.source == null) {
            if (lastWorldFrame == null) {
                return;
            }
            try {
                current.frame = lastWorldFrame.copy();
                current.source = download(minecraft.getMainRenderTarget());
                NativeImage displayImage = copyImage(current.source);
                current.frozenTexture = minecraft.getTextureManager().register(
                        "scp_photo_mode_frozen", new DynamicTexture(displayImage));
            } catch (RuntimeException ex) {
                fail(Component.literal("Photo Mode could not capture the frozen frame: "
                        + ex.getMessage()));
                return;
            }
        }

        if (current.pendingTarget != null && !current.capturing) {
            current.capturing = true;
            try {
                saveSelectedObject(current, current.pendingTarget);
            } catch (Exception ex) {
                fail(Component.literal("Photo Mode capture failed: " + ex.getMessage()));
                return;
            }
            finish();
            event.setCanceled(true);
            return;
        }

        // The frozen texture is drawn by PhotoModeSelectionScreen. Suppressing
        // the live HUD prevents one-frame flashes beneath it.
        event.setCanceled(true);
    }

    public static PhotoTarget pick(double mouseX, double mouseY,
                                   int guiWidth, int guiHeight) {
        Session current = session;
        Minecraft minecraft = Minecraft.getInstance();
        if (current == null || current.frame == null || minecraft.level == null
                || minecraft.getCameraEntity() == null
                || guiWidth <= 0 || guiHeight <= 0) {
            return null;
        }

        FrameSnapshot frame = current.frame;
        double ndcX = mouseX / (double) guiWidth * 2.0D - 1.0D;
        double ndcY = 1.0D - mouseY / (double) guiHeight * 2.0D;

        float projectionX = frame.projection.m00();
        float projectionY = frame.projection.m11();
        if (Math.abs(projectionX) < 1.0E-6F
                || Math.abs(projectionY) < 1.0E-6F) {
            return null;
        }

        Vec3 right = frame.left.scale(-1.0D);
        Vec3 direction = frame.look
                .add(right.scale(ndcX / projectionX))
                .add(frame.up.scale(ndcY / projectionY))
                .normalize();
        Vec3 start = frame.cameraPosition;
        Vec3 end = start.add(direction.scale(PhotoModeFeature.PICK_DISTANCE));

        BlockHitResult blockHit = minecraft.level.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                minecraft.getCameraEntity()));
        double nearestDistance = blockHit.getType() == HitResult.Type.MISS
                ? PhotoModeFeature.PICK_DISTANCE * PhotoModeFeature.PICK_DISTANCE
                : start.distanceToSqr(blockHit.getLocation());

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                minecraft.getCameraEntity(), start, end,
                new AABB(start, end).inflate(1.0D),
                entity -> entity != minecraft.getCameraEntity()
                        && !entity.isSpectator() && entity.isPickable(),
                nearestDistance);

        if (entityHit != null) {
            return new EntityTarget(entityHit.getEntity());
        }
        if (blockHit.getType() != HitResult.Type.MISS) {
            return new BlockTarget(blockHit.getBlockPos());
        }
        return null;
    }

    public static void requestCapture(PhotoTarget target) {
        if (session != null && session.source != null && target != null
                && session.pendingTarget == null) {
            session.pendingTarget = target;
        }
    }

    public static void cancel() {
        cleanup();
    }

    private static void finish() {
        cleanup();
    }

    private static void cleanup() {
        Session old = session;
        session = null;
        if (old == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (old.frozenTexture != null) {
            minecraft.getTextureManager().release(old.frozenTexture);
        }
        if (old.source != null) {
            old.source.close();
        }
        if (minecraft.screen instanceof PhotoModeSelectionScreen) {
            minecraft.setScreen(null);
        }
    }

    private static void fail(Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        cleanup();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, false);
        }
    }

    private static void saveSelectedObject(Session current, PhotoTarget target)
            throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        if (current.frame == null || current.source == null
                || minecraft.level == null) {
            throw new IOException("the frozen world frame is no longer available");
        }

        NativeImage matte = renderMatte(target, current.frame,
                current.source.getWidth(), current.source.getHeight());
        try {
            NativeImage output = compositeAndCrop(current.source, matte);
            try {
                Path directory = minecraft.gameDirectory.toPath()
                        .resolve("screenshots").resolve("photo_mode");
                Files.createDirectories(directory);
                String filename = "scp_photo_"
                        + FILE_TIME.format(LocalDateTime.now()) + ".png";
                Path path = directory.resolve(filename);
                output.writeToFile(path);

                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.literal(
                            "Photo saved: screenshots/photo_mode/" + filename), false);
                }
            } finally {
                output.close();
            }
        } finally {
            matte.close();
        }
    }

    /**
     * The old GPU isolation passes are intentionally gone. Oculus proved that a
     * RenderType may route geometry into shader-owned g-buffers instead of the
     * framebuffer Photo Mode binds. Capture the renderer's emitted vertices and
     * rasterize the silhouette on CPU instead; RGB still comes from the frozen
     * shader-composited source frame.
     */
    private static NativeImage renderMatte(PhotoTarget target, FrameSnapshot frame,
                                           int width, int height) throws IOException {
        return GeometryMatteRasterizer.render(target,
                frame.pose, frame.normal, frame.projection,
                frame.cameraPosition, frame.partialTick, width, height);
    }

    private static NativeImage compositeAndCrop(NativeImage source, NativeImage matte)
            throws IOException {
        if (source.getWidth() != matte.getWidth()
                || source.getHeight() != matte.getHeight()) {
            throw new IOException("source and matte dimensions differ");
        }

        int width = source.getWidth();
        int height = source.getHeight();
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (matte.getPixelRGBA(x, y) >>> 24) & 0xFF;
                if (alpha > 2) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            throw new IOException("the selected object produced an empty geometry matte");
        }

        minX = Math.max(0, minX - PhotoModeFeature.OUTPUT_PADDING);
        minY = Math.max(0, minY - PhotoModeFeature.OUTPUT_PADDING);
        maxX = Math.min(width - 1, maxX + PhotoModeFeature.OUTPUT_PADDING);
        maxY = Math.min(height - 1, maxY + PhotoModeFeature.OUTPUT_PADDING);

        NativeImage output = new NativeImage(
                maxX - minX + 1, maxY - minY + 1, false);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int matteAlpha = (matte.getPixelRGBA(x, y) >>> 24) & 0xFF;
                int sourcePixel = source.getPixelRGBA(x, y);
                output.setPixelRGBA(x - minX, y - minY,
                        (sourcePixel & 0x00FFFFFF) | (matteAlpha << 24));
            }
        }
        return output;
    }

    private static NativeImage download(RenderTarget target) {
        RenderSystem.assertOnRenderThread();
        NativeImage image = new NativeImage(target.width, target.height, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        image.downloadTexture(0, false);
        image.flipY();
        return image;
    }

    private static NativeImage copyImage(NativeImage source) {
        NativeImage copy = new NativeImage(
                source.getWidth(), source.getHeight(), false);
        copy.copyFrom(source);
        return copy;
    }

    public interface PhotoTarget {
        Component label();
    }

    public record EntityTarget(Entity entity) implements PhotoTarget {
        @Override
        public Component label() {
            return entity.getDisplayName();
        }
    }

    public record BlockTarget(BlockPos pos) implements PhotoTarget {
        @Override
        public Component label() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return Component.literal(pos.toShortString());
            }
            return minecraft.level.getBlockState(pos).getBlock().getName();
        }
    }

    private record FrameSnapshot(Matrix4f pose, Matrix3f normal,
                                 Matrix4f projection, Vec3 cameraPosition,
                                 Vec3 look, Vec3 up, Vec3 left,
                                 float partialTick) {
        FrameSnapshot copy() {
            return new FrameSnapshot(new Matrix4f(pose), new Matrix3f(normal),
                    new Matrix4f(projection), cameraPosition, look, up, left,
                    partialTick);
        }
    }

    private static final class Session {
        private FrameSnapshot frame;
        private NativeImage source;
        private net.minecraft.resources.ResourceLocation frozenTexture;
        private PhotoTarget pendingTarget;
        private boolean capturing;
    }
}
