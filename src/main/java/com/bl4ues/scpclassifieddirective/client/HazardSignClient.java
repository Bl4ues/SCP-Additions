package com.bl4ues.scpclassifieddirective.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.HazardSignModule;

/** Client registration for the Hazard Sign block-entity renderer. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HazardSignClient {
    private HazardSignClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HazardSignModule.BLOCK_ENTITY.get(),
                HazardSignBlockEntityRenderer::new);
    }
}
