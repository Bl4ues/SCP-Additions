package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.block.entity.Scp902BlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** World renderer for the authored SCP-902 GeckoLib model. */
public final class Scp902BlockEntityRenderer
        extends GeoBlockRenderer<Scp902BlockEntity> {
    public Scp902BlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(new Scp902GeoModel());
    }

    @Override
    public RenderType getRenderType(Scp902BlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
