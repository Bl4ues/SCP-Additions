package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Starts SCP-714's authored soundtrack once per local exposure. */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp714MusicClient {
    private static Scp714MusicSound music;
    private static boolean exposureWasActive;

    private Scp714MusicClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean active = minecraft.player != null && minecraft.level != null
                && minecraft.player.isAlive() && Scp714ClientState.isActive();

        if (active && !exposureWasActive) {
            startMusic(minecraft);
        } else if (!active && exposureWasActive && music != null) {
            music.beginFadeOut();
        }
        exposureWasActive = active;

        if (music != null && !minecraft.getSoundManager().isActive(music)) {
            music = null;
        }
    }

    private static void startMusic(Minecraft minecraft) {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
        }
        music = new Scp714MusicSound();
        minecraft.getSoundManager().play(music);
    }
}
