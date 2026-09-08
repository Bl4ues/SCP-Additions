package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Suppresses human hurt/death audio for the non-human SCP-079 host failure. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class Scp079HostFailureAudioClient {
    private static long suppressUntilNanos;

    private Scp079HostFailureAudioClient() {
    }

    public static void arm(int durationTicks) {
        long duration = Math.max(1, durationTicks) * 50_000_000L;
        // Network/death-screen scheduling can land a few frames after the exact
        // two-second no-signal timer. Keep a short grace window so the synthetic
        // host death never leaks a human grunt at the transition boundary.
        suppressUntilNanos = Math.max(suppressUntilNanos,
                System.nanoTime() + duration + 1_250_000_000L);
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (System.nanoTime() >= suppressUntilNanos) return;
        SoundInstance sound = event.getOriginalSound();
        if (sound == null || sound.getLocation() == null
                || !isLocalPlayerSound(sound)) return;

        ResourceLocation location = sound.getLocation();
        String path = location.getPath();
        boolean vanillaHuman = "minecraft".equals(location.getNamespace())
                && (path.startsWith("entity.player.hurt")
                || path.startsWith("entity.player.death"));
        boolean replacementHuman = sound.getSource() == SoundSource.PLAYERS
                && (path.contains("player_hurt")
                || path.contains("hurt_voice")
                || path.contains("voice_profile") && path.contains("hurt"));
        if (vanillaHuman || replacementHuman) {
            event.setSound(null);
        }
    }

    private static boolean isLocalPlayerSound(SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        if (sound.isRelative()) return true;
        double dx = sound.getX() - minecraft.player.getX();
        double dy = sound.getY() - minecraft.player.getY();
        double dz = sound.getZ() - minecraft.player.getZ();
        return dx * dx + dy * dy + dz * dz <= 9.0D;
    }
}
