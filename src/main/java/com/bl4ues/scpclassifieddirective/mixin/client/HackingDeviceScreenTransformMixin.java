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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
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
    /* 0.04 model pixel: visually flush, but unambiguously in front of the plane. */
    private static final double TEXT_EPSILON = 0.04D / 16.0D;
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
        Vec3 surfaceCenter = frame.center()
                .add(frame.outward().scale(seating));
        Vec3 textCenter = surfaceCenter
                .add(frame.outward().scale(TEXT_EPSILON))
                .subtract(camera);

        /*
         * Do not reconstruct the CRT with yaw/pitch a second time. The Frame is
         * already the transformed physical plane used by the renderer and camera.
         * Mapping font axes straight onto that basis prevents the previous
         * "text floating in a wall / inside the model" failures caused by a
         * second, slightly different transform chain.
         *
         * Font +X = screen-right. Font +Y runs downward, hence -frame.up().
         */
        Vec3 right = frame.right().normalize();
        Vec3 down = frame.up().scale(-1.0D).normalize();
        Vec3 normal = frame.outward().normalize();

        Matrix4f physicalScreen = new Matrix4f().identity();
        // JOML local X/Y/Z axes are columns 0/1/2; translation is column 3.
        physicalScreen.m00((float) right.x);
        physicalScreen.m01((float) right.y);
        physicalScreen.m02((float) right.z);
        physicalScreen.m10((float) down.x);
        physicalScreen.m11((float) down.y);
        physicalScreen.m12((float) down.z);
        physicalScreen.m20((float) normal.x);
        physicalScreen.m21((float) normal.y);
        physicalScreen.m22((float) normal.z);
        physicalScreen.m30((float) textCenter.x);
        physicalScreen.m31((float) textCenter.y);
        physicalScreen.m32((float) textCenter.z);

        poseStack.pushPose();
        poseStack.mulPoseMatrix(physicalScreen);
        poseStack.scale(SCALE, SCALE, SCALE);
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
                Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);
    }
}
