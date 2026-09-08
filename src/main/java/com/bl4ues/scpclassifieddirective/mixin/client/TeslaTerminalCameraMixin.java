package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceFocusClient;
import com.bl4ues.scpclassifieddirective.client.TeslaTerminalFocusClient;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies physical-interface views to Minecraft's actual render camera after
 * vanilla has finished deriving its pose from the player.
 */
@Mixin(Camera.class)
public abstract class TeslaTerminalCameraMixin {
    @Invoker("setPosition")
    protected abstract void scpclassifieddirective$setPosition(Vec3 position);

    @Invoker("setRotation")
    protected abstract void scpclassifieddirective$setRotation(float yaw,
            float pitch);

    @Inject(method = "setup", at = @At("RETURN"))
    private void scpclassifieddirective$applyTerminalPose(CallbackInfo ci) {
        HackingDeviceFocusClient.Pose hackingPose =
                HackingDeviceFocusClient.cameraPose();
        if (hackingPose != null) {
            scpclassifieddirective$setPosition(hackingPose.position());
            scpclassifieddirective$setRotation(hackingPose.yaw(),
                    hackingPose.pitch());
            return;
        }

        TeslaTerminalFocusClient.Pose pose = TeslaTerminalFocusClient.cameraPose();
        if (pose == null) return;
        scpclassifieddirective$setPosition(pose.position());
        scpclassifieddirective$setRotation(pose.yaw(), pose.pitch());
    }
}
