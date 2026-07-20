package net.mcreator.scpadditions.vitals.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client MOD-bus registration for the custom vitals overlay. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientVitalsModEvents {
    private ClientVitalsModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("player_vitals_overlay",
                (gui, graphics, partialTick, width, height) ->
                        PlayerVitalsOverlay.render(graphics, width, height, partialTick));
    }
}
