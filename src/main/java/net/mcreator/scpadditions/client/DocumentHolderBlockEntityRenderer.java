package net.mcreator.scpadditions.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.facility.DocumentHolderBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Double-sided renderer for the thin authored Document Holder geometry. */
public final class DocumentHolderBlockEntityRenderer
        extends GeoBlockRenderer<DocumentHolderBlockEntity> {
    public DocumentHolderBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        super(new DocumentHolderGeoModel());
    }

    @Override
    public RenderType getRenderType(DocumentHolderBlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityTranslucent(texture, true);
    }
}
