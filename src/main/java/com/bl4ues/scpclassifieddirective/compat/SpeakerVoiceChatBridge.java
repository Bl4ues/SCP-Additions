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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Relays an operator microphone through active facility speaker endpoints. */
public final class SpeakerVoiceChatBridge {
    private static final Object CODEC_LOCK = new Object();
    private static final Map<UUID, OpusDecoder> DECODERS = new HashMap<>();
    private static final Map<ChannelKey, FilterChannel> CHANNELS = new HashMap<>();

    // Temporary QA monitor. Flip this single switch off once the Speaker filter
    // has been validated; ordinary receivers are unaffected either way.
    private static final boolean OPERATOR_SELF_MONITOR = true;
    private static final double VOICE_OUTPUT_GAIN = 0.50D;

    private SpeakerVoiceChatBridge() {
    }

    public static void relay(MicrophonePacketEvent event, ServerPlayer operator) {
        if (event == null || operator == null || operator.getServer() == null
                || !ModCompatibilityConfig.simpleVoiceChatEnabled()) return;
        List<SpeakerBroadcastManager.VoiceSource> sources =
                SpeakerBroadcastManager.voiceSources(operator.getServer(),
                        operator.getUUID());
        prune(operator.getUUID(), sources);
        if (sources.isEmpty()) return;

        VoicechatServerApi api = event.getVoicechat();
        MicrophonePacket microphone = event.getPacket();
        byte[] opus = microphone.getOpusEncodedData();
        if (api == null || opus == null || opus.length == 0) return;
        short[] decoded = decode(api, operator.getUUID(), opus);
        if (decoded == null || decoded.length == 0) return;

        for (SpeakerBroadcastManager.VoiceSource source : sources) {
            byte[] filtered = encode(api, operator.getUUID(), source, decoded);
            if (filtered == null || filtered.length == 0) continue;
            LocationalSoundPacket packet;
            try {
                packet = microphone.locationalSoundPacketBuilder()
                        .channelId(channelId(operator.getUUID(), source))
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
            sendToReceivers(api, operator.getServer(), operator, source, packet);
        }
    }

    private static short[] decode(VoicechatServerApi api, UUID operator,
            byte[] opus) {
        synchronized (CODEC_LOCK) {
            try {
                OpusDecoder decoder = DECODERS.computeIfAbsent(operator,
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

    private static byte[] encode(VoicechatServerApi api, UUID operator,
            SpeakerBroadcastManager.VoiceSource source, short[] decoded) {
        ChannelKey key = new ChannelKey(operator,
                source.dimension().location().toString(), source.pos().asLong());
        synchronized (CODEC_LOCK) {
            try {
                FilterChannel channel = CHANNELS.computeIfAbsent(key,
                        ignored -> new FilterChannel(api.createEncoder()));
                if (channel.encoder == null) return null;
                return channel.encoder.encode(channel.filter(decoded));
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "Could not encode a filtered facility Speaker voice frame",
                        exception);
                return null;
            }
        }
    }

    private static void sendToReceivers(VoicechatServerApi api,
            MinecraftServer server, ServerPlayer operator,
            SpeakerBroadcastManager.VoiceSource source,
            LocationalSoundPacket packet) {
        for (ServerPlayer receiver : server.getPlayerList().getPlayers()) {
            if (!receiver.level().dimension().equals(source.dimension())
                    || DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)
                    || (!OPERATOR_SELF_MONITOR
                    && receiver.getUUID().equals(operator.getUUID()))) {
                continue;
            }
            VoicechatConnection connection = api.getConnectionOf(
                    receiver.getUUID());
            if (connection == null || !connection.isConnected()
                    || !connection.isInstalled()) continue;
            try {
                // While QA monitoring is enabled, the operator receives the
                // same positional Speaker packet as everyone else. There is no
                // separate camera-anchored or non-positional echo path.
                api.sendLocationalSoundPacketTo(connection, packet);
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.debug(
                        "Could not send a facility Speaker voice frame",
                        exception);
            }
        }
    }

    private static void prune(UUID operator,
            List<SpeakerBroadcastManager.VoiceSource> sources) {
        Set<ChannelKey> active = new HashSet<>();
        for (SpeakerBroadcastManager.VoiceSource source : sources) {
            active.add(new ChannelKey(operator,
                    source.dimension().location().toString(),
                    source.pos().asLong()));
        }
        synchronized (CODEC_LOCK) {
            CHANNELS.entrySet().removeIf(entry -> {
                if (!entry.getKey().operator.equals(operator)
                        || active.contains(entry.getKey())) return false;
                entry.getValue().close();
                return true;
            });
        }
    }

    public static void forgetOperator(UUID operator) {
        if (operator == null) return;
        synchronized (CODEC_LOCK) {
            close(DECODERS.remove(operator));
            CHANNELS.entrySet().removeIf(entry -> {
                if (!entry.getKey().operator.equals(operator)) return false;
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

    private static UUID channelId(UUID operator,
            SpeakerBroadcastManager.VoiceSource source) {
        String key = ScpClassifiedDirectiveMod.MODID + ":speaker:"
                + operator + ":" + source.dimension().location() + ":"
                + source.pos().asLong();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static void close(OpusDecoder decoder) {
        if (decoder == null) return;
        try {
            decoder.close();
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private record ChannelKey(UUID operator, String dimension, long position) {
    }

    /**
     * Intentionally obvious low-quality PA/radio coloration. The authored
     * Speaker loop already provides static, so this stage only narrows the
     * spectrum, reduces temporal/detail resolution and compresses the voice.
     */
    private static final class FilterChannel {
        private final OpusEncoder encoder;
        private double lowCut;
        private double bandOne;
        private double bandTwo;
        private double heldSample;
        private long sampleClock;

        private FilterChannel(OpusEncoder encoder) {
            this.encoder = encoder;
        }

        private short[] filter(short[] input) {
            short[] output = new short[input.length];
            for (int index = 0; index < input.length; index++) {
                double sample = input[index];

                // Remove bass first, then pass the remainder through two
                // low-pass stages. At the 48 kHz SVC sample rate this leaves a
                // deliberately narrow telephone/PA speech band.
                lowCut += 0.045D * (sample - lowCut);
                double highPassed = sample - lowCut;
                bandOne += 0.34D * (highPassed - bandOne);
                bandTwo += 0.34D * (bandOne - bandTwo);

                // Hard compression/saturation gives the small wall speaker its
                // unmistakable cheap-amplifier character without adding hiss.
                double saturated = Math.tanh(bandTwo / 3000.0D) * 13200.0D;
                double clipped = Mth.clamp(saturated, -10800.0D, 10800.0D);
                double quantized = Math.rint(clipped / 240.0D) * 240.0D;

                // Hold every third sample. This keeps speech intelligible while
                // removing the pristine high-resolution quality of normal voice
                // chat and remains deterministic/noise-free.
                if (sampleClock % 3L == 0L) heldSample = quantized;
                sampleClock++;

                output[index] = (short) Mth.clamp(
                        (int) Math.round(heldSample * 1.08D
                                * VOICE_OUTPUT_GAIN),
                        Short.MIN_VALUE, Short.MAX_VALUE);
            }
            return output;
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
