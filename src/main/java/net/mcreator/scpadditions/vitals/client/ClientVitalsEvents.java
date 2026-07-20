package net.mcreator.scpadditions.vitals.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
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
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !player.isCreative() && !player.isSpectator()
                && VitalsModule.healthHudEnabled()
                && event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
        }
    }
}
