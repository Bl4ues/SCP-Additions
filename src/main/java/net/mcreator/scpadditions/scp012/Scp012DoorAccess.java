package net.mcreator.scpadditions.scp012;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.effect.Scp714ProtectionAccess;
import net.mcreator.scpadditions.facility.FacilityModule;
import net.mcreator.scpadditions.facility.HeavyDoorControlPanelAccess;
import net.mcreator.scpadditions.facility.Scp079ProcessingManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Opens controlled heavy doors that actually lie between a player and SCP-012. */
public final class Scp012DoorAccess {
    private static final int PLAYER_DOOR_RADIUS = 5;
    private static final int SCP_DOOR_RADIUS = 15;
    private static final int CONTEST_MEMORY_TICKS = 240;
    private static final int ABANDONED_DEVICE_COOLDOWN_TICKS = 160;
    private static final double MAX_ROUTE_OFFSET_SQR = 2.75D * 2.75D;
    private static final double[] ATTEMPT_COSTS = {7.0D, 11.0D, 18.0D, 29.0D};

    private static final Map<DoorKey, Long> COOLDOWNS =
            new ConcurrentHashMap<>();
    private static final Map<DoorKey, ContestState> CONTESTS =
            new ConcurrentHashMap<>();

    private Scp012DoorAccess() {
    }

    public static boolean tryOpen(ServerLevel level, ServerPlayer player,
                                   BlockPos scpPos) {
        return tryOpen(level, player, scpPos, 0.0D);
    }

    /**
     * @param reservedPower processing that must remain available for the box
     *                      opening or another action in the same trap sequence
     */
    public static boolean tryOpen(ServerLevel level, ServerPlayer player,
                                   BlockPos scpPos, double reservedPower) {
        BlockPos center = player.blockPosition();
        Vec3 playerPosition = player.position();
        Vec3 targetPosition = Scp012Module.attractionPoint(level, scpPos);
        DoorCandidate best = null;
        double bestDistance = Double.MAX_VALUE;
        long time = level.getGameTime();
        clean(level, time);

        if (Scp079ProcessingManager.getPower(level)
                < ATTEMPT_COSTS[0] + reservedPower) {
            return false;
        }

        for (BlockPos mutable : BlockPos.betweenClosed(
                center.offset(-PLAYER_DOOR_RADIUS, -2, -PLAYER_DOOR_RADIUS),
                center.offset(PLAYER_DOOR_RADIUS, 3, PLAYER_DOOR_RADIUS))) {
            BlockPos pos = mutable.immutable();
            if (Vec3.atCenterOf(pos).distanceToSqr(Vec3.atCenterOf(scpPos))
                    > SCP_DOOR_RADIUS * SCP_DOOR_RADIUS) {
                continue;
            }

            DoorMatch match = matchClosedDoor(level, pos);
            if (match == null
                    || !HeavyDoorControlPanelAccess.hasControllableInterface(
                    level, pos)
                    || !liesOnRoute(match.state(), pos, playerPosition,
                    targetPosition)) {
                continue;
            }

            DoorKey key = new DoorKey(level.dimension(), pos.asLong());
            ContestState contest = CONTESTS.get(key);
            boolean activeContest = contest != null
                    && contest.player().equals(player.getUUID())
                    && contest.expiresAt() >= time;
            if (contest != null && contest.expiresAt() >= time
                    && !contest.player().equals(player.getUUID())) {
                continue;
            }
            if (!activeContest
                    && COOLDOWNS.getOrDefault(key, 0L) > time) {
                continue;
            }

            int attempts = activeContest ? contest.attempts() : 0;
            double cost = attemptCost(attempts);
            if (!Scp079ProcessingManager.canAfford(level,
                    cost + reservedPower)) {
                if (activeContest) abandon(key, time);
                continue;
            }
            if (activeContest && !worthContinuing(level, player, scpPos,
                    contest, cost)) {
                abandon(key, time);
                continue;
            }

            double distance = player.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = new DoorCandidate(match, key, contest, cost);
            }
        }

        if (best == null) return false;
        if (!Scp079ProcessingManager.trySpend(level, best.cost())) {
            return false;
        }

        BlockState current = level.getBlockState(best.match().pos());
        if (current.getBlock() != best.match().family().closed().get()
                || !current.hasProperty(HorizontalDirectionalBlock.FACING)
                || HeavyDoorControlPanelAccess.openConnectedControls(level,
                best.match().pos()) <= 0) {
            Scp079ProcessingManager.refund(level, best.cost());
            return false;
        }

        Direction facing = current.getValue(HorizontalDirectionalBlock.FACING);
        level.playSound(null, best.match().pos(),
                best.match().family().openingSound().get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        Block firstOpeningStage = best.match().family().opening().get(0).get();
        level.setBlock(best.match().pos(), firstOpeningStage.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing),
                Block.UPDATE_ALL);
        emitOverrideParticles(level, best.match().pos());

