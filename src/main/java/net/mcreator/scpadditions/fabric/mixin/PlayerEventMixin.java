package net.mcreator.scpadditions.fabric.mixin;

import java.util.Optional;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerEventMixin {
    @ModifyVariable(
            method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private ItemStack scpAdditions$beforeToss(ItemStack stack) {
        Player player = (Player) (Object) this;
        if (stack.isEmpty() || player.level().isClientSide()) return stack;
        ItemEntity candidate = new ItemEntity(
                player.level(), player.getX(), player.getEyeY() - 0.3D,
                player.getZ(), stack);
        ItemTossEvent event = new ItemTossEvent(candidate, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return ItemStack.EMPTY;
        return candidate.getItem();
    }

    @Inject(
            method = "getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F",
            at = @At("RETURN"),
            cancellable = true)
    private void scpAdditions$modifyBreakSpeed(
            BlockState state,
            CallbackInfoReturnable<Float> callback) {
        Player player = (Player) (Object) this;
        float original = callback.getReturnValue();
        PlayerEvent.BreakSpeed event = new PlayerEvent.BreakSpeed(
                player, state, Optional.of(player.blockPosition()), original);
        NeoForge.EVENT_BUS.post(event);
        callback.setReturnValue(Math.max(0.0F, event.getNewSpeed()));
    }
}
