package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Render-only block entity for the static construction-area notice. */
public final class AreaUnderConstructionSignBlockEntity extends BlockEntity {
    public AreaUnderConstructionSignBlockEntity(BlockPos pos,
            BlockState state) {
        super(AreaUnderConstructionSignModule.BLOCK_ENTITY.get(), pos, state);
    }
}
