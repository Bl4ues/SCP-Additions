package net.mcreator.scpadditions.fabric.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityPickupMixin {
    @Inject(
            method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void scpAdditions$beforePickup(
            Player player,
            CallbackInfo callback) {
        ItemEntityPickupEvent.Pre event = new ItemEntityPickupEvent.Pre(
                player, (ItemEntity) (Object) this);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled() || event.getCanPickup() == TriState.FALSE) {
            callback.cancel();
        }
    }
}
