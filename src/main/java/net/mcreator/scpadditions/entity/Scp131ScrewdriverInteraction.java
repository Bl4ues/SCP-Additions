package net.mcreator.scpadditions.entity;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.UnifiedReaderItems;

/**
 * Handles the screwdriver before SCP-131's normal follow interaction consumes
 * the right click. Mob#mobInteract runs before Item#interactLivingEntity, so the
 * old item-only implementation could never receive the action from SCP-131.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp131ScrewdriverInteraction {
    private Scp131ScrewdriverInteraction() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event) {
        handle(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(
            PlayerInteractEvent.EntityInteractSpecific event) {
        handle(event, event.getTarget());
    }

    private static void handle(PlayerInteractEvent event, Entity target) {
        if (!(target instanceof AbstractScp131Entity scp131)) return;

        Player player = event.getEntity();
        if (!player.getItemInHand(event.getHand())
                .is(UnifiedReaderItems.SCREWDRIVER.get())) {
            return;
        }

        if (!player.level().isClientSide) {
            scp131.stopFollowing();
            scp131.discard();
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(
                player.level().isClientSide));
        event.setCanceled(true);
    }
}
