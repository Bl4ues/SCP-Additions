package com.bl4ues.scpinventory.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.mcreator.scpadditions.client.Scp1176MusicClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import com.bl4ues.scpinventory.context.ContextInteractionRegistry;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class ClientGameplayEvents {
    private ClientGameplayEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ScpInventoryConfig.clearServerSnapshot();
        ContextInteractionRegistry.clearServerSnapshot();
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
