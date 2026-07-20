package net.mcreator.scpadditions.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlinkClientEvents {
    private BlinkClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) BlinkClient.clientTick();
    }
}
