package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Maintains only strategically useful SCP-079 door denials.
 *
 * The initial action pays its normal fixed cost. Once per second, this manager
 * spends additional processing to extend the denied controls. The lock ends as
 * soon as its maximum duration is reached, the relevant player/threat geometry
 * stops being useful, System Control is disabled, the door is no longer closed,
 * or SCP-079 cannot afford another upkeep payment.
 */
public final class Scp079SustainedDoorLocks {
    public static final double UPKEEP_COST_PER_SECOND = 1.5D;

    private static final int CHARGE_INTERVAL_TICKS = 20;
    private static final int DENIAL_EXTENSION_TICKS = 30;
    private static final double RELEVANCE_DISTANCE_SQR = 15.0D * 15.0D;
    private static final AtomicLong GENERATIONS = new AtomicLong();
    private static final Map<MinecraftServer, Map<DoorKey, ActiveLock>> ACTIVE =
            new WeakHashMap<>();

    private Scp079SustainedDoorLocks() {
    }

    public static void begin(ServerLevel level, BlockPos doorPos,
            UUID playerId, UUID threatId, LockReason reason,
            int maximumDurationTicks) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null || doorPos == null || playerId == null
                || threatId == null || reason == null
                || maximumDurationTicks <= CHARGE_INTERVAL_TICKS) {
            return;
        }

        long now = server.getTickCount();
        DoorKey key = new DoorKey(level.dimension(), doorPos.asLong());
        ActiveLock lock = new ActiveLock(GENERATIONS.incrementAndGet(),
                playerId, threatId, reason, now + maximumDurationTicks);
        synchronized (ACTIVE) {
            ACTIVE.computeIfAbsent(server, ignored -> new HashMap<>())
                    .put(key, lock);
        }
        schedule(server, key, lock.generation());
    }

    private static void schedule(MinecraftServer server, DoorKey key,
            long generation) {
        ScpAdditionsMod.queueServerWork(CHARGE_INTERVAL_TICKS,
                () -> maintain(server, key, generation));
    }

    private static void maintain(MinecraftServer server, DoorKey key,
            long generation) {
        ActiveLock lock;
        synchronized (ACTIVE) {
            Map<DoorKey, ActiveLock> locks = ACTIVE.get(server);
            lock = locks == null ? null : locks.get(key);
            if (lock == null || lock.generation() != generation) return;
        }

        ServerLevel level = server.getLevel(key.dimension());
        BlockPos doorPos = BlockPos.of(key.pos());
        long now = server.getTickCount();
        if (level == null || now >= lock.endTick()
                || !isStrategicallyUseful(level, doorPos, lock)
                || !Scp079ProcessingManager.trySpend(level,
                UPKEEP_COST_PER_SECOND)
                || HeavyDoorControlPanelAccess.temporarilyDenyConnectedControls(
                level, doorPos, DENIAL_EXTENSION_TICKS) <= 0) {
            remove(server, key, generation);
            return;
        }

        schedule(server, key, generation);
    }

    private static boolean isStrategicallyUseful(ServerLevel level,
            BlockPos doorPos, ActiveLock lock) {
        BlockState doorState = level.getBlockState(doorPos);
        if (!level.getGameRules().getBoolean(
                ScpAdditionsModGameRules.SCP079CONTROLON)
                || !isClosedOrClosingDoor(doorState)) {
            return false;
        }

        ServerPlayer player = level.getServer().getPlayerList()
                .getPlayer(lock.playerId());
        Entity threat = level.getEntity(lock.threatId());
        if (player == null || player.serverLevel() != level
                || !player.isAlive() || threat == null || !threat.isAlive()
                || player.distanceToSqr(Vec3.atCenterOf(doorPos))
                > RELEVANCE_DISTANCE_SQR
                || threat.distanceToSqr(Vec3.atCenterOf(doorPos))
                > RELEVANCE_DISTANCE_SQR) {
            return false;
        }

        return switch (lock.reason()) {
            case PURSUIT -> threat instanceof Mob mob
                    && mob.getTarget() == player
                    && sameDoorSide(doorState, doorPos, player.position(),
                    threat.position());
            case SCP_131_SEPARATION -> threat instanceof Scp173Entity
                    && oppositeDoorSides(doorState, doorPos, player.position(),
                    threat.position());
        };
    }

    private static boolean isClosedOrClosingDoor(BlockState state) {
        Block block = state.getBlock();
        FacilityModule.DoorFamily[] families = {
                FacilityModule.DEFAULT_DOOR,
                FacilityModule.YELLOW_DOOR,
                FacilityModule.BLACK_DOOR
        };
        for (FacilityModule.DoorFamily family : families) {
            if (block == family.closed().get()
                    || family.closing().stream().anyMatch(
                    entry -> entry.get() == block)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameDoorSide(BlockState doorState, BlockPos doorPos,
            Vec3 first, Vec3 second) {
        DoorSides sides = doorSides(doorState, doorPos, first, second);
        return sides != null
                && Math.abs(sides.first()) >= 0.35D
                && Math.abs(sides.second()) >= 0.35D
                && sides.first() * sides.second() > 0.0D;
    }

    private static boolean oppositeDoorSides(BlockState doorState,
            BlockPos doorPos, Vec3 first, Vec3 second) {
        DoorSides sides = doorSides(doorState, doorPos, first, second);
        return sides != null
                && Math.abs(sides.first()) >= 0.55D
                && Math.abs(sides.second()) >= 0.55D
                && sides.first() * sides.second() < 0.0D;
    }

    private static DoorSides doorSides(BlockState doorState, BlockPos doorPos,
            Vec3 first, Vec3 second) {
        if (!doorState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return null;
        }
        Direction facing = doorState.getValue(HorizontalDirectionalBlock.FACING);
        Vec3 center = Vec3.atCenterOf(doorPos);
        return new DoorSides(signedSide(center, facing, first),
                signedSide(center, facing, second));
    }

    private static double signedSide(Vec3 center, Direction facing,
            Vec3 position) {
        return (position.x - center.x) * facing.getStepX()
                + (position.z - center.z) * facing.getStepZ();
    }

    private static void remove(MinecraftServer server, DoorKey key,
            long generation) {
        synchronized (ACTIVE) {
            Map<DoorKey, ActiveLock> locks = ACTIVE.get(server);
            if (locks == null) return;
            ActiveLock current = locks.get(key);
            if (current != null && current.generation() == generation) {
                locks.remove(key);
            }
            if (locks.isEmpty()) ACTIVE.remove(server);
        }
    }

    public enum LockReason {
        PURSUIT,
        SCP_131_SEPARATION
    }

    private record ActiveLock(long generation, UUID playerId, UUID threatId,
            LockReason reason, long endTick) {
    }

    private record DoorSides(double first, double second) {
    }

    private record DoorKey(ResourceKey<Level> dimension, long pos) {
    }
}
