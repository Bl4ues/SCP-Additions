package net.mcreator.scpadditions.roamer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.entity.Scp173Entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Shared lifecycle and scheduling state for SCP entities with their own spawn
 * cycle. No world scan runs continuously: entity activity is tracked through
 * join/leave events, and spawn checks remain owned by each roamer scheduler.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoamerManager {
    private static final AABB WORLD_BOUNDS = new AABB(-30000000.0D,
            -2048.0D, -30000000.0D, 30000000.0D, 4096.0D,
            30000000.0D);
    private static final Map<MinecraftServer, ServerState> STATES =
            new WeakHashMap<>();

    private RoamerManager() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        synchronized (STATES) {
            for (RoamerType type : RoamerType.values()) {
                normalizePlayer(player, type, data(server, type));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        UUID playerId = event.getEntity().getUUID();
        synchronized (STATES) {
            ServerState state = STATES.get(server);
            if (state == null) return;
            for (RoamerData data : state.data.values()) {
                data.nextCheckTicks.remove(playerId);
                data.lastResults.remove(playerId);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        RoamerType type = RoamerType.fromEntity(event.getEntity());
        if (type == null) return;
        MinecraftServer server = event.getLevel().getServer();
        if (server != null) markSpawned(server, type, event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        Entity entity = event.getEntity();
        RoamerType type = RoamerType.fromEntity(entity);
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (type == null || (reason != Entity.RemovalReason.KILLED
                && reason != Entity.RemovalReason.DISCARDED)) {
            return;
        }
        MinecraftServer server = event.getLevel().getServer();
        if (server != null) markRemoved(server, type, entity.getUUID());
    }

    /** Returns true once when this player's scheduler reaches its next check. */
    public static boolean pollSpawnCheck(ServerPlayer player, RoamerType type) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null || type == null) return false;
        synchronized (STATES) {
            RoamerData data = data(server, type);
            if (normalizePlayer(player, type, data)
                    != RoamerState.COUNTDOWN) {
                return false;
            }
            int currentTick = server.getTickCount();
            int nextTick = data.nextCheckTicks.get(player.getUUID());
            if (currentTick < nextTick) return false;
            // Reserve the next interval before evaluating this attempt so a
            // failed chance or invalid position cannot retrigger every tick.
            data.nextCheckTicks.put(player.getUUID(), currentTick
                    + Math.max(1, type.spawnIntervalTicks()));
            return true;
        }
    }

    public static void recordResult(ServerPlayer player, RoamerType type,
            RoamerResult result) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null || type == null) return;
        synchronized (STATES) {
            data(server, type).lastResults.put(player.getUUID(),
                    result == null ? RoamerResult.NONE : result);
        }
    }

    public static void markSpawned(MinecraftServer server, RoamerType type,
            UUID entityId) {
        if (server == null || type == null || entityId == null) return;
        synchronized (STATES) {
            RoamerData data = data(server, type);
            data.activeEntityIds.add(entityId);
            data.nextCheckTicks.clear();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                data.lastResults.put(player.getUUID(), RoamerResult.SPAWNED);
            }
        }
    }

    public static void markRemoved(MinecraftServer server, RoamerType type,
            UUID entityId) {
        if (server == null || type == null || entityId == null) return;
        synchronized (STATES) {
            RoamerData data = data(server, type);
            data.activeEntityIds.remove(entityId);
            if (data.activeEntityIds.isEmpty()) {
                restartAllSchedules(server, type, data,
                        RoamerResult.DESPAWNED_TIMER_RESET);
            }
        }
    }

    public static RoamerDebugSnapshot debugSnapshot(ServerPlayer player,
            RoamerType type) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null || type == null) {
            return new RoamerDebugSnapshot(type, RoamerState.DISABLED,
                    RoamerResult.NONE, -1);
        }
        synchronized (STATES) {
            RoamerData data = data(server, type);
            RoamerState state = normalizePlayer(player, type, data);
            int nextCheckTick = state == RoamerState.COUNTDOWN
                    ? data.nextCheckTicks.getOrDefault(player.getUUID(), -1)
                    : -1;
            return new RoamerDebugSnapshot(type, state,
                    resultFor(player, type, data, state), nextCheckTick);
        }
    }

    public static List<RoamerDebugSnapshot> debugSnapshots(
            ServerPlayer player) {
        List<RoamerDebugSnapshot> snapshots = new ArrayList<>();
        for (RoamerType type : RoamerType.values()) {
            snapshots.add(debugSnapshot(player, type));
        }
        return List.copyOf(snapshots);
    }

    public static boolean isSpawnRuleEnabled(MinecraftServer server,
            RoamerType type) {
        if (server == null || type == null) return false;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        return overworld != null && overworld.getGameRules()
                .getBoolean(type.spawnRule());
    }

    public static void setSpawnRule(MinecraftServer server, RoamerType type,
            boolean enabled) {
        if (server == null || type == null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        overworld.getGameRules().getRule(type.spawnRule()).set(enabled, server);
        synchronized (STATES) {
            RoamerData data = data(server, type);
            if (!enabled || !type.spawnImplemented()
                    || !moduleEnabled(type)) {
                data.nextCheckTicks.clear();
                RoamerResult result = !type.spawnImplemented()
                        ? RoamerResult.NOT_IMPLEMENTED
                        : !moduleEnabled(type)
                                ? RoamerResult.MODULE_DISABLED
                                : RoamerResult.RULE_DISABLED;
                setResultForAll(server, data, result);
            } else if (data.activeEntityIds.isEmpty() && !data.contained) {
                restartAllSchedules(server, type, data,
                        RoamerResult.TIMER_STARTED);
            }
        }
    }

    /** Future containment systems can hold a roamer without changing gamerules. */
    public static void setContained(MinecraftServer server, RoamerType type,
            boolean contained) {
        if (server == null || type == null) return;
        synchronized (STATES) {
            RoamerData data = data(server, type);
            data.contained = contained;
            if (contained) {
                data.nextCheckTicks.clear();
            } else if (data.activeEntityIds.isEmpty()
                    && isSpawnRuleEnabled(server, type)
                    && type.spawnImplemented() && moduleEnabled(type)) {
                restartAllSchedules(server, type, data,
                        RoamerResult.TIMER_STARTED);
            }
        }
    }

    public static boolean isContained(MinecraftServer server,
            RoamerType type) {
        if (server == null || type == null) return false;
        synchronized (STATES) {
            return data(server, type).contained;
        }
    }

    public static int despawn(MinecraftServer server, RoamerType type) {
        if (server == null || type == null) return 0;
        List<Entity> loaded = findLoaded(server, type);
        for (Entity entity : loaded) entity.discard();
        // EntityLeaveLevel normally reconciles immediately. Repeat the state
        // cleanup once so command behavior stays deterministic across loaders.
        synchronized (STATES) {
            RoamerData data = data(server, type);
            for (Entity entity : loaded) {
                data.activeEntityIds.remove(entity.getUUID());
            }
            if (data.activeEntityIds.isEmpty()) {
                restartAllSchedules(server, type, data,
                        RoamerResult.DESPAWNED_TIMER_RESET);
            }
        }
        return loaded.size();
    }

    public static int despawnAll(MinecraftServer server) {
        int removed = 0;
        for (RoamerType type : RoamerType.values()) {
            removed += despawn(server, type);
        }
        return removed;
    }

    private static RoamerState normalizePlayer(ServerPlayer player,
            RoamerType type, RoamerData data) {
        UUID playerId = player.getUUID();
        if (!type.spawnImplemented()) {
            data.nextCheckTicks.remove(playerId);
            data.lastResults.put(playerId, RoamerResult.NOT_IMPLEMENTED);
            return RoamerState.DISABLED;
        }
        if (!moduleEnabled(type)) {
            data.nextCheckTicks.remove(playerId);
            data.lastResults.put(playerId, RoamerResult.MODULE_DISABLED);
            return RoamerState.DISABLED;
        }
        if (data.contained) {
            data.nextCheckTicks.remove(playerId);
            return RoamerState.CONTAINED;
        }
        if (!isSpawnRuleEnabled(player.getServer(), type)) {
            data.nextCheckTicks.remove(playerId);
            data.lastResults.put(playerId, RoamerResult.RULE_DISABLED);
            return RoamerState.DISABLED;
        }
        if (!data.activeEntityIds.isEmpty()) {
            data.nextCheckTicks.remove(playerId);
            data.lastResults.put(playerId, RoamerResult.SPAWNED);
            return RoamerState.SPAWNED;
        }
        if (player.isSpectator()) {
            data.nextCheckTicks.remove(playerId);
            data.lastResults.put(playerId, RoamerResult.PAUSED_SPECTATOR);
            return RoamerState.PAUSED;
        }
        if (player.isCreative()) {
            data.nextCheckTicks.remove(playerId);
            data.lastResults.put(playerId, RoamerResult.PAUSED_CREATIVE);
            return RoamerState.PAUSED;
        }

        if (!data.nextCheckTicks.containsKey(playerId)) {
            data.nextCheckTicks.put(playerId, player.getServer().getTickCount()
                    + Math.max(1, type.spawnIntervalTicks()));
            RoamerResult previous = data.lastResults.get(playerId);
            if (previous == null || previous == RoamerResult.RULE_DISABLED
                    || previous == RoamerResult.MODULE_DISABLED
                    || previous == RoamerResult.PAUSED_CREATIVE
                    || previous == RoamerResult.PAUSED_SPECTATOR) {
                data.lastResults.put(playerId, RoamerResult.TIMER_STARTED);
            }
        }
        return RoamerState.COUNTDOWN;
    }

    private static RoamerResult resultFor(ServerPlayer player, RoamerType type,
            RoamerData data, RoamerState state) {
        if (!type.spawnImplemented()) return RoamerResult.NOT_IMPLEMENTED;
        if (!moduleEnabled(type)) return RoamerResult.MODULE_DISABLED;
        if (state == RoamerState.DISABLED) return RoamerResult.RULE_DISABLED;
        if (state == RoamerState.SPAWNED) return RoamerResult.SPAWNED;
        if (state == RoamerState.PAUSED) {
            return player.isSpectator() ? RoamerResult.PAUSED_SPECTATOR
                    : RoamerResult.PAUSED_CREATIVE;
        }
        return data.lastResults.getOrDefault(player.getUUID(),
                RoamerResult.NONE);
    }

    private static void restartAllSchedules(MinecraftServer server,
            RoamerType type, RoamerData data, RoamerResult result) {
        data.nextCheckTicks.clear();
        int next = server.getTickCount()
                + Math.max(1, type.spawnIntervalTicks());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            data.lastResults.put(player.getUUID(), result);
            if (type.spawnImplemented() && moduleEnabled(type)
                    && !data.contained && isSpawnRuleEnabled(server, type)
                    && !player.isCreative() && !player.isSpectator()) {
                data.nextCheckTicks.put(player.getUUID(), next);
            }
        }
    }

    private static void setResultForAll(MinecraftServer server,
            RoamerData data, RoamerResult result) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            data.lastResults.put(player.getUUID(), result);
        }
    }

    private static boolean moduleEnabled(RoamerType type) {
        return switch (type) {
            case SCP_173 -> ScpAdditionsModulesConfig.get().scp173.enabled;
            case SCP_106 -> true;
        };
    }

    private static List<Entity> findLoaded(MinecraftServer server,
            RoamerType type) {
        List<Entity> entities = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            switch (type) {
                case SCP_173 -> entities.addAll(level.getEntitiesOfClass(
                        Scp173Entity.class, WORLD_BOUNDS,
                        entity -> entity.isAlive() && !entity.isRemoved()));
                case SCP_106 -> entities.addAll(level.getEntitiesOfClass(
                        Scp106Entity.class, WORLD_BOUNDS,
                        entity -> entity.isAlive() && !entity.isRemoved()));
            }
        }
        return entities;
    }

    private static RoamerData data(MinecraftServer server, RoamerType type) {
        ServerState state = STATES.computeIfAbsent(server,
                ignored -> new ServerState());
        return state.data.computeIfAbsent(type, ignored -> new RoamerData());
    }

    private static final class ServerState {
        private final EnumMap<RoamerType, RoamerData> data =
                new EnumMap<>(RoamerType.class);
    }

    private static final class RoamerData {
        private final Map<UUID, Integer> nextCheckTicks = new HashMap<>();
        private final Map<UUID, RoamerResult> lastResults = new HashMap<>();
        private final Set<UUID> activeEntityIds = new HashSet<>();
        private boolean contained;
    }
}
