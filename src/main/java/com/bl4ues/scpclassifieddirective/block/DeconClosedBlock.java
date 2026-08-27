package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.procedures.DecontaminationCheckpointController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy transient ID retained for existing worlds and MineZero snapshots. */
public class DeconClosedBlock extends AbstractDecontaminationBlock {
    @Override
    protected boolean isClosedState() {
        return true;
    }

    @Override
    protected void controllerPlaced(BlockState state, Level level,
            BlockPos pos, BlockState oldState, boolean moving) {
        if (!level.isClientSide) {
            DecontaminationCheckpointController.beginClosed(level,
                    pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
