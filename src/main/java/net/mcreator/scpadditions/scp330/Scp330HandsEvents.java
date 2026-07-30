package net.mcreator.scpadditions.scp330;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Prevents all hand-driven gameplay until SCP-330's victim dies. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class Scp330HandsEvents {
    private Scp330HandsEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) Scp330Hands.maintain(event.player);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) Scp330Hands.resetAfterDeath(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Scp330Hands.resetAfterDeath(event.getEntity());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancel(event, Scp330Hands.isDisabled(event.getEntity()));
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancel(event, Scp330Hands.isDisabled(event.getEntity()));
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancel(event, Scp330Hands.isDisabled(event.getEntity()));
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancel(event, Scp330Hands.isDisabled(event.getEntity()));
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        cancel(event, Scp330Hands.isDisabled(event.getEntity()));
    }

    private static void cancel(Event event, boolean disabled) {
        if (disabled && event.isCancelable()) event.setCanceled(true);
    }
}
