package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Static decorative SCP-914 notice using the shared glass-backed sign frame. */
public final class Scp914UsageNoticeBlock extends AbstractFramedSignBlock {
    public Scp914UsageNoticeBlock() {
        super(FacilityLargePropStructure.Kind.SCP_914_NOTICE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Scp914UsageNoticeBlockEntity(pos, state);
    }
}
