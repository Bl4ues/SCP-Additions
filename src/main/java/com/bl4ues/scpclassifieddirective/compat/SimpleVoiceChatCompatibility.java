package com.bl4ues.scpclassifieddirective.compat;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.packets.EntitySoundPacket;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticStimulusSystem;
import com.bl4ues.scpclassifieddirective.death.DeathSpectateCoordinator;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.effect.Scp714ExposureManager;
import com.bl4ues.scpclassifieddirective.scp939.Scp939MimicryHooks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional Simple Voice Chat bridge for SCP: Classified Directive.
 *
 * Living players keep Simple Voice Chat's normal routing. Dead players are
 * isolated from living receivers, share a non-positional dead-only call, and
 * receive the exact voice-chat packets that reach the living player they are
 * currently watching. The watched player's own microphone is added explicitly,
 * since Simple Voice Chat normally never echoes a speaker back to themselves.
 *
 * Living microphone packets also publish positional acoustic evidence for
 * sound-driven SCP AI. SCP-939 voice mimicry is a separate, explicit opt-in
 * feature: only consenting players have compressed Opus fragments copied into a
 * short in-memory ring buffer. Nothing is written to disk, buffers expire, and
 * opting out or disconnecting clears the player's fragments immediately.
 */
@ForgeVoicechatPlugin
public final class SimpleVoiceChatCompatibility implements VoicechatPlugin {
    private static final String PLUGIN_ID = "scp_classified_directive_death_voice";
    private static final int MIN_API_MAJOR = 2;
    private static final int MIN_API_MINOR = 6;
    private static final int MIN_API_PATCH = 20;
    private static final int MICROPHONE_PRIORITY = Integer.MAX_VALUE;
    private static final int ROUTING_PRIORITY = Integer.MIN_VALUE;
    private static final Pattern SEMVER = Pattern.compile(
            "(\\d+)\\.(\\d+)\\.(\\d+)");

    private static final int MAX_MIMIC_FRAMES_PER_SPEAKER = 360;
    private static final int MIN_MIMIC_CLIP_FRAMES = 18;
    private static final int MAX_MIMIC_CLIP_FRAMES = 180;
    private static final int MIMIC_FRAMES_PER_SERVER_TICK = 3;
    private static final long MIMIC_FRAME_EXPIRY_NANOS =
            TimeUnit.SECONDS.toNanos(120L);
    private static final long MIMIC_UTTERANCE_GAP_NANOS =
            TimeUnit.MILLISECONDS.toNanos(650L);
    private static final float MIMIC_DISTANCE = 24.0F;

    /**
     * API sends synchronously pass through the packet events again. Mark our own
     * forwards so the dead-receiver isolation rule does not cancel them or mirror
     * them recursively.
     */
    private static final ThreadLocal<Boolean> FORWARDING =
            ThreadLocal.withInitial(() -> false);

    private static final Object MIMIC_LOCK = new Object();
    private static final Map<UUID, SpeakerBuffer> MIMIC_BUFFERS =
            new HashMap<>();
    private static final Set<UUID> MIMIC_CONSENT =
            ConcurrentHashMap.newKeySet();
    private static final Set<UUID> MIMIC_PROMPTED =
            ConcurrentHashMap.newKeySet();
    private static final Map<UUID, MimicPlayback> ACTIVE_MIMICS =
            new ConcurrentHashMap<>();
    private static volatile VoicechatServerApi serverApi;

