package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the authored no-signal screen completely free of vanilla HUD layers. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079SignalHudSuppressionClient {
    private Scp079SignalHudSuppressionClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hideHudDuringSignalLoss(RenderGuiOverlayEvent.Pre event) {
        if (Scp079CameraEffectsClient.signalEffectActive()) {
            event.setCanceled(true);
        }
    }
}
