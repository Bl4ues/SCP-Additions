package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.DoorBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.DoorItem;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.Family;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** GeckoLib block/item rendering for the rebuilt facility door family. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FacilityGeckoDoorClient {
    private static final ResourceLocation WORKSHOP_PANEL = id(
            "textures/block/workshop.png");

    private FacilityGeckoDoorClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                FacilityGeckoDoorModule.BLOCK_ENTITY.get(),
                BlockRenderer::new);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private static String asset(Family family) {
        return switch (family) {
            case FACILITY -> "facility_door";
            case LOGISTICS_LEFT -> "logistics_door_left";
            case LOGISTICS_RIGHT -> "logistics_door_right";
            case OFFICE -> "office_door";
            case BATHROOM -> "bathroom_door";
            case WORKSHOP -> "workshop_door";
        };
    }

    private static ResourceLocation texture(Family family) {
        return id("textures/block/" + switch (family) {
            case FACILITY -> "normal_door";
            case LOGISTICS_LEFT, LOGISTICS_RIGHT -> "logisticsdoor";
            case OFFICE -> "officedoor";
            case BATHROOM -> "bathroomdoor";
            case WORKSHOP -> "workshopdoor";
        } + ".png");
    }

    private abstract static class DoorModel<T extends GeoAnimatable>
            extends GeoModel<T> {
        private Family activeFamily;

        protected abstract Family family(T animatable);

        @Override
        public ResourceLocation getModelResource(T animatable) {
            return id("geo/block/" + asset(family(animatable))
                    + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return texture(family(animatable));
        }

        @Override
        public ResourceLocation getAnimationResource(T animatable) {
            return id("animations/block/" + asset(family(animatable))
                    + ".animation.json");
        }

        @Override
        public void setCustomAnimations(T animatable, long instanceId,
                AnimationState<T> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            activeFamily = family(animatable);
            prepareBase();
        }

        final void prepareBase() {
            setHidden("door", false);
            setHidden("door_leaf", false);
            setHidden("body", false);
            setHidden("handle", false);
            setHidden("frame", false);
            setHidden("glass", true);
            setHidden("workshop_panel", true);
        }

        final void prepareGlass() {
            if (activeFamily != Family.FACILITY
                    && activeFamily != Family.OFFICE) return;
            setHidden("door", false);
            setHidden("door_leaf", true);
            setHidden("body", true);
            setHidden("handle", true);
            setHidden("frame", true);
            setHidden("glass", false);
            setHidden("workshop_panel", true);
        }

        final void prepareWorkshopPanel() {
            if (activeFamily != Family.WORKSHOP) return;
            setHidden("door", false);
            setHidden("door_leaf", true);
            setHidden("body", true);
            setHidden("handle", true);
            setHidden("frame", true);
            setHidden("glass", true);
            setHidden("workshop_panel", false);
        }

        private void setHidden(String name, boolean hidden) {
            CoreGeoBone bone = getAnimationProcessor().getBone(name);
            if (bone != null) bone.setHidden(hidden);
        }
    }

    private static final class BlockModel
            extends DoorModel<DoorBlockEntity> {
        @Override
        protected Family family(DoorBlockEntity animatable) {
            return animatable.family();
        }
    }

    private static final class ItemModel extends DoorModel<DoorItem> {
        @Override
        protected Family family(DoorItem animatable) {
            return animatable.family();
        }
    }

    public static final class BlockRenderer
            extends GeoBlockRenderer<DoorBlockEntity> {
        private final BlockModel doorModel;

        public BlockRenderer(BlockEntityRendererProvider.Context context) {
            this(new BlockModel());
        }

        private BlockRenderer(BlockModel model) {
            super(model);
            this.doorModel = model;
            addGlassLayer();
            addWorkshopLayer();
        }

        private void addGlassLayer() {
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        DoorBlockEntity animatable, BakedGeoModel bakedModel,
                        RenderType renderType, MultiBufferSource bufferSource,
                        VertexConsumer buffer, float partialTick,
                        int packedLight, int packedOverlay) {
                    Family family = animatable.family();
                    if (family != Family.FACILITY
                            && family != Family.OFFICE) return;
                    doorModel.prepareGlass();
                    try {
                        RenderType glass = RenderType.entityTranslucentCull(
                                texture(family));
                        getRenderer().reRender(bakedModel, poseStack,
                                bufferSource, animatable, glass,
                                bufferSource.getBuffer(glass), partialTick,
                                packedLight, packedOverlay,
                                1.0F, 1.0F, 1.0F, 1.0F);
                    } finally {
                        doorModel.prepareBase();
                    }
                }
            });
        }

        private void addWorkshopLayer() {
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        DoorBlockEntity animatable, BakedGeoModel bakedModel,
                        RenderType renderType, MultiBufferSource bufferSource,
                        VertexConsumer buffer, float partialTick,
                        int packedLight, int packedOverlay) {
                    if (animatable.family() != Family.WORKSHOP) return;
                    doorModel.prepareWorkshopPanel();
                    try {
                        RenderType panel = RenderType.entityCutoutNoCull(
                                WORKSHOP_PANEL);
                        getRenderer().reRender(bakedModel, poseStack,
                                bufferSource, animatable, panel,
                                bufferSource.getBuffer(panel), partialTick,
                                packedLight, packedOverlay,
                                1.0F, 1.0F, 1.0F, 1.0F);
                    } finally {
                        doorModel.prepareBase();
                    }
                }
            });
        }

        @Override
        public RenderType getRenderType(DoorBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(DoorBlockEntity blockEntity) {
            return true;
        }
    }

    public static final class ItemRenderer extends GeoItemRenderer<DoorItem> {
        private final ItemModel doorModel;

        public ItemRenderer() {
            this(new ItemModel());
        }

        private ItemRenderer(ItemModel model) {
            super(model);
            this.doorModel = model;
            addGlassLayer();
            addWorkshopLayer();
        }

        @Override
        public void preRender(PoseStack poseStack, DoorItem animatable,
                BakedGeoModel model, MultiBufferSource bufferSource,
                VertexConsumer buffer, boolean isReRender, float partialTick,
                int packedLight, int packedOverlay, float red, float green,
                float blue, float alpha) {
            super.preRender(poseStack, animatable, model, bufferSource, buffer,
                    isReRender, partialTick, packedLight, packedOverlay,
                    red, green, blue, alpha);
            if (!isReRender && animatable.family() == Family.WORKSHOP
                    && isHandContext(this.renderPerspective)) {
                // Inventory/display keeps the authored 180-degree workshop
                // root. Held views get one extra 180 around the model centre.
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
        }

        private static boolean isHandContext(ItemDisplayContext context) {
            return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                    || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }

        private void addGlassLayer() {
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack, DoorItem animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    Family family = animatable.family();
                    if (family != Family.FACILITY
                            && family != Family.OFFICE) return;
                    doorModel.prepareGlass();
                    try {
                        RenderType glass = RenderType.entityTranslucentCull(
                                texture(family));
                        getRenderer().reRender(bakedModel, poseStack,
                                bufferSource, animatable, glass,
                                bufferSource.getBuffer(glass), partialTick,
                                packedLight, packedOverlay,
                                1.0F, 1.0F, 1.0F, 1.0F);
                    } finally {
                        doorModel.prepareBase();
                    }
                }
            });
        }

        private void addWorkshopLayer() {
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack, DoorItem animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    if (animatable.family() != Family.WORKSHOP) return;
                    doorModel.prepareWorkshopPanel();
                    try {
                        RenderType panel = RenderType.entityCutoutNoCull(
                                WORKSHOP_PANEL);
                        getRenderer().reRender(bakedModel, poseStack,
                                bufferSource, animatable, panel,
                                bufferSource.getBuffer(panel), partialTick,
                                packedLight, packedOverlay,
                                1.0F, 1.0F, 1.0F, 1.0F);
                    } finally {
                        doorModel.prepareBase();
                    }
                }
            });
        }

        @Override
        public RenderType getRenderType(DoorItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
