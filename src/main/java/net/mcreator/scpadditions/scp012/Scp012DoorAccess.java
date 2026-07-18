package net.mcreator.scpadditions.scp012;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public final class Scp012DoorAccess {
    private static final int PLAYER_DOOR_RADIUS = 5;
    private static final int SCP_DOOR_RADIUS = 15;
    private static final int COOLDOWN_TICKS = 100;
    private static final Map<Long, Long> COOLDOWNS = new HashMap<>();

    private Scp012DoorAccess() {
    }

    public static boolean tryOpen(ServerLevel level, ServerPlayer player,
                                  BlockPos scpPos) {
        BlockPos center = player.blockPosition();
        DoorMatch best = null;
        double bestDistance = Double.MAX_VALUE;
        long time = level.getGameTime();

        for (BlockPos mutable : BlockPos.betweenClosed(
                center.offset(-PLAYER_DOOR_RADIUS, -PLAYER_DOOR_RADIUS,
                        -PLAYER_DOOR_RADIUS),
                center.offset(PLAYER_DOOR_RADIUS, PLAYER_DOOR_RADIUS,
                        PLAYER_DOOR_RADIUS))) {
            BlockPos pos = mutable.immutable();
            if (Vec3.atCenterOf(pos).distanceToSqr(Vec3.atCenterOf(scpPos))
                    > SCP_DOOR_RADIUS * SCP_DOOR_RADIUS) continue;
            DoorMatch match = matchClosedDoor(level, pos);
            if (match == null
                    || COOLDOWNS.getOrDefault(pos.asLong(), 0L) > time
                    || !HeavyDoorControlPanelAccess.hasControllableInterface(level, pos)) {
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
        if (!current.hasProperty(HorizontalDirectionalBlock.FACING)
                || HeavyDoorControlPanelAccess.openConnectedControls(level,
                best.pos()) <= 0) return false;

        Direction facing = current.getValue(HorizontalDirectionalBlock.FACING);
        level.playSound(null, best.pos(), best.family().openingSound().get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        Block firstOpeningStage = best.family().opening().get(0).get();
        level.setBlock(best.pos(), firstOpeningStage.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing),
                Block.UPDATE_ALL);
        COOLDOWNS.put(best.pos().asLong(), time + COOLDOWN_TICKS);
        return true;
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
                return new DoorMatch(pos, family);
            }
        }
        return null;
    }

    private record DoorMatch(BlockPos pos, FacilityModule.DoorFamily family) {
    }
}
