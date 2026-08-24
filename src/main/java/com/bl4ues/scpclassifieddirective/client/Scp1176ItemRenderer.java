package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.Scp1176BlockItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Item renderer preserving the authored Blockbench display transforms. */
public final class Scp1176ItemRenderer extends GeoItemRenderer<Scp1176BlockItem> {
    private static final float HONEY_ALPHA = 0.72F;
    private static final int CANISTER_PICTOGRAM_CUBE = 1;
    private final Scp1176ItemGeoModel scpModel;
    private boolean renderingGlyphLayer;
    private boolean renderingHoney;

    public Scp1176ItemRenderer() {
        this(new Scp1176ItemGeoModel());
    }

    private Scp1176ItemRenderer(Scp1176ItemGeoModel model) {
        super(model);
        this.scpModel = model;

        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    Scp1176BlockItem animatable,
                    BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                scpModel.showGlyphGeometry();
                renderingGlyphLayer = true;
                try {
                    RenderType glyphs = RenderType.entityTranslucent(
                            Scp1176ItemGeoModel.TEXTURE, true);
                    getRenderer().reRender(bakedModel, poseStack, bufferSource,
                            animatable, glyphs,
                            bufferSource.getBuffer(glyphs), partialTick,
                            packedLight, packedOverlay,
                            1.0F, 1.0F, 1.0F, 1.0F);
                } finally {
                    renderingGlyphLayer = false;
                    scpModel.showSolidGeometry();
                }
            }
        });

        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    Scp1176BlockItem animatable,
                    BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                scpModel.showHoneyGeometry();
                renderingHoney = true;
                try {
                    RenderType honey = RenderType.entityTranslucent(
                            Scp1176ItemGeoModel.HONEY_TEXTURE, true);
                    getRenderer().reRender(bakedModel, poseStack, bufferSource,
                            animatable, honey,
                            bufferSource.getBuffer(honey), partialTick,
                            packedLight, packedOverlay,
                            1.0F, 1.0F, 1.0F, HONEY_ALPHA);
                } finally {
                    renderingHoney = false;
                    scpModel.showSolidGeometry();
                }
            }
        });
    }

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone,
            VertexConsumer buffer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        if (!"bone".equals(bone.getName()) || bone.isHidden()) {
            super.renderCubesOfBone(poseStack, bone, buffer, packedLight,
                    packedOverlay, red, green, blue, alpha);
            return;
        }

        for (int index = 0; index < bone.getCubes().size(); index++) {
            boolean pictogram = index == CANISTER_PICTOGRAM_CUBE;
            if (pictogram != renderingGlyphLayer) continue;
            GeoCube cube = bone.getCubes().get(index);
            poseStack.pushPose();
            renderCube(poseStack, cube, buffer, packedLight, packedOverlay,
                    red, green, blue, alpha);
            poseStack.popPose();
        }
    }

    @Override
    public void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState,
            Vector3f normal, VertexConsumer buffer, int packedLight,
            int packedOverlay, float red, float green, float blue,
            float alpha) {
        // The honey geometry is a zero-height cube. Render only its authored
        // local UP face so the item cannot blend a coplanar DOWN quad over it.
        // Local normal is used rather than the transformed normal because item
        // display transforms freely rotate the entire model.
        if (renderingHoney && quad.normal().y() < 0.5F) return;

        for (GeoVertex vertex : quad.vertices()) {
            Vector3f position = vertex.position();
            Vector4f transformed = poseState.transform(new Vector4f(
                    position.x(), position.y(), position.z(), 1.0F));
            buffer.vertex(transformed.x(), transformed.y(), transformed.z(),
                    red, green, blue, alpha, vertex.texU(), vertex.texV(),
                    packedOverlay, packedLight,
                    normal.x(), normal.y(), normal.z());
        }
    }

    @Override
    public RenderType getRenderType(Scp1176BlockItem animatable,
            ResourceLocation texture, MultiBufferSource bufferSource,
            float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
