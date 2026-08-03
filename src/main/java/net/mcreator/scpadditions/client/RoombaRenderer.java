package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.entity.RoombaEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class RoombaRenderer extends GeoEntityRenderer<RoombaEntity> {
    public RoombaRenderer(EntityRendererProvider.Context context) {
        super(context, new RoombaModel());
        shadowRadius = 0.28F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public RenderType getRenderType(RoombaEntity animatable,
                                    ResourceLocation texture,
                                    MultiBufferSource bufferSource,
                                    float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
