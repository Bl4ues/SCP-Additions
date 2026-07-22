package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.procedures.DeconOpenUpdateTickProcedure;

public class DeconOpenBlock extends AbstractDecontaminationBlock {
    @Override
    protected boolean isClosedState() {
        return false;
    }

    @Override
    protected boolean raisesOnInitialPlacement() {
        return true;
    }

    @Override
    protected void controllerPlaced(BlockState state, Level level,
            BlockPos pos, BlockState oldState, boolean moving) {
        level.scheduleTick(pos, this, 5);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        ensureStructure(level, pos, state);
        DeconOpenUpdateTickProcedure.execute(level,
                pos.getX(), pos.getY(), pos.getZ());
        if (level.getBlockState(pos).is(this)) {
            level.scheduleTick(pos, this, 5);
        }
    }
}
