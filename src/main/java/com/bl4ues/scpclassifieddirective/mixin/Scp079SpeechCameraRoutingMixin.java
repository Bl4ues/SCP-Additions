package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079SpeechManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A remote camera without a Speaker is silent. The physical local host is only
 * a valid TTS origin while SCP-079 is actually at the local host.
 */
@Mixin(value = Scp079SpeechManager.class, remap = false)
public abstract class Scp079SpeechCameraRoutingMixin {
    @Inject(method = "hostOutput", at = @At("HEAD"),
            cancellable = true, remap = false)
    private static void scpclassifieddirective$noRemoteHostFallback(
            ServerPlayer player,
            CallbackInfoReturnable<Scp079SpeechManager.Output> cir) {
        if (Scp079PlayableManager.isCameraMode(player)) {
            cir.setReturnValue(null);
        }
    }
}
