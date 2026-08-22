package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.DocumentHolderBlockEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Renders opaque holder/document geometry first, then the glass door. Keeping
 * the glass in its own final translucent pass prevents angle-dependent depth
 * sorting from hiding the stored document behind it.
 */
public final class DocumentHolderBlockEntityRenderer
        extends GeoBlockRenderer<DocumentHolderBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/block/document_holder.png");

    private final DocumentHolderGeoModel holderModel;

    public DocumentHolderBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {
        this(new DocumentHolderGeoModel());
    }

    private DocumentHolderBlockEntityRenderer(DocumentHolderGeoModel model) {
        super(model);
        this.holderModel = model;

        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    DocumentHolderBlockEntity animatable,
                    BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                holderModel.prepareGlassPass();
                try {
                    RenderType glass = RenderType.entityTranslucent(
                            TEXTURE, true);
                    getRenderer().reRender(bakedModel, poseStack, bufferSource,
                            animatable, glass,
                            bufferSource.getBuffer(glass), partialTick,
                            packedLight, packedOverlay,
                            1.0F, 1.0F, 1.0F, 1.0F);
                } finally {
                    holderModel.prepareOpaquePass();
                }
            }
        });
    }

    @Override
    public RenderType getRenderType(DocumentHolderBlockEntity animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
