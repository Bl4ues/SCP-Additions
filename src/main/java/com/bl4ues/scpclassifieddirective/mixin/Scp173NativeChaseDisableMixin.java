package com.bl4ues.scpclassifieddirective.mixin;

import net.minecraft.world.entity.LivingEntity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scp173Entity used to move once during its own tick and then let the external
 * movement controller decide whether to keep or repair that move. The two
 * planners could therefore alternate legal sideways steps around a doorway.
 * Pursuit movement now belongs exclusively to Scp173UnityNavigator.
 */
@Mixin(value = Scp173Entity.class, remap = false)
public abstract class Scp173NativeChaseDisableMixin {
    @Inject(method = "reactImmediatelyToTarget", at = @At("HEAD"),
            cancellable = true)
    private void scpClassifiedDirective$deferPursuitToUnityNavigator(
            LivingEntity target, CallbackInfo callback) {
        callback.cancel();
    }
}
