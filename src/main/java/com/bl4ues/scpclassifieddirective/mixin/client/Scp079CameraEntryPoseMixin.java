package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * A player taking over a surveillance camera inherits the angle its physical
 * head was already holding instead of snapping every feed back to its neutral
 * authored pose. Autonomous SCP-079 does not use this client path.
 */
@Mixin(Scp079PlayableClient.class)
public abstract class Scp079CameraEntryPoseMixin {
    @Shadow private static UUID cameraId;
    @Shadow private static Vec3 cameraPosition;
    @Shadow private static float baseYaw;
    @Shadow private static float basePitch;

    private static UUID scpclassifieddirective$lastCameraId;
    private static UUID scpclassifieddirective$pendingCameraId;

    @Inject(method = "receive", at = @At("TAIL"), remap = false)
    private static void scpclassifieddirective$rememberCameraEntry(
            Scp079PlayableNetwork.State state, CallbackInfo ci) {
        if (cameraId != null && !cameraId.equals(
                scpclassifieddirective$lastCameraId)) {
            scpclassifieddirective$pendingCameraId = cameraId;
        } else if (cameraId == null) {
            scpclassifieddirective$pendingCameraId = null;
        }
        scpclassifieddirective$lastCameraId = cameraId;
    }

    @Inject(method = "updateCamera", at = @At("HEAD"), remap = false)
    private static void scpclassifieddirective$inheritPhysicalPose(
            CallbackInfo ci) {
        if (scpclassifieddirective$pendingCameraId == null
                || cameraId == null
                || !scpclassifieddirective$pendingCameraId.equals(cameraId)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        BlockEntity blockEntity = minecraft.level.getBlockEntity(
                BlockPos.containing(cameraPosition));
        float yaw;
        float pitch;
        if (blockEntity instanceof SurveillanceCameraPlaceholderModule
                .SurveillanceCameraBlockEntity wallCamera) {
            yaw = baseYaw + wallCamera.visualYaw(1.0F);
            pitch = wallCamera.visualPitch(1.0F);
        } else if (blockEntity instanceof CeilingCameraModule
                .CeilingCameraBlockEntity ceilingCamera) {
            yaw = baseYaw + ceilingCamera.visualYaw(1.0F);
            pitch = ceilingCamera.visualPitch(1.0F);
        } else {
            // The camera chunk can arrive a frame after the role-state packet.
            // Keep waiting rather than baking a neutral-angle snap into the handoff.
            return;
        }

        yaw = Mth.wrapDegrees(yaw);
        minecraft.player.setYRot(yaw);
        minecraft.player.setXRot(pitch);
        minecraft.player.yRotO = yaw;
        minecraft.player.xRotO = pitch;
        scpclassifieddirective$pendingCameraId = null;
    }
}
