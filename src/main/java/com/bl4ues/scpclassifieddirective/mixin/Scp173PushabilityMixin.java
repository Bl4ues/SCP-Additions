package com.bl4ues.scpclassifieddirective.mixin;

import net.minecraft.world.entity.LivingEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mapping-safe collision hooks that keep SCP-173 physically anchored. */
@Mixin(LivingEntity.class)
public abstract class Scp173PushabilityMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$keepScp173Anchored(
            CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof Scp173Entity) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$preventScp173FromShovingOthers(
            CallbackInfo callback) {
        if ((Object) this instanceof Scp173Entity) {
            callback.cancel();
        }
    }
}
