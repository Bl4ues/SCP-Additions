package com.bl4ues.scpclassifieddirective.safezone.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.ModMusicExclusivityClient;
import com.bl4ues.scpclassifieddirective.safezone.DiscoveryMusicManager;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Plays discovery cues once, without spatial attenuation or looping. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DiscoveryMusicClient {
    private static SimpleSoundInstance active;

    private DiscoveryMusicClient() {
    }

    public static void play(DiscoveryMusicManager.Cue cue) {
        if (cue == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        SoundEvent sound = switch (cue) {
            case CORE_ROOM_DISCOVERY ->
                    SafeZoneSounds.CORE_ROOM_DISCOVERY.get();
            case SCP_131_FOUND -> SafeZoneSounds.SCP_131_FOUND.get();
        };
        if (active != null) minecraft.getSoundManager().stop(active);
        SafeZoneMusicClient.suspendForDiscovery(minecraft);
        active = new SimpleSoundInstance(sound.getLocation(),
                SoundSource.MUSIC, 1.0F, 1.0F, RandomSource.create(),
                false, 0, SoundInstance.Attenuation.NONE,
                0.0D, 0.0D, 0.0D, true);
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(active);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !minecraft.getSoundManager().isActive(active)) {
            active = null;
        }
    }

    public static boolean isPlaying() {
        return active != null;
    }
}
