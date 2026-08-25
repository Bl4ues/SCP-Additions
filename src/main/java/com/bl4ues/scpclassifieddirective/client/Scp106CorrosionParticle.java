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
 * Network/provider bridge for SCP-106 corrosion stains. The visible puddles are
 * rendered by {@link PbrSurfaceDecalClient} so their normal/specular sidecars
 * are available to shader packs instead of being lost in the particle atlas.
 */
public final class Scp106CorrosionParticle extends TextureSheetParticle {
    private Scp106CorrosionParticle(ClientLevel level, double x, double y,
            double z, double sizeScale, double opacityScale,
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
        PbrSurfaceDecalClient.addCorrosion(level, x, y, z,
                sizeScale, opacityScale);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera,
            float partialTick) {
        // Intentionally empty; direct named-texture rendering supplies PBR.
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
            double sizeScale = velocityX > 0.0D ? velocityX : 1.0D;
            double opacityScale = velocityY > 0.0D
                    ? velocityY : 1.0D;
            return new Scp106CorrosionParticle(level, x, y, z,
                    sizeScale, opacityScale, sprites);
        }
    }
}
