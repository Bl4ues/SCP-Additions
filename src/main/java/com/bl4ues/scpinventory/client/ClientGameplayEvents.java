package com.bl4ues.scpinventory.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.mcreator.scpadditions.client.Scp1176MusicClient;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class ClientGameplayEvents {
    private ClientGameplayEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PickupPromptClient.clientTick();
            ContextPromptClient.clientTick();
            StatusEffectTimelineClient.clientTick();
            Scp1176MusicClient.clientTick();
        }
    }
}