    private static final Scp939MimicryHooks.Backend MIMICRY_BACKEND =
            new Scp939MimicryHooks.Backend() {
                @Override
                public boolean request(ServerLevel level, UUID scp939Id,
                        Vec3 position, UUID preferredSpeaker) {
                    return requestMimicPlayback(level, scp939Id,
                            preferredSpeaker);
                }

                @Override
                public boolean setConsent(ServerPlayer player, boolean allowed) {
                    return setMimicConsent(player, allowed);
                }

                @Override
                public boolean hasConsent(UUID playerId) {
                    return playerId != null && MIMIC_CONSENT.contains(playerId);
                }

                @Override
                public void forget(UUID playerId) {
                    forgetMimicPlayer(playerId);
                }
            };

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        if (!supportedApiInstalled()) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Simple Voice Chat was detected, but SCP: Classified Directive voice integration requires API 2.6.20 or newer; integration will remain disabled");
            return;
        }

        // Register once and consult the host-owned setting at packet time. This
        // allows the Config Center toggle to take effect immediately at runtime.
        registration.registerEvent(VoicechatServerStartedEvent.class,
                SimpleVoiceChatCompatibility::onVoiceServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class,
                SimpleVoiceChatCompatibility::onVoiceServerStopped);
        registration.registerEvent(PlayerDisconnectedEvent.class,
                SimpleVoiceChatCompatibility::onVoicePlayerDisconnected);
        registration.registerEvent(MicrophonePacketEvent.class,
                SimpleVoiceChatCompatibility::onMicrophone,
                MICROPHONE_PRIORITY);
        registration.registerEvent(EntitySoundPacketEvent.class,
                SimpleVoiceChatCompatibility::onEntitySound,
                ROUTING_PRIORITY);
        registration.registerEvent(LocationalSoundPacketEvent.class,
                SimpleVoiceChatCompatibility::onLocationalSound,
                ROUTING_PRIORITY);
        registration.registerEvent(StaticSoundPacketEvent.class,
                SimpleVoiceChatCompatibility::onStaticSound,
                ROUTING_PRIORITY);

        ScpClassifiedDirectiveMod.LOGGER.info(
                "Registered Simple Voice Chat death/spectate and SCP-939 mimicry integration");
    }

    private static void onVoiceServerStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
        Scp939MimicryHooks.install(MIMICRY_BACKEND);
    }

    private static void onVoiceServerStopped(VoicechatServerStoppedEvent event) {
        stopAllMimics();
        synchronized (MIMIC_LOCK) {
            MIMIC_BUFFERS.clear();
        }
        MIMIC_CONSENT.clear();
        MIMIC_PROMPTED.clear();
        serverApi = null;
        Scp939MimicryHooks.uninstall(MIMICRY_BACKEND);
    }

    private static void onVoicePlayerDisconnected(PlayerDisconnectedEvent event) {
        forgetMimicPlayer(event.getPlayerUuid());
    }

    private static void onMicrophone(MicrophonePacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled() || forwarding()) {
            return;
        }
        ServerPlayer sender = minecraftPlayer(event.getSenderConnection());
        if (sender == null) return;

        // Camera-mode SCP-079 is an implementation spectator, not a physical
        // microphone source. Its optional room broadcast is handled by the
        // dedicated guard regardless of voice-plugin registration order.
        if (Scp079PlayableManager.isCameraMode(sender)) return;

        // A MineZero SCP-714 coma removes player agency completely. Cancel the
        // microphone at the earliest routing point so an unconscious player
        // cannot speak through proximity/groups, feed dead spectators, create
        // SCP-939 acoustic evidence, or contribute new mimicry samples.
        if (Scp714ExposureManager.isComatose(sender)) {
            event.cancel();
            return;
        }

        VoicechatServerApi api = event.getVoicechat();
        if (api != null) {
            serverApi = api;
            if (!Scp939MimicryHooks.available()) {
                Scp939MimicryHooks.install(MIMICRY_BACKEND);
            }
        }

        if (DeathSpectateCoordinator.isDeadVoiceParticipant(sender)) {
            // Cancel first. Even if another part of the bridge fails, a dead
            // player's microphone must never fall through to living proximity or
            // become physical acoustic evidence for sound-driven SCPs.
            event.cancel();
            StaticSoundPacket packet;
            try {
                packet = event.getPacket().staticSoundPacketBuilder()
                        .channelId(deadCallChannel(sender.getUUID()))
                        .build();
            } catch (RuntimeException | LinkageError exception) {
                ScpClassifiedDirectiveMod.LOGGER.error(
                        "Could not convert dead-player microphone audio for Simple Voice Chat",
                        exception);
                return;
            }

            if (sender.getServer() == null) return;
            for (ServerPlayer dead : DeathSpectateCoordinator
                    .deadVoiceParticipants(sender.getServer())) {
                if (dead.getUUID().equals(sender.getUUID())) continue;
                sendStatic(api, dead, packet);
            }
            return;
        }

        AcousticStimulusSystem.emitVoice(sender,
                event.getPacket().isWhispering() ? 0.45F : 1.00F);
        captureForMimicry(sender, event.getPacket());

        // The watched player does not hear their own microphone through normal
        // Simple Voice Chat routing, but a dead observer must. Feed it directly
        // as entity audio while preserving sender identity and packet sequence.
        List<ServerPlayer> observers = DeathSpectateCoordinator.observersOf(sender);
        if (observers.isEmpty()) return;

        EntitySoundPacket packet;
        try {
            packet = event.getPacket().entitySoundPacketBuilder()
                    .entityUuid(sender.getUUID())
                    .whispering(event.getPacket().isWhispering())
                    .build();
        } catch (RuntimeException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not mirror the watched player's microphone through Simple Voice Chat",
                    exception);
            return;
        }
        for (ServerPlayer observer : observers) {
            sendEntity(api, observer, packet);
        }
    }

    private static void onEntitySound(EntitySoundPacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled() || forwarding()) {
            return;
        }
        ServerPlayer receiver = minecraftPlayer(event.getReceiverConnection());
        if (receiver == null) return;

        if (DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)) {
            event.cancel();
            return;
        }

        for (ServerPlayer observer : DeathSpectateCoordinator.observersOf(receiver)) {
            sendEntity(event.getVoicechat(), observer, event.getPacket());
        }
    }

    private static void onLocationalSound(LocationalSoundPacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled() || forwarding()) {
            return;
        }
        ServerPlayer receiver = minecraftPlayer(event.getReceiverConnection());
        if (receiver == null) return;

        if (DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)) {
            event.cancel();
            return;
        }

        for (ServerPlayer observer : DeathSpectateCoordinator.observersOf(receiver)) {
            sendLocational(event.getVoicechat(), observer, event.getPacket());
        }
    }

    private static void onStaticSound(StaticSoundPacketEvent event) {
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled() || forwarding()) {
            return;
        }
        ServerPlayer receiver = minecraftPlayer(event.getReceiverConnection());
        if (receiver == null) return;

        if (DeathSpectateCoordinator.isDeadVoiceParticipant(receiver)) {
            event.cancel();
            return;
        }

        for (ServerPlayer observer : DeathSpectateCoordinator.observersOf(receiver)) {
            sendStatic(event.getVoicechat(), observer, event.getPacket());
        }
    }

    private static void captureForMimicry(ServerPlayer sender,
            MicrophonePacket packet) {
        UUID playerId = sender.getUUID();
        if (!MIMIC_CONSENT.contains(playerId)) {
            promptForMimicConsent(sender);
            return;
        }

        byte[] opus = packet.getOpusEncodedData();
        if (opus == null || opus.length == 0) return;
        VoiceFrame frame = new VoiceFrame(Arrays.copyOf(opus, opus.length),
                packet.isWhispering(), System.nanoTime());
        synchronized (MIMIC_LOCK) {
            SpeakerBuffer buffer = MIMIC_BUFFERS.computeIfAbsent(playerId,
                    ignored -> new SpeakerBuffer());
            buffer.frames.addLast(frame);
            pruneBuffer(buffer, frame.capturedNanos);
        }
    }

    private static void promptForMimicConsent(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!MIMIC_PROMPTED.add(playerId)) return;
        if (player.getServer() == null) return;

        player.getServer().execute(() -> {
            if (!player.isAlive() || !ModCompatibilityConfig
                    .simpleVoiceChatEnabled()) return;

            Component allow = Component.literal("[ALLOW THIS SESSION]")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.RED)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/scp939 mimicry allow"))
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal(
                                            "Allows SCP-939 to temporarily reuse short fragments of your Simple Voice Chat audio. Nothing is written to disk."))));

            player.sendSystemMessage(Component.literal(
                    "SCP-939 voice mimicry is opt-in. ")
                    .append(allow)
                    .append(Component.literal(
                            " Voice fragments are kept only in memory, expire automatically, and are cleared on opt-out or disconnect.")));
        });
    }

    private static boolean setMimicConsent(ServerPlayer player,
            boolean allowed) {
        if (player == null || serverApi == null
                || !ModCompatibilityConfig.simpleVoiceChatEnabled()) {
            return false;
        }
        UUID playerId = player.getUUID();
        MIMIC_PROMPTED.add(playerId);
        if (allowed) {
            MIMIC_CONSENT.add(playerId);
        } else {
            MIMIC_CONSENT.remove(playerId);
            synchronized (MIMIC_LOCK) {
                MIMIC_BUFFERS.remove(playerId);
            }
        }
        return true;
    }

    private static void forgetMimicPlayer(UUID playerId) {
        if (playerId == null) return;
        MIMIC_CONSENT.remove(playerId);
        MIMIC_PROMPTED.remove(playerId);
        synchronized (MIMIC_LOCK) {
            MIMIC_BUFFERS.remove(playerId);
        }
    }

    private static boolean requestMimicPlayback(ServerLevel level,
            UUID scp939Id, UUID preferredSpeaker) {
        VoicechatServerApi api = serverApi;
        if (api == null || level == null || scp939Id == null
                || !ModCompatibilityConfig.simpleVoiceChatEnabled()
                || ACTIVE_MIMICS.containsKey(scp939Id)) {
            return false;
        }

        MimicClip clip;
        synchronized (MIMIC_LOCK) {
            clip = chooseClip(preferredSpeaker, System.nanoTime());
        }
        if (clip == null || clip.frames.isEmpty()) return false;

        Entity entity = level.getEntity(scp939Id);
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }

        EntityAudioChannel channel;
        try {
            channel = api.createEntityAudioChannel(UUID.randomUUID(),
                    api.fromEntity(entity));
            if (channel == null) return false;
            channel.setDistance(MIMIC_DISTANCE);
            channel.setWhispering(clip.whispering);
        } catch (RuntimeException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not create the SCP-939 Simple Voice Chat mimic channel",
                    exception);
            return false;
        }

        MimicPlayback playback = new MimicPlayback(scp939Id, level,
                entity.getUUID(), api, channel, new ArrayDeque<>(clip.frames));
        MimicPlayback existing = ACTIVE_MIMICS.putIfAbsent(scp939Id, playback);
        if (existing != null) {
            safeFlush(channel);
            return false;
        }
        scheduleMimicTick(playback);
        return true;
    }

    private static MimicClip chooseClip(UUID preferredSpeaker, long nowNanos) {
        pruneAllBuffers(nowNanos);

        SpeakerBuffer preferred = preferredSpeaker == null
                || !MIMIC_CONSENT.contains(preferredSpeaker)
                ? null : MIMIC_BUFFERS.get(preferredSpeaker);
        MimicClip clip = extractClip(preferred);
        if (clip != null) return clip;

        List<SpeakerBuffer> candidates = new ArrayList<>();
        for (Map.Entry<UUID, SpeakerBuffer> entry : MIMIC_BUFFERS.entrySet()) {
            if (!MIMIC_CONSENT.contains(entry.getKey())) continue;
            if (entry.getValue().frames.size() >= MIN_MIMIC_CLIP_FRAMES) {
                candidates.add(entry.getValue());
            }
        }
        if (candidates.isEmpty()) return null;

        int start = ThreadLocalRandom.current().nextInt(candidates.size());
        for (int offset = 0; offset < candidates.size(); offset++) {
            SpeakerBuffer candidate = candidates.get(
                    (start + offset) % candidates.size());
            clip = extractClip(candidate);
            if (clip != null) return clip;
        }
        return null;
    }

    private static MimicClip extractClip(SpeakerBuffer buffer) {
        if (buffer == null || buffer.frames.size() < MIN_MIMIC_CLIP_FRAMES) {
            return null;
        }

        List<VoiceFrame> all = new ArrayList<>(buffer.frames);
        List<List<VoiceFrame>> utterances = new ArrayList<>();
        List<VoiceFrame> current = new ArrayList<>();
        VoiceFrame previous = null;
        for (VoiceFrame frame : all) {
            if (previous != null && frame.capturedNanos - previous.capturedNanos
                    > MIMIC_UTTERANCE_GAP_NANOS) {
                if (!current.isEmpty()) utterances.add(current);
                current = new ArrayList<>();
            }
            current.add(frame);
            previous = frame;
        }
        if (!current.isEmpty()) utterances.add(current);

        List<List<VoiceFrame>> usable = new ArrayList<>();
        for (List<VoiceFrame> utterance : utterances) {
            if (utterance.size() >= MIN_MIMIC_CLIP_FRAMES) {
                usable.add(utterance);
            }
        }
        if (usable.isEmpty()) return null;

        // Prefer one of the newest three usable utterances, preserving natural
        // phrase boundaries instead of stitching unrelated words together.
        int newestStart = Math.max(0, usable.size() - 3);
        List<VoiceFrame> utterance = usable.get(newestStart
                + ThreadLocalRandom.current().nextInt(
                        usable.size() - newestStart));

        int from = 0;
        if (utterance.size() > MAX_MIMIC_CLIP_FRAMES) {
            int possible = utterance.size() - MAX_MIMIC_CLIP_FRAMES;
            from = ThreadLocalRandom.current().nextInt(possible + 1);
        }
        int to = Math.min(utterance.size(), from + MAX_MIMIC_CLIP_FRAMES);
        List<byte[]> frames = new ArrayList<>(to - from);
        int whispers = 0;
        for (int i = from; i < to; i++) {
            VoiceFrame frame = utterance.get(i);
            frames.add(frame.opusData);
            if (frame.whispering) whispers++;
        }
        return frames.size() < MIN_MIMIC_CLIP_FRAMES ? null
                : new MimicClip(List.copyOf(frames),
                        whispers > frames.size() / 2);
    }

    private static void pruneAllBuffers(long nowNanos) {
        MIMIC_BUFFERS.entrySet().removeIf(entry -> {
            pruneBuffer(entry.getValue(), nowNanos);
            return entry.getValue().frames.isEmpty();
        });
    }

    private static void pruneBuffer(SpeakerBuffer buffer, long nowNanos) {
        long oldest = nowNanos - MIMIC_FRAME_EXPIRY_NANOS;
        while (!buffer.frames.isEmpty()
                && buffer.frames.peekFirst().capturedNanos < oldest) {
            buffer.frames.removeFirst();
        }
        while (buffer.frames.size() > MAX_MIMIC_FRAMES_PER_SPEAKER) {
            buffer.frames.removeFirst();
        }
    }

    private static void scheduleMimicTick(MimicPlayback playback) {
        ScpClassifiedDirectiveMod.queueServerWork(1, () -> advanceMimic(playback));
    }

    private static void advanceMimic(MimicPlayback playback) {
        if (playback == null
                || ACTIVE_MIMICS.get(playback.scp939Id) != playback) {
            return;
        }
        if (!ModCompatibilityConfig.simpleVoiceChatEnabled()
                || serverApi != playback.api
                || playback.level.getEntity(playback.entityId) == null) {
            finishMimic(playback);
            return;
        }

        try {
            for (int i = 0; i < MIMIC_FRAMES_PER_SERVER_TICK; i++) {
                byte[] frame = playback.frames.pollFirst();
                if (frame == null) break;
                playback.channel.send(frame);
            }
        } catch (RuntimeException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "SCP-939 voice mimic playback failed", exception);
            finishMimic(playback);
            return;
        }

        if (playback.frames.isEmpty()) {
            finishMimic(playback);
        } else {
            scheduleMimicTick(playback);
        }
    }

    private static void finishMimic(MimicPlayback playback) {
        ACTIVE_MIMICS.remove(playback.scp939Id, playback);
        safeFlush(playback.channel);
    }

    private static void stopAllMimics() {
        for (MimicPlayback playback : ACTIVE_MIMICS.values()) {
            safeFlush(playback.channel);
        }
        ACTIVE_MIMICS.clear();
    }

    private static void safeFlush(EntityAudioChannel channel) {
        if (channel == null) return;
        try {
            channel.flush();
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static void sendEntity(VoicechatServerApi api, ServerPlayer player,
            EntitySoundPacket packet) {
        VoicechatConnection connection = connection(api, player);
        if (connection == null) return;
        forward(() -> api.sendEntitySoundPacketTo(connection, packet));
    }

    private static void sendLocational(VoicechatServerApi api,
            ServerPlayer player, LocationalSoundPacket packet) {
        VoicechatConnection connection = connection(api, player);
        if (connection == null) return;
        forward(() -> api.sendLocationalSoundPacketTo(connection, packet));
    }

    private static void sendStatic(VoicechatServerApi api, ServerPlayer player,
            StaticSoundPacket packet) {
        VoicechatConnection connection = connection(api, player);
        if (connection == null) return;
        forward(() -> api.sendStaticSoundPacketTo(connection, packet));
    }

    private static VoicechatConnection connection(VoicechatServerApi api,
            ServerPlayer player) {
        if (api == null || player == null) return null;
        VoicechatConnection connection = api.getConnectionOf(player.getUUID());
        return connection != null && connection.isConnected()
                && connection.isInstalled() ? connection : null;
    }

    private static void forward(Runnable action) {
        boolean previous = forwarding();
        FORWARDING.set(true);
        try {
            action.run();
        } catch (RuntimeException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not forward Simple Voice Chat audio into the SCP: Classified Directive death feed",
                    exception);
        } finally {
            FORWARDING.set(previous);
        }
    }

    private static boolean forwarding() {
        return Boolean.TRUE.equals(FORWARDING.get());
    }

    private static ServerPlayer minecraftPlayer(VoicechatConnection connection) {
        if (connection == null || connection.getPlayer() == null) return null;
        Object player = connection.getPlayer().getPlayer();
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private static UUID deadCallChannel(UUID speaker) {
        return UUID.nameUUIDFromBytes((ScpClassifiedDirectiveMod.MODID
                + ":dead_voice:" + speaker).getBytes(StandardCharsets.UTF_8));
    }

    private static boolean supportedApiInstalled() {
        String apiVersion = ModList.get()
                .getModContainerById(SimpleVoiceChatPresence.API_MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("");
        if (versionAtLeast(apiVersion, MIN_API_MAJOR, MIN_API_MINOR,
                MIN_API_PATCH)) {
            return true;
        }

        // Development environments and a few older packaging layouts may expose
        // only the parent voicechat mod. Its version is Minecraft-prefixed, so
        // compare the last semantic triplet (e.g. 1.20.1-2.6.22 -> 2.6.22).
        String voicechatVersion = ModList.get()
                .getModContainerById(SimpleVoiceChatPresence.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("");
        return versionAtLeast(voicechatVersion, MIN_API_MAJOR, MIN_API_MINOR,
                MIN_API_PATCH);
    }

    private static boolean versionAtLeast(String version, int major, int minor,
            int patch) {
        if (version == null || version.isBlank()) return false;
        Matcher matcher = SEMVER.matcher(version);
        int foundMajor = -1;
        int foundMinor = -1;
        int foundPatch = -1;
        while (matcher.find()) {
            try {
                foundMajor = Integer.parseInt(matcher.group(1));
                foundMinor = Integer.parseInt(matcher.group(2));
                foundPatch = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        if (foundMajor < 0) return false;
        if (foundMajor != major) return foundMajor > major;
        if (foundMinor != minor) return foundMinor > minor;
        return foundPatch >= patch;
    }

    private static final class SpeakerBuffer {
        private final Deque<VoiceFrame> frames = new ArrayDeque<>();
    }

    private record VoiceFrame(byte[] opusData, boolean whispering,
            long capturedNanos) {
    }

    private record MimicClip(List<byte[]> frames, boolean whispering) {
    }

    private static final class MimicPlayback {
        private final UUID scp939Id;
        private final ServerLevel level;
        private final UUID entityId;
        private final VoicechatServerApi api;
        private final EntityAudioChannel channel;
        private final Deque<byte[]> frames;

        private MimicPlayback(UUID scp939Id, ServerLevel level, UUID entityId,
                VoicechatServerApi api, EntityAudioChannel channel,
                Deque<byte[]> frames) {
            this.scp939Id = scp939Id;
            this.level = level;
            this.entityId = entityId;
            this.api = api;
            this.channel = channel;
            this.frames = frames;
        }
    }
}
