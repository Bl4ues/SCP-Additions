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

public final class Scp106CorrosionParticle extends TextureSheetParticle {
    private static final int EDGE_SEGMENTS = 14;
    private static final float MAX_ALPHA = 0.84F;

    private final SpriteSet sprites;
    private final float puddleRotation;
    private final float firstLobeAngle;
    private final float secondLobeAngle;
    private final float firstLobeDistance;
    private final float secondLobeDistance;
    private final float[] edgeVariation = new float[EDGE_SEGMENTS];

    private Scp106CorrosionParticle(ClientLevel level, double x, double y,
            double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = 60 + this.random.nextInt(31);
        this.quadSize = 0.34F + this.random.nextFloat() * 0.14F;
        this.puddleRotation = this.random.nextFloat()
                * ((float) Math.PI * 2.0F);
        this.firstLobeAngle = this.puddleRotation
                + 1.75F + this.random.nextFloat() * 0.75F;
        this.secondLobeAngle = this.puddleRotation
                + 3.85F + this.random.nextFloat() * 0.85F;
        this.firstLobeDistance = 0.48F
                + this.random.nextFloat() * 0.20F;
        this.secondLobeDistance = 0.42F
                + this.random.nextFloat() * 0.18F;
        for (int i = 0; i < EDGE_SEGMENTS; i++) {
            float broadWave = Mth.sin(i * 1.73F + puddleRotation) * 0.08F;
            edgeVariation[i] = 0.82F + broadWave
                    + this.random.nextFloat() * 0.28F;
        }
        this.setColor(
                0.095F + this.random.nextFloat() * 0.055F,
                0.030F + this.random.nextFloat() * 0.030F,
                0.012F + this.random.nextFloat() * 0.018F);
        this.setAlpha(MAX_ALPHA);
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float remaining = 1.0F - Mth.clamp(
                this.age / (float) this.lifetime, 0.0F, 1.0F);
        float fade = Mth.clamp(remaining / 0.32F, 0.0F, 1.0F);
        this.setAlpha(MAX_ALPHA * fade);
        this.quadSize += 0.00055F;
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
        float size = getQuadSize(partialTick);
        int light = getLightColor(partialTick);

        renderLobe(vertexConsumer, renderX, renderY, renderZ,
  size * 1.22F, size * 0.82F, puddleRotation,
  light, 1.0F);
        renderLobe(vertexConsumer,
  renderX + Mth.cos(firstLobeAngle) * size * firstLobeDistance,
  renderY + 0.0004F,
  renderZ + Mth.sin(firstLobeAngle) * size * firstLobeDistance,
  size * 0.76F, size * 0.57F,
  puddleRotation + 0.68F, light, 0.92F);
        renderLobe(vertexConsumer,
  renderX + Mth.cos(secondLobeAngle) * size * secondLobeDistance,
  renderY + 0.0008F,
  renderZ + Mth.sin(secondLobeAngle) * size * secondLobeDistance,
  size * 0.63F, size * 0.50F,
  puddleRotation - 0.54F, light, 0.86F);
    }

    private void renderLobe(VertexConsumer vertexConsumer,
            float centerX, float centerY, float centerZ,
            float radiusX, float radiusZ, float rotation,
            int light, float alphaMultiplier) {
        float cos = Mth.cos(rotation);
        float sin = Mth.sin(rotation);
        float x0 = centerX + (-radiusX * cos + radiusZ * sin);
        float z0 = centerZ + (-radiusX * sin - radiusZ * cos);
        float x1 = centerX + (-radiusX * cos - radiusZ * sin);
        float z1 = centerZ + (-radiusX * sin + radiusZ * cos);
        float x2 = centerX + (radiusX * cos - radiusZ * sin);
        float z2 = centerZ + (radiusX * sin + radiusZ * cos);
        float x3 = centerX + (radiusX * cos + radiusZ * sin);
        float z3 = centerZ + (radiusX * sin - radiusZ * cos);
        float renderedAlpha = alpha * alphaMultiplier;

        vertexConsumer.vertex(x0, centerY, z0).uv(getU1(), getV1())
  .color(rCol, gCol, bCol, renderedAlpha).uv2(light).endVertex();
        vertexConsumer.vertex(x1, centerY, z1).uv(getU1(), getV0())
  .color(rCol, gCol, bCol, renderedAlpha).uv2(light).endVertex();
        vertexConsumer.vertex(x2, centerY, z2).uv(getU0(), getV0())
  .color(rCol, gCol, bCol, renderedAlpha).uv2(light).endVertex();
        vertexConsumer.vertex(x3, centerY, z3).uv(getU0(), getV1())
  .color(rCol, gCol, bCol, renderedAlpha).uv2(light).endVertex();
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
