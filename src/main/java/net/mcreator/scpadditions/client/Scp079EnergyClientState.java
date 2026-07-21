package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.Scp079EnergyPacket.SpawnStatus;

/** Client-only snapshot used by the optional developer HUDs. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp079EnergyClientState {
    private static boolean energyVisible;
    private static boolean active;
    private static float energy;
    private static boolean spawnTimersVisible;
    private static int scp173RemainingTicksAtSync = -1;
    private static long clientGameTickAtSync;
    private static SpawnStatus scp173Status = SpawnStatus.COUNTDOWN;

    private Scp079EnergyClientState() {
    }

    public static void update(boolean shouldShowEnergy, boolean systemActive,
            float currentEnergy, boolean shouldShowSpawnTimers,
            int scp173RemainingTicks, SpawnStatus status) {
        energyVisible = shouldShowEnergy;
        active = systemActive;
        energy = Math.max(0.0F, Math.min(100.0F, currentEnergy));
        spawnTimersVisible = shouldShowSpawnTimers;
        scp173RemainingTicksAtSync = Math.max(-1, scp173RemainingTicks);
        scp173Status = status == null ? SpawnStatus.COUNTDOWN : status;
        Minecraft minecraft = Minecraft.getInstance();
        clientGameTickAtSync = minecraft.level == null
                ? 0L : minecraft.level.getGameTime();
    }

    public static boolean visible() {
        return energyVisible;
    }

    public static boolean active() {
        return active;
    }

    public static float energy() {
        return energy;
    }

    public static boolean spawnTimersVisible() {
        return spawnTimersVisible;
    }

    public static SpawnStatus scp173Status() {
        return scp173Status;
    }

    public static int scp173RemainingTicks() {
        if (scp173RemainingTicksAtSync < 0 || !scp173Status.showsTimer()) {
            return -1;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null
                ? clientGameTickAtSync : minecraft.level.getGameTime();
        long elapsed = Math.max(0L, now - clientGameTickAtSync);
        return (int) Math.max(0L, scp173RemainingTicksAtSync - elapsed);
    }

    public static void clear() {
        energyVisible = false;
        active = false;
        energy = 0.0F;
        spawnTimersVisible = false;
        scp173RemainingTicksAtSync = -1;
        clientGameTickAtSync = 0L;
        scp173Status = SpawnStatus.COUNTDOWN;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
