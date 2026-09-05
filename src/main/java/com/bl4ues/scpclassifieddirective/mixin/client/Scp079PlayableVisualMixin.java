package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraViewGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces first-pass SCP-079 presentation/input while preserving gameplay state. */
@Mixin(Scp079PlayableClient.class)
public abstract class Scp079PlayableVisualMixin {
    @Shadow private static boolean cursorReleased;
    @Shadow private static float frozenYaw;
    @Shadow private static float frozenPitch;
    @Shadow private static float baseYaw;
    @Shadow private static float basePitch;
    @Shadow private static float yawLimit;
    @Shadow private static float minPitch;
    @Shadow private static float maxPitch;
    @Shadow private static Vec3 cameraPosition;
    @Shadow private static ArmorStand cameraRig;

    @Inject(method = "onClientTickStart", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$inventoryRouting(
            TickEvent.ClientTickEvent event, CallbackInfo ci) {
        Scp079PlayableVisualsV2.handleInventoryKey(event);
        ci.cancel();
    }

    @Inject(method = "renderRecognition", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$screenRecognition(
            RenderLevelStageEvent event, CallbackInfo ci) {
        Scp079PlayableVisualsV2.captureRecognition(event);
        ci.cancel();
    }

    @Inject(method = "renderCameraHud", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$cameraHud(
            GuiGraphics graphics, CallbackInfo ci) {
        Scp079PlayableVisualsV2.renderCameraHud(graphics);
        ci.cancel();
    }

    @Inject(method = "renderLocalHud", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$localHud(
            GuiGraphics graphics, CallbackInfo ci) {
        Scp079PlayableVisualsV2.renderLocalHud(graphics);
        ci.cancel();
    }

    @Inject(method = "consumeCameraActions", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$disableLegacyAimClicks(
            Minecraft minecraft, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Camera control is always direct mouse-look. Shift no longer releases a
     * free cursor over the feed; the cursor only exists while an actual SCP-079
     * Screen is open, such as the surveillance map or leave-role confirmation.
     */
    @Inject(method = "handleShiftCursor", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$keepCameraMouseCaptured(
            Minecraft minecraft, CallbackInfo ci) {
        if (cursorReleased) {
            cursorReleased = false;
            if (minecraft.screen == null && minecraft.isWindowActive()) {
                minecraft.mouseHandler.grabMouse();
            }
        }
        ci.cancel();
    }

    /**
     * The surveillance viewpoint follows the authored pan/tilt pivot instead
     * of rotating around a fixed point. The neutral camera is mounted fifteen
     * degrees downward, and the eye moves on the same small arc as the lens.
     */
    @Inject(method = "updateFeedCamera", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$cameraLensView(
            Minecraft minecraft, CallbackInfo ci) {
        if (minecraft.player == null || cameraRig == null) return;

        float playerYaw = minecraft.player.getYRot();
        float deltaYaw = Mth.wrapDegrees(playerYaw - baseYaw);
        float yaw = baseYaw + Mth.clamp(deltaYaw, -yawLimit, yawLimit);
        float logicalPitch = Mth.clamp(minecraft.player.getXRot(),
                minPitch, maxPitch);

        if (!cursorReleased) {
            minecraft.player.setYRot(yaw);
            minecraft.player.setXRot(logicalPitch);
            minecraft.player.yRotO = yaw;
            minecraft.player.xRotO = logicalPitch;
        } else {
            yaw = frozenYaw;
            logicalPitch = frozenPitch;
        }

        float renderedPitch = logicalPitch
                + SurveillanceCameraViewGeometry.DEFAULT_DOWN_PITCH;
        Vec3 lens = SurveillanceCameraViewGeometry.lensFromBaseEye(
                cameraPosition, baseYaw, basePitch, yaw, renderedPitch);

        cameraRig.setPos(lens.x, lens.y, lens.z);
        cameraRig.setYRot(yaw);
        cameraRig.setXRot(renderedPitch);
        cameraRig.yRotO = yaw;
        cameraRig.xRotO = renderedPitch;
        ci.cancel();
    }

    @Redirect(method = "updateLocalCamera",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;lerp(DDD)D"),
            remap = false)
    private static double scpclassifieddirective$swapOrbitDistances(
            double delta, double start, double end) {
        return Mth.lerp(delta, end, start);
    }
}
