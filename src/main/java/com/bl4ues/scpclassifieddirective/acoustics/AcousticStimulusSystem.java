package com.bl4ues.scpclassifieddirective.acoustics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Shared server-side acoustic evidence bus.
 *
 * Producers publish short-lived positioned stimuli; listeners query what they
 * could plausibly hear from their own position. The system never assigns a
 * player target and never stores voice audio. Occlusion is intentionally an
 * inexpensive gameplay approximation that counts solid collision layers along
 * the listener-to-source segment, so several walls attenuate a stimulus much
 * more strongly than an unobstructed corridor.
 */
public final class AcousticStimulusSystem {
    public static final int RETENTION_TICKS = 20 * 12;
    private static final int MAX_STIMULI_PER_LEVEL = 512;
    private static final int MAX_OCCLUSION_LAYERS = 6;
    private static final double OCCLUSION_SAMPLE_STEP = 0.35D;
    private static final float OCCLUSION_FACTOR_PER_LAYER = 0.52F;
    private static final int VOICE_EMIT_INTERVAL_TICKS = 4;

    private static final Map<MinecraftServer, ServerState> STATES =
            new WeakHashMap<>();

    private AcousticStimulusSystem() {
    }

    public static AcousticStimulus emit(ServerLevel level, Vec3 position,
            AcousticCategory category, float intensity, Entity source) {
        if (level == null || position == null || category == null) return null;
        if (source instanceof Player player
                && (player.isCreative() || player.isSpectator()
                        || SafeZoneManager.isInside(player))) {
            // Creative/spectator players are observers and builders, not prey.
            // Filtering here prevents every producer from having to remember it.
            return null;
        }
        float clampedIntensity = Mth.clamp(intensity, 0.0F, 4.0F);
        if (clampedIntensity <= 0.0F) return null;

        AcousticStimulus stimulus = new AcousticStimulus(level.dimension(),
                position, category, clampedIntensity, level.getGameTime(),
                source == null ? null : source.getUUID());
        synchronized (STATES) {
            LevelState state = levelState(level);
            prune(state, level.getGameTime());
            state.stimuli.addLast(stimulus);
            while (state.stimuli.size() > MAX_STIMULI_PER_LEVEL) {
                state.stimuli.removeFirst();
            }
        }
        return stimulus;
    }

    /**
     * Emits positional voice evidence without retaining the packet or encoded
     * audio. Packet-heavy voice chat is throttled to one stimulus every four
     * server ticks per speaker; the newest packet remains ordinary SVC traffic.
     */
    public static void emitVoice(ServerPlayer player, float intensity) {
        if (player == null || !player.isAlive() || player.isCreative()
                || player.isSpectator()) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        long now = player.serverLevel().getGameTime();

        synchronized (STATES) {
            ServerState state = serverState(server);
            long previous = state.lastVoiceEmission.getOrDefault(
                    player.getUUID(), Long.MIN_VALUE / 4L);
            if (now - previous < VOICE_EMIT_INTERVAL_TICKS) return;
            state.lastVoiceEmission.put(player.getUUID(), now);
        }
        emit(player.serverLevel(), player.position().add(0.0D,
                player.getBbHeight() * 0.75D, 0.0D), AcousticCategory.VOICE,
                intensity, player);
    }

    /** Removes ephemeral per-player throttling state on disconnect. */
    public static void forgetPlayer(MinecraftServer server, UUID playerId) {
        if (server == null || playerId == null) return;
        synchronized (STATES) {
            ServerState state = STATES.get(server);
            if (state != null) state.lastVoiceEmission.remove(playerId);
        }
    }

    /** Returns a copy of recent evidence without applying listener perception. */
    public static List<AcousticStimulus> recent(ServerLevel level,
            long sinceGameTime) {
        if (level == null) return List.of();
        synchronized (STATES) {
            LevelState state = levelState(level);
            prune(state, level.getGameTime());
            List<AcousticStimulus> result = new ArrayList<>();
            for (AcousticStimulus stimulus : state.stimuli) {
                if (stimulus.gameTime() >= sinceGameTime) result.add(stimulus);
            }
            return List.copyOf(result);
        }
    }

