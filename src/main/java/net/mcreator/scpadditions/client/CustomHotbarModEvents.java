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
                    CustomHotbarOverlay.render(graphics, width, height);
                    SimpleVoiceChatHudBridge.restoreCanceledVanillaHotbarPost(
                            graphics, partialTick);
                });
    }
}
