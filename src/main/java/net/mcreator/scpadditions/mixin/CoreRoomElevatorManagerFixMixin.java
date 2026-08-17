package net.mcreator.scpadditions.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.facility.elevator.CoreRoomElevatorManager;
import net.mcreator.scpadditions.facility.elevator.CoreRoomElevatorModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Makes the pulley the authority that closes and generates an elevator column. */
@Mixin(value = CoreRoomElevatorManager.class, remap = false)
public abstract class CoreRoomElevatorManagerFixMixin {
    @Inject(method = "findPulleyFacing", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$enforcePulleyRange(Level level,
            BlockPos pulleyPos, CallbackInfoReturnable<Direction> callback) {
        Direction facing = null;
        int highestStationY = Integer.MIN_VALUE;
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            BlockState state = level.getBlockState(new BlockPos(
                    pulleyPos.getX(), y, pulleyPos.getZ()));
            if (!state.is(CoreRoomElevatorModule.STATION.get())) continue;
            if (y >= pulleyPos.getY()) {
                callback.setReturnValue(null);
                return;
            }
            Direction stationFacing = state.getValue(CoreRoomElevatorModule.FACING);
            if (facing == null) facing = stationFacing;
            else if (facing != stationFacing) {
                callback.setReturnValue(null);
                return;
            }
            highestStationY = Math.max(highestStationY, y);
        }

        if (facing == null || highestStationY == Integer.MIN_VALUE
                || !scpAdditions$validPulleyRange(highestStationY,
                        pulleyPos.getY())) {
            callback.setReturnValue(null);
            return;
        }
        callback.setReturnValue(facing);
    }

    @Inject(method = "discover", at = @At("RETURN"), cancellable = true)
    private static void scpAdditions$rejectOutOfRangePulley(ServerLevel level,
            int x, int z,
            CallbackInfoReturnable<CoreRoomElevatorManager.ColumnLayout> callback) {
        CoreRoomElevatorManager.ColumnLayout layout = callback.getReturnValue();
        if (layout == null || layout.pulley() == null
                || layout.stations().isEmpty()) {
            return;
        }
        List<BlockPos> stations = layout.stations();
        int highestStationY = stations.get(stations.size() - 1).getY();
        if (scpAdditions$validPulleyRange(highestStationY,
                layout.pulley().getY())) {
            return;
        }
        callback.setReturnValue(new CoreRoomElevatorManager.ColumnLayout(
                layout.x(), layout.z(), layout.facing(), layout.stations(),
                null, false));
    }

    @Inject(method = "fillBeams", at = @At("HEAD"), cancellable = true)
    private static void scpAdditions$requireCompletedPulley(ServerLevel level,
            int x, int z, int startY, int endY, Direction facing,
            CallbackInfoReturnable<Boolean> callback) {
        CoreRoomElevatorManager.ColumnLayout layout =
                CoreRoomElevatorManager.discover(level, x, z);
        if (!layout.complete()) {
            // rebuildColumn removes old generated beams before reaching here.
            // Returning success avoids treating an intentionally open column as
            // an obstruction while preventing any station-only beam generation.
            callback.setReturnValue(true);
        }
    }

    @Unique
    private static boolean scpAdditions$validPulleyRange(int stationY,
            int pulleyY) {
        int distance = pulleyY - stationY;
        return distance >= CoreRoomElevatorModule.STATION_HEIGHT_BLOCKS
                && distance <= CoreRoomElevatorModule.MAX_FLOOR_SPACING;
    }
}