    /**
     * Finds the strongest stimulus this listener could currently perceive.
     * rangeMultiplier and threshold belong to the listening creature, allowing
     * different SCPs to reuse the same world evidence without changing producers.
     */
    public static Optional<AcousticPerception> loudest(ServerLevel level,
            Vec3 listenerPosition, long sinceGameTime,
            double rangeMultiplier, float threshold) {
        if (level == null || listenerPosition == null
                || rangeMultiplier <= 0.0D) {
            return Optional.empty();
        }

        long now = level.getGameTime();
        AcousticPerception best = null;
        for (AcousticStimulus stimulus : recent(level, sinceGameTime)) {
            if (!stimulus.dimension().equals(level.dimension())) continue;

            double distance = listenerPosition.distanceTo(stimulus.position());
            double maxRange = stimulus.category().baseRange()
                    * stimulus.intensity() * rangeMultiplier;
            if (maxRange <= 0.0D || distance > maxRange) continue;

            int occlusionLayers = countOcclusionLayers(level,
                    listenerPosition, stimulus.position());
            float distanceFactor = (float) Mth.clamp(
                    1.0D - distance / maxRange, 0.0D, 1.0D);
            float occlusionFactor = (float) Math.pow(
                    OCCLUSION_FACTOR_PER_LAYER, occlusionLayers);
            long age = Math.max(0L, now - stimulus.gameTime());
            float ageFactor = 1.0F - 0.35F * Mth.clamp(
                    age / (float) RETENTION_TICKS, 0.0F, 1.0F);
            float perceived = stimulus.intensity()
                    * stimulus.category().salience()
                    * distanceFactor * occlusionFactor * ageFactor;
            if (perceived < threshold) continue;

            AcousticPerception perception = new AcousticPerception(stimulus,
                    distance, occlusionLayers, perceived, age);
            if (best == null || perception.perceivedIntensity()
                    > best.perceivedIntensity()) {
                best = perception;
            }
        }
        return Optional.ofNullable(best);
    }

    private static int countOcclusionLayers(ServerLevel level, Vec3 from,
            Vec3 to) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length <= OCCLUSION_SAMPLE_STEP) return 0;

        int steps = Mth.clamp((int) Math.ceil(length
                / OCCLUSION_SAMPLE_STEP), 2, 384);
        boolean previouslyBlocked = false;
        int layers = 0;
        for (int i = 1; i < steps; i++) {
            double fraction = i / (double) steps;
            Vec3 point = from.add(delta.scale(fraction));
            boolean blocked = isPointInsideCollision(level, point);
            if (blocked && !previouslyBlocked) {
                layers++;
                if (layers >= MAX_OCCLUSION_LAYERS) return layers;
            }
            previouslyBlocked = blocked;
        }
        return layers;
    }

    /**
     * Samples the actual collision boxes at the ray point instead of treating
     * every non-air block as a full wall. Open doors and thin props therefore do
     * not automatically muffle sound merely because their block cell is occupied.
     */
    private static boolean isPointInsideCollision(ServerLevel level,
            Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) return false;

        Vec3 local = new Vec3(point.x - pos.getX(), point.y - pos.getY(),
                point.z - pos.getZ());
        for (AABB box : shape.toAabbs()) {
            if (box.inflate(0.015D).contains(local)) return true;
        }
        return false;
    }

    private static LevelState levelState(ServerLevel level) {
        return serverState(level.getServer()).levels.computeIfAbsent(
                level.dimension(), ignored -> new LevelState());
    }

    private static ServerState serverState(MinecraftServer server) {
        return STATES.computeIfAbsent(server, ignored -> new ServerState());
    }

    private static void prune(LevelState state, long now) {
        long oldest = now - RETENTION_TICKS;
        while (!state.stimuli.isEmpty()
                && state.stimuli.peekFirst().gameTime() < oldest) {
            state.stimuli.removeFirst();
        }
    }

    private static final class ServerState {
        private final Map<net.minecraft.resources.ResourceKey<Level>, LevelState>
                levels = new HashMap<>();
        private final Map<UUID, Long> lastVoiceEmission = new HashMap<>();
    }

    private static final class LevelState {
        private final Deque<AcousticStimulus> stimuli = new ArrayDeque<>();
    }
}
