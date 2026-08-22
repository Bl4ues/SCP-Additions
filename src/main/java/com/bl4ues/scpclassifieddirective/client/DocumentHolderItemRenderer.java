package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.facility.DocumentHolderBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Double-sided item renderer for the Document Holder. */
public final class DocumentHolderItemRenderer
        extends GeoItemRenderer<DocumentHolderBlockItem> {
    public DocumentHolderItemRenderer() {
        super(new DocumentHolderItemGeoModel());
    }

    @Override
    public RenderType getRenderType(DocumentHolderBlockItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture, true);
    }
}
