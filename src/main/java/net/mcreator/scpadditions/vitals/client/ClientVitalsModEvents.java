package net.mcreator.scpadditions.vitals.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client MOD-bus registration for the custom vitals overlay. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientVitalsModEvents {
    private ClientVitalsModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        ScpAdditionsMod.MODID, "player_vitals_overlay"),
                (graphics, deltaTracker) -> PlayerVitalsOverlay.render(
                        graphics, graphics.guiWidth(), graphics.guiHeight(),
                        deltaTracker.getGameTimeDeltaPartialTick(false)));
    }
}
