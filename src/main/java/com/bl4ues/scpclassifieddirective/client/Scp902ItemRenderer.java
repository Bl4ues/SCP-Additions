package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.Scp902BlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Item renderer preserving the authored Blockbench display transforms. */
public final class Scp902ItemRenderer extends GeoItemRenderer<Scp902BlockItem> {
    public Scp902ItemRenderer() {
        super(new Scp902ItemGeoModel());
    }

    @Override
    public RenderType getRenderType(Scp902BlockItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
