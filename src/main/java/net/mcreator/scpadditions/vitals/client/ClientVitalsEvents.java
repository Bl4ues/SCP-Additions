package net.mcreator.scpadditions.vitals.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.vitals.VitalsModule;

/** Client Forge-bus hooks for stamina prediction and vanilla HUD replacement. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ClientVitalsEvents {
    private ClientVitalsEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PlayerVitalsClient.clientTick();
        }
    }

    @SubscribeEvent
    public static void beforeOverlay(RenderGuiOverlayEvent.Pre event) {
        if (VitalsModule.healthHudEnabled()
                && event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
        }
    }
}
