package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.entity.Scp173Entity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp173Renderer extends GeoEntityRenderer<Scp173Entity> {
    public Scp173Renderer(EntityRendererProvider.Context context) {
        super(context, new Scp173Model<>());
        this.shadowRadius = 0.45F;
        this.scaleHeight = 0.939F;
    }

    @Override
    public RenderType getRenderType(Scp173Entity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
