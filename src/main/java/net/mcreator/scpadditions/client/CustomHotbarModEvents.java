package net.mcreator.scpadditions.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CustomHotbarModEvents {
    private CustomHotbarModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("elevator_arrival_overlay",
                (gui, graphics, partialTick, width, height) ->
                        ElevatorArrivalOverlay.render(
                                graphics, width, height));
        event.registerAboveAll("custom_hotbar_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (Scp939ClientState.pinned()) return;
                    CustomHotbarOverlay.render(graphics, width, height);
                    SimpleVoiceChatHudBridge.restoreCanceledVanillaHotbarPost(
                            graphics, partialTick);
                });
        // Register last in this local overlay group so the encounter prompt sits
        // over normal gameplay UI, while the pin state suppresses that UI first.
        event.registerAboveAll("scp939_interaction_overlay",
                (gui, graphics, partialTick, width, height) -> {
                    if (Scp939ClientState.pinned()) {
                        Scp939MaulImpactOverlay.render(
                                graphics, width, height);
                        Scp939ClientEvents.renderOverlay(
                                graphics, width, height);
                    } else {
                        Scp939BreathOverlay.render(graphics, width, height);
                    }
                });
    }
}
