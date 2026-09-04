package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079ProcessingManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A human SCP-079 controller replaces, rather than competes with, the AI strategist. */
@Mixin(value = Scp079ProcessingManager.class, remap = false)
public abstract class Scp079PlayerControlAiGateMixin {
    @Inject(method = "trySpend", at = @At("HEAD"), cancellable = true,
            remap = false)
    private static void scpClassifiedDirective$playerOwns079Decisions(
            ServerLevel level, double cost,
            CallbackInfoReturnable<Boolean> callback) {
        if (level != null
                && Scp079PlayableManager.hasController(level.getServer())) {
            callback.setReturnValue(false);
        }
    }
}
