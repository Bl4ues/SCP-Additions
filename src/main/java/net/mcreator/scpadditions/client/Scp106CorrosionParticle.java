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
import org.joml.Vector3f;

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
        Vec3 cameraPosition = camera.getPosition();
        float renderX = (float) (Mth.lerp(partialTick, this.xo, this.x)
                - cameraPosition.x());
        float renderY = (float) (Mth.lerp(partialTick, this.yo, this.y)
                - cameraPosition.y());
        float renderZ = (float) (Mth.lerp(partialTick, this.zo, this.z)
                - cameraPosition.z());

        Vector3f[] corners = new Vector3f[] {
                new Vector3f(-1.0F, 0.0F, -1.0F),
                new Vector3f(-1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, -1.0F)
        };
        float size = getQuadSize(partialTick);
        for (Vector3f corner : corners) {
            corner.mul(size);
            corner.add(renderX, renderY, renderZ);
        }

        float minU = getU0();
        float maxU = getU1();
        float minV = getV0();
        float maxV = getV1();
        int light = getLightColor(partialTick);

        vertexConsumer.vertex(corners[0].x(), corners[0].y(), corners[0].z())
                .uv(maxU, maxV).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        vertexConsumer.vertex(corners[1].x(), corners[1].y(), corners[1].z())
                .uv(maxU, minV).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        vertexConsumer.vertex(corners[2].x(), corners[2].y(), corners[2].z())
                .uv(minU, minV).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        vertexConsumer.vertex(corners[3].x(), corners[3].y(), corners[3].z())
                .uv(minU, maxV).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
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
