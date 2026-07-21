package net.mcreator.scpadditions.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client-only snapshot used by the optional SCP-079 processing HUD. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp079EnergyClientState {
    private static boolean visible;
    private static boolean active;
    private static float energy;

    private Scp079EnergyClientState() {
    }

    public static void update(boolean shouldShow, boolean systemActive,
            float currentEnergy) {
        visible = shouldShow;
        active = systemActive;
        energy = Math.max(0.0F, Math.min(100.0F, currentEnergy));
    }

    public static boolean visible() {
        return visible;
    }

    public static boolean active() {
        return active;
    }

    public static float energy() {
        return energy;
    }

    public static void clear() {
        visible = false;
        active = false;
        energy = 0.0F;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
