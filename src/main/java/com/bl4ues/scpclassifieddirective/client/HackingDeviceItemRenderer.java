package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class HackingDeviceItemRenderer
        extends GeoItemRenderer<HackingDeviceItem> {
    public HackingDeviceItemRenderer() {
        super(new HackingDeviceGeoModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
            PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, int packedOverlay) {
        boolean rawWorldPlacement = displayContext == ItemDisplayContext.NONE;
        if (rawWorldPlacement) {
            poseStack.pushPose();
            /*
             * GeoItemRenderer centers item models on half-block coordinates.
             * The attached-device world geometry is authored around Blockbench's
             * actual zero, so cancel that item-only centering before rendering it.
             * Without this the model sits roughly half a block above/outside the
             * physical screen frame used by the camera.
             */
            poseStack.translate(-0.5D, -0.5D, -0.5D);
        }
        try {
            super.renderByItem(stack, displayContext, poseStack, bufferSource,
                    packedLight, packedOverlay);
        } finally {
            if (rawWorldPlacement) poseStack.popPose();
        }
    }
}
