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
 * Client-side named-texture renderer for blood and SCP-106 surface stains.
 *
 * <p>The particle atlas does not expose per-sprite LabPBR sidecars in the same
 * way named entity textures do. These decals therefore render through concrete
 * texture paths so splatter_s and scp_106_puddle_n/_s remain available to
 * shader packs. Every flat stain is deliberately lifted from the support plane
 * by a small but real world-space distance; relying on nearly-coplanar polygon
 * offset was not stable with shaders at grazing camera angles.</p>
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

    // Do not use the view-dependent Z-offset render type here. Shader packs can
    // turn that camera-relative bias into visible shaking at grazing angles.
    // The decals instead use an ordinary no-cull pass plus a real world-space
    // separation from the supporting surface.
    private static final RenderType BLOOD_RENDER_TYPE =
            RenderType.entityCutoutNoCull(BLOOD_TEXTURE, false);
    private static final RenderType CORROSION_RENDER_TYPE =
            RenderType.entityTranslucent(CORROSION_TEXTURE, true);

    private static final int BLOOD_LIFETIME_TICKS = 10 * 20;
    private static final float BLOOD_MAX_ALPHA = 0.90F;
    private static final float BLOOD_FADE_PORTION = 0.30F;
    private static final float CORROSION_MAX_ALPHA = 0.84F;
    private static final float PORTAL_MAX_ALPHA = 0.82F;

    // Roughly one texture pixel of physical separation is intentionally used.
    // 1/32 block was still close enough for shader depth precision to alternate
    // between the floor and the decal as the camera angle changed.
    private static final double BLOOD_SURFACE_LIFT = 0.0675D;
    private static final double CORROSION_SURFACE_LIFT = 0.0675D;
    private static final double CORROSION_LAYER_STEP = 0.0125D;
    // Portal spawn positions are already surface-offset server-side. Keep the
    // named-texture PBR pass similarly clear of the supporting plane.
    private static final double PORTAL_SURFACE_LIFT = 0.0725D;
    private static final double PORTAL_LAYER_STEP = 0.014D;
    private static final double MAX_RENDER_DISTANCE_SQ = 96.0D * 96.0D;

    private static final List<SurfaceDecal> DECALS = new ArrayList<>();
    private static final List<PortalDecal> PORTALS = new ArrayList<>();
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

    /**
     * Adds one of SCP-106's large phase/emergence puddles using the same named
     * texture and LabPBR sidecars as the smaller corrosion trail.
     */
    public static void addPortal(ClientLevel level, double x, double y, double z,
            double normalX, double normalY, double normalZ) {
        if (level == null) return;
        ensureLevel(level);

        RandomSource random = level.random;
        Vec3 requestedNormal = new Vec3(normalX, normalY, normalZ);
        double normalStrength = requestedNormal.length();
        boolean transientSurface = normalStrength > 0.0001D
                && normalStrength < 0.75D;
        Vec3 normal = requestedNormal.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : requestedNormal.normalize();
        int lifetime = transientSurface
                ? 30 + random.nextInt(17)
                : 90 + random.nextInt(41);
        float baseSize = transientSurface
                ? 0.58F + random.nextFloat() * 0.18F
                : 0.95F + random.nextFloat() * 0.25F;
        int fadeInTicks = transientSurface ? 6 : 10;
        float rotation = random.nextFloat() * ((float) Math.PI * 2.0F);
        float lobeAngleA = rotation + 1.05F + random.nextFloat() * 0.45F;
        float lobeAngleB = rotation + 2.85F + random.nextFloat() * 0.55F;
        float lobeAngleC = rotation + 4.75F + random.nextFloat() * 0.50F;
        float lobeDistanceA = 0.48F + random.nextFloat() * 0.18F;
        float lobeDistanceB = 0.52F + random.nextFloat() * 0.20F;
        float lobeDistanceC = 0.44F + random.nextFloat() * 0.18F;
        int brightness = Math.round((0.86F + random.nextFloat() * 0.12F)
                * 255.0F);
        int color = (brightness << 16) | (brightness << 8) | brightness;

        synchronized (DECALS) {
            PORTALS.add(new PortalDecal(x, y, z, normal, baseSize,
                    fadeInTicks, rotation,
                    lobeAngleA, lobeAngleB, lobeAngleC,
                    lobeDistanceA, lobeDistanceB, lobeDistanceC,
                    color, lifetime, level.getGameTime()));
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
            Iterator<PortalDecal> portalIterator = PORTALS.iterator();
            while (portalIterator.hasNext()) {
                PortalDecal portal = portalIterator.next();
                if (now - portal.spawnTick >= portal.lifetime) {
                    portalIterator.remove();
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
        List<PortalDecal> portalSnapshot;
        synchronized (DECALS) {
            if (DECALS.isEmpty() && PORTALS.isEmpty()) return;
            snapshot = List.copyOf(DECALS);
            portalSnapshot = List.copyOf(PORTALS);
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
            if (distanceSquared(decal.x, decal.y, decal.z, camera)
                    > MAX_RENDER_DISTANCE_SQ) {
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
                renderFloorQuad(poseStack, bloodConsumer, camera,
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
                double baseY = decal.y + CORROSION_SURFACE_LIFT;

                renderFloorQuad(poseStack, corrosionConsumer, camera,
                        decal.x, baseY, decal.z,
                        size * 1.22F, size * 0.82F,
                        decal.rotation, decal.color, alpha, light);
                renderFloorQuad(poseStack, corrosionConsumer, camera,
                        decal.x + Mth.cos(decal.firstLobeAngle)
                                * size * decal.firstLobeDistance,
                        baseY + CORROSION_LAYER_STEP,
                        decal.z + Mth.sin(decal.firstLobeAngle)
                                * size * decal.firstLobeDistance,
                        size * 0.76F, size * 0.57F,
                        decal.rotation + 0.68F, decal.color,
                        alpha * 0.92F, light);
                renderFloorQuad(poseStack, corrosionConsumer, camera,
                        decal.x + Mth.cos(decal.secondLobeAngle)
                                * size * decal.secondLobeDistance,
                        baseY + CORROSION_LAYER_STEP * 2.0D,
                        decal.z + Mth.sin(decal.secondLobeAngle)
                                * size * decal.secondLobeDistance,
                        size * 0.63F, size * 0.50F,
                        decal.rotation - 0.54F, decal.color,
                        alpha * 0.86F, light);
                renderedCorrosion = true;
            }
        }

        for (PortalDecal portal : portalSnapshot) {
            if (distanceSquared(portal.x, portal.y, portal.z, camera)
                    > MAX_RENDER_DISTANCE_SQ) {
                continue;
            }
            if (corrosionConsumer == null) {
                corrosionConsumer = buffers.getBuffer(CORROSION_RENDER_TYPE);
            }

            float ticksOld = (float) Math.max(0L, now - portal.spawnTick);
            float age = Mth.clamp(ticksOld / portal.lifetime, 0.0F, 1.0F);
            float remaining = 1.0F - age;
            float fadeOut = Mth.clamp(remaining / 0.28F, 0.0F, 1.0F);
            float appear = Mth.clamp(ticksOld / portal.fadeInTicks,
                    0.0F, 1.0F);
            float smoothAppear = appear * appear * (3.0F - 2.0F * appear);
            float alpha = PORTAL_MAX_ALPHA * smoothAppear * fadeOut;
            float size = portal.baseSize
                    * (0.18F + 0.82F * smoothAppear)
                    + ticksOld * 0.0009F;
            int light = LevelRenderer.getLightColor(level,
                    BlockPos.containing(portal.x, portal.y, portal.z));
            Vec3 center = new Vec3(portal.x, portal.y, portal.z)
                    .add(portal.normal.scale(PORTAL_SURFACE_LIFT));
            Vec3[] basis = surfaceBasis(portal.normal);
            Vec3 planeU = basis[0];
            Vec3 planeV = basis[1];

            renderOrientedQuad(poseStack, corrosionConsumer, camera,
                    center, portal.normal, planeU, planeV,
                    size * 1.35F, size * 0.82F, portal.rotation,
                    portal.color, alpha, light);
            renderPortalLobe(poseStack, corrosionConsumer, camera,
                    center, portal.normal, planeU, planeV, size,
                    portal.lobeAngleA, portal.lobeDistanceA,
                    0.82F, 0.58F, portal.rotation + 0.57F,
                    portal.color, alpha * 0.95F, light,
                    PORTAL_LAYER_STEP);
            renderPortalLobe(poseStack, corrosionConsumer, camera,
                    center, portal.normal, planeU, planeV, size,
                    portal.lobeAngleB, portal.lobeDistanceB,
                    0.76F, 0.52F, portal.rotation - 0.71F,
                    portal.color, alpha * 0.90F, light,
                    PORTAL_LAYER_STEP * 2.0D);
            renderPortalLobe(poseStack, corrosionConsumer, camera,
                    center, portal.normal, planeU, planeV, size,
                    portal.lobeAngleC, portal.lobeDistanceC,
                    0.68F, 0.48F, portal.rotation + 1.08F,
                    portal.color, alpha * 0.86F, light,
                    PORTAL_LAYER_STEP * 3.0D);
            renderedCorrosion = true;
        }

        if (renderedBlood) buffers.endBatch(BLOOD_RENDER_TYPE);
        if (renderedCorrosion) buffers.endBatch(CORROSION_RENDER_TYPE);
    }

    private static double distanceSquared(double x, double y, double z,
            Vec3 camera) {
        double dx = x - camera.x;
        double dy = y - camera.y;
        double dz = z - camera.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void renderFloorQuad(PoseStack poseStack,
            VertexConsumer consumer, Vec3 camera,
            double worldX, double worldY, double worldZ,
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

        vertex(consumer, matrix, normal, x0, 0.0F, z0,
                1.0F, 1.0F, red, green, blue, alphaByte, packedLight,
                0.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, x1, 0.0F, z1,
                1.0F, 0.0F, red, green, blue, alphaByte, packedLight,
                0.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, x2, 0.0F, z2,
                0.0F, 0.0F, red, green, blue, alphaByte, packedLight,
                0.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, x3, 0.0F, z3,
                0.0F, 1.0F, red, green, blue, alphaByte, packedLight,
                0.0F, 1.0F, 0.0F);
        poseStack.popPose();
    }

    private static void renderPortalLobe(PoseStack poseStack,
            VertexConsumer consumer, Vec3 camera, Vec3 center,
            Vec3 surfaceNormal, Vec3 planeU, Vec3 planeV,
            float size, float angle, float distance,
            float scaleU, float scaleV, float rotation,
            int packedColor, float alpha, int packedLight,
            double layerOffset) {
        Vec3 offset = rotatedAxis(angle, planeU, planeV)
                .scale(size * distance)
                .add(surfaceNormal.scale(layerOffset));
        renderOrientedQuad(poseStack, consumer, camera,
                center.add(offset), surfaceNormal, planeU, planeV,
                size * scaleU, size * scaleV, rotation,
                packedColor, alpha, packedLight);
    }

    private static void renderOrientedQuad(PoseStack poseStack,
            VertexConsumer consumer, Vec3 camera, Vec3 worldCenter,
            Vec3 surfaceNormal, Vec3 planeU, Vec3 planeV,
            float radiusU, float radiusV, float rotation,
            int packedColor, float alpha, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(worldCenter.x - camera.x,
                worldCenter.y - camera.y, worldCenter.z - camera.z);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        Vec3 u = rotatedAxis(rotation, planeU, planeV).scale(radiusU);
        Vec3 v = rotatedAxis(rotation + ((float) Math.PI * 0.5F),
                planeU, planeV).scale(radiusV);
        Vec3 corner0 = u.scale(-1.0D).subtract(v);
        Vec3 corner1 = u.scale(-1.0D).add(v);
        Vec3 corner2 = u.add(v);
        Vec3 corner3 = u.subtract(v);

        int red = (packedColor >> 16) & 0xFF;
        int green = (packedColor >> 8) & 0xFF;
        int blue = packedColor & 0xFF;
        int alphaByte = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        float nx = (float) surfaceNormal.x;
        float ny = (float) surfaceNormal.y;
        float nz = (float) surfaceNormal.z;

        vertex(consumer, matrix, normalMatrix,
                (float) corner0.x, (float) corner0.y, (float) corner0.z,
                1.0F, 1.0F, red, green, blue, alphaByte, packedLight,
                nx, ny, nz);
        vertex(consumer, matrix, normalMatrix,
                (float) corner1.x, (float) corner1.y, (float) corner1.z,
                1.0F, 0.0F, red, green, blue, alphaByte, packedLight,
                nx, ny, nz);
        vertex(consumer, matrix, normalMatrix,
                (float) corner2.x, (float) corner2.y, (float) corner2.z,
                0.0F, 0.0F, red, green, blue, alphaByte, packedLight,
                nx, ny, nz);
        vertex(consumer, matrix, normalMatrix,
                (float) corner3.x, (float) corner3.y, (float) corner3.z,
                0.0F, 1.0F, red, green, blue, alphaByte, packedLight,
                nx, ny, nz);
        poseStack.popPose();
    }

    private static Vec3[] surfaceBasis(Vec3 normal) {
        Vec3 reference = Math.abs(normal.y) > 0.82D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 planeV = normal.cross(reference).normalize();
        Vec3 planeU = planeV.cross(normal).normalize();
        return new Vec3[] { planeU, planeV };
    }

    private static Vec3 rotatedAxis(float angle, Vec3 u, Vec3 v) {
        return u.scale(Mth.cos(angle)).add(v.scale(Mth.sin(angle)));
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float y, float z, float u, float v,
            int red, int green, int blue, int alpha, int packedLight,
            float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private static void ensureLevel(ClientLevel level) {
        if (trackedLevel == level) return;
        synchronized (DECALS) {
            DECALS.clear();
            PORTALS.clear();
            trackedLevel = level;
        }
    }

    private static void clear() {
        synchronized (DECALS) {
            DECALS.clear();
            PORTALS.clear();
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

    private static final class PortalDecal {
        private final double x;
        private final double y;
        private final double z;
        private final Vec3 normal;
        private final float baseSize;
        private final int fadeInTicks;
        private final float rotation;
        private final float lobeAngleA;
        private final float lobeAngleB;
        private final float lobeAngleC;
        private final float lobeDistanceA;
        private final float lobeDistanceB;
        private final float lobeDistanceC;
        private final int color;
        private final int lifetime;
        private final long spawnTick;

        private PortalDecal(double x, double y, double z, Vec3 normal,
                float baseSize, int fadeInTicks, float rotation,
                float lobeAngleA, float lobeAngleB, float lobeAngleC,
                float lobeDistanceA, float lobeDistanceB,
                float lobeDistanceC, int color, int lifetime,
                long spawnTick) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.normal = normal;
            this.baseSize = baseSize;
            this.fadeInTicks = fadeInTicks;
            this.rotation = rotation;
            this.lobeAngleA = lobeAngleA;
            this.lobeAngleB = lobeAngleB;
            this.lobeAngleC = lobeAngleC;
            this.lobeDistanceA = lobeDistanceA;
            this.lobeDistanceB = lobeDistanceB;
            this.lobeDistanceC = lobeDistanceC;
            this.color = color;
            this.lifetime = lifetime;
            this.spawnTick = spawnTick;
        }
    }
}
