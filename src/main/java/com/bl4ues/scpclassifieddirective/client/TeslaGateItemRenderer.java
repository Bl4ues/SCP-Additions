package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.TeslaGateBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class TeslaGateItemRenderer extends GeoItemRenderer<TeslaGateBlockItem> {
    public TeslaGateItemRenderer() {
        super(new TeslaGateItemGeoModel());
    }

    @Override
    public RenderType getRenderType(TeslaGateBlockItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
