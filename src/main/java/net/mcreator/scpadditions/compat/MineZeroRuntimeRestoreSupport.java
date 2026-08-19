package net.mcreator.scpadditions.compat;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.FacilityModule;
import net.mcreator.scpadditions.facility.elevator.CoreRoomElevatorCarriageEntity;
import net.mcreator.scpadditions.facility.elevator.CoreRoomElevatorManager;

import java.util.ArrayList;
import java.util.List;

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
            }
        }

        snapshot.put("Carriages", carriages);
        data.snapshot = snapshot;
        data.setDirty();
    }

    public static void restoreCarriages(MinecraftServer server) {
        if (server == null) return;
        RuntimeData data = data(server);
        ListTag saved = data.snapshot.getList("Carriages", Tag.TAG_COMPOUND);

        for (ServerLevel level : server.getAllLevels()) {
            List<CoreRoomElevatorCarriageEntity> stale = new ArrayList<>();
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
                        ScpAdditionsMod.LOGGER.warn(
                                "Could not restore Core Room elevator carriage at {}, {}: column is incomplete",
                                carriage.columnX(), carriage.columnZ());
                        return carriage;
                    }
                    carriage.applyLayout(layout);
                    if (!level.addFreshEntity(carriage)) {
                        ScpAdditionsMod.LOGGER.warn(
                                "Could not re-add Core Room elevator carriage at {}, {} after MineZero restore",
                                carriage.columnX(), carriage.columnZ());
                    }
                    return carriage;
                });
            } catch (Exception exception) {
                ScpAdditionsMod.LOGGER.error(
                        "Could not restore a Core Room elevator carriage from the MineZero checkpoint",
                        exception);
            }
        }
    }

    /**
     * MineZero rewinds game time but does not rewind Minecraft's scheduled-tick
     * queues. Clear stale ticks for restored SCP blocks, then rebuild the local
     * neighbor state and restart only the runtime machines that need ticking.
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

        for (RestoredBlock restoredBlock : restored) {
            ServerLevel level = restoredBlock.level();
            BlockPos pos = restoredBlock.pos();
            level.getBlockTicks().clearArea(new BoundingBox(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX(), pos.getY(), pos.getZ()));
        }

        for (RestoredBlock restoredBlock : restored) {
            rehydrateBlock(restoredBlock.level(), restoredBlock.pos());
        }
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

        if (FacilityModule.isFacilityDoor(state)) {
            level.scheduleTick(pos, block, 1);
            return;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !ScpAdditionsMod.MODID.equals(id.getNamespace())
                || !id.getPath().startsWith("button_")) {
            return;
        }
        String path = id.getPath();
        if (path.contains("opening") || path.contains("closing")) {
            level.scheduleTick(pos, block, 1);
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
