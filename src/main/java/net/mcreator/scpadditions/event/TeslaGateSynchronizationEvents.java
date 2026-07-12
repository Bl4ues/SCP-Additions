package net.mcreator.scpadditions.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.procedures.TeslaGateUpdateTickProcedure;

import java.util.HashSet;
import java.util.Set;

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
        if (!enabled && !override) {
            return;
        }

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
                        if (!state.is(ScpAdditionsModBlocks.TESLA_GATE.get())) {
                            continue;
                        }

                        TeslaGateUpdateTickProcedure.execute(level,
                                pos.getX(), pos.getY(), pos.getZ());
                        level.scheduleTick(pos, state.getBlock(), 10);
                    }
                }
            }
        }
    }
}
