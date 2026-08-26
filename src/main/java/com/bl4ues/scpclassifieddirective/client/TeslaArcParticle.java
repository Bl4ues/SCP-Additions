package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Short-lived, full-bright particle used as a point along procedural Tesla arcs.
 * The line shape itself is produced by TeslaGateElectricity spawning these along
 * randomized polyline segments, so the particle can remain deliberately cheap.
 */
public final class TeslaArcParticle extends TextureSheetParticle {
    private final float initialAlpha;

    private TeslaArcParticle(ClientLevel level, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        this.xd = velocityX * 0.08D;
        this.yd = velocityY * 0.08D;
        this.zd = velocityZ * 0.08D;
        this.gravity = 0.0F;
        this.friction = 0.72F;
        this.hasPhysics = false;
        this.lifetime = 3 + this.random.nextInt(3);
        this.quadSize = 0.055F + this.random.nextFloat() * 0.035F;

        float whiteBias = 0.72F + this.random.nextFloat() * 0.20F;
        this.rCol = 0.45F + whiteBias * 0.45F;
        this.gCol = 0.78F + whiteBias * 0.20F;
        this.bCol = 1.0F;
        this.initialAlpha = 0.78F + this.random.nextFloat() * 0.20F;
        this.alpha = initialAlpha;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float life = age / (float) Math.max(1, lifetime);
        alpha = initialAlpha * (1.0F - life * life);
        quadSize *= 0.94F;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0x00F000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type,
                ClientLevel level, double x, double y, double z,
                double velocityX, double velocityY, double velocityZ) {
            return new TeslaArcParticle(level, x, y, z,
                    velocityX, velocityY, velocityZ, sprites);
        }
    }
}
