package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.DoorBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.DoorItem;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.Family;
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
            setHidden("door_leaf", false);
            setHidden("body", false);
            setHidden("handle", false);
            setHidden("frame", false);
            setHidden("workshop_panel", activeFamily == Family.WORKSHOP);
        }

        final void prepareWorkshopPanel() {
            if (activeFamily != Family.WORKSHOP) return;
            setHidden("door_leaf", true);
            setHidden("body", true);
            setHidden("handle", true);
            setHidden("frame", true);
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
            addWorkshopLayer();
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
                        RenderType panel = RenderType.entityTranslucent(
                                WORKSHOP_PANEL, true);
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
            return RenderType.entityTranslucent(texture, true);
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
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack, DoorItem animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    if (animatable.family() != Family.WORKSHOP) return;
                    doorModel.prepareWorkshopPanel();
                    try {
                        RenderType panel = RenderType.entityTranslucent(
                                WORKSHOP_PANEL, true);
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
            return RenderType.entityTranslucent(texture, true);
        }
    }
}
