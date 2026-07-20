package com.bl4ues.scpadditions.compat;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only half of the Forge TickEvent compatibility bridge. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class LegacyClientTickBridge {
    private LegacyClientTickBridge() {
    }

    @SubscribeEvent
    public static void onClientPre(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
        NeoForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START));
    }

    @SubscribeEvent
    public static void onClientPost(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        NeoForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
    }
}
