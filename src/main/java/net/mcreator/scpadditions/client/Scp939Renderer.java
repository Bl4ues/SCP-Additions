package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.entity.Scp939Entity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp939Renderer extends GeoEntityRenderer<Scp939Entity> {
    public Scp939Renderer(EntityRendererProvider.Context context) {
        super(context, new Scp939Model<>());
        this.shadowRadius = 0.55F;
    }

    @Override
    public RenderType getRenderType(Scp939Entity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
