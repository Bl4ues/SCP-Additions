package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.TeslaTerminalFocusClient;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the physical-terminal view to Minecraft's actual render camera after
 * vanilla has finished deriving its pose from the player. This keeps the camera
 * inside the normal render lifecycle and avoids detached-entity interpolation.
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
        TeslaTerminalFocusClient.Pose pose = TeslaTerminalFocusClient.cameraPose();
        if (pose == null) return;
        scpclassifieddirective$setPosition(pose.position());
        scpclassifieddirective$setRotation(pose.yaw(), pose.pitch());
    }
}
