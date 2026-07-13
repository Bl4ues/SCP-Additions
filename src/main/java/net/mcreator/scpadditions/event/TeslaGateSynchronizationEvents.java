package net.mcreator.scpadditions.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.block.TeslaGateStructure;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.procedures.TeslaGateTransitionHelper;
import net.mcreator.scpadditions.procedures.TeslaGateUpdateTickProcedure;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Re-attaches the scheduled-tick loop to Tesla Gates that already existed before
 * the 3.0 placement code was installed. Scheduled block ticks are not guaranteed
 * to exist for legacy world blocks, so a loaded gate could otherwise remain idle
 * until broken and placed again.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TeslaGateSynchronizationEvents {
    private static final int HORIZONTAL_RADIUS = 5;
    private static final int VERTICAL_RADIUS = 4;
    private static final int STUCK_TRANSITION_TICKS = 40;
    private static final Map<GateKey, TransitionObservation> TRANSITION_OBSERVATIONS =
            new ConcurrentHashMap<>();

    private TeslaGateSynchronizationEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 20L != 0L) {
            return;
        }

        boolean enabled = level.getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
        boolean override = level.getGameRules()
                .getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
        Set<BlockPos> visited = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();
            for (int dx = -HORIZONTAL_RADIUS; dx <= HORIZONTAL_RADIUS; dx++) {
                for (int dy = -VERTICAL_RADIUS; dy <= VERTICAL_RADIUS; dy++) {
                    for (int dz = -HORIZONTAL_RADIUS; dz <= HORIZONTAL_RADIUS; dz++) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (!visited.add(pos)) {
                            continue;
                        }

                        BlockState state = level.getBlockState(pos);
                        if (!TeslaGateStructure.isController(state)) {
                            continue;
                        }

                        GateKey key = new GateKey(level.dimension(), pos.asLong());
                        if (!state.is(ScpAdditionsModBlocks.TESLA_GATE.get())) {
                            recoverStuckTransition(level, pos, state, key);
                            continue;
                        }

                        TRANSITION_OBSERVATIONS.remove(key);
                        if (!enabled && !override) continue;

                        TeslaGateUpdateTickProcedure.execute(level,
                                pos.getX(), pos.getY(), pos.getZ());
                        level.scheduleTick(pos, state.getBlock(), 10);
                    }
                }
            }
        }

        long staleBefore = level.getGameTime() - STUCK_TRANSITION_TICKS * 3L;
        TRANSITION_OBSERVATIONS.entrySet().removeIf(entry ->
                entry.getKey().dimension().equals(level.dimension())
                        && entry.getValue().lastSeen() < staleBefore);
    }

    private static void recoverStuckTransition(ServerLevel level, BlockPos pos,
            BlockState state, GateKey key) {
        long gameTime = level.getGameTime();
        Block block = state.getBlock();
        TransitionObservation previous = TRANSITION_OBSERVATIONS.get(key);
        long firstSeen = previous != null && previous.block() == block
                ? previous.firstSeen() : gameTime;
        TRANSITION_OBSERVATIONS.put(key,
                new TransitionObservation(block, firstSeen, gameTime));

        if (gameTime - firstSeen < STUCK_TRANSITION_TICKS) return;
        if (TeslaGateTransitionHelper.resetStuckController(level, pos)) {
            TRANSITION_OBSERVATIONS.remove(key);
        }
    }

    private record GateKey(ResourceKey<Level> dimension, long pos) {
    }

    private record TransitionObservation(Block block, long firstSeen, long lastSeen) {
    }
}
