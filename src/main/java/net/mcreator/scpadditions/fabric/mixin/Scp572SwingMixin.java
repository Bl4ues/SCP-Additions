package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.mcreator.scpadditions.item.SCP572Item;
import net.mcreator.scpadditions.procedures.SCP572LivingEntityIsHitWithItemProcedure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class Scp572SwingMixin {
    @Inject(
            method = "swing(Lnet/minecraft/world/InteractionHand;Z)V",
            at = @At("HEAD"))
    private void scpAdditions$scp572Swing(
            InteractionHand hand,
            boolean updateSelf,
            CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide()
                && entity.getItemInHand(hand).getItem() instanceof SCP572Item) {
            SCP572LivingEntityIsHitWithItemProcedure.execute(entity);
        }
    }
}
