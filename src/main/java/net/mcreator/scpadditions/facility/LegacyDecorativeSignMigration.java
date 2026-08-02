package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.ArrayList;
import java.util.List;

/** Converts retired standalone notice blocks without damaging existing worlds. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyDecorativeSignMigration {
    private LegacyDecorativeSignMigration() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        List<BlockPos> positions = new ArrayList<>();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof Scp914UsageNoticeBlockEntity
                    || blockEntity
                    instanceof AreaUnderConstructionSignBlockEntity) {
                positions.add(blockEntity.getBlockPos().immutable());
            }
        }
        if (!positions.isEmpty()) {
            level.getServer().execute(() -> positions.forEach(pos ->
                    migrateIfNeeded(level, pos)));
        }
    }

    @SubscribeEvent
    public static void onPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Block block = event.getPlacedBlock().getBlock();
        if (block != FacilityModule.SCP_914_USAGE_NOTICE.get()
                && block != AreaUnderConstructionSignModule.BLOCK.get()) {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        level.getServer().execute(() -> migrateIfNeeded(level, pos));
    }

    private static void migrateIfNeeded(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        BlockState oldState = level.getBlockState(pos);
        String templateId;
        if (oldState.is(FacilityModule.SCP_914_USAGE_NOTICE.get())) {
            templateId = ScpSignTemplates.SCP_914_NOTICE;
        } else if (oldState.is(AreaUnderConstructionSignModule.BLOCK.get())) {
            templateId = ScpSignTemplates.UNDER_CONSTRUCTION;
        } else {
            return;
        }

        BlockState replacement = FacilityModule.SIGN_SUPPORT.get()
                .defaultBlockState()
                .setValue(AbstractFramedSignBlock.FACING,
                        oldState.getValue(AbstractFramedSignBlock.FACING))
                .setValue(AbstractFramedSignBlock.POSITION,
                        oldState.getValue(AbstractFramedSignBlock.POSITION))
                .setValue(AbstractFramedSignBlock.WATERLOGGED,
                        oldState.getValue(AbstractFramedSignBlock.WATERLOGGED));
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos)
                instanceof ScpSignSupportBlockEntity sign) {
            sign.setData(ScpSignData.DEFAULT.withTemplateId(templateId));
        }
    }
}
