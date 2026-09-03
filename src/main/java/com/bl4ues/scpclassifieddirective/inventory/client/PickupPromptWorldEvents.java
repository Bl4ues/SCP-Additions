package com.bl4ues.scpclassifieddirective.inventory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_classified_directive",
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PickupPromptWorldEvents {
    private PickupPromptWorldEvents() {
    }

    @SubscribeEvent
    public static void renderPickupOutline(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            PickupOutlineRenderer.captureMask(event.getPoseStack(), event.getCamera());
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            PickupOutlineRenderer.composite();
        }
    }
}
