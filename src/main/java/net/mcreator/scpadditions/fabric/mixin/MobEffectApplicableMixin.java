package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class MobEffectApplicableMixin {
    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$checkEffect(
            MobEffectInstance instance,
            Entity source,
            CallbackInfoReturnable<Boolean> callback) {
        MobEffectEvent.Applicable event = new MobEffectEvent.Applicable(
                (LivingEntity) (Object) this, instance);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()
                || event.getResult() == Event.Result.DENY
                || event.getResult() == Event.Result.DO_NOT_APPLY) {
            callback.setReturnValue(false);
        }
    }
}
