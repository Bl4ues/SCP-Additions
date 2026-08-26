package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Small client-side world decal renderer for flat liquid stains that need
 * ordinary named textures instead of the particle atlas.
 *
 * <p>The particle atlas does not expose per-sprite LabPBR sidecars in the same
 * way block/entity texture bindings do. These decals therefore use entity
 * render types with a concrete base texture path so shader packs can discover
 * splatter_s and scp_106_puddle_n/_s normally. Blood deliberately uses the
 * cutout + z-offset entity path: it stays anchored to the supporting surface,
 * avoids translucent depth sorting against the floor, and clips the soft alpha
 * fringe instead of letting shader bloom turn that fringe into a bright halo.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PbrSurfaceDecalClient {
    private static final ResourceLocation BLOOD_TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/particle/splatter.png");
    private static final ResourceLocation CORROSION_TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/particle/scp_106_puddle.png");

    private static final RenderType BLOOD_RENDER_TYPE =
            RenderType.entityCutoutNoCullZOffset(BLOOD_TEXTURE, false);
    private static final RenderType CORROSION_RENDER_TYPE =
            RenderType.entityTranslucent(CORROSION_TEXTURE, true);

    private static final int BLOOD_LIFETIME_TICKS = 10 * 20;
    private static final float BLOOD_MAX_ALPHA = 0.90F;
    private static final float BLOOD_FADE_PORTION = 0.30F;
    // The damage sampler already places the anchor fractionally above the
    // support block. Keep an additional renderer-local lift so shader depth
    // precision can never make the decal fight the floor while the camera moves.
    private static final double BLOOD_SURFACE_LIFT = 0.010D;
    private static final float CORROSION_MAX_ALPHA = 0.84F;
    private static final double MAX_RENDER_DISTANCE_SQ = 96.0D * 96.0D;

    private static final List<SurfaceDecal> DECALS = new ArrayList<>();
    private static ClientLevel trackedLevel;

    private PbrSurfaceDecalClient() {
    }

    public static void addBlood(ClientLevel level, double x, double y, double z,
            double size, int packedColor, double rotation) {
        if (level == null) return;
        ensureLevel(level);
        float safeSize = (float) Mth.clamp(size, 0.06D, 0.95D);
        synchronized (DECALS) {
            DECALS.add(SurfaceDecal.blood(x, y, z, safeSize,
                    packedColor, (float) rotation, level.getGameTime()));
        }
    }

    public static void addCorrosion(ClientLevel level, double x, double y,
            double z, double sizeScale, double opacityScale) {
        if (level == null) return;
        ensureLevel(level);

        RandomSource random = level.random;
        float safeScale = (float) Mth.clamp(sizeScale, 0.30D, 1.45D);
        float safeOpacity = (float) Mth.clamp(
                opacityScale > 0.0D ? opacityScale : 1.0D,
                0.25D, 1.0D);
        int lifetime = 120 + random.nextInt(61);
        float baseSize = (0.34F + random.nextFloat() * 0.14F) * safeScale;
        float rotation = random.nextFloat() * ((float) Math.PI * 2.0F);
        float firstAngle = rotation + 1.75F + random.nextFloat() * 0.75F;
        float secondAngle = rotation + 3.85F + random.nextFloat() * 0.85F;
        float firstDistance = 0.48F + random.nextFloat() * 0.20F;
        float secondDistance = 0.42F + random.nextFloat() * 0.18F;

        int red = Math.round((0.095F + random.nextFloat() * 0.055F) * 255.0F);
        int green = Math.round((0.030F + random.nextFloat() * 0.030F) * 255.0F);
        int blue = Math.round((0.012F + random.nextFloat() * 0.018F) * 255.0F);
        int color = (red << 16) | (green << 8) | blue;

        synchronized (DECALS) {
            DECALS.add(SurfaceDecal.corrosion(x, y, z, baseSize,
                    CORROSION_MAX_ALPHA * safeOpacity, color, rotation,
                    firstAngle, secondAngle, firstDistance, secondDistance,
                    lifetime, level.getGameTime()));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clear();
            return;
        }
        ensureLevel(level);
        long now = level.getGameTime();
        synchronized (DECALS) {
            Iterator<SurfaceDecal> iterator = DECALS.iterator();
            while (iterator.hasNext()) {
                SurfaceDecal decal = iterator.next();
                if (now - decal.spawnTick >= decal.lifetime) {
                    iterator.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        ensureLevel(level);

        List<SurfaceDecal> snapshot;
        synchronized (DECALS) {
            if (DECALS.isEmpty()) return;
            snapshot = List.copyOf(DECALS);
        }

        Vec3 camera = event.getCamera().getPosition();
        long now = level.getGameTime();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();
        VertexConsumer bloodConsumer = null;
        VertexConsumer corrosionConsumer = null;
        boolean renderedBlood = false;
        boolean renderedCorrosion = false;

        for (SurfaceDecal decal : snapshot) {
            double dx = decal.x - camera.x;
            double dy = decal.y - camera.y;
            double dz = decal.z - camera.z;
            if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DISTANCE_SQ) {
                continue;
            }

            float age = Mth.clamp((now - decal.spawnTick)
                    / (float) decal.lifetime, 0.0F, 1.0F);
            int light = LevelRenderer.getLightColor(level,
                    BlockPos.containing(decal.x, decal.y, decal.z));

            if (decal.kind == Kind.BLOOD) {
                if (bloodConsumer == null) {
                    bloodConsumer = buffers.getBuffer(BLOOD_RENDER_TYPE);
                }
                float remaining = 1.0F - age;
                float fade = Mth.clamp(remaining / BLOOD_FADE_PORTION,
                        0.0F, 1.0F);
                fade = fade * fade * (3.0F - 2.0F * fade);
                renderQuad(poseStack, bloodConsumer, camera,
                        decal.x, decal.y + BLOOD_SURFACE_LIFT, decal.z,
                        decal.baseSize * 1.16F, decal.baseSize * 0.92F,
                        decal.rotation, decal.color,
                        BLOOD_MAX_ALPHA * fade, light);
                renderedBlood = true;
            } else {
                if (corrosionConsumer == null) {
                    corrosionConsumer = buffers.getBuffer(CORROSION_RENDER_TYPE);
                }
                float remaining = 1.0F - age;
                float fade = Mth.clamp(remaining / 0.32F, 0.0F, 1.0F);
                float alpha = decal.maxAlpha * fade;
                float ticksOld = (float) Math.max(0L,
                        now - decal.spawnTick);
                float size = decal.baseSize + ticksOld * 0.00055F;

                renderQuad(poseStack, corrosionConsumer, camera,
                        decal.x, decal.y, decal.z,
                        size * 1.22F, size * 0.82F,
                        decal.rotation, decal.color, alpha, light);
                renderQuad(poseStack, corrosionConsumer, camera,
                        decal.x + Mth.cos(decal.firstLobeAngle)
                                * size * decal.firstLobeDistance,
                        decal.y + 0.0004D,
                        decal.z + Mth.sin(decal.firstLobeAngle)
                                * size * decal.firstLobeDistance,
                        size * 0.76F, size * 0.57F,
                        decal.rotation + 0.68F, decal.color,
                        alpha * 0.92F, light);
                renderQuad(poseStack, corrosionConsumer, camera,
                        decal.x + Mth.cos(decal.secondLobeAngle)
                                * size * decal.secondLobeDistance,
                        decal.y + 0.0008D,
                        decal.z + Mth.sin(decal.secondLobeAngle)
                                * size * decal.secondLobeDistance,
                        size * 0.63F, size * 0.50F,
                        decal.rotation - 0.54F, decal.color,
                        alpha * 0.86F, light);
                renderedCorrosion = true;
            }
        }

        if (renderedBlood) buffers.endBatch(BLOOD_RENDER_TYPE);
        if (renderedCorrosion) buffers.endBatch(CORROSION_RENDER_TYPE);
    }

    private static void renderQuad(PoseStack poseStack, VertexConsumer consumer,
            Vec3 camera, double worldX, double worldY, double worldZ,
            float radiusX, float radiusZ, float rotation, int packedColor,
            float alpha, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(worldX - camera.x,
                worldY - camera.y, worldZ - camera.z);

        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float cos = Mth.cos(rotation);
        float sin = Mth.sin(rotation);

        float x0 = -radiusX * cos + radiusZ * sin;
        float z0 = -radiusX * sin - radiusZ * cos;
        float x1 = -radiusX * cos - radiusZ * sin;
        float z1 = -radiusX * sin + radiusZ * cos;
        float x2 = radiusX * cos - radiusZ * sin;
        float z2 = radiusX * sin + radiusZ * cos;
        float x3 = radiusX * cos + radiusZ * sin;
        float z3 = radiusX * sin - radiusZ * cos;

        int red = (packedColor >> 16) & 0xFF;
        int green = (packedColor >> 8) & 0xFF;
        int blue = packedColor & 0xFF;
        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);

        vertex(consumer, matrix, normal, x0, z0,
                1.0F, 1.0F, red, green, blue, alphaByte, packedLight);
        vertex(consumer, matrix, normal, x1, z1,
                1.0F, 0.0F, red, green, blue, alphaByte, packedLight);
        vertex(consumer, matrix, normal, x2, z2,
                0.0F, 0.0F, red, green, blue, alphaByte, packedLight);
        vertex(consumer, matrix, normal, x3, z3,
                0.0F, 1.0F, red, green, blue, alphaByte, packedLight);
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float z, float u, float v,
            int red, int green, int blue, int alpha, int packedLight) {
        consumer.vertex(matrix, x, 0.0F, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private static void ensureLevel(ClientLevel level) {
        if (trackedLevel == level) return;
        synchronized (DECALS) {
            DECALS.clear();
            trackedLevel = level;
        }
    }

    private static void clear() {
        synchronized (DECALS) {
            DECALS.clear();
            trackedLevel = null;
        }
    }

    private enum Kind {
        BLOOD,
        CORROSION
    }

    private static final class SurfaceDecal {
        private final Kind kind;
        private final double x;
        private final double y;
        private final double z;
        private final float baseSize;
        private final float maxAlpha;
        private final int color;
        private final float rotation;
        private final float firstLobeAngle;
        private final float secondLobeAngle;
        private final float firstLobeDistance;
        private final float secondLobeDistance;
        private final int lifetime;
        private final long spawnTick;

        private SurfaceDecal(Kind kind, double x, double y, double z,
                float baseSize, float maxAlpha, int color, float rotation,
                float firstLobeAngle, float secondLobeAngle,
                float firstLobeDistance, float secondLobeDistance,
                int lifetime, long spawnTick) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.baseSize = baseSize;
            this.maxAlpha = maxAlpha;
            this.color = color;
            this.rotation = rotation;
            this.firstLobeAngle = firstLobeAngle;
            this.secondLobeAngle = secondLobeAngle;
            this.firstLobeDistance = firstLobeDistance;
            this.secondLobeDistance = secondLobeDistance;
            this.lifetime = lifetime;
            this.spawnTick = spawnTick;
        }

        private static SurfaceDecal blood(double x, double y, double z,
                float size, int color, float rotation, long spawnTick) {
            return new SurfaceDecal(Kind.BLOOD, x, y, z, size,
                    BLOOD_MAX_ALPHA, color, rotation,
                    0.0F, 0.0F, 0.0F, 0.0F,
                    BLOOD_LIFETIME_TICKS, spawnTick);
        }

        private static SurfaceDecal corrosion(double x, double y, double z,
                float size, float maxAlpha, int color, float rotation,
                float firstAngle, float secondAngle,
                float firstDistance, float secondDistance,
                int lifetime, long spawnTick) {
            return new SurfaceDecal(Kind.CORROSION, x, y, z, size,
                    maxAlpha, color, rotation,
                    firstAngle, secondAngle, firstDistance, secondDistance,
                    lifetime, spawnTick);
        }
    }
}
