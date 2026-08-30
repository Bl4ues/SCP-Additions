package com.bl4ues.scpclassifieddirective.facility;

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
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import com.bl4ues.scpclassifieddirective.network.ScpEntityNetwork;
import com.bl4ues.scpclassifieddirective.roamer.RoamerDebugSnapshot;
import com.bl4ues.scpclassifieddirective.roamer.RoamerManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared processing-power budget for SCP-079's facility decisions.
 *
 * Besides tracking raw AP, this class now applies a strategic admission model:
 * it protects an emergency reserve, reacts to recent spending velocity, slows
 * repeated actions from the same tactical lane, and only permits low-power
 * expenditure for unusually valuable opportunities. Existing feature code can
 * keep asking to spend normally; the manager classifies the calling subsystem
 * and decides whether the expenditure is merely affordable or actually wise.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079ProcessingManager {
    public static final float MAX_POWER = 100.0F;
    public static final float INITIAL_POWER = 25.0F;
    public static final float AP_PER_POWERED_GENERATOR_PER_SECOND = 0.1F;
    public static final float OFFLINE_DECAY_PER_SECOND = 0.5F;

    private static final double OFFLINE_DECAY_PER_TICK =
            OFFLINE_DECAY_PER_SECOND / 20.0D;
    private static final long STRATEGIC_WINDOW_TICKS = 200L;
    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

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

    public static void resetPower(LevelAccessor level) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null) return;
        synchronized (STATES) {
            State state = state(server, false);
            state.active = false;
            state.data.setPower(0.0D);
            state.recentSpend.clear();
            state.lastPurposeTick.clear();
            state.lastTick = server.getTickCount();
        }
        LAST_CLIENT_SYNC.clear();
    }

    public static boolean isActive(ServerLevel level) {
        return level != null
                && Scp079FacilityAccessManager.hasFacilityAccess(level);
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

    /** Raw affordability check. Strategic permission is applied by trySpend. */
    public static boolean canAfford(ServerLevel level, double cost) {
        return cost <= 0.0D || getPower(level) + 0.0001D >= cost;
    }

    public static boolean trySpend(ServerLevel level, double cost) {
        if (level == null || cost < 0.0D || !isActive(level)) return false;
        MinecraftServer server = level.getServer();
        SpendProfile profile = inferSpendProfile();
        synchronized (STATES) {
            State state = state(server, true);
            state.active = true;
            update(server, state);
            long now = server.getTickCount();
            pruneStrategicHistory(state, now);
            double power = state.data.power();
            if (power + 0.0001D < cost
                    || !strategicallyPermitted(state, now, power,
                    cost, profile)) {
                return false;
            }
            state.data.setPower(power - cost);
            if (cost > 0.0D) {
                state.recentSpend.addLast(new SpendSample(now, cost,
                        profile.purpose()));
                state.lastPurposeTick.put(profile.purpose(), now);
            }
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
            refundRecentSpend(state, amount);
        }
    }

    private static boolean strategicallyPermitted(State state, long now,
            double power, double cost, SpendProfile profile) {
        if (cost <= 0.0D) return true;
        double ratio = power / MAX_POWER;
        double postSpend = power - cost;
        double recentTotal = 0.0D;
        double recentPurpose = 0.0D;
        for (SpendSample sample : state.recentSpend) {
            recentTotal += sample.cost();
            if (sample.purpose() == profile.purpose()) {
                recentPurpose += sample.cost();
            }
        }

        long lastPurpose = state.lastPurposeTick.getOrDefault(
                profile.purpose(), Long.MIN_VALUE / 2L);
        long elapsed = Math.max(0L, now - lastPurpose);
        int minimumGap = profile.purpose().minimumGapTicks(ratio);
        boolean abundantFollowup = elapsed == 0L
                && profile.purpose() == SpendPurpose.TACTICAL_DOOR
                && ratio >= 0.75D && recentPurpose <= 12.0D;
        if (elapsed < minimumGap && !abundantFollowup) return false;

        if (ratio < 0.30D
                && (!profile.critical() || profile.utility() < 95.0D)) {
            return false;
        }

        double reserve = reserveFor(ratio, profile);
        boolean exceptional = profile.critical()
                && profile.utility() >= 100.0D && postSpend >= 5.0D;
        if (postSpend + 0.0001D < reserve && !exceptional) return false;

        double requiredUtility = baseUtilityThreshold(ratio)
                + cost * costPenalty(ratio)
                + Math.min(30.0D, recentTotal * 0.85D)
                + Math.min(22.0D, recentPurpose * 0.70D)
                + profile.purpose().utilityAdjustment();

        double burstLimit = burstLimit(ratio);
        if (recentTotal + cost > burstLimit
                && profile.utility() < requiredUtility + 18.0D) {
            return false;
        }
        return profile.utility() + 0.0001D >= requiredUtility;
    }

    private static double reserveFor(double ratio, SpendProfile profile) {
        if (ratio >= 0.75D) return profile.critical() ? 18.0D : 30.0D;
        if (ratio >= 0.60D) return profile.critical() ? 16.0D : 34.0D;
        if (ratio >= 0.30D) return profile.critical() ? 12.0D : 30.0D;
        return 6.0D;
    }

    private static double baseUtilityThreshold(double ratio) {
        if (ratio >= 0.75D) return 34.0D;
        if (ratio >= 0.60D) return 48.0D;
        if (ratio >= 0.30D) return 70.0D;
        return 92.0D;
    }

    private static double costPenalty(double ratio) {
        if (ratio >= 0.75D) return 0.40D;
        if (ratio >= 0.60D) return 0.55D;
        if (ratio >= 0.30D) return 0.80D;
        return 1.00D;
    }

    private static double burstLimit(double ratio) {
        if (ratio >= 0.75D) return 30.0D;
        if (ratio >= 0.60D) return 22.0D;
        if (ratio >= 0.30D) return 13.0D;
        return 7.0D;
    }

    private static SpendProfile inferSpendProfile() {
        return STACK_WALKER.walk(stream -> {
            List<StackWalker.StackFrame> frames = stream
                    .filter(frame -> frame.getClassName().startsWith(
                            "com.bl4ues.scpclassifieddirective"))
                    .filter(frame -> !frame.getClassName().equals(
                            Scp079ProcessingManager.class.getName()))
                    .limit(12).toList();

            for (StackWalker.StackFrame frame : frames) {
                String className = frame.getClassName();
                String method = frame.getMethodName();
                if (className.endsWith("Scp079FacilityThreatEvents")) {
                    if (method.contains("trySeparateScp131")) {
                        return new SpendProfile(SpendPurpose.CRITICAL_TRAP,
                                118.0D, true);
                    }
                    if (method.contains("evaluateUnprovokedPressure")) {
                        return new SpendProfile(SpendPurpose.AMBIENT,
                                42.0D, false);
                    }
                }
            }
            for (StackWalker.StackFrame frame : frames) {
                String className = frame.getClassName();
                if (className.endsWith("Scp079TeslaSuppression")) {
                    return new SpendProfile(SpendPurpose.CRITICAL_DEVICE,
                            112.0D, true);
                }
                if (className.endsWith("Scp079SustainedDoorLocks")) {
                    return new SpendProfile(SpendPurpose.UPKEEP,
                            68.0D, false);
                }
                if (className.endsWith("Scp012DoorAccess")) {
                    return new SpendProfile(SpendPurpose.SPECIAL_TRAP,
                            88.0D, false);
                }
                if (className.endsWith("Scp012InfluenceEvents")) {
                    return new SpendProfile(SpendPurpose.SPECIAL_TRAP,
                            84.0D, false);
                }
                if (className.endsWith("Scp079FacilityThreatEvents")) {
                    return new SpendProfile(SpendPurpose.TACTICAL_DOOR,
                            88.0D, false);
                }
            }
            return new SpendProfile(SpendPurpose.GENERAL,
                    64.0D, false);
        });
    }

    private static void pruneStrategicHistory(State state, long now) {
        while (!state.recentSpend.isEmpty()
                && now - state.recentSpend.peekFirst().tick()
                > STRATEGIC_WINDOW_TICKS) {
            state.recentSpend.removeFirst();
        }
    }

    private static void refundRecentSpend(State state, double amount) {
        double remaining = amount;
        var iterator = state.recentSpend.descendingIterator();
        while (iterator.hasNext() && remaining > 0.0001D) {
            SpendSample sample = iterator.next();
            if (sample.cost() <= remaining + 0.0001D) {
                remaining -= sample.cost();
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || (player.tickCount + player.getId()) % 10 != 0) {
            return;
        }

        boolean active = isActive(player.serverLevel());
        int roundedPower = Math.round(getPower(player.serverLevel()));
        int roundedDiscovery = Math.round(
                Scp079FacilityAccessManager.discoveryProgress(
                        player.getServer()));
        boolean auxiliaryOnline =
                Scp079FacilityAccessManager.auxiliaryPowerOnline(
                        player.getServer());
        boolean hostPresent = Scp079FacilityAccessManager.hasPhysicalScp079(
                player.getServer());
        boolean protocolExposed = Scp079FacilityAccessManager.protocolExposed(
                player.getServer());
        List<RoamerDebugSnapshot> roamers = RoamerManager.debugSnapshots(player);
        Scp079DecisionLog.Snapshot decisionSnapshot =
                Scp079DecisionLog.snapshot(player.getServer());

        ClientSnapshot next = new ClientSnapshot(active, roundedPower,
                roundedDiscovery, auxiliaryOnline, hostPresent,
                protocolExposed, roamers, decisionSnapshot.version());
        ClientSnapshot previous = LAST_CLIENT_SYNC.get(player.getUUID());

        if (previous == null || !next.sameCoreState(previous)) {
            ScpEntityNetwork.syncDebugState(player, true, active,
                    roundedPower, roundedDiscovery, auxiliaryOnline,
                    hostPresent, protocolExposed, true, roamers);
        }

        if (previous == null
                || previous.decisionVersion() != decisionSnapshot.version()) {
            ScpEntityNetwork.syncScp079Decisions(player, true,
                    decisionSnapshot);
        }

        LAST_CLIENT_SYNC.put(player.getUUID(), next);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_CLIENT_SYNC.remove(event.getEntity().getUUID());
    }

    /** Capture any lazily accrued or drained power before the final save. */
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
        if (elapsed > 0L) {
            double power = state.data.power();
            if (state.active) {
                int generators = Scp079FacilityAccessManager
                        .activeAuxiliaryGenerators(server);
                double regenPerTick = generators
                        * AP_PER_POWERED_GENERATOR_PER_SECOND / 20.0D;
                state.data.setPower(power + elapsed * regenPerTick);
            } else if (power > INITIAL_POWER) {
                state.data.setPower(Math.max(INITIAL_POWER,
                        power - elapsed * OFFLINE_DECAY_PER_TICK));
            }
        }
        state.lastTick = now;
    }

    private enum SpendPurpose {
        AMBIENT(28, 24.0D),
        TACTICAL_DOOR(30, 0.0D),
        UPKEEP(18, 8.0D),
        SPECIAL_TRAP(40, -8.0D),
        CRITICAL_TRAP(30, -18.0D),
        CRITICAL_DEVICE(60, -20.0D),
        GENERAL(35, 6.0D);

        private final int baseGapTicks;
        private final double utilityAdjustment;

        SpendPurpose(int baseGapTicks, double utilityAdjustment) {
            this.baseGapTicks = baseGapTicks;
            this.utilityAdjustment = utilityAdjustment;
        }

        private int minimumGapTicks(double ratio) {
            if (this == UPKEEP) return baseGapTicks;
            if (ratio >= 0.75D) return baseGapTicks;
            if (ratio >= 0.60D) return Math.round(baseGapTicks * 1.5F);
            if (ratio >= 0.30D) return baseGapTicks * 2;
            return baseGapTicks * 3;
        }

        private double utilityAdjustment() {
            return utilityAdjustment;
        }
    }

    private record SpendProfile(SpendPurpose purpose, double utility,
            boolean critical) {
    }

    private record SpendSample(long tick, double cost,
            SpendPurpose purpose) {
    }

    private static final class State {
        private final Scp079ProcessingSavedData data;
        private final Deque<SpendSample> recentSpend = new ArrayDeque<>();
        private final Map<SpendPurpose, Long> lastPurposeTick =
                new EnumMap<>(SpendPurpose.class);
        private long lastTick;
        private boolean active;

        private State(Scp079ProcessingSavedData data, long lastTick,
                boolean active) {
            this.data = data;
            this.lastTick = lastTick;
            this.active = active;
        }
    }

    private record ClientSnapshot(boolean active, int roundedPower,
            int roundedDiscovery, boolean auxiliaryOnline,
            boolean hostPresent, boolean protocolExposed,
            List<RoamerDebugSnapshot> roamers, long decisionVersion) {
        private ClientSnapshot {
            roamers = roamers == null ? List.of() : List.copyOf(roamers);
        }

        private boolean sameCoreState(ClientSnapshot other) {
            return other != null
                    && active == other.active
                    && roundedPower == other.roundedPower
                    && roundedDiscovery == other.roundedDiscovery
                    && auxiliaryOnline == other.auxiliaryOnline
                    && hostPresent == other.hostPresent
                    && protocolExposed == other.protocolExposed
                    && roamers.equals(other.roamers);
        }
    }
}
