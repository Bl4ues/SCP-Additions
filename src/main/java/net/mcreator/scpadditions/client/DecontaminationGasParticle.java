package net.mcreator.scpadditions.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class DecontaminationGasParticle extends TextureSheetParticle {
    private static final float MAX_ALPHA = 0.14F;
    private final SpriteSet sprites;

    private DecontaminationGasParticle(ClientLevel level, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        this.sprites = sprites;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.friction = 0.995F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 32 + this.random.nextInt(17);
        this.quadSize = 0.38F + this.random.nextFloat() * 0.20F;
        this.setColor(0.72F, 0.76F, 0.78F);
        this.setAlpha(0.0F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);
        float progress = Mth.clamp(this.age / (float) this.lifetime, 0.0F, 1.0F);
        float fadeIn = Mth.clamp(progress / 0.18F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - progress) / 0.30F, 0.0F, 1.0F);
        this.setAlpha(MAX_ALPHA * Math.min(fadeIn, fadeOut));
        this.quadSize += 0.0025F;
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new DecontaminationGasParticle(
                    level, x, y, z, velocityX, velocityY, velocityZ, sprites);
        }
    }
}
