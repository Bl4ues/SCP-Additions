package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import com.bl4ues.scpclassifieddirective.procedures.TeslaGateUpdateTickProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Idle controller for the replacement, floor-anchored Tesla Gate. */
public class TeslaGateBlock extends TeslaGateControllerBlock {
    private static final int SENSOR_INTERVAL_TICKS = 1;

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos controllerPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!context.getLevel().getBlockState(controllerPos.below())
                .isFaceSturdy(context.getLevel(), controllerPos.below(), Direction.UP)
                || !TeslaGateStructure.canPlace(context.getLevel(), controllerPos, facing)) {
            return null;
        }

        boolean waterlogged = context.getLevel().getFluidState(controllerPos).getType()
                == Fluids.WATER;
        return defaultBlockState().setValue(FACING, facing)
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level.isClientSide()) return;

        Direction facing = state.getValue(FACING);
        if (!TeslaGateStructure.isController(oldState)) {
            if (!TeslaGateStructure.placeCollisionParts(level, pos, facing)) {
                level.destroyBlock(pos, true);
                return;
            }
        } else {
            TeslaGateStructure.ensureCollisionParts(level, pos, facing);
        }

        if (level instanceof ServerLevel server) {
            Scp079FacilityAccessManager.registerTeslaGate(server, pos);
        }
        boolean activationQueued = TeslaGateUpdateTickProcedure.execute(
                level, pos.getX(), pos.getY(), pos.getZ());
        if (!activationQueued && isEnabled(level)) {
            level.scheduleTick(pos, this, SENSOR_INTERVAL_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        boolean activationQueued = TeslaGateUpdateTickProcedure.execute(
                level, pos.getX(), pos.getY(), pos.getZ());
        if (!activationQueued && isEnabled(level)) {
            level.scheduleTick(pos, this, SENSOR_INTERVAL_TICKS);
        }
    }

    private static boolean isEnabled(Level level) {
        return Scp079FacilityAccessManager.isAuxiliaryPowerOnline(level)
                && (level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.TESLAGATEON)
                || level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE));
    }
}
