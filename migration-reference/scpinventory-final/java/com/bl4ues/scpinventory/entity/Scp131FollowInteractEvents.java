package com.bl4ues.scpinventory.entity;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpInventoryMod.MODID)
public final class Scp131FollowInteractEvents {
    private Scp131FollowInteractEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfAlreadyFollowing(event.getEntity(), event.getTarget(), event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancelIfAlreadyFollowing(event.getEntity(), event.getTarget(), event);
    }

    private static void cancelIfAlreadyFollowing(Player player, Entity target, PlayerInteractEvent event) {
        if (player == null || target == null) {
            return;
        }
        if (target instanceof AbstractScp131Entity scp131 && scp131.isFollowingPlayer(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
