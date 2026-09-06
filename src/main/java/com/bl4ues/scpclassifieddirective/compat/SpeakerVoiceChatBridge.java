package com.bl4ues.scpclassifieddirective.compat;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.death.DeathSpectateCoordinator;
import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerBroadcastManager;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Relays microphone audio through active facility Speaker endpoints. */
public final class SpeakerVoiceChatBridge {
    private static final Object CODEC_LOCK = new Object();
    private static final Map<UUID, OpusDecoder> DECODERS = new HashMap<>();
    private static final Map<ChannelKey, FilterChannel> CHANNELS = new HashMap<>();

    // Speaker users can hear the same physical endpoint packet as everybody
    // else. This is intentional for Intercom proximity and current 079 QA.
    private static final boolean OPERATOR_SELF_MONITOR = true;
    private static final double VOICE_OUTPUT_GAIN = 0.50D;
    private static final double VOICE_SAMPLE_RATE = 48_000.0D;

    private SpeakerVoiceChatBridge() {
    }

    /** SCP-079's operator microphone, already routed to its selected Speakers. */
    public static void relay(MicrophonePacketEvent event, ServerPlayer operator) {
        if (event == null || operator == null || operator.getServer() == null
                || !ModCompatibilityConfig.simpleVoiceChatEnabled()) return;
        relaySources(event, operator, SpeakerBroadcastManager.voiceSources(
                operator.getServer(), operator.getUUID()));
    }

    /** Any ordinary player inside an active Intercom's five-block pickup radius. */
    public static void relayIntercom(MicrophonePacketEvent event,
            ServerPlayer speaker) {
        if (event == null || speaker == null || speaker.getServer() == null
                || !ModCompatibilityConfig.simpleVoiceChatEnabled()) return;
        relaySources(event, speaker,
                SpeakerBroadcastManager.intercomVoiceSources(speaker));
    }

    private static void relaySources(MicrophonePacketEvent event,
            ServerPlayer speaker,
            List<SpeakerBroadcastManager.VoiceSource> sources) {
        prune(speaker.getUUID(), sources);
        if (sources.isEmpty()) return;

        VoicechatServerApi api = event.getVoicechat();
        MicrophonePacket microphone = event.getPacket();
        byte[] opus = microphone.getOpusEncodedData();
        if (api == null || opus == null || opus.length == 0) return;
        short[] decoded = decode(api, speaker.getUUID(), opus);
        if (decoded == null || decoded.length == 0) return;

        for (SpeakerBroadcastManager.VoiceSource source : sources) {
            byte[] filtered = encode(api, speaker.getUUID(), source, decoded);
            if (filtered == null || filtered.length == 0) continue;
            LocationalSoundPacket packet;
            try {
                packet = microphone.locationalSoundPacketBuilder()
                        .channelId(channelId(speaker.getUUID(), source))
                        .opusEncodedData(filtered)
                        .position(api.createPosition(source.position().x,
                                source.position().y, source.position().z))
                        .distance((float) api.getVoiceChatDistance())
                        .build();
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.error(
                        "Could not build facility Speaker voice packet",
                        exception);
                continue;
            }
            sendToReceivers(api, speaker.getServer(), speaker, source, packet);
        }
    }

