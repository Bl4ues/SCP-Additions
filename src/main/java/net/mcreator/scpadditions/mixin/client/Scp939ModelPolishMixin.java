package net.mcreator.scpadditions.mixin.client;

import net.mcreator.scpadditions.client.Scp939Model;
import net.mcreator.scpadditions.client.Scp939ModelPolish;
import net.mcreator.scpadditions.entity.Scp939Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animation.AnimationState;

/** Delegates SCP-939 render-only polish to a normal client helper class. */
@Mixin(value = Scp939Model.class, remap = false)
public abstract class Scp939ModelPolishMixin {
    @Inject(method = "applyTurnLead", at = @At("HEAD"), cancellable = true)
    private void scpadditions$replaceTurnLead(Scp939Entity entity,
            CallbackInfo ci) {
        ci.cancel();
        Scp939ModelPolish.applyTurnMotion(
                (Scp939Model<?>) (Object) this, entity);
    }

    /**
     * Disable the old world-space foot servo. The replacement gait is applied
     * immediately before the model's own locomotion transition blend instead.
     */
    @Inject(method = "applyWalkFootLocking", at = @At("HEAD"), cancellable = true)
    private void scpadditions$disableOldWalkFootLocking(Scp939Entity entity,
            CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Apply the canine gait first, then deliberately allow Scp939Model's original
     * locomotion blend to continue. The previous mixin cancelled this method,
     * accidentally removing the walk/idle/listen transition smoothing entirely.
     */
    @Inject(method = "applyLocomotionBlend", at = @At("HEAD"))
    private void scpadditions$applyWalkBeforeLocomotionBlend(Scp939Entity entity,
            AnimationState<?> animationState, CallbackInfo ci) {
        Scp939ModelPolish.applyWalkGait(
                (Scp939Model<?>) (Object) this, entity, animationState);
    }
}
