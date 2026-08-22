package com.bl4ues.scpclassifieddirective.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.HeavyDoorPowerRelay;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorCarriageEntity;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorManager;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runtime state MineZero does not include in its generic checkpoint snapshot. */
public final class MineZeroRuntimeRestoreSupport {
    private static final String DATA_NAME =
            "scp_additions_minezero_runtime_checkpoint";

    private MineZeroRuntimeRestoreSupport() {
    }

    public static void captureCarriages(MinecraftServer server) {
        if (server == null) return;
        RuntimeData data = data(server);
        CompoundTag snapshot = new CompoundTag();
        ListTag carriages = new ListTag();
        ListTag stations = new ListTag();
        Set<String> capturedStations = new HashSet<>();

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof CoreRoomElevatorCarriageEntity carriage)
                        || carriage.isRemoved()) {
                    continue;
                }
                CompoundTag entityTag = new CompoundTag();
                carriage.save(entityTag);
                CompoundTag entry = new CompoundTag();
                entry.putString("Dimension",
                        level.dimension().location().toString());
                entry.put("Entity", entityTag);
                carriages.add(entry);

                for (int floorY : carriage.floorHeights()) {
                    BlockPos stationPos = new BlockPos(carriage.columnX(),
                            floorY, carriage.columnZ());
                    String stationKey = level.dimension().location()
                            + "|" + stationPos.asLong();
                    if (!capturedStations.add(stationKey)
                            || !(level.getBlockEntity(stationPos)
                            instanceof CoreRoomElevatorModule.StationBlockEntity station)) {
                        continue;
                    }
                    CompoundTag stationEntry = new CompoundTag();
                    stationEntry.putString("Dimension",
                            level.dimension().location().toString());
                    stationEntry.putLong("Pos", stationPos.asLong());
                    stationEntry.put("BlockEntity",
                            station.saveWithFullMetadata());
                    stations.add(stationEntry);
                }
            }
        }

        snapshot.put("Carriages", carriages);
        snapshot.put("Stations", stations);
        data.snapshot = snapshot;
        data.setDirty();
    }

    public static void restoreCarriages(MinecraftServer server) {
        if (server == null) return;
        RuntimeData data = data(server);
        boolean hasCarriageSnapshot = data.snapshot.contains(
                "Carriages", Tag.TAG_LIST);

        if (hasCarriageSnapshot) {
            ListTag saved = data.snapshot.getList("Carriages",
                    Tag.TAG_COMPOUND);

            for (ServerLevel level : server.getAllLevels()) {
                List<CoreRoomElevatorCarriageEntity> stale =
                        new ArrayList<>();
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof CoreRoomElevatorCarriageEntity carriage
                            && !carriage.isRemoved()) {
                        stale.add(carriage);
                    }
                }
                stale.forEach(Entity::discard);
            }

            for (int index = 0; index < saved.size(); index++) {
                CompoundTag entry = saved.getCompound(index);
                ResourceLocation dimensionId = ResourceLocation.tryParse(
                        entry.getString("Dimension"));
                if (dimensionId == null
                        || !entry.contains("Entity", Tag.TAG_COMPOUND)) {
                    continue;
                }
                ServerLevel level = server.getLevel(ResourceKey.create(
                        Registries.DIMENSION, dimensionId));
                if (level == null) continue;

                CompoundTag entityTag = entry.getCompound("Entity").copy();
                try {
                    EntityType.loadEntityRecursive(entityTag, level, loaded -> {
                        if (!(loaded instanceof CoreRoomElevatorCarriageEntity carriage)) {
                            return loaded;
                        }
                        CoreRoomElevatorManager.ColumnLayout layout =
                                CoreRoomElevatorManager.discover(level,
                                        carriage.columnX(), carriage.columnZ());
                        if (!layout.complete()) {
                            ScpClassifiedDirectiveMod.LOGGER.warn(
                                    "Could not restore Core Room elevator carriage at {}, {}: column is incomplete",
                                    carriage.columnX(), carriage.columnZ());
                            return carriage;
                        }
                        carriage.applyLayout(layout);
                        if (!level.addFreshEntity(carriage)) {
                            ScpClassifiedDirectiveMod.LOGGER.warn(
                                    "Could not re-add Core Room elevator carriage at {}, {} after MineZero restore",
                                    carriage.columnX(), carriage.columnZ());
                        }
                        return carriage;
                    });
                } catch (Exception exception) {
                    ScpClassifiedDirectiveMod.LOGGER.error(
                            "Could not restore a Core Room elevator carriage from the MineZero checkpoint",
                            exception);
                }
            }
        }

        restoreStationState(server, data.snapshot);
        resynchronizeLoadedScpBlockEntities(server);
        ScpClassifiedDirectiveMod.queueServerWork(2,
                () -> resynchronizeLoadedScpBlockEntities(server));
    }

    private static void restoreStationState(MinecraftServer server,
            CompoundTag snapshot) {
        if (snapshot == null
                || !snapshot.contains("Stations", Tag.TAG_LIST)) return;
        ListTag stations = snapshot.getList("Stations", Tag.TAG_COMPOUND);
        for (int index = 0; index < stations.size(); index++) {
            CompoundTag entry = stations.getCompound(index);
            ResourceLocation dimensionId = ResourceLocation.tryParse(
                    entry.getString("Dimension"));
            if (dimensionId == null
                    || !entry.contains("BlockEntity", Tag.TAG_COMPOUND)) {
                continue;
            }
            ServerLevel level = server.getLevel(ResourceKey.create(
                    Registries.DIMENSION, dimensionId));
            if (level == null) continue;
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            if (!(level.getBlockEntity(pos)
                    instanceof CoreRoomElevatorModule.StationBlockEntity station)) {
                continue;
            }
            station.load(entry.getCompound("BlockEntity").copy());
            station.setChanged();
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
    }

    /**
     * MineZero rewinds game time but does not rewind Minecraft's scheduled-tick
     * queues. Clear stale ticks for restored SCP runtime blocks and their linked
     * neighbors, then rebuild local power state and restart required machines.
     */
    public static void rehydrateBlockDiff(MinecraftServer server,
            CompoundTag diff) {
        if (server == null || diff == null || diff.isEmpty()) return;

        List<RestoredBlock> restored = new ArrayList<>();
        for (String key : diff.getAllKeys()) {
            CompoundTag entry = diff.getCompound(key);
            ResourceLocation dimensionId = ResourceLocation.tryParse(
                    entry.getString("Dimension"));
            if (dimensionId == null) continue;
            ServerLevel level = server.getLevel(ResourceKey.create(
                    Registries.DIMENSION, dimensionId));
            if (level == null) continue;
            restored.add(new RestoredBlock(level,
                    BlockPos.of(entry.getLong("Pos"))));
        }

        Set<RestoredBlock> affected = new LinkedHashSet<>(restored);
        for (RestoredBlock restoredBlock : restored) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = restoredBlock.pos().relative(direction);
                if (isFacilityRuntimeBlock(
                        restoredBlock.level().getBlockState(neighborPos))) {
                    affected.add(new RestoredBlock(restoredBlock.level(),
                            neighborPos.immutable()));
                }
            }
        }

        for (RestoredBlock affectedBlock : affected) {
            if (isFacilityRuntimeBlock(affectedBlock.level()
                    .getBlockState(affectedBlock.pos()))) {
                clearRuntimeTick(affectedBlock.level(), affectedBlock.pos());
            }
        }

        for (RestoredBlock affectedBlock : affected) {
            rehydrateBlock(affectedBlock.level(), affectedBlock.pos());
        }
    }

    private static void clearRuntimeTick(ServerLevel level, BlockPos pos) {
        level.getBlockTicks().clearArea(new BoundingBox(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ()));
    }

    private static void rehydrateBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, block);
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighbor = level.getBlockState(neighborPos);
            level.updateNeighborsAt(neighborPos, neighbor.getBlock());
        }

        if (state.is(HeavyDoorPowerRelay.RELAY.get())) {
            level.scheduleTick(pos, block, 1);
            return;
        }

        if (FacilityModule.isFacilityDoor(state)) {
            level.scheduleTick(pos, block, 1);
            return;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())
                || !id.getPath().startsWith("button_")) {
            return;
        }
        String path = id.getPath();
        if (path.contains("opening") || path.contains("closing")) {
            level.scheduleTick(pos, block, 1);
        }
    }

    private static boolean isFacilityRuntimeBlock(BlockState state) {
        if (state == null) return false;
        if (state.is(HeavyDoorPowerRelay.RELAY.get())
                || FacilityModule.isFacilityDoor(state)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())
                && id.getPath().startsWith("button_");
    }

    /** MineZero reloads block-entity NBT without sending client update packets. */
    private static void resynchronizeLoadedScpBlockEntities(
            MinecraftServer server) {
        if (server == null) return;
        int viewDistance = server.getPlayerList().getViewDistance();

        for (ServerLevel level : server.getAllLevels()) {
            ServerChunkCache chunkCache = level.getChunkSource();
            Set<ChunkPos> processed = new HashSet<>();
            for (ServerPlayer player : level.players()) {
                ChunkPos center = player.chunkPosition();
                for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                    for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                        ChunkPos chunkPos = new ChunkPos(center.x + dx,
                                center.z + dz);
                        if (!processed.add(chunkPos)) continue;
                        LevelChunk chunk = chunkCache.getChunkNow(
                                chunkPos.x, chunkPos.z);
                        if (chunk == null) continue;

                        for (BlockEntity blockEntity
                                : chunk.getBlockEntities().values()) {
                            if (blockEntity == null || blockEntity.isRemoved()) {
                                continue;
                            }
                            BlockPos pos = blockEntity.getBlockPos();
                            BlockState state = level.getBlockState(pos);
                            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(
                                    state.getBlock());
                            if (id == null
                                    || !ScpClassifiedDirectiveMod.MODID.equals(
                                    id.getNamespace())) {
                                continue;
                            }
                            blockEntity.setChanged();
                            level.sendBlockUpdated(pos, state, state,
                                    Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
    }

    private static RuntimeData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                RuntimeData::load, RuntimeData::new, DATA_NAME);
    }

    private record RestoredBlock(ServerLevel level, BlockPos pos) {
    }

    private static final class RuntimeData extends SavedData {
        private CompoundTag snapshot = new CompoundTag();

        private RuntimeData() {
        }

        private static RuntimeData load(CompoundTag tag) {
            RuntimeData data = new RuntimeData();
            if (tag.contains("Snapshot", Tag.TAG_COMPOUND)) {
                data.snapshot = tag.getCompound("Snapshot").copy();
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.put("Snapshot", snapshot.copy());
            return tag;
        }
    }
}
