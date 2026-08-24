package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.Scp1176BlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Item renderer preserving the authored Blockbench display transforms. */
public final class Scp1176ItemRenderer extends GeoItemRenderer<Scp1176BlockItem> {
    public Scp1176ItemRenderer() {
        super(new Scp1176ItemGeoModel());
    }

    @Override
    public RenderType getRenderType(Scp1176BlockItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture, true);
    }
}