        ContestState previous = best.previous();
        int attempts = previous == null ? 1 : previous.attempts() + 1;
        double spent = best.cost()
                + (previous == null ? 0.0D : previous.processingSpent());
        CONTESTS.put(best.key(), new ContestState(player.getUUID(), attempts,
                spent, time + CONTEST_MEMORY_TICKS));
        return true;
    }

    private static boolean worthContinuing(ServerLevel level,
            ServerPlayer player, BlockPos scpPos, ContestState contest,
            double nextCost) {
        boolean protectedBy714 = Scp714ProtectionAccess.isProtected(player);

        // The first re-open remains a plausible precaution against SCP-714, but
        // repeatedly fighting a protected player is recognized as wasteful.
        if (protectedBy714 && contest.attempts() >= 2) return false;

        double distance = player.position().distanceTo(
                Scp012Module.attractionPoint(level, scpPos));
        double utility = 62.0D - contest.attempts() * 18.0D;
        if (distance <= 6.0D) utility += 15.0D;
        if (protectedBy714) utility -= 35.0D;
        if (Scp079ProcessingManager.getPower(level) < 30.0F) utility -= 10.0D;
        utility -= nextCost * 0.35D;
        return utility >= 20.0D;
    }

    private static double attemptCost(int completedAttempts) {
        int index = Math.min(completedAttempts, ATTEMPT_COSTS.length - 1);
        return ATTEMPT_COSTS[index];
    }

    private static void abandon(DoorKey key, long time) {
        CONTESTS.remove(key);
        COOLDOWNS.put(key, time + ABANDONED_DEVICE_COOLDOWN_TICKS);
    }

    private static void clean(ServerLevel level, long time) {
        if (time % 20L != 0L) return;
        CONTESTS.entrySet().removeIf(entry -> {
            if (!entry.getKey().dimension().equals(level.dimension())
                    || entry.getValue().expiresAt() > time) {
                return false;
            }
            COOLDOWNS.put(entry.getKey(),
                    time + ABANDONED_DEVICE_COOLDOWN_TICKS);
            return true;
        });
        COOLDOWNS.entrySet().removeIf(entry ->
                entry.getKey().dimension().equals(level.dimension())
                        && entry.getValue() <= time);
    }

    private static boolean liesOnRoute(BlockState state, BlockPos doorPos,
                                        Vec3 playerPosition,
                                        Vec3 targetPosition) {
        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false;
        }

        Vec3 route = targetPosition.subtract(playerPosition);
        double routeLengthSqr = route.lengthSqr();
        if (routeLengthSqr < 0.0001D) return false;

        Vec3 doorCenter = Vec3.atCenterOf(doorPos);
        double projection = doorCenter.subtract(playerPosition).dot(route)
                / routeLengthSqr;
        if (projection <= 0.02D || projection >= 0.98D) return false;

        Vec3 closestPoint = playerPosition.add(route.scale(projection));
        if (doorCenter.distanceToSqr(closestPoint) > MAX_ROUTE_OFFSET_SQR) {
            return false;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        double playerSide = sideOfDoor(doorCenter, playerPosition, facing);
        double targetSide = sideOfDoor(doorCenter, targetPosition, facing);
        return playerSide * targetSide < -0.10D;
    }

    private static double sideOfDoor(Vec3 center, Vec3 point,
                                      Direction facing) {
        return (point.x - center.x) * facing.getStepX()
                + (point.z - center.z) * facing.getStepZ();
    }

    private static void emitOverrideParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                8, 0.45D, 0.55D, 0.45D, 0.03D);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.95D, pos.getZ() + 0.5D,
                2, 0.35D, 0.30D, 0.35D, 0.01D);
    }

    private static DoorMatch matchClosedDoor(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FacilityModule.DoorFamily[] families = {
                FacilityModule.DEFAULT_DOOR,
                FacilityModule.YELLOW_DOOR,
                FacilityModule.BLACK_DOOR
        };
        for (FacilityModule.DoorFamily family : families) {
            if (state.getBlock() == family.closed().get()) {
                return new DoorMatch(pos, state, family);
            }
        }
        return null;
    }

    private record DoorMatch(BlockPos pos, BlockState state,
                              FacilityModule.DoorFamily family) {
    }

    private record DoorKey(ResourceKey<Level> dimension, long pos) {
    }

    private record ContestState(UUID player, int attempts,
                                double processingSpent, long expiresAt) {
    }

    private record DoorCandidate(DoorMatch match, DoorKey key,
                                 ContestState previous, double cost) {
    }
}