    private static short[] decode(VoicechatServerApi api, UUID speaker,
            byte[] opus) {
        synchronized (CODEC_LOCK) {
            try {
                OpusDecoder decoder = DECODERS.computeIfAbsent(speaker,
                        ignored -> api.createDecoder());
                return decoder == null ? null : decoder.decode(opus);
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "Could not decode a facility Speaker voice frame",
                        exception);
                return null;
            }
        }
    }

    private static byte[] encode(VoicechatServerApi api, UUID speaker,
            SpeakerBroadcastManager.VoiceSource source, short[] decoded) {
        ChannelKey key = new ChannelKey(speaker, source.sourceId(),
                source.dimension().location().toString(), source.pos().asLong(),
                source.sourceType());
        synchronized (CODEC_LOCK) {
            try {
                FilterChannel channel = CHANNELS.computeIfAbsent(key,
                        ignored -> new FilterChannel(api.createEncoder()));
                if (channel.encoder == null) return null;
                return channel.encoder.encode(channel.filter(decoded,
                        source.sourceType(), source.captureGain()));
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "Could not encode a filtered facility Speaker voice frame",
                        exception);
                return null;
            }
        }
    }

    private static void sendToReceivers(VoicechatServerApi api,
            MinecraftServer server, ServerPlayer speaker,
            SpeakerBroadcastManager.VoiceSource source,
            LocationalSoundPacket packet) {
        for (ServerPlayer receiver : server.getPlayerList().getPlayers()) {
            if (!receiver.level().dimension().equals(source.dimension())
                    || DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)
                    || (!OPERATOR_SELF_MONITOR
                    && receiver.getUUID().equals(speaker.getUUID()))) {
                continue;
            }
            VoicechatConnection connection = api.getConnectionOf(
                    receiver.getUUID());
            if (connection == null || !connection.isConnected()
                    || !connection.isInstalled()) continue;
            try {
                api.sendLocationalSoundPacketTo(connection, packet);
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "Could not send a facility Speaker voice frame",
                        exception);
            }
        }
    }

    private static void prune(UUID speaker,
            List<SpeakerBroadcastManager.VoiceSource> sources) {
        Set<ChannelKey> active = new HashSet<>();
        for (SpeakerBroadcastManager.VoiceSource source : sources) {
            active.add(new ChannelKey(speaker, source.sourceId(),
                    source.dimension().location().toString(),
                    source.pos().asLong(), source.sourceType()));
        }
        synchronized (CODEC_LOCK) {
            CHANNELS.entrySet().removeIf(entry -> {
                if (!entry.getKey().speaker.equals(speaker)
                        || active.contains(entry.getKey())) return false;
                entry.getValue().close();
                return true;
            });
        }
    }

    public static void forgetOperator(UUID speaker) {
        if (speaker == null) return;
        synchronized (CODEC_LOCK) {
            close(DECODERS.remove(speaker));
            CHANNELS.entrySet().removeIf(entry -> {
                if (!entry.getKey().speaker.equals(speaker)) return false;
                entry.getValue().close();
                return true;
            });
        }
    }

    public static void closeAll() {
        synchronized (CODEC_LOCK) {
            DECODERS.values().forEach(SpeakerVoiceChatBridge::close);
            DECODERS.clear();
            CHANNELS.values().forEach(FilterChannel::close);
            CHANNELS.clear();
        }
    }

    private static UUID channelId(UUID speaker,
            SpeakerBroadcastManager.VoiceSource source) {
        String key = ScpClassifiedDirectiveMod.MODID + ":speaker:"
                + source.sourceType().name() + ":" + speaker + ":"
                + source.sourceId() + ":" + source.dimension().location()
                + ":" + source.pos().asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static void close(OpusDecoder decoder) {
        if (decoder == null) return;
        try {
            decoder.close();
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private record ChannelKey(UUID speaker, UUID sourceId, String dimension,
            long position, SpeakerBroadcastManager.SourceType sourceType) {
    }

    /** Shared cheap-PA filter, with the extra robot chain exclusive to SCP-079. */
    private static final class FilterChannel {
        private static final int ROBOT_DELAY_SAMPLES = 168;
        private static final double ROBOT_RING_HZ = 86.0D;

        private final OpusEncoder encoder;
        private final double[] robotDelay = new double[ROBOT_DELAY_SAMPLES];
        private double lowCut;
        private double bandOne;
        private double bandTwo;
        private double heldSample;
        private double robotResonance;
        private double robotHeldSample;
        private double robotPhase;
        private long sampleClock;
        private long robotClock;
        private int robotDelayIndex;

        private FilterChannel(OpusEncoder encoder) {
            this.encoder = encoder;
        }

        private short[] filter(short[] input,
                SpeakerBroadcastManager.SourceType sourceType,
                float captureGain) {
            short[] output = new short[input.length];
            boolean robotic = sourceType
                    == SpeakerBroadcastManager.SourceType.SCP_079;
            double capture = Mth.clamp(captureGain, 0.0F, 1.0F);
            for (int index = 0; index < input.length; index++) {
                double sample = input[index];

                // Shared Speaker coloration: remove bass, narrow the speech band
                // and compress it hard enough to sound like a cheap wall PA.
                lowCut += 0.045D * (sample - lowCut);
                double highPassed = sample - lowCut;
                bandOne += 0.34D * (highPassed - bandOne);
                bandTwo += 0.34D * (bandOne - bandTwo);
                double saturated = Math.tanh(bandTwo / 3000.0D) * 13200.0D;
                double clipped = Mth.clamp(saturated, -10800.0D, 10800.0D);
                double quantized = Math.rint(clipped / 240.0D) * 240.0D;
                if (sampleClock % 3L == 0L) heldSample = quantized;
                sampleClock++;

                double processed = heldSample * 1.08D;
                if (robotic) processed = robotize(processed);
                processed *= capture;
                output[index] = (short) Mth.clamp(
                        (int) Math.round(processed * VOICE_OUTPUT_GAIN),
                        Short.MIN_VALUE, Short.MAX_VALUE);
            }
            return output;
        }

        private double robotize(double sample) {
            double delayed = robotDelay[robotDelayIndex];
            robotDelay[robotDelayIndex] = sample;
            robotDelayIndex = (robotDelayIndex + 1) % robotDelay.length;

            double comb = sample * 0.74D + delayed * 0.34D;
            robotResonance += 0.22D * (comb - robotResonance);
            double metallic = comb * 0.72D + robotResonance * 0.42D;

            robotPhase += (Math.PI * 2.0D * ROBOT_RING_HZ)
                    / VOICE_SAMPLE_RATE;
            if (robotPhase >= Math.PI * 2.0D) robotPhase -= Math.PI * 2.0D;
            double ring = metallic * (0.76D + 0.24D * Math.sin(robotPhase));
            double stepped = Math.rint(ring / 420.0D) * 420.0D;

            if (robotClock % 2L == 0L) robotHeldSample = stepped;
            robotClock++;
            return Mth.clamp(robotHeldSample, -11800.0D, 11800.0D);
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
