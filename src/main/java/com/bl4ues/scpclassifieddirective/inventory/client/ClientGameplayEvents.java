package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.client.Scp1176MusicClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpclassifieddirective.inventory.config.ScpInventoryConfig;
import com.bl4ues.scpclassifieddirective.inventory.context.ContextInteractionRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_classified_directive", value = Dist.CLIENT)
public final class ClientGameplayEvents {
    private ClientGameplayEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        InventoryModuleRuntimeState.clearServerState();
        ScpInventoryConfig.clearServerSnapshot();
        ContextInteractionRegistry.clearServerSnapshot();
        Scp714ComaPromptClient.clearClientState();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Scp714ComaPromptClient.clientTick();
            if (!Scp714ComaPromptClient.blocksInteractionLayer()) {
                PickupPromptClient.clientTick();
                ContextPromptClient.clientTick();
            } else {
                ContextPromptClient.clear();
            }
            StatusEffectTimelineClient.clientTick();
            Scp1176MusicClient.clientTick();
        }
    }
}
