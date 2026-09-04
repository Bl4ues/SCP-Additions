package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpclassifieddirective.safezone.client.SafeZoneMusicClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Keeps vanilla background music out of SCP: Classified Directive soundtrack sequences. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class ModMusicExclusivityClient {
    private static boolean suppressionActive;

    private ModMusicExclusivityClient() {
    }

    /**
     * Stops the currently tracked vanilla song once when suppression begins.
     * Repeating MusicManager.stopPlaying() every tick can overflow its internal
     * delay after a song starts and make Minecraft immediately queue another.
     */
    public static void stopVanillaMusicNow() {
        if (suppressionActive) return;
        suppressionActive = true;
        Minecraft.getInstance().getMusicManager().stopPlaying();
    }

    /** Prevents new Minecraft music instances from reaching the sound engine. */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null || !shouldSuppressVanillaMusic()) return;
        if (sound.getSource() == SoundSource.MUSIC
                && "minecraft".equals(sound.getLocation().getNamespace())) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (shouldSuppressVanillaMusic()) {
            stopVanillaMusicNow();
        } else {
            suppressionActive = false;
        }
    }

    private static boolean shouldSuppressVanillaMusic() {
        return InventoryModuleRuntimeState.disableVanillaMusicForClient()
                || hasActiveModMusic();
    }

    private static boolean hasActiveModMusic() {
        return MainMenuMusicClient.isPlaying()
                || Scp1176MusicClient.isPlaying()
                || Scp106ChaseAudioClient.isPlaying()
                || Scp173EncounterAudioClient.isPlaying()
                || Scp939AudioClient.isMusicPlaying()
                || Scp714MusicClient.isPlaying()
                || SafeZoneMusicClient.isPlaying();
    }
}
