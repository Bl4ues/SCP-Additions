package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.DecontaminationBlockItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** One translucent-capable pass keeps opaque/cutout texels solid while allowing the window alpha. */
public final class DecontaminationItemRenderer
        extends GeoItemRenderer<DecontaminationBlockItem> {
    public DecontaminationItemRenderer() {
        super(new DecontaminationItemGeoModel());
    }

    @Override
    public RenderType getRenderType(DecontaminationBlockItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
}
