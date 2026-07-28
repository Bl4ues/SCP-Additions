package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Render-only block entity for the static SCP-914 usage notice. */
public final class Scp914UsageNoticeBlockEntity extends BlockEntity {
    public Scp914UsageNoticeBlockEntity(BlockPos pos, BlockState state) {
        super(FacilityModule.SCP_914_NOTICE_BLOCK_ENTITY.get(), pos, state);
    }
}
