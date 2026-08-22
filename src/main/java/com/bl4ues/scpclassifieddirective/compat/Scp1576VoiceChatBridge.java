package com.bl4ues.scpclassifieddirective.compat;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.death.DeathSpectateCoordinator;
import com.bl4ues.scpclassifieddirective.scp1576.Scp1576Manager;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Relays dead-player microphones from active SCP-1576 sources. Speech is
 * band-limited and softly saturated, then given subtle unstable timbre,
 * flutter and brief signal losses. The active SCP-1576 sound already supplies
 * the audible surface/static bed, so the voice filter deliberately adds no
 * extra hiss of its own.
 */
public final class Scp1576VoiceChatBridge {
    private static final Object CODEC_LOCK = new Object();
    private static final Map<UUID, OpusDecoder> DECODERS = new HashMap<>();
    private static final Map<ChannelKey, FilterChannel> CHANNELS = new HashMap<>();
    private static final Set<UUID> SCP1576_CHANNEL_IDS = new HashSet<>();

    private Scp1576VoiceChatBridge() {
    }

    public static void relay(MicrophonePacketEvent event) {
        if (event == null || !ModCompatibilityConfig.simpleVoiceChatEnabled()) {
            return;
        }
        ServerPlayer speaker = minecraftPlayer(event.getSenderConnection());
        if (speaker == null
                || !DeathSpectateCoordinator.isDeadVoiceParticipant(speaker)
                || speaker.getServer() == null) {
            return;
        }

        List<Scp1576Manager.VoiceSource> sources =
                Scp1576Manager.voiceSources(speaker.getServer());
        pruneInactiveChannels(sources);
        if (sources.isEmpty()) return;

        VoicechatServerApi api = event.getVoicechat();
        MicrophonePacket microphone = event.getPacket();
        byte[] original = microphone.getOpusEncodedData();
        if (api == null || original == null || original.length == 0) return;

        short[] decoded = decode(api, speaker.getUUID(), original);
        for (Scp1576Manager.VoiceSource source : sources) {
            ServerLevel level = speaker.getServer().getLevel(source.dimension());
            if (level == null) continue;

            byte[] output = decoded == null
                    ? Arrays.copyOf(original, original.length)
                    : encodeFiltered(api, speaker.getUUID(), source, decoded);
            if (output == null || output.length == 0) continue;

            UUID channelId = channelId(source.sessionId(), speaker.getUUID());
            synchronized (CODEC_LOCK) {
                SCP1576_CHANNEL_IDS.add(channelId);
            }

            LocationalSoundPacket packet;
            try {
                packet = microphone.locationalSoundPacketBuilder()
                        .channelId(channelId)
                        .opusEncodedData(output)
                        .position(api.createPosition(source.position().x,
                                source.position().y, source.position().z))
                        .distance((float) api.getVoiceChatDistance())
                        .build();
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.error(
                        "Could not build SCP-1576 voice packet", exception);
                continue;
            }

            if (sendToLivingReceivers(api, speaker.getServer(), source, packet)) {
                Scp1576Manager.recordContact(speaker.getServer(),
                        source.sessionId());
            }
        }
    }

    /** Prevents the personnel-feed bridge from mirroring 1576 back to the dead. */
    public static boolean isScp1576Channel(UUID channelId) {
        if (channelId == null) return false;
        synchronized (CODEC_LOCK) {
            return SCP1576_CHANNEL_IDS.contains(channelId);
        }
    }

    private static void pruneInactiveChannels(
            List<Scp1576Manager.VoiceSource> sources) {
        Set<UUID> activeSessions = new HashSet<>();
        for (Scp1576Manager.VoiceSource source : sources) {
            activeSessions.add(source.sessionId());
        }
        synchronized (CODEC_LOCK) {
            CHANNELS.entrySet().removeIf(entry -> {
                if (activeSessions.contains(entry.getKey().sessionId)) {
                    return false;
                }
                entry.getValue().close();
                SCP1576_CHANNEL_IDS.remove(channelId(entry.getKey().sessionId,
                        entry.getKey().speakerId));
                return true;
            });
        }
    }

