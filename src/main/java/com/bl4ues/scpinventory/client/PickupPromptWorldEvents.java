package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PickupPromptWorldEvents {

    private PickupPromptWorldEvents() {
    }

    @SubscribeEvent
    public static void renderPickupOutline(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            PickupPromptClient.renderWorldOutline(event.getPoseStack(), event.getCamera());
        }
    }
}
