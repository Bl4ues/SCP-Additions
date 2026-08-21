package net.mcreator.scpadditions.mixin.client;

import net.mcreator.scpadditions.client.Scp939Model;
import net.mcreator.scpadditions.client.Scp939ModelPolish;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * Tiny delegation shim for SCP-939 render polish.
 *
 * The actual implementation deliberately lives outside the mixin package. Mixin
 * forbids target classes from directly loading helper types that remain inside a
 * configured mixin package, which caused the client to crash while constructing
 * Scp939Renderer before the title screen finished loading.
 */
@Mixin(value = Scp939Model.class, remap = false)
public abstract class Scp939ModelPolishMixin {
    @Inject(method = "applyWalkFootLocking", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replaceWalkFootLocking(Scp939Entity entity,
            CallbackInfo ci) {
        ci.cancel();
        Scp939ModelPolish.applyWalkFootLocking(
                (Scp939Model<?>) (Object) this, entity);
    }

    @Inject(method = "applyLocomotionBlend", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replaceLocomotionBlend(Scp939Entity entity,
            AnimationState<?> animationState, CallbackInfo ci) {
        ci.cancel();
        Scp939ModelPolish.applyLocomotionBlend(
                (Scp939Model<?>) (Object) this, entity, animationState);
    }
}
