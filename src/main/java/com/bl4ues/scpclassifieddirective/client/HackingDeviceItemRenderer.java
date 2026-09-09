package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class HackingDeviceItemRenderer
        extends GeoItemRenderer<HackingDeviceItem> {
    /* Exact transformed center of the authored `screen` plane. */
    private static final double SCREEN_X = -0.03D / 16.0D;
    private static final double SCREEN_Y = 7.41817334D / 16.0D;
    private static final double SCREEN_Z = 1.16496434D / 16.0D;
    private static final double SCREEN_TEXT_OFFSET = 0.04D / 16.0D;
    private static final float SCREEN_SCALE = (float)
            ((2.5D / 16.0D) / HackingDeviceScreenTextClient.LOGICAL_WIDTH);

    private ItemStack renderedStack = ItemStack.EMPTY;
    private ItemDisplayContext renderedContext = ItemDisplayContext.NONE;

    public HackingDeviceItemRenderer() {
        super(new HackingDeviceGeoModel());
        addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack,
                    HackingDeviceItem animatable,
                    BakedGeoModel bakedModel, RenderType renderType,
                    MultiBufferSource bufferSource, VertexConsumer buffer,
                    float partialTick, int packedLight, int packedOverlay) {
                renderCooldownScreen(poseStack, bufferSource);
            }
        });
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        renderedStack = stack;
        renderedContext = displayContext;
        try {
            super.renderByItem(stack, displayContext, poseStack, bufferSource,
                    packedLight, packedOverlay);
        } finally {
            renderedStack = ItemStack.EMPTY;
            renderedContext = ItemDisplayContext.NONE;
        }
    }

    private void renderCooldownScreen(PoseStack poseStack,
            MultiBufferSource bufferSource) {
        Minecraft minecraft = Minecraft.getInstance();
        if (renderedContext == ItemDisplayContext.NONE
                || renderedStack.isEmpty() || minecraft.level == null
                || !HackingDeviceItem.isCoolingDown(renderedStack,
                        minecraft.level)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(SCREEN_X, SCREEN_Y, SCREEN_Z);
        /* Match body Y=180 and the authored screen X=-22.5 hierarchy. */
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
        /*
         * Same real separation as the attached CRT. With NORMAL depth-tested
         * glyphs this remains shader-safe and visually flush to the black plane.
         */
        poseStack.translate(0.0D, 0.0D, SCREEN_TEXT_OFFSET);
        poseStack.scale(SCREEN_SCALE, -SCREEN_SCALE, SCREEN_SCALE);
        poseStack.translate(-HackingDeviceScreenTextClient.LOGICAL_WIDTH * 0.5F,
                -HackingDeviceScreenTextClient.LOGICAL_HEIGHT * 0.5F, 0.0F);
        HackingDeviceScreenTextClient.renderItemCooldown(renderedStack,
                minecraft.font, poseStack, bufferSource);
        poseStack.popPose();
    }
}
