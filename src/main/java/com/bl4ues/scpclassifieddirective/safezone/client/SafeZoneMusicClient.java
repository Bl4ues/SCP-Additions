package com.bl4ues.scpclassifieddirective.safezone.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.ModMusicExclusivityClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.safezone.SafeZone;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneSounds;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneTrack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Selects the local player's zone track without restarting its loop. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class SafeZoneMusicClient {
    private static SafeZoneMusicSound active;
    private static SafeZoneMusicSound fading;
    private static String activeTrack = "";
    private static int retryTicks;

    private SafeZoneMusicClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.player.isAlive()) {
            stopImmediately(minecraft);
            SafeZoneClientState.clear();
            return;
        }

        ResourceLocation musicDimension = minecraft.level.dimension().location();
        Vec3 musicPosition = minecraft.player.position();
        if (Scp079PlayableClient.active()
                && musicDimension.equals(Scp079PlayableClient.hostDimension())) {
            musicDimension = Scp079PlayableClient.hostDimension();
            musicPosition = Vec3.atCenterOf(Scp079PlayableClient.hostPos());
        }
        SafeZone zone = SafeZoneClientState.zoneAt(musicDimension, musicPosition);
        String desired = !DiscoveryMusicClient.isPlaying()
                && zone != null && zone.musicEnabled()
                ? zone.effectiveTrack() : "";
        if (!desired.equals(activeTrack)) {
            transitionTo(minecraft, desired);
        }

        if (retryTicks > 0) retryTicks--;
        if (active != null
                && !minecraft.getSoundManager().isActive(active)) {
            active = null;
            if (!activeTrack.isEmpty()) retryTicks = 100;
        }
        if (fading != null
                && !minecraft.getSoundManager().isActive(fading)) {
            fading = null;
        }
        if (active == null && !activeTrack.isEmpty()
                && retryTicks <= 0) {
            start(minecraft, activeTrack);
        }
    }

    private static void transitionTo(Minecraft minecraft, String desired) {
        if (active != null) {
            if (fading != null) minecraft.getSoundManager().stop(fading);
            active.beginFadeOut();
            fading = active;
            active = null;
        }
        activeTrack = desired == null ? "" : desired;
        retryTicks = 0;
        if (!activeTrack.isEmpty()) start(minecraft, activeTrack);
    }

    private static void start(Minecraft minecraft, String trackId) {
        SafeZoneTrack track = SafeZoneTrack.byId(trackId);
        SoundEvent event = SafeZoneSounds.forTrack(track);
        if (event == null) return;
        active = new SafeZoneMusicSound(event);
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(active);
    }

    private static void stopImmediately(Minecraft minecraft) {
        if (active != null) minecraft.getSoundManager().stop(active);
        if (fading != null) minecraft.getSoundManager().stop(fading);
        active = null;
        fading = null;
        activeTrack = "";
        retryTicks = 0;
    }

    static void suspendForDiscovery(Minecraft minecraft) {
        stopImmediately(minecraft);
    }

    public static boolean isPlaying() {
        return active != null || fading != null;
    }
}
