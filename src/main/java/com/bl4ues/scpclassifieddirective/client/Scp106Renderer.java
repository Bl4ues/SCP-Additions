package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp106Renderer extends GeoEntityRenderer<Scp106Entity> {
    public Scp106Renderer(EntityRendererProvider.Context context) {
        super(context, new Scp106Model<>());
        this.shadowRadius = 0.45F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(Scp106Entity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