    public static void forgetSpeaker(UUID speakerId) {
        if (speakerId == null) return;
        synchronized (CODEC_LOCK) {
            OpusDecoder decoder = DECODERS.remove(speakerId);
            close(decoder);
            CHANNELS.entrySet().removeIf(entry -> {
                if (!speakerId.equals(entry.getKey().speakerId)) return false;
                entry.getValue().close();
                SCP1576_CHANNEL_IDS.remove(channelId(entry.getKey().sessionId,
                        entry.getKey().speakerId));
                return true;
            });
        }
    }

    public static void closeAll() {
        synchronized (CODEC_LOCK) {
            DECODERS.values().forEach(Scp1576VoiceChatBridge::close);
            DECODERS.clear();
            CHANNELS.values().forEach(FilterChannel::close);
            CHANNELS.clear();
            SCP1576_CHANNEL_IDS.clear();
        }
    }

    private static short[] decode(VoicechatServerApi api, UUID speakerId,
            byte[] opus) {
        synchronized (CODEC_LOCK) {
            try {
                OpusDecoder decoder = DECODERS.computeIfAbsent(speakerId,
                        ignored -> api.createDecoder());
                return decoder == null ? null : decoder.decode(opus);
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "SCP-1576 could not decode one voice frame; forwarding it without the spectral filter",
                        exception);
                return null;
            }
        }
    }

    private static byte[] encodeFiltered(VoicechatServerApi api, UUID speakerId,
            Scp1576Manager.VoiceSource source, short[] decoded) {
        ChannelKey key = new ChannelKey(source.sessionId(), speakerId);
        synchronized (CODEC_LOCK) {
            try {
                FilterChannel channel = CHANNELS.computeIfAbsent(key,
                        ignored -> new FilterChannel(api.createEncoder()));
                if (channel.encoder == null) return null;
                short[] filtered = channel.filter(decoded, voiceGain(source));
                return channel.encoder.encode(filtered);
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "SCP-1576 could not encode one filtered voice frame",
                        exception);
                return null;
            }
        }
    }

    private static float voiceGain(Scp1576Manager.VoiceSource source) {
        float fadeIn = Mth.clamp(source.voiceAgeTicks() / 20.0F,
                0.0F, 1.0F);
        float fadeOut = Mth.clamp(source.remainingTicks() / 30.0F,
                0.0F, 1.0F);
        return fadeIn * fadeOut;
    }

    private static boolean sendToLivingReceivers(VoicechatServerApi api,
            MinecraftServer server, Scp1576Manager.VoiceSource source,
            LocationalSoundPacket packet) {
        boolean sent = false;
        for (ServerPlayer receiver : server.getPlayerList().getPlayers()) {
            if (!receiver.level().dimension().equals(source.dimension())
                    || DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)) {
                continue;
            }
            VoicechatConnection connection = api.getConnectionOf(receiver.getUUID());
            if (connection == null || !connection.isConnected()
                    || !connection.isInstalled()) {
                continue;
            }
            try {
                api.sendLocationalSoundPacketTo(connection, packet);
                sent = true;
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "Could not send an SCP-1576 voice frame", exception);
            }
        }
        return sent;
    }

    private static ServerPlayer minecraftPlayer(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null) return null;
        Object player = connection.getPlayer().getPlayer();
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static UUID channelId(UUID sessionId, UUID speakerId) {
        return UUID.nameUUIDFromBytes((ScpClassifiedDirectiveMod.MODID + ":scp1576:"
                + sessionId + ":" + speakerId)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static void close(OpusDecoder decoder) {
        if (decoder == null) return;
        try {
            decoder.close();
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private record ChannelKey(UUID sessionId, UUID speakerId) {
    }

    private static final class FilterChannel {
        private static final int DELAY_SIZE = 2048;
        private final OpusEncoder encoder;
        private final short[] spectralDelay = new short[DELAY_SIZE];
        private double lowPass;
        private double rumble;
        private long sampleClock;
        private int delayIndex;
        private int glitchState = 0x1576A11;
        private int dropoutRemaining;
        private double dropoutDepth = 1.0D;
        private double dropoutEnvelope = 1.0D;

        private FilterChannel(OpusEncoder encoder) {
            this.encoder = encoder;
        }

        private short[] filter(short[] input, float gain) {
            short[] output = new short[input.length];

            // Roughly once per second at normal 20 ms voice frames, create a
            // very short loss of intelligibility rather than a full mute. It is
            // smoothed in and out so the interruption feels like a failing
            // anomalous transmission, not packet loss or digital clicking.
            if (dropoutRemaining <= 0 && nextGlitchInt(50) == 0) {
                dropoutRemaining = 240 + nextGlitchInt(720);
                dropoutDepth = 0.22D + nextGlitchInt(34) / 100.0D;
            }

            for (int i = 0; i < input.length; i++) {
                double sample = input[i];

                // Slowly move the spectral window. No noise is added here: the
                // communicator's authored active sound already owns the hiss.
                double timbre = 0.235D
                        + Math.sin(sampleClock * 0.00019D) * 0.055D
                        + Math.sin(sampleClock * 0.000047D) * 0.025D;
                timbre = Mth.clamp(timbre, 0.145D, 0.325D);
                lowPass += timbre * (sample - lowPass);
                rumble += 0.026D * (lowPass - rumble);
                double band = lowPass - rumble;

                // A tiny, slowly changing delayed copy produces shifting comb
                // coloration. The result is a subtle second "presence" in the
                // voice rather than an obvious echo effect.
                int delaySamples = 245 + (int) Math.round(
                        (Math.sin(sampleClock * 0.000083D) + 1.0D) * 62.0D);
                int readIndex = delayIndex - delaySamples;
                if (readIndex < 0) readIndex += DELAY_SIZE;
                double delayed = spectralDelay[readIndex];
                spectralDelay[delayIndex] = (short) Mth.clamp(
                        (int) Math.round(band), Short.MIN_VALUE, Short.MAX_VALUE);
                delayIndex = (delayIndex + 1) % DELAY_SIZE;

                double ghostMix = 0.075D
                        + (Math.sin(sampleClock * 0.000121D) + 1.0D) * 0.025D;
                double colored = band + delayed * ghostMix;
                double saturated = Math.tanh(colored / 8600.0D) * 11900.0D;

                // Gentle amplitude flutter changes the apparent timbre and
                // mechanical stability without obscuring ordinary speech.
                double flutter = 0.965D
                        + Math.sin(sampleClock * 0.00037D) * 0.018D
                        + Math.sin(sampleClock * 0.00111D) * 0.010D;

                double dropoutTarget = dropoutRemaining > 0
                        ? dropoutDepth : 1.0D;
                double response = dropoutTarget < dropoutEnvelope
                        ? 0.24D : 0.065D;
                dropoutEnvelope += response
                        * (dropoutTarget - dropoutEnvelope);
                if (dropoutRemaining > 0) dropoutRemaining--;

                // Mild resolution loss keeps the voice materially different
                // from normal proximity chat without introducing white noise.
                double quantized = Math.rint(saturated / 28.0D) * 28.0D;
                int value = Mth.clamp((int) Math.round(quantized * flutter
                                * dropoutEnvelope * gain),
                        Short.MIN_VALUE, Short.MAX_VALUE);
                output[i] = (short) value;
                sampleClock++;
            }
            return output;
        }

        private int nextGlitchInt(int bound) {
            glitchState = glitchState * 1664525 + 1013904223;
            return Integer.remainderUnsigned(glitchState, bound);
        }

        private void close() {
            if (encoder == null) return;
            try {
                encoder.close();
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }
}
