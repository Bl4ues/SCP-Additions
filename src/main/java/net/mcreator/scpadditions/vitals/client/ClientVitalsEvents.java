package net.mcreator.scpadditions.vitals.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.vitals.VitalsModule;

/** Client Forge-bus hooks for stamina prediction and vanilla HUD replacement. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
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
    public static void beforeOverlay(RenderGuiLayerEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !player.isCreative() && !player.isSpectator()
                && VitalsModule.healthHudEnabled()
                && event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);
        }
    }
}
