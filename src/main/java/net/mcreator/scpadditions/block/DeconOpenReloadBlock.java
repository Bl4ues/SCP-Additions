package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.procedures.DeconOpenReloadBlockAddedProcedure;

public class DeconOpenReloadBlock extends AbstractDecontaminationBlock {
    @Override
    protected boolean isClosedState() {
        return false;
    }

    @Override
    protected void controllerPlaced(BlockState state, Level level,
            BlockPos pos, BlockState oldState, boolean moving) {
        level.scheduleTick(pos, this, 40);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        ensureStructure(level, pos, state);
        DeconOpenReloadBlockAddedProcedure.execute(level,
                pos.getX(), pos.getY(), pos.getZ());
    }
}
