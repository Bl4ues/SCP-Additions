package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/** Renders the authored shell while leaving the live screen to its emissive pass. */
public final class InventoryPdaRenderer extends GeoObjectRenderer<InventoryPdaAnimatable> {
    public static final InventoryPdaRenderer INSTANCE = new InventoryPdaRenderer();

    private InventoryPdaRenderer() {
        super(new InventoryPdaGeoModel());
    }

    public void render(PoseStack poseStack, int packedLight) {
        renderInternal(poseStack, false, packedLight);
    }

    public void renderThirdPerson(PoseStack poseStack, int packedLight) {
        renderInternal(poseStack, true, packedLight);
    }

    private void renderInternal(PoseStack poseStack, boolean thirdPerson,
            int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ((InventoryPdaGeoModel) getGeoModel()).setThirdPerson(thirdPerson);
        render(poseStack, InventoryPdaAnimatable.INSTANCE, buffers,
                RenderType.entityCutoutNoCull(getTextureLocation(InventoryPdaAnimatable.INSTANCE)),
                null, packedLight);
        buffers.endBatch();
        ((InventoryPdaGeoModel) getGeoModel()).setThirdPerson(false);
    }
}
