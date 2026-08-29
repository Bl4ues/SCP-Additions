package com.bl4ues.scpclassifieddirective.mixin;

import com.bl4ues.scpclassifieddirective.stealth.PerceptionFramework;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gates new player targets through the global visual-perception framework. */
@Mixin(Mob.class)
public abstract class MobTargetPerceptionMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$gateVisualPlayerTarget(
            LivingEntity target, CallbackInfo ci) {
        if (!(target instanceof Player player)) return;
        Mob mob = (Mob) (Object) this;
        if (mob.getTarget() == target) return;
        if (!PerceptionFramework.canAcquire(mob, player)) ci.cancel();
    }
}
