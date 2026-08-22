package com.bl4ues.scpclassifieddirective.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Covers the case where a routine SCP-173 remains near a player but a sealed
 * layout leaves it with no reachable target. Manually placed statues remain
 * persistent; only natural/forced roamer encounters use this timeout.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID)
public final class Scp173AccessDespawnController {
    private static final int NO_ACCESS_DESPAWN_TICKS = 1200;
    private static final int ACCESS_RECHECK_INTERVAL_TICKS = 20;
    private static final double ACCESS_SEARCH_RANGE = 48.0D;
    private static final double ACCESS_SEARCH_RANGE_SQR =
            ACCESS_SEARCH_RANGE * ACCESS_SEARCH_RANGE;

    private static final Map<UUID, Long> NO_ACCESS_SINCE = new HashMap<>();
    private static final Map<UUID, Long> NEXT_ACCESS_CHECK = new HashMap<>();

    private Scp173AccessDespawnController() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Scp173Entity scp173)
                    || !scp173.isAlive() || scp173.isRemoved()
                    || !scp173.isRoutineSpawn()) {
                continue;
            }

            UUID id = scp173.getUUID();
            long nextCheck = NEXT_ACCESS_CHECK.getOrDefault(id, 0L);
            if (gameTime < nextCheck) continue;
            NEXT_ACCESS_CHECK.put(id,
                    gameTime + ACCESS_RECHECK_INTERVAL_TICKS);

            if (hasReachablePlayer(level, scp173)) {
                NO_ACCESS_SINCE.remove(id);
                continue;
            }

            long since = NO_ACCESS_SINCE.computeIfAbsent(id,
                    ignored -> gameTime);
            if (gameTime - since >= NO_ACCESS_DESPAWN_TICKS) {
                scp173.completeRoutineEncounter();
                scp173.discard();
                NO_ACCESS_SINCE.remove(id);
                NEXT_ACCESS_CHECK.remove(id);
            }
        }
    }

    private static boolean hasReachablePlayer(ServerLevel level,
            Scp173Entity scp173) {
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isCreative()
                    || player.isSpectator()
                    || scp173.distanceToSqr(player)
                    > ACCESS_SEARCH_RANGE_SQR) {
                continue;
            }

            if (scp173.isObservedBy(player)) return true;
            Path path = scp173.getNavigation().createPath(player, 0);
            if (path != null && path.canReach()) return true;
        }
        return false;
    }
}
