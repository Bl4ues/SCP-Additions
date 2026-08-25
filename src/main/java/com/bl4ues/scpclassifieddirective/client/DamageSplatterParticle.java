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

/**
 * Compatibility particle used as the spawn/provider bridge for blood decals.
 * The visual itself is rendered by {@link PbrSurfaceDecalClient} using the
 * texture directly, so shader packs can pair splatter.png with splatter_s.png.
 */
public final class DamageSplatterParticle extends TextureSheetParticle {
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
        this.lifetime = 1;
        pickSprite(sprites);
        PbrSurfaceDecalClient.addBlood(level, x, y, z,
                size, packedColor, rotation);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera,
            float partialTick) {
        // Intentionally empty. Rendering through the particle atlas would lose
        // the texture-specific LabPBR sidecar association.
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Keep the registered sprite-set pipeline valid, but emit no vertices.
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
