package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Short-lived vapor emitted directly from the rebuilt checkpoint vents. */
public final class DecontaminationGasParticle extends TextureSheetParticle {
    private static final float MAX_ALPHA = 0.20F;
    /** Last emission is tick 94; lifetime 10 guarantees removal at tick 105. */
    private static final int VAPOR_LIFETIME_TICKS = 10;
    private final SpriteSet sprites;

    private DecontaminationGasParticle(ClientLevel level, double x, double y,
            double z, double velocityX, double velocityY, double velocityZ,
            SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        this.sprites = sprites;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.friction = 0.985F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = VAPOR_LIFETIME_TICKS;
        this.quadSize = 0.42F + this.random.nextFloat() * 0.16F;
        this.setColor(0.72F, 0.76F, 0.78F);
        this.setAlpha(0.0F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age >= this.lifetime) return;
        this.setSpriteFromAge(sprites);
        float progress = Mth.clamp(this.age / (float) this.lifetime,
                0.0F, 1.0F);
        float fadeIn = Mth.clamp(progress / 0.20F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - progress) / 0.40F,
                0.0F, 1.0F);
        this.setAlpha(MAX_ALPHA * Math.min(fadeIn, fadeOut));
        this.quadSize += 0.0040F;
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
            return new DecontaminationGasParticle(level, x, y, z,
                    velocityX, velocityY, velocityZ, sprites);
        }
    }
}
