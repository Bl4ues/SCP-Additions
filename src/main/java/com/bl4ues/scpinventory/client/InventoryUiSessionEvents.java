package com.bl4ues.scpinventory.client;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = "scp_additions", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class InventoryUiSessionEvents {
    private InventoryUiSessionEvents() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ScpInventoryScreen.resetSessionState();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ScpInventoryScreen.resetSessionState();
    }
}
