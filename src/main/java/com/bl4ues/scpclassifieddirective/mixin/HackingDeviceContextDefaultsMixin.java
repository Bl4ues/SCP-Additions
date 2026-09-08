package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.inventory.context.DefaultContextInteractions;
import com.bl4ues.scpclassifieddirective.inventory.context.HackingDeviceContextDefaults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps new Hacking Device prompts update-safe alongside other integrated defaults. */
@Mixin(value = DefaultContextInteractions.class, remap = false)
public abstract class HackingDeviceContextDefaultsMixin {
    @Inject(method = "withRuntimeDefaults", at = @At("RETURN"),
            cancellable = true)
    private static void scpclassifieddirective$appendHackingDeviceDefaults(
            String raw, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(HackingDeviceContextDefaults.append(
                cir.getReturnValue()));
    }
}
