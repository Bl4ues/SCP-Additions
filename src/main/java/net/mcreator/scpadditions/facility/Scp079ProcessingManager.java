package net.mcreator.scpadditions.facility;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.network.ScpEntityNetwork;
import net.mcreator.scpadditions.roamer.RoamerDebugSnapshot;
import net.mcreator.scpadditions.roamer.RoamerManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared processing-power budget for SCP-079's facility decisions.
 *
 * The power value is stored in the world's SavedData. Regeneration remains
 * lazy, so no permanent server tick loop is needed, and restarting the world
 * neither resets the budget nor grants offline regeneration. Developer HUD
 * synchronization is staggered. The decision feed uses a separate snapshot
 * packet and is only resent when a meaningful decision changes its history.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079ProcessingManager {
    public static final float MAX_POWER = 100.0F;
    public static final float INITIAL_POWER = 25.0F;
    public static final float REGEN_PER_SECOND = 0.5F;

    private static final double REGEN_PER_TICK = REGEN_PER_SECOND / 20.0D;
    private static final Map<MinecraftServer, State> STATES = new WeakHashMap<>();
    private static final Map<UUID, ClientSnapshot> LAST_CLIENT_SYNC =
            new ConcurrentHashMap<>();

    private Scp079ProcessingManager() {
    }

    public static void onControlEnabled(LevelAccessor level) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null) return;
        synchronized (STATES) {
            State state = state(server, true);
            update(server, state);
            state.active = true;
        }
    }

    public static void onControlDisabled(LevelAccessor level) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null) return;
        synchronized (STATES) {
            State state = state(server, false);
            update(server, state);
            state.active = false;
        }
    }

    public static boolean isActive(ServerLevel level) {
        return level != null && level.getGameRules().getBoolean(
                ScpAdditionsModGameRules.SCP079CONTROLON);
    }

    public static float getPower(ServerLevel level) {
        if (level == null) return 0.0F;
        MinecraftServer server = level.getServer();
        synchronized (STATES) {
            State state = state(server, isActive(level));
            state.active = isActive(level);
            update(server, state);
            return (float) state.data.power();
        }
    }

    public static boolean canAfford(ServerLevel level, double cost) {
        return cost <= 0.0D || getPower(level) + 0.0001D >= cost;
    }

    public static boolean trySpend(ServerLevel level, double cost) {
        if (level == null || cost < 0.0D || !isActive(level)) return false;
        MinecraftServer server = level.getServer();
        synchronized (STATES) {
            State state = state(server, true);
            state.active = true;
            update(server, state);
            double power = state.data.power();
            if (power + 0.0001D < cost) return false;
            state.data.setPower(power - cost);
            return true;
        }
    }

    /** Refunds a reserved cost when a world mutation becomes invalid mid-action. */
    public static void refund(ServerLevel level, double amount) {
        if (level == null || amount <= 0.0D) return;
        MinecraftServer server = level.getServer();
        synchronized (STATES) {
            State state = state(server, isActive(level));
            state.active = isActive(level);
            update(server, state);
            state.data.setPower(state.data.power() + amount);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || (player.tickCount + player.getId()) % 10 != 0) {
            return;
        }

        ScpAdditionsModulesConfig.Debug debug =
                ScpAdditionsModulesConfig.get().debug;
        boolean energyVisible = debug.showScp079EnergyHud;
        boolean decisionVisible = debug.showScp079DecisionLogHud;
        boolean spawnTimersVisible = debug.showScpSpawnTimersHud;
        boolean active = energyVisible && isActive(player.serverLevel());
        int roundedPower = energyVisible
                ? Math.round(getPower(player.serverLevel())) : 0;
        List<RoamerDebugSnapshot> roamers = spawnTimersVisible
                ? RoamerManager.debugSnapshots(player) : List.of();
        Scp079DecisionLog.Snapshot decisionSnapshot = decisionVisible
                ? Scp079DecisionLog.snapshot(player.getServer())
                : new Scp079DecisionLog.Snapshot(-1L, List.of());

        ClientSnapshot next = new ClientSnapshot(energyVisible, decisionVisible,
                active, roundedPower, spawnTimersVisible, roamers,
                decisionSnapshot.version());
        ClientSnapshot previous = LAST_CLIENT_SYNC.get(player.getUUID());

        if (previous == null || !next.sameCoreState(previous)) {
            ScpEntityNetwork.syncDebugState(player, energyVisible, active,
                    roundedPower, spawnTimersVisible, roamers);
        }

        if (decisionVisible) {
            if (previous == null || !previous.decisionVisible()
                    || previous.decisionVersion()
                    != decisionSnapshot.version()) {
                ScpEntityNetwork.syncScp079Decisions(player, true,
                        decisionSnapshot);
            }
        } else if (previous != null && previous.decisionVisible()) {
            ScpEntityNetwork.syncScp079Decisions(player, false,
                    new Scp079DecisionLog.Snapshot(-1L, List.of()));
        }

        LAST_CLIENT_SYNC.put(player.getUUID(), next);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_CLIENT_SYNC.remove(event.getEntity().getUUID());
    }

    /** Capture any lazily accrued power before the world's final save. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        synchronized (STATES) {
            State state = STATES.get(server);
            if (state != null) update(server, state);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (STATES) {
            STATES.remove(event.getServer());
        }
        LAST_CLIENT_SYNC.clear();
    }

    private static State state(MinecraftServer server, boolean active) {
        return STATES.computeIfAbsent(server,
                ignored -> new State(Scp079ProcessingSavedData.get(server),
                        server.getTickCount(), active));
    }

    private static void update(MinecraftServer server, State state) {
        long now = server.getTickCount();
        long elapsed = Math.max(0L, now - state.lastTick);
        if (state.active && elapsed > 0L) {
            state.data.setPower(state.data.power()
                    + elapsed * REGEN_PER_TICK);
        }
        state.lastTick = now;
    }

    private static final class State {
        private final Scp079ProcessingSavedData data;
        private long lastTick;
        private boolean active;

        private State(Scp079ProcessingSavedData data, long lastTick,
                boolean active) {
            this.data = data;
            this.lastTick = lastTick;
            this.active = active;
        }
    }

    private record ClientSnapshot(boolean energyVisible,
            boolean decisionVisible, boolean active, int roundedPower,
            boolean spawnTimersVisible, List<RoamerDebugSnapshot> roamers,
            long decisionVersion) {
        private ClientSnapshot {
            roamers = roamers == null ? List.of() : List.copyOf(roamers);
        }

        private boolean sameCoreState(ClientSnapshot other) {
            return other != null
                    && energyVisible == other.energyVisible
                    && active == other.active
                    && roundedPower == other.roundedPower
                    && spawnTimersVisible == other.spawnTimersVisible
                    && roamers.equals(other.roamers);
        }
    }
}
