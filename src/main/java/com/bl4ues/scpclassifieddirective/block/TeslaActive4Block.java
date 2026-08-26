package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.procedures.TeslaActive4BProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TeslaActive4Block extends TeslaGateControllerBlock {
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        TeslaActive4BProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
    }
}
