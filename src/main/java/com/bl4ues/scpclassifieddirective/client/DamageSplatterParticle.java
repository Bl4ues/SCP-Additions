package com.bl4ues.scpclassifieddirective.client;

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

/** Horizontal, client-only blood/debris decal that fades after ten seconds. */
public final class DamageSplatterParticle extends TextureSheetParticle {
    private static final int LIFETIME_TICKS = 10 * 20;
    private static final float MAX_ALPHA = 0.90F;
    private static final float FADE_PORTION = 0.30F;

    private final float splatterRotation;

    private DamageSplatterParticle(ClientLevel level, double x, double y,
            double z, double size, int packedColor, double rotation,
            SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = LIFETIME_TICKS;
        this.quadSize = (float) Mth.clamp(size, 0.06D, 0.95D);
        this.splatterRotation = (float) rotation;

        int red = (packedColor >> 16) & 0xFF;
        int green = (packedColor >> 8) & 0xFF;
        int blue = packedColor & 0xFF;
        setColor(red / 255.0F, green / 255.0F, blue / 255.0F);
        setAlpha(MAX_ALPHA);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float remaining = 1.0F - Mth.clamp(
                age / (float) lifetime, 0.0F, 1.0F);
        float fade = Mth.clamp(remaining / FADE_PORTION, 0.0F, 1.0F);
        // Smoothstep keeps the decal stable for most of its life and avoids a
        // visibly linear disappearance near the end.
        fade = fade * fade * (3.0F - 2.0F * fade);
        setAlpha(MAX_ALPHA * fade);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera,
            float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float centerX = (float) (Mth.lerp(partialTick, xo, x)
                - cameraPosition.x());
        float centerY = (float) (Mth.lerp(partialTick, yo, y)
                - cameraPosition.y());
        float centerZ = (float) (Mth.lerp(partialTick, zo, z)
                - cameraPosition.z());
        float size = getQuadSize(partialTick);
        int light = getLightColor(partialTick);

        float width = size * 1.16F;
        float depth = size * 0.92F;
        float cos = Mth.cos(splatterRotation);
        float sin = Mth.sin(splatterRotation);

        float x0 = centerX + (-width * cos + depth * sin);
        float z0 = centerZ + (-width * sin - depth * cos);
        float x1 = centerX + (-width * cos - depth * sin);
        float z1 = centerZ + (-width * sin + depth * cos);
        float x2 = centerX + (width * cos - depth * sin);
        float z2 = centerZ + (width * sin + depth * cos);
        float x3 = centerX + (width * cos + depth * sin);
        float z3 = centerZ + (width * sin - depth * cos);

        consumer.vertex(x0, centerY, z0).uv(getU1(), getV1())
                .color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        consumer.vertex(x1, centerY, z1).uv(getU1(), getV0())
                .color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        consumer.vertex(x2, centerY, z2).uv(getU0(), getV0())
                .color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        consumer.vertex(x3, centerY, z3).uv(getU0(), getV1())
                .color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
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
                double velocityX, double velocityY, double velocityZ) {
            double size = velocityX > 0.0D ? velocityX : 0.30D;
            int packedColor = (int) Math.round(velocityY);
            return new DamageSplatterParticle(level, x, y, z,
                    size, packedColor, velocityZ, sprites);
        }
    }
}
