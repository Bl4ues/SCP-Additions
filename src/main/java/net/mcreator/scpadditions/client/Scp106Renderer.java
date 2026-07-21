package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.entity.Scp106Entity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp106Renderer extends GeoEntityRenderer<Scp106Entity> {
    public Scp106Renderer(EntityRendererProvider.Context context) {
        super(context, new Scp106Model<>());
        this.shadowRadius = 0.45F;
    }

    @Override
    public RenderType getRenderType(Scp106Entity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
