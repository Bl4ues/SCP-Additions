package com.bl4ues.scpadditions.compat;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Re-emits NeoForge 1.21.1 tick events through the compatibility event types.
 * This preserves the exact START/END timing expected by the existing gameplay
 * handlers while the rest of the port is validated.
 */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID)
public final class LegacyGameTickBridge {
    private LegacyGameTickBridge() {
    }

    @SubscribeEvent
    public static void onPlayerPre(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre event) {
        NeoForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, event.getEntity()));
    }

    @SubscribeEvent
    public static void onPlayerPost(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        NeoForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, event.getEntity()));
    }

    @SubscribeEvent
    public static void onServerPre(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        NeoForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.START, event.getServer()));
    }

    @SubscribeEvent
    public static void onServerPost(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        NeoForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END, event.getServer()));
    }

    @SubscribeEvent
    public static void onLevelPre(net.neoforged.neoforge.event.tick.LevelTickEvent.Pre event) {
        NeoForge.EVENT_BUS.post(new TickEvent.LevelTickEvent(TickEvent.Phase.START, event.getLevel()));
    }

    @SubscribeEvent
    public static void onLevelPost(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        NeoForge.EVENT_BUS.post(new TickEvent.LevelTickEvent(TickEvent.Phase.END, event.getLevel()));
    }
}
