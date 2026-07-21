package net.mcreator.scpadditions.vitals.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client MOD-bus registration for the custom gameplay overlays. */
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
        event.registerAboveAll("scp_079_energy_debug",
                (gui, graphics, partialTick, width, height) ->
                        Scp079EnergyOverlay.render(graphics, width, height, partialTick));
        event.registerAboveAll("scp_spawn_timers_debug",
                (gui, graphics, partialTick, width, height) ->
                        ScpSpawnTimersOverlay.render(graphics, width, height,
                                partialTick));
    }
}
