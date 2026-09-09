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
 * Keeps characters on the model's actual screen plane instead of reconstructing
 * that plane with yaw/pitch guesses. The authored black screen texture remains
 * the background; only emissive characters are added above it.
 */
@Mixin(value = HackingDeviceAttachedRenderer.class, remap = false)
public abstract class HackingDeviceScreenTransformMixin {
    private static final double TEXT_EPSILON = 0.0015D;
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
        Frame source = attachment.screen();
        Frame frame = new Frame(
                source.center().add(source.outward().scale(seating)),
                source.right(), source.up(), source.outward(),
                source.width(), source.height());

        /*
         * The focus camera deliberately locks itself to the side of the CRT the
         * player was already standing on. The previous text pass always used the
         * authored +OUTWARD side, so whenever the camera selected the opposite
         * face the glyphs were rendered behind the zero-thickness black plane and
         * vanished (or appeared detached from the device at grazing angles).
         *
         * Choose the visible normal from the actual render camera every frame.
         * Flip screen-right with it so text remains readable instead of mirrored.
         */
        Vec3 authoredOutward = frame.outward().normalize();
        double side = camera.subtract(frame.center()).dot(authoredOutward);
        double faceSign = side >= 0.0D ? 1.0D : -1.0D;
        Vec3 visibleOutward = authoredOutward.scale(faceSign);
        Vec3 right = frame.right().normalize().scale(faceSign);
        Vec3 down = frame.up().normalize().scale(-1.0D);

        Vec3 origin = frame.center()
                .add(visibleOutward.scale(TEXT_EPSILON))
                .subtract(camera);

        Matrix4f basis = new Matrix4f().identity();
        basis.m00((float) right.x);
        basis.m01((float) right.y);
        basis.m02((float) right.z);
        basis.m10((float) down.x);
        basis.m11((float) down.y);
        basis.m12((float) down.z);
        basis.m20((float) visibleOutward.x);
        basis.m21((float) visibleOutward.y);
        basis.m22((float) visibleOutward.z);

        poseStack.pushPose();
        poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.mulPoseMatrix(basis);
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
                Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
    }
}
