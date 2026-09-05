package com.bl4ues.scpclassifieddirective.facility.surveillance;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * One-way compatibility bridge for test worlds that already contained the old
 * non-BlockEntity surveillance placeholder when the block was upgraded to the
 * GeckoLib implementation. Those chunks have the correct block state but no
 * BlockEntity NBT, which leaves ENTITYBLOCK_ANIMATED with nothing to render.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SurveillanceCameraBlockEntityMigration {
    private SurveillanceCameraBlockEntityMigration() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        ChunkPos loaded = chunk.getPos();
        for (FacilityCameraDefinition definition :
                FacilitySurveillanceSavedData.get(level.getServer()).all()) {
            if (!definition.dimension().equals(level.dimension().location())) {
                continue;
            }
            BlockPos pos = definition.anchorPos();
            if (SectionCoordinates.chunk(pos.getX()) != loaded.x
                    || SectionCoordinates.chunk(pos.getZ()) != loaded.z) {
                continue;
            }

            BlockState state = chunk.getBlockState(pos);
            if (!state.is(SurveillanceCameraPlaceholderModule.BLOCK.get())
                    || chunk.getBlockEntity(pos) != null) {
                continue;
            }

            BlockEntity blockEntity =
                    new SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity(
                            pos, state);
            chunk.setBlockEntity(blockEntity);
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Avoids any Level#getChunk call while already inside ChunkEvent.Load. */
    private static final class SectionCoordinates {
        private SectionCoordinates() {
        }

        private static int chunk(int blockCoordinate) {
            return blockCoordinate >> 4;
        }
    }
}
