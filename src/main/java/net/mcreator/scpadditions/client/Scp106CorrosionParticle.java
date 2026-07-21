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
import org.joml.Quaternionf;

public final class Scp106CorrosionParticle extends TextureSheetParticle {
    private static final float MAX_ALPHA = 0.76F;
    private final SpriteSet sprites;

    private Scp106CorrosionParticle(ClientLevel level, double x, double y, double z,
            SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = 35 + this.random.nextInt(21);
        this.quadSize = 0.075F + this.random.nextFloat() * 0.055F;
        this.setColor(
                0.045F + this.random.nextFloat() * 0.035F,
                0.018F + this.random.nextFloat() * 0.020F,
                0.010F + this.random.nextFloat() * 0.012F);
        this.setAlpha(MAX_ALPHA);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);

        float remaining = 1.0F - Mth.clamp(
                this.age / (float) this.lifetime, 0.0F, 1.0F);
        float fade = Mth.clamp(remaining / 0.35F, 0.0F, 1.0F);
        this.setAlpha(MAX_ALPHA * fade);
        this.quadSize += 0.00045F;
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera,
            float partialTick) {
        Quaternionf floorRotation = new Quaternionf();
        floorRotation.rotateX(-1.5707964F);
        this.renderRotatedQuad(vertexConsumer, camera, floorRotation, partialTick);
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
            return new Scp106CorrosionParticle(level, x, y, z, sprites);
        }
    }
}
