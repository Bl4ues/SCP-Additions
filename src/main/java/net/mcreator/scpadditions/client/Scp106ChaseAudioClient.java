package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Owns the single local SCP-106 chase soundtrack. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp106ChaseAudioClient {
    private static Scp106ChaseSound chase;

    private Scp106ChaseAudioClient() {
    }

    public static void start() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (chase != null && minecraft.getSoundManager().isActive(chase)
                && !chase.isFadingOut()) {
            return;
        }
        if (chase != null) minecraft.getSoundManager().stop(chase);
        chase = new Scp106ChaseSound();
        minecraft.getSoundManager().play(chase);
    }

    public static void stop() {
        if (chase != null) chase.beginFadeOut(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || chase == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.getSoundManager().isActive(chase)) chase = null;
    }
}
