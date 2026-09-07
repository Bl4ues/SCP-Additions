package com.bl4ues.scpclassifieddirective.compat;

import com.bl4ues.scpclassifieddirective.facility.Scp079SpeechManager;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Streams generated 48 kHz PCM through Simple Voice Chat positional channels. */
final class Scp079SpeechVoiceChatBackend
        implements Scp079SpeechManager.PlaybackBackend {
    private final VoicechatServerApi api;

    Scp079SpeechVoiceChatBackend(VoicechatServerApi api) {
        this.api = api;
    }

    @Override
    public Scp079SpeechManager.Playback play(MinecraftServer server,
            List<Scp079SpeechManager.Output> outputs, short[] pcm48Khz,
            Runnable onStopped) {
        if (server == null || outputs == null || outputs.isEmpty()
                || pcm48Khz == null || pcm48Khz.length == 0) return null;

        List<AudioPlayer> players = new ArrayList<>();
        for (Scp079SpeechManager.Output output : outputs) {
            if (output == null) continue;
            ServerLevel level = server.getLevel(output.dimension());
            if (level == null) continue;
            LocationalAudioChannel channel = api.createLocationalAudioChannel(
                    UUID.randomUUID(), api.fromServerLevel(level),
                    api.createPosition(output.position().x, output.position().y,
                            output.position().z));
            if (channel == null) continue;
            channel.setDistance(output.distance());
            AudioPlayer player = api.createAudioPlayer(channel,
                    api.createEncoder(), pcm48Khz);
            if (player != null) players.add(player);
        }
        if (players.isEmpty()) return null;

        AtomicInteger remaining = new AtomicInteger(players.size());
        AtomicBoolean completed = new AtomicBoolean();
        Runnable finishedOne = () -> {
            if (remaining.decrementAndGet() <= 0
                    && completed.compareAndSet(false, true)
                    && onStopped != null) {
                onStopped.run();
            }
        };
        players.forEach(player -> player.setOnStopped(finishedOne));
        players.forEach(AudioPlayer::startPlaying);

        return () -> {
            for (AudioPlayer player : players) {
                if (!player.isStopped()) player.stopPlaying();
            }
            if (players.stream().allMatch(AudioPlayer::isStopped)
                    && completed.compareAndSet(false, true)
                    && onStopped != null) {
                onStopped.run();
            }
        };
    }
}
