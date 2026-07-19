package net.mcreator.scpadditions.scp012;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.facility.FacilityModule;
import net.mcreator.scpadditions.facility.HeavyDoorControlPanelAccess;

import java.util.HashMap;
import java.util.Map;

/** Opens controlled heavy doors that actually lie between a player and SCP-012. */
public final class Scp012DoorAccess {
    private static final int PLAYER_DOOR_RADIUS = 5;
    private static final int SCP_DOOR_RADIUS = 15;
    private static final int COOLDOWN_TICKS = 100;
    private static final double MAX_ROUTE_OFFSET_SQR = 2.75D * 2.75D;
    private static final Map<Long, Long> COOLDOWNS = new HashMap<>();

    private Scp012DoorAccess() {
    }

    public static boolean tryOpen(ServerLevel level, ServerPlayer player,
                                  BlockPos scpPos) {
        BlockPos center = player.blockPosition();
        Vec3 playerPosition = player.position();
        Vec3 targetPosition = Scp012Module.attractionPoint(level, scpPos);
        DoorMatch best = null;
        double bestDistance = Double.MAX_VALUE;
        long time = level.getGameTime();

        if (time % 600L == 0L) {
            COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= time);
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
                    || COOLDOWNS.getOrDefault(pos.asLong(), 0L) > time
                    || !HeavyDoorControlPanelAccess.hasControllableInterface(level, pos)
                    || !liesOnRoute(match.state(), pos, playerPosition,
                    targetPosition)) {
                continue;
            }

            double distance = player.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = match;
            }
        }

        if (best == null) return false;
        BlockState current = level.getBlockState(best.pos());
        if (current.getBlock() != best.family().closed().get()
                || !current.hasProperty(HorizontalDirectionalBlock.FACING)
                || HeavyDoorControlPanelAccess.openConnectedControls(level,
                best.pos()) <= 0) {
            return false;
        }

        Direction facing = current.getValue(HorizontalDirectionalBlock.FACING);
        level.playSound(null, best.pos(), best.family().openingSound().get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        Block firstOpeningStage = best.family().opening().get(0).get();
        level.setBlock(best.pos(), firstOpeningStage.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing),
                Block.UPDATE_ALL);
        emitOverrideParticles(level, best.pos());
        COOLDOWNS.put(best.pos().asLong(), time + COOLDOWN_TICKS);
        return true;
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
}
