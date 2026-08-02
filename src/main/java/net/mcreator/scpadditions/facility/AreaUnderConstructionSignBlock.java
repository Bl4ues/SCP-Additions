package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Decorative construction-area notice in the shared glass frame. */
public final class AreaUnderConstructionSignBlock
        extends AbstractFramedSignBlock {
    public AreaUnderConstructionSignBlock() {
        super(FacilityLargePropStructure.Kind.UNDER_CONSTRUCTION_NOTICE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AreaUnderConstructionSignBlockEntity(pos, state);
    }
}
