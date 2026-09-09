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
    /* Same corrected visible CRT center used by the attached world renderer. */
    private static final double SCREEN_X = -0.03D / 16.0D;
    private static final double SCREEN_Y = 7.80085677D / 16.0D;
    private static final double SCREEN_Z = 1.26063520D / 16.0D;
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
            /*
             * GeoItemRenderer already applies its own item-space centering.
             * The old NONE-only -0.5 translation applied a second compensation,
             * moving the attached body away from the authored world-space CRT.
             */
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
        /*
         * Face the same visible side as the world-space CRT. Local +Z after the
         * -22.5 degree screen tilt is the viewer normal; the previous extra 180
         * degree yaw placed the countdown on the back of the device.
         */
        poseStack.translate(SCREEN_X, SCREEN_Y, SCREEN_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
        poseStack.translate(0.0D, 0.0D, 0.0015D);
        poseStack.scale(-SCREEN_SCALE, -SCREEN_SCALE, SCREEN_SCALE);
        poseStack.translate(-HackingDeviceScreenTextClient.LOGICAL_WIDTH * 0.5F,
                -HackingDeviceScreenTextClient.LOGICAL_HEIGHT * 0.5F, 0.0F);
        HackingDeviceScreenTextClient.renderItemCooldown(renderedStack,
                minecraft.font, poseStack, bufferSource);
        poseStack.popPose();
    }
}
