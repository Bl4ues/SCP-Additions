package com.bl4ues.scpclassifieddirective.facility.intercom;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerBroadcastManager;
import com.bl4ues.scpclassifieddirective.network.Scp079AudioNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server authority for the Intercom's local acoustic microphone. Authoritative
 * ServerLevel sounds and client-only ambient effects meet here before being
 * copied to active facility Speakers.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IntercomWorldSoundBridge {
    private static final Set<String> SELF_FEEDBACK = Set.of(
            "intercom_on", "intercom_off", "intercom_loop",
            "speaker_on", "speaker_off", "speaker_loop");
    private static final int DUPLICATE_WINDOW_TICKS = 4;
    private static final double MAX_CLIENT_REPORT_DISTANCE = 10.0D;
    private static final Map<MinecraftServer, Map<SoundKey, Integer>> RECENT =
            new WeakHashMap<>();

    private IntercomWorldSoundBridge() {
    }

    public static void relayServerSound(ServerLevel level, Vec3 sourcePosition,
            ResourceLocation sound, float volume, float pitch) {
        relay(level, sourcePosition, sound, volume, pitch, true);
    }

    /**
     * Accepts client-local sounds such as random display-tick ambience which has
     * no authoritative ServerLevel sound call. The reporter must physically be
     * close enough that the active Intercom could plausibly have heard it.
     */
    public static void relayClientSound(ServerPlayer reporter,
            ResourceLocation sound, Vec3 sourcePosition, float volume,
            float pitch) {
        if (reporter == null || sound == null || sourcePosition == null
                || !(reporter.level() instanceof ServerLevel level)) return;
        if (reporter.getEyePosition().distanceToSqr(sourcePosition)
                > MAX_CLIENT_REPORT_DISTANCE * MAX_CLIENT_REPORT_DISTANCE) {
            return;
        }
        relay(level, sourcePosition, sound, volume, pitch, false);
    }

    private static void relay(ServerLevel level, Vec3 sourcePosition,
            ResourceLocation sound, float volume, float pitch,
            boolean authoritative) {
        if (level == null || sourcePosition == null || sound == null
                || volume <= 0.0F) return;
        if (ScpClassifiedDirectiveMod.MODID.equals(sound.getNamespace())
                && SELF_FEEDBACK.contains(sound.getPath())) return;

        int now = level.getServer().getTickCount();
        SoundKey key = SoundKey.of(level, sound, sourcePosition);
        synchronized (RECENT) {
            Map<SoundKey, Integer> recent = RECENT.computeIfAbsent(
                    level.getServer(), ignored -> new HashMap<>());
            Integer last = recent.get(key);
            if (last != null && now - last <= DUPLICATE_WINDOW_TICKS) {
                // Server copy wins over later client echoes; the first nearby
                // client also suppresses duplicate reports from other clients.
                if (!authoritative || now >= last) return;
            }
            recent.put(key, now);
            if (recent.size() > 512) {
                recent.entrySet().removeIf(entry ->
                        now - entry.getValue() > DUPLICATE_WINDOW_TICKS * 4);
            }
        }

        for (SpeakerBroadcastManager.AudioSource output :
                SpeakerBroadcastManager.intercomAudioSources(level,
                        sourcePosition)) {
            float relayedVolume = Mth.clamp(volume * output.captureGain()
                    * 0.58F, 0.0F, 2.0F);
            if (relayedVolume <= 0.001F) continue;
            Scp079AudioNetwork.sendSpeakerCue(level.getServer(),
                    output.dimension(), sound,
                    output.position().x, output.position().y,
                    output.position().z, relayedVolume,
                    Mth.clamp(pitch, 0.05F, 2.0F));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (RECENT) {
            RECENT.remove(event.getServer());
        }
    }

    private record SoundKey(String dimension, ResourceLocation sound,
            int x4, int y4, int z4) {
        private static SoundKey of(ServerLevel level, ResourceLocation sound,
                Vec3 position) {
            return new SoundKey(level.dimension().location().toString(), sound,
                    Mth.floor(position.x * 4.0D),
                    Mth.floor(position.y * 4.0D),
                    Mth.floor(position.z * 4.0D));
        }
    }
}
