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
 * Network/provider bridge for SCP-106's large phase/emergence portals.
 * The visible puddle is rendered by {@link PbrSurfaceDecalClient} through the
 * named scp_106_puddle texture so its normal/specular LabPBR sidecars are used.
 */
public final class Scp106PortalParticle extends TextureSheetParticle {
    private Scp106PortalParticle(ClientLevel level, double x, double y,
            double z, double normalX, double normalY, double normalZ,
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
        PbrSurfaceDecalClient.addPortal(level, x, y, z,
                normalX, normalY, normalZ);
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera,
            float partialTick) {
        // Intentionally empty; named-texture decal rendering supplies PBR.
    }

    @Override
    public ParticleRenderType getRenderType() {
        // Retain the registered sprite-set bridge while emitting no vertices.
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
                double normalX, double normalY, double normalZ) {
            double normalStrength = Math.sqrt(normalX * normalX
                    + normalY * normalY + normalZ * normalZ);
            if (normalStrength >= 0.98D) return null;
            return new Scp106PortalParticle(level, x, y, z,
                    normalX, normalY, normalZ, sprites);
        }
    }
}
