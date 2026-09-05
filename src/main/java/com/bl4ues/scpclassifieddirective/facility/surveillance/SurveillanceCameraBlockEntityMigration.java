package com.bl4ues.scpclassifieddirective.facility.surveillance;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-way compatibility bridge for test worlds that already contained the old
 * non-BlockEntity surveillance placeholder when the block was upgraded to the
 * GeckoLib implementation. Those chunks have the correct block state but no
 * BlockEntity NBT, which leaves ENTITYBLOCK_ANIMATED with nothing to render.
 *
 * <p>ChunkEvent.Load is deliberately used only as a notification. The actual
 * migration is deferred until a normal server tick, after the LevelChunk has
 * completed promotion. Performing SavedData lookups, BlockEntity insertion or
 * block-update broadcasts while a chunk is still loading can re-enter the
 * chunk pipeline and deadlock integrated-server spawn preparation at 90%.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SurveillanceCameraBlockEntityMigration {
    private static final int MAX_CHUNKS_PER_TICK = 32;
    private static final Set<PendingChunk> PENDING =
            ConcurrentHashMap.newKeySet();

    private SurveillanceCameraBlockEntityMigration() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        // Do not touch SavedData, block entities or Level methods here. This
        // event can fire before the chunk is fully promoted and must remain a
        // purely local bookkeeping callback.
        PENDING.add(new PendingChunk(level.dimension(),
                chunk.getPos().x, chunk.getPos().z));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;

        MinecraftServer server = event.getServer();
        int processed = 0;
        for (PendingChunk pending : List.copyOf(PENDING)) {
            if (processed >= MAX_CHUNKS_PER_TICK) break;
            if (!PENDING.remove(pending)) continue;
            processed++;

            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null) continue;

            // getChunkNow never starts a chunk load. If the chunk was unloaded
            // before this deferred pass, simply drop it; a later load event will
            // queue it again.
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    pending.chunkX(), pending.chunkZ());
            if (chunk == null) continue;
            migrateChunk(server, level, chunk);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    private static void migrateChunk(MinecraftServer server, ServerLevel level,
            LevelChunk chunk) {
        int loadedX = chunk.getPos().x;
        int loadedZ = chunk.getPos().z;
        for (FacilityCameraDefinition definition :
                FacilitySurveillanceSavedData.get(server).all()) {
            if (!definition.dimension().equals(level.dimension().location())) {
                continue;
            }
            BlockPos pos = definition.anchorPos();
            if ((pos.getX() >> 4) != loadedX || (pos.getZ() >> 4) != loadedZ) {
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

    private record PendingChunk(ResourceKey<Level> dimension,
            int chunkX, int chunkZ) {
    }
}
