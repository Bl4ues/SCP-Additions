package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Renders the observer-facing PDA inside the player's body-local pose. */
public final class InventoryPdaPlayerLayer extends
        RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float MODEL_SCALE = 0.095F;
    private static final float GEO_CENTER_Y = 43.0F / 16.0F;

    public InventoryPdaPlayerLayer(RenderLayerParent<AbstractClientPlayer,
            PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers,
            int packedLight, AbstractClientPlayer player, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch) {
        float progress = InventoryPdaThirdPersonClient.progress(player);
        if (progress <= 0.0F) return;
        if (player == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType()
                        .isFirstPerson()) {
            return;
        }

        poseStack.pushPose();
        // Keep the final two-handed pose unchanged, but begin the draw beside
        // the right hand at chest height. The previous hidden pose started down
        // at the legs and made the PDA visibly fly upward before being grabbed.
        float startY = player.isCrouching() ? 0.50F : 0.42F;
        float heldY = player.isCrouching() ? 0.38F : 0.30F;
        poseStack.translate(
                lerp(0.20F, 0.0F, progress),
                lerp(startY, heldY, progress),
                lerp(-0.48F, -0.72F, progress));
        poseStack.mulPose(Axis.YP.rotationDegrees(
                lerp(205.0F, 180.0F, progress)));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                lerp(11.0F, 15.0F, progress)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                lerp(-50.0F, -90.0F, progress)));
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        // GeoObjectRenderer adds (0.5, 0.51, 0.5) before rendering. Offset the
        // authored display center so the device center meets the hand pose.
        poseStack.translate(-0.5F, -(0.51F + GEO_CENTER_Y), 0.0F);
        InventoryPdaRenderer.INSTANCE.renderThirdPerson(poseStack,
                packedLight);
        poseStack.popPose();
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }
}
