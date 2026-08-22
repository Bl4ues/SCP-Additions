package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.ScpInventoryMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_classified_directive", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
