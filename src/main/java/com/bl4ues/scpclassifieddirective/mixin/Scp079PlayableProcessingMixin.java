package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079ProcessingManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Player-controlled SCP-079 bypasses AI network discovery for AP regeneration. */
@Mixin(Scp079ProcessingManager.class)
public abstract class Scp079PlayableProcessingMixin {
    @Inject(method = "isActive", at = @At("RETURN"), cancellable = true,
            remap = false)
    private static void scpclassifieddirective$playerControlKeepsProcessingActive(
            ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && level != null
                && Scp079PlayableManager.hasController(level.getServer())) {
            cir.setReturnValue(true);
        }
    }
}
