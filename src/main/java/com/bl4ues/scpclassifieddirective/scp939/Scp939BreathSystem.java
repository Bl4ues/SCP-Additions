package com.bl4ues.scpclassifieddirective.scp939;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticCategory;
import com.bl4ues.scpclassifieddirective.acoustics.AcousticStimulusSystem;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.network.Scp939Network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Independent breath reserve used against SCP-939. It deliberately does not
 * touch vanilla airSupply, so underwater breathing and breath suppression are
 * separate mechanics.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp939BreathSystem {
    private static final int MAX_RESERVE = 120;
    private static final int RECOVERY_PER_TICK = 2;
    private static final int BREATH_INTERVAL = 24;
    private static final int EXHAUSTED_LOCK_TICKS = 35;

    // Breath holding is only relevant at true pass-by distance. A small exit
    // hysteresis prevents the HUD flickering without turning a nearby 939 into
    // a corridor-wide oxygen tax.
    private static final double ACTIVE_ENTER_RADIUS = 2.5D;
    private static final double ACTIVE_EXIT_RADIUS = 2.75D;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private Scp939BreathSystem() {
    }

    public static void setHolding(ServerPlayer player, boolean holding) {
        if (player == null) return;
        State state = state(player);
        state.requestHolding = holding;
        if (!holding) state.holding = false;
        sync(player, state, true);
    }

    public static float reserveFraction(ServerPlayer player) {
        State state = player == null ? null : STATES.get(player.getUUID());
        return state == null ? 1.0F : state.reserve / (float) MAX_RESERVE;
    }

    public static boolean isHolding(ServerPlayer player) {
        State state = player == null ? null : STATES.get(player.getUUID());
        return state != null && state.holding;
    }

    public static boolean isActive(ServerPlayer player) {
        State state = player == null ? null : STATES.get(player.getUUID());
        return state != null && state.active;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)) return;

        State state = state(player);
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
            reset(state);
            sync(player, state, false);
            return;
        }

        state.active = hasNearby939(player, state.active
                ? ACTIVE_EXIT_RADIUS : ACTIVE_ENTER_RADIUS);
        if (state.exhaustedTicks > 0) state.exhaustedTicks--;

        boolean canHold = state.active && state.requestHolding
                && state.exhaustedTicks <= 0 && state.reserve > 0;
        state.holding = canHold;

        if (state.holding) {
            state.reserve--;
            if (state.reserve <= 0) {
                state.reserve = 0;
                state.holding = false;
                state.requestHolding = false;
                state.exhaustedTicks = EXHAUSTED_LOCK_TICKS;
                emit(player, AcousticCategory.GASP, 1.30F);
            }
        } else if (state.reserve < MAX_RESERVE) {
            state.reserve = Math.min(MAX_RESERVE,
                    state.reserve + RECOVERY_PER_TICK);
        }

        if (state.active && !state.holding && state.exhaustedTicks <= 0) {
            if (++state.breathTicks >= BREATH_INTERVAL) {
                state.breathTicks = 0;
                emit(player, AcousticCategory.BREATH, 0.75F);
            }
        } else {
            state.breathTicks = 0;
        }

        sync(player, state, false);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        STATES.remove(player.getUUID());
        Scp939Network.forgetPlayer(player.getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
    }

    private static boolean hasNearby939(ServerPlayer player, double radius) {
        return !player.serverLevel().getEntitiesOfClass(Scp939Entity.class,
                player.getBoundingBox().inflate(radius),
                entity -> entity.isAlive() && !entity.isRemoved()
                        && entity.distanceToSqr(player) <= radius * radius
                        && player.hasLineOfSight(entity))
                .isEmpty();
    }

    private static void emit(ServerPlayer player, AcousticCategory category,
            float intensity) {
        Vec3 position = player.position().add(0.0D,
                player.getBbHeight() * 0.72D, 0.0D);
        AcousticStimulusSystem.emit(player.serverLevel(), position, category,
                intensity, player);
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
    }

    private static void reset(State state) {
        state.reserve = MAX_RESERVE;
        state.requestHolding = false;
        state.holding = false;
        state.active = false;
        state.exhaustedTicks = 0;
        state.breathTicks = 0;
    }

    private static void sync(ServerPlayer player, State state, boolean force) {
        MinecraftServer server = player.getServer();
        int tick = server == null ? 0 : server.getTickCount();
        if (!force && tick - state.lastSyncTick < 5) return;
        state.lastSyncTick = tick;
        Scp939Network.sync(player);
    }

    private static final class State {
        private int reserve = MAX_RESERVE;
        private boolean requestHolding;
        private boolean holding;
        private boolean active;
        private int exhaustedTicks;
        private int breathTicks;
        private int lastSyncTick = Integer.MIN_VALUE / 4;
    }
}
