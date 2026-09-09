package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceAttachedRenderer;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceClientState;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceScreenTextClient;
import com.bl4ues.scpclassifieddirective.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry;
import com.bl4ues.scpclassifieddirective.client.render.HackingDeviceAttachmentGeometry.Attachment;
import com.bl4ues.scpclassifieddirective.client.render.PhysicalBlockScreenGeometry.Frame;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders only the emissive character layer on the model's authored CRT plane.
 * The black screen in the Gecko model remains the sole background.
 */
@Mixin(value = HackingDeviceAttachedRenderer.class, remap = false)
public abstract class HackingDeviceScreenTransformMixin {
    private static final double TEXT_EPSILON = 0.00125D;
    private static final float SCALE = (float)
            (HackingDeviceAttachmentGeometry.SCREEN_WIDTH
                    / HackingDeviceScreenTextClient.LOGICAL_WIDTH);

    @Inject(method = "renderScreen", at = @At("HEAD"), cancellable = true)
    private static void scpclassifieddirective$renderOnPhysicalPlane(
            Minecraft minecraft, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers, Vec3 camera, BlockPos pos,
            Attachment attachment, CallbackInfo ci) {
        ci.cancel();
        if (minecraft == null || minecraft.font == null || pos == null
                || attachment == null || camera == null) {
            return;
        }

        double seating = HackingDeviceClientState.seatingOffset(pos);
        Frame frame = attachment.screen();
        Vec3 center = frame.center()
                .add(frame.outward().scale(seating + TEXT_EPSILON))
                .subtract(camera);

        /*
         * This is the exact transform of the authored screen cube:
         * body Y = 180 degrees, then screen X = -22.5 degrees. Rebuilding the
         * plane from a hand-written Matrix4f introduced an axis convention bug
         * that sent the glyphs away from the CRT even though the camera itself
         * was correct. Using the authored rotations keeps body, screen and text
         * in one coordinate system.
         */
        poseStack.pushPose();
        poseStack.translate(center.x, center.y, center.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                HackingDeviceAttachmentGeometry.modelYaw(attachment.facing())
                        + 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));

        // Font +X goes to viewer-right and font +Y goes downward on the CRT.
        poseStack.scale(SCALE, -SCALE, SCALE);
        poseStack.translate(
                -HackingDeviceScreenTextClient.LOGICAL_WIDTH * 0.5F,
                -HackingDeviceScreenTextClient.LOGICAL_HEIGHT * 0.5F,
                0.0F);

        if (HackingDeviceMinigameClient.active()
                && pos.equals(HackingDeviceMinigameClient.pos())) {
            HackingDeviceAttachedRendererInvoker
                    .scpclassifieddirective$renderSession(
                            minecraft.font, poseStack, buffers);
        } else {
            drawStandby(minecraft.font, poseStack, buffers);
        }
        poseStack.popPose();
    }

    private static void drawStandby(Font font, PoseStack poseStack,
            MultiBufferSource.BufferSource buffers) {
        var text = ScpFonts.anonymousPro("CI FIELD UNIT // STANDBY")
                .getVisualOrderText();
        font.drawInBatch(text, 10.0F, 68.0F,
                HackingDeviceScreenTextClient.GREEN_DIM, false,
                poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
    }
}
