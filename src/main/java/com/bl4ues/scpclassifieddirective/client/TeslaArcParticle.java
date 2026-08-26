package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Tiny full-bright samples used to draw a procedural Tesla bolt. The generator
 * places them densely enough that neighboring quads overlap into a continuous
 * line, so individual particles deliberately do not drift like sparks.
 */
public final class TeslaArcParticle extends TextureSheetParticle {
    private final float initialAlpha;
    private final float initialSize;

    private TeslaArcParticle(ClientLevel level, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = 2 + this.random.nextInt(2);
        this.initialSize = 0.043F + this.random.nextFloat() * 0.020F;
        this.quadSize = initialSize;

        // Keep a saturated electric-blue base so shader bloom can push the
        // overlapping core toward blue-white without bleaching the whole bolt
        // into plain white. Slight per-particle variation prevents a flat neon
        // strip while preserving the SCP: Unity-like cold-blue appearance.
        float variation = this.random.nextFloat();
        this.rCol = 0.24F + variation * 0.10F;
        this.gCol = 0.58F + variation * 0.12F;
        this.bCol = 1.0F;
        this.initialAlpha = 0.88F + this.random.nextFloat() * 0.12F;
        this.alpha = initialAlpha;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        float life = age / (float) Math.max(1, lifetime);
        alpha = initialAlpha * (1.0F - life * life);
        quadSize = initialSize * (1.0F - life * 0.18F);
        xd = 0.0D;
        yd = 0.0D;
        zd = 0.0D;
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
