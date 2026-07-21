package net.mcreator.scpadditions.facility;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.event.Scp173SpawnEvents;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.network.Scp079EnergyPacket.SpawnStatus;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared processing-power budget for SCP-079's facility decisions.
 *
 * The value is updated lazily whenever gameplay needs it, avoiding another
 * permanent server tick loop. Developer HUD synchronization is staggered and
 * sends a packet only when a rounded value, toggle, scheduled tick or result
 * actually changes; spawn countdown animation remains client-side.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079ProcessingManager {
    public static final float MAX_POWER = 100.0F;
    public static final float INITIAL_POWER = 50.0F;
    public static final float REGEN_PER_SECOND = 1.5F;

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
            return (float) state.power;
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
            if (state.power + 0.0001D < cost) return false;
            state.power = Math.max(0.0D, state.power - cost);
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
            state.power = Math.min(MAX_POWER, state.power + amount);
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
        boolean spawnTimersVisible = debug.showScpSpawnTimersHud;
        boolean active = isActive(player.serverLevel());
        int roundedPower = energyVisible
                ? Math.round(getPower(player.serverLevel())) : 0;
        Scp173SpawnEvents.DebugSnapshot spawn =
                Scp173SpawnEvents.debugSnapshot(player);
        int currentTick = player.getServer() == null
                ? 0 : player.getServer().getTickCount();

        ClientSnapshot next = new ClientSnapshot(energyVisible, active,
                roundedPower, spawnTimersVisible, spawn.nextCheckTick(),
                spawn.status());
        ClientSnapshot previous = LAST_CLIENT_SYNC.put(player.getUUID(), next);
        if (!next.equals(previous)) {
            int remainingTicks = spawn.status().showsTimer()
                    ? Math.max(0, spawn.nextCheckTick() - currentTick) : -1;
            ScpEntityNetwork.syncDebugState(player, energyVisible, active,
                    roundedPower, spawnTimersVisible, remainingTicks,
                    spawn.status());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_CLIENT_SYNC.remove(event.getEntity().getUUID());
    }

    private static State state(MinecraftServer server, boolean active) {
        return STATES.computeIfAbsent(server,
                ignored -> new State(INITIAL_POWER, server.getTickCount(), active));
    }

    private static void update(MinecraftServer server, State state) {
        long now = server.getTickCount();
        long elapsed = Math.max(0L, now - state.lastTick);
        if (state.active && elapsed > 0L) {
            state.power = Math.min(MAX_POWER,
                    state.power + elapsed * REGEN_PER_TICK);
        }
        state.lastTick = now;
    }

    private static final class State {
        private double power;
        private long lastTick;
        private boolean active;

        private State(double power, long lastTick, boolean active) {
            this.power = power;
            this.lastTick = lastTick;
            this.active = active;
        }
    }

    private record ClientSnapshot(boolean energyVisible, boolean active,
            int roundedPower, boolean spawnTimersVisible, int nextCheckTick,
            SpawnStatus spawnStatus) {
    }
}
