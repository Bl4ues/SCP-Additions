package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.network.Scp079CameraNavigationNetwork;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registers the playable SCP-079 camera-topology extension beside its base channel. */
@Mixin(value = Scp079PlayableNetwork.class, remap = false)
public abstract class Scp079CameraNavigationRegistrationMixin {
    @Inject(method = "register", at = @At("RETURN"), remap = false)
    private static void scpclassifieddirective$registerCameraNavigation(
            CallbackInfo ci) {
        Scp079CameraNavigationNetwork.register();
    }
}
