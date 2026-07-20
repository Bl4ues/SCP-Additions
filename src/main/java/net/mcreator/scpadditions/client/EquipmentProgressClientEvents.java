package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.neoforged.api.distmarker.Dist;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client Forge-bus hook for timed equipment progress and local Hazmat audio. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class EquipmentProgressClientEvents {
    private EquipmentProgressClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            EquipmentProgressOverlay.clientTick();
            HazmatAudioClient.clientTick();
        }
    }
}
