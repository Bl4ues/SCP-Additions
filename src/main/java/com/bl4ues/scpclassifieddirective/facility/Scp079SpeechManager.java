package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.ModCompatibilityConfig;
import com.bl4ues.scpclassifieddirective.compat.SimpleVoiceChatPresence;
import com.bl4ues.scpclassifieddirective.facility.speaker.FacilitySpeakerRegistry;
import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerBroadcastManager;
import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerTransientBroadcastManager;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server-authoritative speech queue for playable SCP-079.
 *
 * Text never needs to become a Minecraft chat packet. It is synthesized off the
 * server thread, then handed to an optional audio backend as positional PCM. A
 * camera room with Speakers wins over the physical host; without a room Speaker,
 * the voice falls back to the computer that actually contains SCP-079.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079SpeechManager {
    private static final int MAX_TEXT_LENGTH = 220;
    private static final int MAX_PENDING_LINES = 4;
    private static final float OUTPUT_DISTANCE = 10.0F;
    private static final Object LOCK = new Object();
    private static final Map<UUID, SpeechState> STATES = new HashMap<>();
    private static final ExecutorService SYNTHESIS = Executors.newSingleThreadExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "SCP-079 speech synthesis");
                thread.setDaemon(true);
                return thread;
            });

    private static volatile PlaybackBackend backend;
    private static int watchdogTick;

    private Scp079SpeechManager() {
    }

    public static void installBackend(PlaybackBackend playbackBackend) {
        backend = playbackBackend;
    }

    public static void removeBackend(PlaybackBackend playbackBackend) {
        if (backend == playbackBackend) backend = null;
    }

    public static boolean request(ServerPlayer player, String rawText) {
        if (player == null || player.getServer() == null
                || !Scp079PlayableManager.isController(player)
                || !SimpleVoiceChatPresence.installed()
                || !ModCompatibilityConfig.simpleVoiceChatEnabled()
                || backend == null) {
            return false;
        }
        String text = sanitize(rawText);
        if (text.isEmpty() || text.startsWith("/")) return false;

        boolean start;
        synchronized (LOCK) {
            SpeechState state = STATES.computeIfAbsent(player.getUUID(),
                    ignored -> new SpeechState(player.getServer()));
            if (state.server != player.getServer()) return false;
            while (state.pending.size() >= MAX_PENDING_LINES) {
                state.pending.removeFirst();
            }
            state.pending.addLast(text);
            start = state.token == null && state.playback == null;
        }
        if (start) beginNext(player.getServer(), player.getUUID());
        return true;
    }

    private static String sanitize(String rawText) {
        if (rawText == null) return "";
        String value = rawText.replace('\n', ' ').replace('\r', ' ')
                .replace('\t', ' ').replaceAll("\\s+", " ").trim();
        if (value.length() > MAX_TEXT_LENGTH) {
            value = value.substring(0, MAX_TEXT_LENGTH).trim();
        }
        return value;
    }

    private static void beginNext(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        String text;
        UUID token;
        synchronized (LOCK) {
            SpeechState state = STATES.get(playerId);
            if (state == null || state.server != server || state.token != null
                    || state.playback != null || state.pending.isEmpty()) return;
            text = state.pending.removeFirst();
            token = UUID.randomUUID();
            state.token = token;
        }

        CompletableFuture.supplyAsync(
                () -> Scp079SpeechSynthesizer.synthesise(text), SYNTHESIS)
                .whenComplete((audio, error) -> server.execute(() -> {
                    if (error != null) {
                        ScpClassifiedDirectiveMod.LOGGER.error(
                                "Could not synthesize SCP-079 speech", error);
                        finish(server, playerId, token);
                        return;
                    }
                    startPlayback(server, playerId, token, audio);
                }));
    }

    private static void startPlayback(MinecraftServer server, UUID playerId,
            UUID token, short[] audio) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null || !Scp079PlayableManager.isController(player)
                || audio == null || audio.length == 0
                || !SimpleVoiceChatPresence.installed()
                || !ModCompatibilityConfig.simpleVoiceChatEnabled()) {
            finish(server, playerId, token);
            return;
        }

        PlaybackBackend currentBackend = backend;
        if (currentBackend == null) {
            finish(server, playerId, token);
            return;
        }

        Route route = route(player);
        if (route.outputs.isEmpty()) {
            finish(server, playerId, token);
            return;
        }

        Playback playback;
        try {
            playback = currentBackend.play(server, route.outputs, audio,
                    () -> server.execute(() -> finish(server, playerId, token)));
        } catch (RuntimeException | LinkageError exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not start SCP-079 speech playback", exception);
            playback = null;
        }
        if (playback == null) {
            if (route.lease != null) {
                SpeakerTransientBroadcastManager.end(server, route.lease);
            }
            finish(server, playerId, token);
            return;
        }

        synchronized (LOCK) {
            SpeechState state = STATES.get(playerId);
            if (state == null || state.server != server
                    || !token.equals(state.token)) {
                playback.stop();
                if (route.lease != null) {
                    SpeakerTransientBroadcastManager.end(server, route.lease);
                }
                return;
            }
            state.playback = playback;
            state.lease = route.lease;
        }
    }

    private static Route route(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return Route.EMPTY;

        if (Scp079PlayableManager.isCameraMode(player)) {
            List<FacilitySpeakerRegistry.SpeakerEndpoint> speakers =
                    currentCameraSpeakers(player);
            if (!speakers.isEmpty()) {
                if (SpeakerBroadcastManager.isBroadcasting(player)) {
                    List<Output> outputs = SpeakerBroadcastManager
                            .voiceSources(server, player.getUUID()).stream()
                            .map(source -> new Output(source.dimension(),
                                    source.position(), OUTPUT_DISTANCE))
                            .toList();
                    if (!outputs.isEmpty()) return new Route(outputs, null);
                }

                SpeakerTransientBroadcastManager.Lease lease =
                        SpeakerTransientBroadcastManager.begin(server, speakers);
                if (lease != null) {
                    List<Output> outputs = lease.endpoints().stream()
                            .map(endpoint -> new Output(endpoint.dimension(),
                                    endpoint.soundPosition(), OUTPUT_DISTANCE))
                            .toList();
                    return new Route(outputs, lease);
                }
            }
        }

        Output host = hostOutput(player);
        return host == null ? Route.EMPTY : new Route(List.of(host), null);
    }

    private static List<FacilitySpeakerRegistry.SpeakerEndpoint>
            currentCameraSpeakers(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)
                || player.getServer() == null) return List.of();
        Vec3 anchor = player.position();
        FacilityCameraDefinition closest = null;
        double best = 1.0D;
        for (FacilityCameraDefinition camera : FacilitySurveillanceSavedData
                .get(player.getServer()).all()) {
            if (!camera.dimension().equals(level.dimension().location())) continue;
            double distance = camera.eyePosition().distanceToSqr(anchor);
            if (distance < best) {
                best = distance;
                closest = camera;
            }
        }
        return closest == null ? List.of()
                : FacilitySpeakerRegistry.speakersForCamera(level,
                        closest.anchorPos());
    }

    private static Output hostOutput(ServerPlayer player) {
        Scp079ScreenState.HostRef host = Scp079ScreenState.speakerHost(player);
        MinecraftServer server = player.getServer();
        if (host == null || server == null) return null;
        ResourceLocation id = ResourceLocation.tryParse(host.dimension());
        if (id == null) return null;
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return null;
        BlockPos pos = BlockPos.of(host.packedPos());
        return new Output(dimension,
                Vec3.atCenterOf(pos).add(0.0D, 0.15D, 0.0D),
                OUTPUT_DISTANCE);
    }

    private static void finish(MinecraftServer server, UUID playerId,
            UUID token) {
        SpeakerTransientBroadcastManager.Lease lease = null;
        boolean next = false;
        synchronized (LOCK) {
            SpeechState state = STATES.get(playerId);
            if (state == null || state.server != server
                    || !token.equals(state.token)) return;
            lease = state.lease;
            state.lease = null;
            state.playback = null;
            state.token = null;
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || !Scp079PlayableManager.isController(player)) {
                state.pending.clear();
            }
            next = !state.pending.isEmpty();
            if (!next) STATES.remove(playerId);
        }
        if (lease != null) SpeakerTransientBroadcastManager.end(server, lease);
        if (next) beginNext(server, playerId);
    }

    public static void clear(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        Playback playback;
        SpeakerTransientBroadcastManager.Lease lease;
        synchronized (LOCK) {
            SpeechState state = STATES.remove(playerId);
            if (state == null || state.server != server) return;
            playback = state.playback;
            lease = state.lease;
            state.pending.clear();
        }
        if (playback != null) playback.stop();
        if (lease != null) SpeakerTransientBroadcastManager.end(server, lease);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++watchdogTick % 5 != 0) return;
        MinecraftServer server = event.getServer();
        List<UUID> stale = new ArrayList<>();
        synchronized (LOCK) {
            for (Map.Entry<UUID, SpeechState> entry : STATES.entrySet()) {
                if (entry.getValue().server != server) continue;
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null || !Scp079PlayableManager.isController(player)) {
                    stale.add(entry.getKey());
                }
            }
        }
        stale.forEach(id -> clear(server, id));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MinecraftServer server = event.getServer();
        List<UUID> ids = new ArrayList<>();
        synchronized (LOCK) {
            STATES.forEach((id, state) -> {
                if (state.server == server) ids.add(id);
            });
        }
        ids.forEach(id -> clear(server, id));
    }

    public interface PlaybackBackend {
        Playback play(MinecraftServer server, List<Output> outputs,
                short[] pcm48Khz, Runnable onStopped);
    }

    public interface Playback {
        void stop();
    }

    public record Output(ResourceKey<Level> dimension, Vec3 position,
            float distance) {
    }

    private static final class SpeechState {
        private final MinecraftServer server;
        private final Deque<String> pending = new ArrayDeque<>();
        private UUID token;
        private Playback playback;
        private SpeakerTransientBroadcastManager.Lease lease;

        private SpeechState(MinecraftServer server) {
            this.server = server;
        }
    }

    private record Route(List<Output> outputs,
            SpeakerTransientBroadcastManager.Lease lease) {
        private static final Route EMPTY = new Route(List.of(), null);
    }
}
