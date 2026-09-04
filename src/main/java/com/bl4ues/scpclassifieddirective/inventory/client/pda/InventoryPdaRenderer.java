package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/** Renders the authored shell at full brightness; the live GUI is a separate pass. */
public final class InventoryPdaRenderer extends GeoObjectRenderer<InventoryPdaAnimatable> {
    public static final InventoryPdaRenderer INSTANCE = new InventoryPdaRenderer();

    private InventoryPdaRenderer() {
        super(new InventoryPdaGeoModel());
    }

    public void render(PoseStack poseStack) {
        renderInternal(poseStack, false);
    }

    public void renderThirdPerson(PoseStack poseStack) {
        renderInternal(poseStack, true);
    }

    private void renderInternal(PoseStack poseStack, boolean thirdPerson) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        ((InventoryPdaGeoModel) getGeoModel()).setThirdPerson(thirdPerson);
        render(poseStack, InventoryPdaAnimatable.INSTANCE, buffers,
                RenderType.entityCutoutNoCull(getTextureLocation(InventoryPdaAnimatable.INSTANCE)),
                null, LightTexture.FULL_BRIGHT);
        buffers.endBatch();
        ((InventoryPdaGeoModel) getGeoModel()).setThirdPerson(false);
    }
}
