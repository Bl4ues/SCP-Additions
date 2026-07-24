package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A large irregular SCP-106 portal laid on a floor or against a wall. */
public final class Scp106PortalParticle extends TextureSheetParticle {
    private static final float MAX_ALPHA = 0.82F;
    private static final float UV_CROP = 0.0F;
    private static final double SURFACE_OFFSET = 0.055D;

    private final SpriteSet sprites;
    private final Vec3 normal;
    private final Vec3 planeU;
    private final Vec3 planeV;
    private final float rotation;
    private final float lobeAngleA;
    private final float lobeAngleB;
    private final float lobeAngleC;
    private final float lobeDistanceA;
    private final float lobeDistanceB;
    private final float lobeDistanceC;

    private Scp106PortalParticle(ClientLevel level, double x, double y,
            double z, double normalX, double normalY, double normalZ,
            SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        Vec3 requestedNormal = new Vec3(normalX, normalY, normalZ);
        double normalStrength = requestedNormal.length();
        boolean transientSurface = normalStrength > 0.0001D
                && normalStrength < 0.75D;
        this.normal = requestedNormal.lengthSqr() < 0.0001D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : requestedNormal.normalize();

        Vec3 reference = Math.abs(this.normal.y) > 0.82D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 v = this.normal.cross(reference).normalize();
        Vec3 u = v.cross(this.normal).normalize();
        this.planeU = u;
        this.planeV = v;

        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = transientSurface
                ? 30 + this.random.nextInt(17)
                : 90 + this.random.nextInt(41);
        this.quadSize = transientSurface
                ? 0.58F + this.random.nextFloat() * 0.18F
                : 0.95F + this.random.nextFloat() * 0.25F;
        this.rotation = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        this.lobeAngleA = rotation + 1.05F
                + this.random.nextFloat() * 0.45F;
        this.lobeAngleB = rotation + 2.85F
                + this.random.nextFloat() * 0.55F;
        this.lobeAngleC = rotation + 4.75F
                + this.random.nextFloat() * 0.50F;
        this.lobeDistanceA = 0.48F + this.random.nextFloat() * 0.18F;
        this.lobeDistanceB = 0.52F + this.random.nextFloat() * 0.20F;
        this.lobeDistanceC = 0.44F + this.random.nextFloat() * 0.18F;
        float brightness = 0.86F + this.random.nextFloat() * 0.12F;
        this.setColor(brightness, brightness, brightness);
        this.setAlpha(MAX_ALPHA);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float remaining = 1.0F - Mth.clamp(
                this.age / (float) this.lifetime, 0.0F, 1.0F);
        float fade = Mth.clamp(remaining / 0.28F, 0.0F, 1.0F);
        this.setAlpha(MAX_ALPHA * fade);
        this.quadSize += 0.0009F;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera,
            float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        Vec3 center = new Vec3(
                Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x,
                Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y,
                Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z)
                .add(normal.scale(SURFACE_OFFSET));
        float size = getQuadSize(partialTick);

        float fullMinU = getU0();
        float fullMaxU = getU1();
        float fullMinV = getV0();
        float fullMaxV = getV1();
        float minU = Mth.lerp(UV_CROP, fullMinU, fullMaxU);
        float maxU = Mth.lerp(1.0F - UV_CROP, fullMinU, fullMaxU);
        float minV = Mth.lerp(UV_CROP, fullMinV, fullMaxV);
        float maxV = Mth.lerp(1.0F - UV_CROP, fullMinV, fullMaxV);
        int light = getLightColor(partialTick);

        renderLobe(vertexConsumer, center,
                size * 1.35F, size * 0.82F, rotation,
                minU, maxU, minV, maxV, light, 1.0F);
        renderOffsetLobe(vertexConsumer, center, size, lobeAngleA,
                lobeDistanceA, 0.82F, 0.58F, rotation + 0.57F,
                minU, maxU, minV, maxV, light, 0.95F, 0.012D);
        renderOffsetLobe(vertexConsumer, center, size, lobeAngleB,
                lobeDistanceB, 0.76F, 0.52F, rotation - 0.71F,
                minU, maxU, minV, maxV, light, 0.90F, 0.024D);
        renderOffsetLobe(vertexConsumer, center, size, lobeAngleC,
                lobeDistanceC, 0.68F, 0.48F, rotation + 1.08F,
                minU, maxU, minV, maxV, light, 0.86F, 0.036D);
    }

    private void renderOffsetLobe(VertexConsumer vertexConsumer, Vec3 center,
            float size, float angle, float distance,
            float scaleU, float scaleV, float lobeRotation,
            float minU, float maxU, float minV, float maxV,
            int light, float alphaMultiplier, double layerOffset) {
        Vec3 offset = rotatedAxis(angle, planeU, planeV)
                .scale(size * distance)
                .add(normal.scale(layerOffset));
        renderLobe(vertexConsumer, center.add(offset),
                size * scaleU, size * scaleV, lobeRotation,
                minU, maxU, minV, maxV, light, alphaMultiplier);
    }

    private void renderLobe(VertexConsumer vertexConsumer, Vec3 center,
            float radiusU, float radiusV, float lobeRotation,
            float minU, float maxU, float minV, float maxV,
            int light, float alphaMultiplier) {
        Vec3 u = rotatedAxis(lobeRotation, planeU, planeV).scale(radiusU);
        Vec3 v = rotatedAxis(lobeRotation + ((float) Math.PI * 0.5F),
                planeU, planeV).scale(radiusV);

        Vec3 corner0 = center.subtract(u).subtract(v);
        Vec3 corner1 = center.subtract(u).add(v);
        Vec3 corner2 = center.add(u).add(v);
        Vec3 corner3 = center.add(u).subtract(v);
        float renderedAlpha = alpha * alphaMultiplier;

        vertex(vertexConsumer, corner0, maxU, maxV, light, renderedAlpha);
        vertex(vertexConsumer, corner1, maxU, minV, light, renderedAlpha);
        vertex(vertexConsumer, corner2, minU, minV, light, renderedAlpha);
        vertex(vertexConsumer, corner3, minU, maxV, light, renderedAlpha);

        vertex(vertexConsumer, corner3, minU, maxV, light, renderedAlpha);
        vertex(vertexConsumer, corner2, minU, minV, light, renderedAlpha);
        vertex(vertexConsumer, corner1, maxU, minV, light, renderedAlpha);
        vertex(vertexConsumer, corner0, maxU, maxV, light, renderedAlpha);
    }

    private void vertex(VertexConsumer consumer, Vec3 position,
            float u, float v, int light, float renderedAlpha) {
        consumer.vertex((float) position.x, (float) position.y,
                        (float) position.z)
                .uv(u, v).color(rCol, gCol, bCol, renderedAlpha)
                .uv2(light).endVertex();
    }

    private static Vec3 rotatedAxis(float angle, Vec3 u, Vec3 v) {
        return u.scale(Mth.cos(angle)).add(v.scale(Mth.sin(angle)));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider
            implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type,
                ClientLevel level, double x, double y, double z,
                double normalX, double normalY, double normalZ) {
            double normalStrength = Math.sqrt(normalX * normalX
                    + normalY * normalY + normalZ * normalZ);
            if (normalStrength >= 0.98D) return null;
            return new Scp106PortalParticle(level, x, y, z,
                    normalX, normalY, normalZ, sprites);
        }
    }
}
