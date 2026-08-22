package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.ScpInventoryMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_classified_directive", value = Dist.CLIENT)
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
