package net.mcreator.scpadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * GeckoLib emissive layer that uses the vanilla eyes pass, preserving shader
 * bloom without placing the glowmask in the normal texture pass.
 */
public final class ShaderCompatibleGlowLayer<T extends GeoAnimatable>
        extends GeoRenderLayer<T> {
    private static final int FULL_BRIGHT = 0xF000F0;

    private final ResourceLocation glowmask;

    public ShaderCompatibleGlowLayer(GeoRenderer<T> renderer,
            ResourceLocation baseTexture) {
        this(renderer, glowmaskFor(baseTexture), true);
    }

    public static <T extends GeoAnimatable> ShaderCompatibleGlowLayer<T> explicit(
            GeoRenderer<T> renderer, ResourceLocation glowmask) {
        return new ShaderCompatibleGlowLayer<>(renderer, glowmask, true);
    }

    private ShaderCompatibleGlowLayer(GeoRenderer<T> renderer,
            ResourceLocation texture, boolean ignored) {
        super(renderer);
        this.glowmask = texture;
    }

    private static ResourceLocation glowmaskFor(ResourceLocation baseTexture) {
        String path = baseTexture.getPath();
        String glowPath = path.endsWith(".png")
                ? path.substring(0, path.length() - 4) + "_e.png"
                : path + "_e.png";
        return new ResourceLocation(baseTexture.getNamespace(), glowPath);
    }

    @Override
    public void render(PoseStack poseStack, T animatable,
            BakedGeoModel bakedModel, RenderType renderType,
            MultiBufferSource bufferSource, VertexConsumer buffer,
            float partialTick, int packedLight, int packedOverlay) {
        RenderType emissive = RenderType.eyes(glowmask);
        getRenderer().reRender(bakedModel, poseStack, bufferSource,
                animatable, emissive, bufferSource.getBuffer(emissive),
                partialTick, FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
    }
}
