package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp1176BlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Client renderer for SCP-1176.
 *
 * Solid geometry, translucent wall markings and the honey surface are kept in
 * separate render passes. The block glyph pass deliberately stays on the main
 * atlas because binding a second translucent texture in the GeckoLib block
 * renderer can make the complete block-entity model disappear with shader
 * pipelines. Honey still uses its dedicated alias and PBR maps. The item
 * renderer may safely use the glyph-only alias because it follows a different
 * rendering path.
 */
public final class Scp1176Client {
    private static final float HONEY_ALPHA = 0.72F;
    private static final int CANISTER_PICTOGRAM_CUBE = 1;

    private Scp1176Client() {
    }

    @Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    ScpClassifiedDirectiveModBlockEntities.SCP_1176.get(),
                    Renderer::new);
        }
    }

    private static final class Model extends GeoModel<Scp1176BlockEntity> {
        private static final ResourceLocation MODEL = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "geo/block/scp1176.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "textures/block/scp1176.png");
        private static final ResourceLocation HONEY_TEXTURE = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID,
                "textures/block/scp1176_honey.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID,
                "animations/block/scp1176.animation.json");

        @Override
        public ResourceLocation getModelResource(Scp1176BlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(Scp1176BlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(Scp1176BlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(Scp1176BlockEntity animatable,
                long instanceId,
                AnimationState<Scp1176BlockEntity> animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);
            prepareOpaquePass();
        }

        private void prepareOpaquePass() {
            setHidden("bb_main", true);
            setHidden("glyphs", true);
            setHidden("sarc", false);
            setHidden("1176", false);
            setHidden("lid", false);
            setHidden("2", false);
            setHidden("faucet", false);
            setHidden("bone", false);
        }

        private void prepareGlyphPass() {
            setHidden("bb_main", true);
            setHidden("glyphs", false);
            setHidden("sarc", true);
            setHidden("1176", true);
            setHidden("lid", true);
            setHidden("2", true);
            setHidden("faucet", true);
            // The green canister body lives in this same authored bone. The
            // renderer filters it so only its second cube, the triangular
            // pictogram plane, participates in this translucent pass.
            setHidden("bone", false);
        }

        private void prepareHoneyPass() {
            setHidden("bb_main", false);
            setHidden("glyphs", true);
            setHidden("sarc", true);
            setHidden("1176", true);
            setHidden("lid", true);
            setHidden("2", true);
            setHidden("faucet", true);
            setHidden("bone", true);
        }

        private void setHidden(String name, boolean hidden) {
            CoreGeoBone bone = getAnimationProcessor().getBone(name);
            if (bone != null) bone.setHidden(hidden);
        }
    }

    private static final class Renderer extends GeoBlockRenderer<Scp1176BlockEntity> {
        private final Model scpModel;
        private boolean renderingGlyphLayer;
        private boolean renderingHoney;

        private Renderer(BlockEntityRendererProvider.Context context) {
            this(new Model());
        }

        private Renderer(Model model) {
            super(model);
            this.scpModel = model;

            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        Scp1176BlockEntity animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    scpModel.prepareGlyphPass();
                    renderingGlyphLayer = true;
                    try {
                        RenderType glyphs = RenderType.entityTranslucent(
                                Model.TEXTURE, true);
                        getRenderer().reRender(bakedModel, poseStack, bufferSource,
                                animatable, glyphs,
                                bufferSource.getBuffer(glyphs), partialTick,
                                packedLight, packedOverlay,
                                1.0F, 1.0F, 1.0F, 1.0F);
                    } finally {
                        renderingGlyphLayer = false;
                        scpModel.prepareOpaquePass();
                    }
                }
            });

            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        Scp1176BlockEntity animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    scpModel.prepareHoneyPass();
                    renderingHoney = true;
                    try {
                        RenderType honey = RenderType.entityTranslucent(
                                Model.HONEY_TEXTURE, true);
                        getRenderer().reRender(bakedModel, poseStack, bufferSource,
                                animatable, honey,
                                bufferSource.getBuffer(honey), partialTick,
                                packedLight, packedOverlay,
                                1.0F, 1.0F, 1.0F, HONEY_ALPHA);
                    } finally {
                        renderingHoney = false;
                        scpModel.prepareOpaquePass();
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

            // The second cube in the authored canister bone is the zero-depth
            // warning pictogram. Keep it out of the opaque pass and render it
            // with the same alpha-preserving pass as the wall glyphs.
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
            // bb_main is a zero-height cube. Only its local UP quad is the
            // physical honey surface; DOWN is coplanar and the side quads are
            // degenerate. Testing the authored/local normal keeps this correct
            // even after the block has been rotated by its facing.
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
        public RenderType getRenderType(Scp1176BlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(Scp1176BlockEntity blockEntity) {
            return true;
        }
    }
}
