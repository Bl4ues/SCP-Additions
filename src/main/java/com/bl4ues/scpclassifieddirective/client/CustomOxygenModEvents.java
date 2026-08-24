package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CustomOxygenModEvents {
    private CustomOxygenModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("custom_oxygen_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        CustomOxygenOverlay.render(graphics,
                                                virtualWidth, virtualHeight));
                    }
                });
    }
}
