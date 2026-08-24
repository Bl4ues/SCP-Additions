package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CustomHotbarModEvents {
    private CustomHotbarModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("elevator_arrival_overlay",
                (gui, graphics, partialTick, width, height) ->
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        ElevatorArrivalOverlay.render(graphics,
                                                virtualWidth, virtualHeight)));

        event.registerAboveAll("custom_hotbar_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (Scp939ClientState.pinned()) return;
                    ResponsiveUiScale.renderHud(graphics, width, height,
                            (virtualWidth, virtualHeight) -> {
                                CustomHotbarOverlay.render(graphics,
                                        virtualWidth, virtualHeight);
                                SimpleVoiceChatHudBridge
                                        .restoreCanceledVanillaHotbarPost(
                                                graphics, partialTick);
                            });
                });

        event.registerAboveAll("scp939_interaction_overlay",
                (gui, graphics, partialTick, width, height) ->
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) -> {
                                    if (Scp939ClientState.pinned()) {
                                        Scp939MaulImpactOverlay.render(graphics,
                                                virtualWidth, virtualHeight);
                                        Scp939ClientEvents.renderOverlay(graphics,
                                                virtualWidth, virtualHeight);
                                    } else {
                                        Scp939BreathOverlay.render(graphics,
                                                virtualWidth, virtualHeight);
                                    }
                                }));
    }
}
