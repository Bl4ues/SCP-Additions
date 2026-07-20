package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
