package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.procedures.DeconClosedBlockAddedProcedure;
import net.mcreator.scpadditions.procedures.DecontaminationCheckpointController;

public class DeconClosedBlock extends AbstractDecontaminationBlock {
    @Override
    protected boolean isClosedState() {
        return true;
    }

    @Override
    protected void controllerPlaced(BlockState state, Level level,
            BlockPos pos, BlockState oldState, boolean moving) {
        DeconClosedBlockAddedProcedure.execute(level,
                pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        ensureStructure(level, pos, state);
        DecontaminationCheckpointController.finishClosed(level, pos);
    }
}
