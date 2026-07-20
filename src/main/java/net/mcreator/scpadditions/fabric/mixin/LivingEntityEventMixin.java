package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.mcreator.scpadditions.fabric.FabricLivingDropsCapture;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityEventMixin {
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float scpAdditions$modifyHealAmount(float amount) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return amount;
        LivingHealEvent event = new LivingHealEvent(entity, amount);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled() ? 0.0F : Math.max(0.0F, event.getAmount());
    }

    @ModifyVariable(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private float scpAdditions$modifyIncomingDamage(
            float amount,
            DamageSource source) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) return amount;
        LivingIncomingDamageEvent event =
                new LivingIncomingDamageEvent(entity, source, amount);
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled() ? 0.0F : Math.max(0.0F, event.getAmount());
    }

    @Inject(
            method = "startUsingItem(Lnet/minecraft/world/InteractionHand;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$beforeUseItem(
            InteractionHand hand,
            CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack stack = entity.getItemInHand(hand);
        LivingEntityUseItemEvent.Start event =
                new LivingEntityUseItemEvent.Start(entity, stack);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) callback.cancel();
    }

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void scpAdditions$finishUseItem(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        ItemStack used = entity.getUseItem().copy();
        if (!used.isEmpty()) {
            NeoForge.EVENT_BUS.post(
                    new LivingEntityUseItemEvent.Finish(entity, used));
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void scpAdditions$afterLivingTick(CallbackInfo callback) {
        NeoForge.EVENT_BUS.post(
                new EntityTickEvent.Post((LivingEntity) (Object) this));
    }

    @Inject(
            method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD"))
    private void scpAdditions$beginDeathDrops(
            ServerLevel level,
            DamageSource source,
            CallbackInfo callback) {
        FabricLivingDropsCapture.begin((LivingEntity) (Object) this, level);
    }

    @Inject(
            method = "dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("RETURN"))
    private void scpAdditions$finishDeathDrops(
            ServerLevel level,
            DamageSource source,
            CallbackInfo callback) {
        FabricLivingDropsCapture.finish((LivingEntity) (Object) this, level);
    }
}
