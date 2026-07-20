package com.bl4ues.scpinventory.client;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class ContextPromptClickGuard {
    private ContextPromptClickGuard() {
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isUseItem() && ContextPromptClient.hasRightClickTarget()) {
            event.setCanceled(true);
        }
    }
}
