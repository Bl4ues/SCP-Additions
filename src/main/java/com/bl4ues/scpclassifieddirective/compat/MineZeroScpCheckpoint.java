package com.bl4ues.scpclassifieddirective.compat;

import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.saveddata.SavedData;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079RollbackSupport;
import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;

import java.util.Set;

/** Complements MineZero snapshots with SCP: Classified Directive-owned state. */
public final class MineZeroScpCheckpoint {
    private static final String DATA_NAME =
            "scp_additions_minezero_compat_checkpoint";

    private MineZeroScpCheckpoint() {
    }

    public static void capture(MinecraftServer server) {
        if (server == null) return;
        SnapshotData data = data(server);
        CompoundTag root = new CompoundTag();
        CompoundTag players = new CompoundTag();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CompoundTag playerTag = new CompoundTag();
            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inv ->
                    playerTag.put("ScpInventory", inv.serializeNBT().copy()));
            player.getCapability(
                    ScpClassifiedDirectiveModVariables.PLAYER_VARIABLES_CAPABILITY)
                    .ifPresent(variables -> {
                        Tag tag = variables.writeNBT();
                        if (tag instanceof CompoundTag compound) {
                            playerTag.put("PlayerVariables", compound.copy());
                        }
                    });
            playerTag.put("Persistent", capturePersistent(player));
            players.put(player.getUUID().toString(), playerTag);
        }
        root.put("Players", players);

        ScpClassifiedDirectiveModVariables.MapVariables map =
                ScpClassifiedDirectiveModVariables.MapVariables.get(server.overworld());
        ScpClassifiedDirectiveModVariables.WorldVariables world =
                ScpClassifiedDirectiveModVariables.WorldVariables.get(server.overworld());
        root.put("MapVariables", map.save(new CompoundTag()));
        root.put("WorldVariables", world.save(new CompoundTag()));
        root.put("Scp079", Scp079RollbackSupport.capture(server));

        data.snapshot = root;
        data.blockDiff = new CompoundTag();
        data.setDirty();
    }

    /** Records the original side of a SCP: Classified Directive block mutation exactly once. */
    public static void recordBlockBeforeChange(ServerLevel level, BlockPos pos,
            BlockState replacement) {
        if (level == null || pos == null || replacement == null
                || !MineZeroCompatibility.enabled()
                || MineZeroCompatibility.restoring()) return;

        BlockState original = level.getBlockState(pos);
        if (!isScpClassifiedDirectiveBlock(original)
                && !isScpClassifiedDirectiveBlock(replacement)) return;

        SnapshotData data = data(level.getServer());
        String key = blockDiffKey(level, pos);
        if (data.blockDiff.contains(key, Tag.TAG_COMPOUND)) return;

        CompoundTag entry = new CompoundTag();
        entry.putString("Dimension", level.dimension().location().toString());
        entry.putLong("Pos", pos.asLong());
        entry.put("State", writeBlockState(original));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            entry.put("BlockEntity", blockEntity.saveWithFullMetadata());
        }
        data.blockDiff.put(key, entry);
        data.setDirty();
    }

    public static void restore(MinecraftServer server) {
        if (server == null) return;
        SnapshotData data = data(server);
        CompoundTag root = data.snapshot;
        if (root == null || root.isEmpty()) return;

        restoreBlockDiff(server, data.blockDiff);

        CompoundTag players = root.getCompound("Players");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!players.contains(player.getUUID().toString(), Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag playerTag = players.getCompound(player.getUUID().toString());
            if (playerTag.contains("ScpInventory", Tag.TAG_COMPOUND)) {
                player.getCapability(ScpInventoryCapability.INSTANCE)
                        .ifPresent(inv -> {
                            inv.deserializeNBT(playerTag.getCompound("ScpInventory").copy());
                            ModNetwork.syncTo(player, inv);
                        });
            }
            if (playerTag.contains("PlayerVariables", Tag.TAG_COMPOUND)) {
                player.getCapability(
                        ScpClassifiedDirectiveModVariables.PLAYER_VARIABLES_CAPABILITY)
                        .ifPresent(variables -> {
                            variables.readNBT(playerTag.getCompound("PlayerVariables").copy());
                            variables.syncPlayerVariables(player);
                        });
            }
            restorePersistent(player, playerTag.getCompound("Persistent"));
        }

        if (root.contains("MapVariables", Tag.TAG_COMPOUND)) {
            ScpClassifiedDirectiveModVariables.MapVariables map =
                    ScpClassifiedDirectiveModVariables.MapVariables.get(server.overworld());
            map.read(root.getCompound("MapVariables"));
            map.syncData(server.overworld());
        }
        if (root.contains("WorldVariables", Tag.TAG_COMPOUND)) {
            ScpClassifiedDirectiveModVariables.WorldVariables world =
                    ScpClassifiedDirectiveModVariables.WorldVariables.get(server.overworld());
            world.read(root.getCompound("WorldVariables"));
            world.syncData(server.overworld());
        }
        if (root.contains("Scp079", Tag.TAG_COMPOUND)) {
            Scp079RollbackSupport.restore(server, root.getCompound("Scp079"));
        }

        data.blockDiff = new CompoundTag();
        data.setDirty();
    }

    private static void restoreBlockDiff(MinecraftServer server,
            CompoundTag diff) {
        if (diff == null || diff.isEmpty()) return;
        for (String key : diff.getAllKeys()) {
            CompoundTag entry = diff.getCompound(key);
            ResourceLocation dimensionId = ResourceLocation.tryParse(
                    entry.getString("Dimension"));
            if (dimensionId == null) continue;
            ServerLevel level = server.getLevel(ResourceKey.create(
                    Registries.DIMENSION, dimensionId));
            if (level == null) continue;

            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            BlockState state = readBlockState(entry.getCompound("State"));
            level.setBlock(pos, state, 3);
            if (entry.contains("BlockEntity", Tag.TAG_COMPOUND)) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    blockEntity.load(entry.getCompound("BlockEntity").copy());
                    blockEntity.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                }
            }
        }
    }

    private static CompoundTag writeBlockState(BlockState state) {
        CompoundTag tag = new CompoundTag();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        tag.putString("Block", id.toString());
        CompoundTag properties = new CompoundTag();
        for (Property<?> property : state.getProperties()) {
            properties.putString(property.getName(), propertyValue(state, property));
        }
        tag.put("Properties", properties);
        return tag;
    }

    private static BlockState readBlockState(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Block"));
        Block block = id == null ? Blocks.AIR
                : BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
        BlockState state = block.defaultBlockState();
        CompoundTag properties = tag.getCompound("Properties");
        for (Property<?> property : state.getProperties()) {
            if (properties.contains(property.getName(), Tag.TAG_STRING)) {
                state = applyProperty(state, property,
                        properties.getString(property.getName()));
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> String propertyValue(
            BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState applyProperty(
            BlockState state, Property<T> property, String value) {
        return property.getValue(value)
                .map(parsed -> state.setValue(property, parsed))
                .orElse(state);
    }

    private static boolean isScpClassifiedDirectiveBlock(BlockState state) {
        if (state == null) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace());
    }

    private static String blockDiffKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.asLong();
    }

    private static CompoundTag capturePersistent(ServerPlayer player) {
        CompoundTag captured = new CompoundTag();
        CompoundTag source = player.getPersistentData();
        for (String key : source.getAllKeys()) {
            if (!ownedPersistentKey(key)) continue;
            Tag value = source.get(key);
            if (value != null) captured.put(key, value.copy());
        }
        return captured;
    }

    private static void restorePersistent(ServerPlayer player,
            CompoundTag captured) {
        CompoundTag target = player.getPersistentData();
        Set<String> keys = Set.copyOf(target.getAllKeys());
        for (String key : keys) {
            if (ownedPersistentKey(key)) target.remove(key);
        }
        for (String key : captured.getAllKeys()) {
            Tag value = captured.get(key);
            if (value != null) target.put(key, value.copy());
        }
    }

    private static boolean ownedPersistentKey(String key) {
        if (key == null) return false;
        return key.startsWith("scp_classified_directive")
                || key.startsWith("scp_additions")
                || key.equals("candy0") || key.equals("candy1")
                || key.equals("candy2");
    }

    private static SnapshotData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SnapshotData::load, SnapshotData::new, DATA_NAME);
    }

    private static final class SnapshotData extends SavedData {
        private CompoundTag snapshot = new CompoundTag();
        private CompoundTag blockDiff = new CompoundTag();

        private SnapshotData() {
        }

        private static SnapshotData load(CompoundTag tag) {
            SnapshotData data = new SnapshotData();
            if (tag.contains("Snapshot", Tag.TAG_COMPOUND)) {
                data.snapshot = tag.getCompound("Snapshot").copy();
            }
            if (tag.contains("BlockDiff", Tag.TAG_COMPOUND)) {
                data.blockDiff = tag.getCompound("BlockDiff").copy();
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.put("Snapshot", snapshot.copy());
            tag.put("BlockDiff", blockDiff.copy());
            return tag;
        }
    }
}
