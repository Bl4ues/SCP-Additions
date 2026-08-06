package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Starts the authored menu soundtrack and carries it through world loading. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class MainMenuMusicClient {
    private static boolean startedFromMenu;

    private MainMenuMusicClient() {
    }

    public static boolean isPlaying() {
        return startedFromMenu;
    }

    /** Stops the soundtrack only after EnterSoundClient sees a rendered world. */
    public static void onWorldEntryCue() {
        stopMusic(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientModulePreferences.mainMenuMusicEnabled()) {
            stopMusic(true);
            return;
        }

        if (!startedFromMenu) {
            // A pause screen must never start a fresh menu soundtrack session.
            if (minecraft.level == null && minecraft.screen != null) {
                startedFromMenu = true;
                ModMusicExclusivityClient.stopVanillaMusicNow();
                PersistentMenuMusicPlayer.startOrMaintain(minecraft);
            }
            return;
        }

        PersistentMenuMusicPlayer.startOrMaintain(minecraft);
    }

    /**
     * Client ticks can stall while the receiving-level percentage is rendered.
     * Keep pumping the untracked OpenAL stream from render ticks as well.
     */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && startedFromMenu) {
            PersistentMenuMusicPlayer.startOrMaintain(
                    Minecraft.getInstance());
        }
    }

    private static void stopMusic(boolean resetSession) {
        PersistentMenuMusicPlayer.stop();
        if (resetSession) startedFromMenu = false;
    }
}
