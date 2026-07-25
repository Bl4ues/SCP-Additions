package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Keeps vanilla background music out of active SCP soundtrack sequences. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ModMusicExclusivityClient {
    private ModMusicExclusivityClient() {
    }

    public static void stopVanillaMusicNow() {
        Minecraft.getInstance().getMusicManager().stopPlaying();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !hasActiveModMusic()) {
            return;
        }
        stopVanillaMusicNow();
    }

    private static boolean hasActiveModMusic() {
        return Scp1176MusicClient.isPlaying()
                || Scp106ChaseAudioClient.isPlaying();
    }
}
