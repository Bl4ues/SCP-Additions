from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[2]

def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")

def replace(path: str, old: str, new: str, count: int = -1) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Missing replacement anchor in {path}: {old[:100]!r}")
    target.write_text(text.replace(old, new, count), encoding="utf-8")

saved_data = r'''package net.mcreator.scpadditions.facility;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/** Persistent state for the powered facility diagnostic bus and SCP-079 discovery. */
final class Scp079FacilityAccessSavedData extends SavedData {
    private static final String DATA_NAME = "scp_additions_scp079_facility_access";

    private boolean auxiliaryPowerOnline;
    private boolean protocolExposed;
    private boolean facilityAccess;
    private double discoveryProgress;

    private final Set<TrackedPosition> hosts = new LinkedHashSet<>();
    private final Set<TrackedPosition> doors = new LinkedHashSet<>();
    private final Set<TrackedPosition> teslaGates = new LinkedHashSet<>();
    private final Set<TrackedPosition> auxiliaryUnits = new LinkedHashSet<>();

    private Scp079FacilityAccessSavedData() {
    }

    static Scp079FacilityAccessSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                Scp079FacilityAccessSavedData::load,
                Scp079FacilityAccessSavedData::new,
                DATA_NAME);
    }

    private static Scp079FacilityAccessSavedData load(CompoundTag tag) {
        Scp079FacilityAccessSavedData data = new Scp079FacilityAccessSavedData();
        data.auxiliaryPowerOnline = tag.getBoolean("AuxiliaryPowerOnline");
        data.protocolExposed = tag.getBoolean("ProtocolExposed");
        data.facilityAccess = tag.getBoolean("FacilityAccess");
        if (tag.contains("DiscoveryProgress", Tag.TAG_ANY_NUMERIC)) {
            data.discoveryProgress = clamp(tag.getDouble("DiscoveryProgress"));
        }
        readPositions(tag, "Hosts", data.hosts);
        readPositions(tag, "Doors", data.doors);
        readPositions(tag, "TeslaGates", data.teslaGates);
        readPositions(tag, "AuxiliaryUnits", data.auxiliaryUnits);
        return data;
    }

    boolean auxiliaryPowerOnline() {
        return auxiliaryPowerOnline;
    }

    void setAuxiliaryPowerOnline(boolean value) {
        if (auxiliaryPowerOnline == value) return;
        auxiliaryPowerOnline = value;
        setDirty();
    }

    boolean protocolExposed() {
        return protocolExposed;
    }

    void setProtocolExposed(boolean value) {
        if (protocolExposed == value) return;
        protocolExposed = value;
        setDirty();
    }

    boolean facilityAccess() {
        return facilityAccess;
    }

    void setFacilityAccess(boolean value) {
        if (facilityAccess == value) return;
        facilityAccess = value;
        setDirty();
    }

    double discoveryProgress() {
        return discoveryProgress;
    }

    void setDiscoveryProgress(double value) {
        double clamped = clamp(value);
        if (Math.abs(clamped - discoveryProgress) < 0.000001D) return;
        discoveryProgress = clamped;
        setDirty();
    }

    Set<TrackedPosition> hosts() {
        return hosts;
    }

    Set<TrackedPosition> doors() {
        return doors;
    }

    Set<TrackedPosition> teslaGates() {
        return teslaGates;
    }

    Set<TrackedPosition> auxiliaryUnits() {
        return auxiliaryUnits;
    }

    void markChanged() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("AuxiliaryPowerOnline", auxiliaryPowerOnline);
        tag.putBoolean("ProtocolExposed", protocolExposed);
        tag.putBoolean("FacilityAccess", facilityAccess);
        tag.putDouble("DiscoveryProgress", discoveryProgress);
        writePositions(tag, "Hosts", hosts);
        writePositions(tag, "Doors", doors);
        writePositions(tag, "TeslaGates", teslaGates);
        writePositions(tag, "AuxiliaryUnits", auxiliaryUnits);
        return tag;
    }

    private static void writePositions(CompoundTag root, String key,
            Set<TrackedPosition> positions) {
        ListTag list = new ListTag();
        for (TrackedPosition position : positions) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", position.dimension());
            entry.putLong("Pos", position.packedPos());
            list.add(entry);
        }
        root.put(key, list);
    }

    private static void readPositions(CompoundTag root, String key,
            Set<TrackedPosition> target) {
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            String dimension = entry.getString("Dimension");
            if (!dimension.isBlank()) {
                target.add(new TrackedPosition(dimension, entry.getLong("Pos")));
            }
        }
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    record TrackedPosition(String dimension, long packedPos) {
        static TrackedPosition of(String dimension, long packedPos) {
            return new TrackedPosition(dimension, packedPos);
        }

        int chunkX() {
            return net.minecraft.core.BlockPos.getX(packedPos) >> 4;
        }

        int chunkZ() {
            return net.minecraft.core.BlockPos.getZ(packedPos) >> 4;
        }
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessSavedData.java", saved_data)

manager = r'''package net.mcreator.scpadditions.facility;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.block.Scp079AuxiliaryPowerBlock;
import net.mcreator.scpadditions.block.TeslaGateStructure;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Global, deliberately simple facility bus used by diagnostics and SCP-079.
 * Nothing is wired by radius or site ID: registered facility blocks share one
 * world-level auxiliary channel.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079FacilityAccessManager {
    private static final double PASSIVE_DISCOVERY_PER_SECOND = 0.025D;
    private static final double SCAN_DISCOVERY = 8.0D;

    private Scp079FacilityAccessManager() {
    }

    public enum Activity {
        DOOR_OPENED(0.75D),
        TESLA_TRAVERSAL(1.50D),
        TESLA_CONFIGURATION(3.00D);

        private final double discovery;

        Activity(double discovery) {
            this.discovery = discovery;
        }
    }

    public static boolean isAuxiliaryPowerOnline(LevelAccessor level) {
        MinecraftServer server = server(level);
        return server != null && data(server).auxiliaryPowerOnline();
    }

    public static boolean hasFacilityAccess(LevelAccessor level) {
        MinecraftServer server = server(level);
        if (server == null) return false;
        Scp079FacilityAccessSavedData data = data(server);
        return data.auxiliaryPowerOnline() && data.facilityAccess()
                && !data.hosts().isEmpty();
    }

    public static boolean hasPhysicalScp079(MinecraftServer server) {
        return server != null && !data(server).hosts().isEmpty();
    }

    public static float discoveryProgress(MinecraftServer server) {
        return server == null ? 0.0F
                : (float) data(server).discoveryProgress();
    }

    public static boolean protocolExposed(MinecraftServer server) {
        return server != null && data(server).protocolExposed();
    }

    public static boolean auxiliaryPowerOnline(MinecraftServer server) {
        return server != null && data(server).auxiliaryPowerOnline();
    }

    public static boolean toggleAuxiliaryPower(ServerLevel level,
            ServerPlayer actor) {
        if (level == null) return false;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        boolean next = !data.auxiliaryPowerOnline();
        setAuxiliaryPower(level.getServer(), next);
        if (actor != null) {
            actor.displayClientMessage(Component.literal(next
                    ? "AUXILIARY FACILITY BUS: ONLINE"
                    : "AUXILIARY FACILITY BUS: ISOLATED"), true);
        }
        return next;
    }

    public static void setAuxiliaryPower(MinecraftServer server,
            boolean online) {
        if (server == null) return;
        Scp079FacilityAccessSavedData data = data(server);
        data.setAuxiliaryPowerOnline(online);
        if (!online) {
            resetCompromise(server, data);
        }
        synchronizeAuxiliaryUnits(server, online);
        if (online) wakeTeslaGates(server, data);
    }

    public static DiagnosticSnapshot performDiagnosticScan(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return DiagnosticSnapshot.EMPTY;
        Scp079FacilityAccessSavedData data = data(server);
        pruneLoadedPositions(server, data);

        if (data.auxiliaryPowerOnline() && !data.hosts().isEmpty()
                && !data.facilityAccess()) {
            data.setProtocolExposed(true);
            addDiscovery(server, data, SCAN_DISCOVERY);
        }

        int uncontained = countUncontainedScps(server);
        boolean override = server.getGameRules().getBoolean(
                ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean configured = server.getGameRules().getBoolean(
                ScpAdditionsModGameRules.TESLAGATEON);
        int totalGates = data.teslaGates().size();
        int activeGates = data.auxiliaryPowerOnline()
                && (configured || override) ? totalGates : 0;
        return new DiagnosticSnapshot(uncontained, activeGates, totalGates,
                override, data.doors().size());
    }

    public static void recordActivity(LevelAccessor level, Activity activity) {
        MinecraftServer server = server(level);
        if (server == null || activity == null) return;
        Scp079FacilityAccessSavedData data = data(server);
        if (!data.auxiliaryPowerOnline() || !data.protocolExposed()
                || data.hosts().isEmpty() || data.facilityAccess()) {
            return;
        }
        addDiscovery(server, data, activity.discovery);
    }

    public static void registerHost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.hosts().add(tracked(level, pos))) data.markChanged();
    }

    public static void unregisterHost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.hosts().remove(tracked(level, pos))) data.markChanged();
        if (data.hosts().isEmpty()) resetCompromise(level.getServer(), data);
    }

    public static void registerDoor(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.doors().add(tracked(level, pos))) data.markChanged();
    }

    public static void unregisterDoor(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.doors().remove(tracked(level, pos))) data.markChanged();
    }

    public static void registerTeslaGate(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.teslaGates().add(tracked(level, pos))) data.markChanged();
    }

    public static void unregisterTeslaGate(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.teslaGates().remove(tracked(level, pos))) data.markChanged();
    }

    public static void registerAuxiliaryUnit(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.auxiliaryUnits().add(tracked(level, pos))) data.markChanged();
    }

    public static void unregisterAuxiliaryUnit(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Scp079FacilityAccessSavedData data = data(level.getServer());
        if (data.auxiliaryUnits().remove(tracked(level, pos))) data.markChanged();
        if (data.auxiliaryUnits().isEmpty()) {
            setAuxiliaryPower(level.getServer(), false);
        }
    }

    public static void awardFirstInterference(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;
        Advancement advancement = player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation(
                        "scp_additions:scp_079control_achi"));
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements()
                .getOrStartProgress(advancement);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        MinecraftServer server = event.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        if (data.facilityAccess()
                && (!data.auxiliaryPowerOnline() || data.hosts().isEmpty())) {
            resetCompromise(server, data);
            return;
        }
        if (data.auxiliaryPowerOnline() && data.protocolExposed()
                && !data.hosts().isEmpty() && !data.facilityAccess()) {
            addDiscovery(server, data, PASSIVE_DISCOVERY_PER_SECOND);
        }
        boolean expectedRule = data.auxiliaryPowerOnline()
                && data.facilityAccess() && !data.hosts().isEmpty();
        if (server.getGameRules().getBoolean(
                ScpAdditionsModGameRules.SCP079CONTROLON) != expectedRule) {
            server.getGameRules().getRule(
                    ScpAdditionsModGameRules.SCP079CONTROLON)
                    .set(expectedRule, server);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        rescanChunk(level, chunk);
    }

    private static void addDiscovery(MinecraftServer server,
            Scp079FacilityAccessSavedData data, double amount) {
        data.setDiscoveryProgress(data.discoveryProgress() + amount);
        if (data.discoveryProgress() >= 100.0D && !data.facilityAccess()) {
            data.setDiscoveryProgress(100.0D);
            data.setFacilityAccess(true);
            server.getGameRules().getRule(
                    ScpAdditionsModGameRules.SCP079CONTROLON)
                    .set(true, server);
            Scp079ProcessingManager.onControlEnabled(server.overworld());
        }
    }

    private static void resetCompromise(MinecraftServer server,
            Scp079FacilityAccessSavedData data) {
        boolean wasActive = data.facilityAccess();
        data.setProtocolExposed(false);
        data.setDiscoveryProgress(0.0D);
        data.setFacilityAccess(false);
        server.getGameRules().getRule(
                ScpAdditionsModGameRules.SCP079CONTROLON)
                .set(false, server);
        if (wasActive) Scp079ProcessingManager.onControlDisabled(server.overworld());
    }

    private static int countUncontainedScps(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.isAlive() && (entity instanceof Scp173Entity
                        || entity instanceof Scp106Entity)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void rescanChunk(ServerLevel level, LevelChunk chunk) {
        Scp079FacilityAccessSavedData data = data(level.getServer());
        String dimension = level.dimension().location().toString();
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;
        removeChunkEntries(data.hosts(), dimension, chunkX, chunkZ);
        removeChunkEntries(data.doors(), dimension, chunkX, chunkZ);
        removeChunkEntries(data.teslaGates(), dimension, chunkX, chunkZ);
        removeChunkEntries(data.auxiliaryUnits(), dimension, chunkX, chunkZ);

        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length;
                sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()
                    || !section.maybeHas(Scp079FacilityAccessManager::isTracked)) {
                continue;
            }
            int baseY = level.getMinBuildHeight() + sectionIndex * 16;
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        BlockState state = section.getBlockState(
                                localX, localY, localZ);
                        if (!isTracked(state)) continue;
                        BlockPos pos = new BlockPos(
                                chunk.getPos().getMinBlockX() + localX,
                                baseY + localY,
                                chunk.getPos().getMinBlockZ() + localZ);
                        Scp079FacilityAccessSavedData.TrackedPosition tracked =
                                tracked(level, pos);
                        if (isScp079(state.getBlock())) data.hosts().add(tracked);
                        if (FacilityModule.isFacilityDoor(state)) data.doors().add(tracked);
                        if (TeslaGateStructure.isController(state)) data.teslaGates().add(tracked);
                        if (state.getBlock()
                                == ScpAdditionsModBlocks.SCP_079_AUXILIARY_POWER.get()) {
                            data.auxiliaryUnits().add(tracked);
                        }
                    }
                }
            }
        }
        data.markChanged();
    }

    private static boolean isTracked(BlockState state) {
        Block block = state.getBlock();
        return isScp079(block) || FacilityModule.isFacilityDoor(state)
                || TeslaGateStructure.isController(state)
                || block == ScpAdditionsModBlocks.SCP_079_AUXILIARY_POWER.get();
    }

    private static boolean isScp079(Block block) {
        return block == ScpAdditionsModBlocks.SCP_079ON.get()
                || block == ScpAdditionsModBlocks.SCP_079OFF.get();
    }

    private static void removeChunkEntries(
            Set<Scp079FacilityAccessSavedData.TrackedPosition> positions,
            String dimension, int chunkX, int chunkZ) {
        positions.removeIf(position -> position.dimension().equals(dimension)
                && position.chunkX() == chunkX
                && position.chunkZ() == chunkZ);
    }

    private static void pruneLoadedPositions(MinecraftServer server,
            Scp079FacilityAccessSavedData data) {
        boolean changed = prune(server, data.hosts(), state ->
                isScp079(state.getBlock()));
        changed |= prune(server, data.doors(), FacilityModule::isFacilityDoor);
        changed |= prune(server, data.teslaGates(),
                TeslaGateStructure::isController);
        changed |= prune(server, data.auxiliaryUnits(), state ->
                state.getBlock()
                        == ScpAdditionsModBlocks.SCP_079_AUXILIARY_POWER.get());
        if (changed) data.markChanged();
        if (data.hosts().isEmpty() && data.facilityAccess()) {
            resetCompromise(server, data);
        }
    }

    private static boolean prune(MinecraftServer server,
            Set<Scp079FacilityAccessSavedData.TrackedPosition> positions,
            java.util.function.Predicate<BlockState> predicate) {
        boolean changed = false;
        Iterator<Scp079FacilityAccessSavedData.TrackedPosition> iterator =
                positions.iterator();
        while (iterator.hasNext()) {
            Scp079FacilityAccessSavedData.TrackedPosition tracked = iterator.next();
            ServerLevel level = resolveLevel(server, tracked.dimension());
            BlockPos pos = BlockPos.of(tracked.packedPos());
            if (level != null && level.hasChunkAt(pos)
                    && !predicate.test(level.getBlockState(pos))) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    private static void synchronizeAuxiliaryUnits(MinecraftServer server,
            boolean online) {
        Scp079FacilityAccessSavedData data = data(server);
        List<Scp079FacilityAccessSavedData.TrackedPosition> stale =
                new ArrayList<>();
        for (Scp079FacilityAccessSavedData.TrackedPosition tracked
                : List.copyOf(data.auxiliaryUnits())) {
            ServerLevel level = resolveLevel(server, tracked.dimension());
            BlockPos pos = BlockPos.of(tracked.packedPos());
            if (level == null || !level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock()
                    != ScpAdditionsModBlocks.SCP_079_AUXILIARY_POWER.get()) {
                stale.add(tracked);
                continue;
            }
            if (state.hasProperty(Scp079AuxiliaryPowerBlock.POWERED)
                    && state.getValue(Scp079AuxiliaryPowerBlock.POWERED)
                    != online) {
                level.setBlock(pos, state.setValue(
                        Scp079AuxiliaryPowerBlock.POWERED, online),
                        Block.UPDATE_CLIENTS);
            }
        }
        if (data.auxiliaryUnits().removeAll(stale)) data.markChanged();
    }

    private static void wakeTeslaGates(MinecraftServer server,
            Scp079FacilityAccessSavedData data) {
        for (Scp079FacilityAccessSavedData.TrackedPosition tracked
                : List.copyOf(data.teslaGates())) {
            ServerLevel level = resolveLevel(server, tracked.dimension());
            BlockPos pos = BlockPos.of(tracked.packedPos());
            if (level == null || !level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (TeslaGateStructure.isController(state)) {
                level.scheduleTick(pos, state.getBlock(), 1);
            }
        }
    }

    private static Scp079FacilityAccessSavedData.TrackedPosition tracked(
            ServerLevel level, BlockPos pos) {
        return Scp079FacilityAccessSavedData.TrackedPosition.of(
                level.dimension().location().toString(), pos.asLong());
    }

    private static ServerLevel resolveLevel(MinecraftServer server,
            String dimension) {
        try {
            ResourceLocation id = new ResourceLocation(dimension);
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
            return server.getLevel(key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Scp079FacilityAccessSavedData data(MinecraftServer server) {
        return Scp079FacilityAccessSavedData.get(server);
    }

    private static MinecraftServer server(LevelAccessor level) {
        return level == null ? null : level.getServer();
    }

    public record DiagnosticSnapshot(int uncontainedScps,
            int activeTeslaGates, int registeredTeslaGates,
            boolean teslaOverride, int connectedDoors) {
        public static final DiagnosticSnapshot EMPTY =
                new DiagnosticSnapshot(0, 0, 0, false, 0);
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java", manager)

aux_block = r'''package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;

import java.util.Collections;
import java.util.List;

/** Placeholder generator block for the global auxiliary facility bus. */
public final class Scp079AuxiliaryPowerBlock extends HorizontalDirectionalBlock
        implements SimpleWaterloggedBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public Scp079AuxiliaryPowerBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                .strength(30.0F, 100.0F).noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,
            BlockState> builder) {
        builder.add(FACING, POWERED, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, Scp079FacilityAccessManager
                        .isAuxiliaryPowerOnline(context.getLevel()))
                .setValue(WATERLOGGED, context.getLevel().getFluidState(
                        context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER,
                    Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos,
                neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel server
                && player instanceof ServerPlayer serverPlayer) {
            Scp079FacilityAccessManager.toggleAuxiliaryPower(server,
                    serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && level instanceof ServerLevel server
                && oldState.getBlock() != this) {
            Scp079FacilityAccessManager.registerAuxiliaryUnit(server, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean moving) {
        if (!level.isClientSide && level instanceof ServerLevel server
                && newState.getBlock() != this) {
            Scp079FacilityAccessManager.unregisterAuxiliaryUnit(server, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Supplies the global diagnostic and Tesla security buses."));
        tooltip.add(Component.literal(
                "Isolation removes SCP-079 access but disables those systems."));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/block/Scp079AuxiliaryPowerBlock.java", aux_block)

terminal_block = r'''package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.Collections;
import java.util.List;

/** Global Foundation diagnostic terminal. It never grants access by redstone. */
public class SCP079SystemControlBlock extends Block
        implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    public SCP079SystemControlBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                .strength(30.0F, 100.0F).noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel
                && player instanceof ServerPlayer serverPlayer) {
            if (!Scp079FacilityAccessManager.isAuxiliaryPowerOnline(level)) {
                serverPlayer.displayClientMessage(Component.literal(
                        "DIAGNOSTIC BUS UNAVAILABLE: AUXILIARY POWER ISOLATED"),
                        true);
            } else {
                ScpEntityNetwork.openFacilityDiagnostics(serverPlayer,
                        Scp079FacilityAccessManager.performDiagnosticScan(
                                serverPlayer));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(
                "Displays a global Foundation facility diagnostic summary."));
        tooltip.add(Component.literal(
                "Requires the Auxiliary Facility Bus to be online."));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state,
            BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level,
            BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,
            BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos())
                        .getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER,
                    Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos,
                neighborPos);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/block/SCP079SystemControlBlock.java", terminal_block)

packet = r'''package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.FacilityDiagnosticsScreen;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager.DiagnosticSnapshot;

import java.util.function.Supplier;

/** Opens the read-only old-terminal facility diagnostic screen. */
public record FacilityDiagnosticsPacket(int uncontainedScps,
        int activeTeslaGates, int registeredTeslaGates,
        boolean teslaOverride, int connectedDoors) {

    public FacilityDiagnosticsPacket(DiagnosticSnapshot snapshot) {
        this(snapshot.uncontainedScps(), snapshot.activeTeslaGates(),
                snapshot.registeredTeslaGates(), snapshot.teslaOverride(),
                snapshot.connectedDoors());
    }

    public static void encode(FacilityDiagnosticsPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.max(0, message.uncontainedScps));
        buffer.writeVarInt(Math.max(0, message.activeTeslaGates));
        buffer.writeVarInt(Math.max(0, message.registeredTeslaGates));
        buffer.writeBoolean(message.teslaOverride);
        buffer.writeVarInt(Math.max(0, message.connectedDoors));
    }

    public static FacilityDiagnosticsPacket decode(FriendlyByteBuf buffer) {
        return new FacilityDiagnosticsPacket(buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarInt());
    }

    public static void handle(FacilityDiagnosticsPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FacilityDiagnosticsScreen.open(message)));
        context.setPacketHandled(true);
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/network/FacilityDiagnosticsPacket.java", packet)

screen = r'''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.network.FacilityDiagnosticsPacket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Read-only black-and-green Foundation diagnostic terminal. */
public final class FacilityDiagnosticsScreen extends Screen {
    private static final int GREEN = 0xFF62E17A;
    private static final int DIM_GREEN = 0xFF3F9850;
    private static final int BLACK = 0xFF020503;
    private static final int PANEL = 0xFF061009;
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FacilityDiagnosticsPacket data;

    private FacilityDiagnosticsScreen(FacilityDiagnosticsPacket data) {
        super(Component.literal("Foundation Facility Diagnostics"));
        this.data = data;
    }

    public static void open(FacilityDiagnosticsPacket data) {
        Minecraft.getInstance().setScreen(new FacilityDiagnosticsScreen(data));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BLACK);
        int panelWidth = Math.min(360, width - 24);
        int panelHeight = Math.min(230, height - 24);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        border(graphics, x, y, panelWidth, panelHeight, DIM_GREEN);

        int tx = x + 12;
        int ty = y + 10;
        line(graphics, "SCP FOUNDATION // FACILITY SYSTEMS DIAGNOSTIC", tx, ty, GREEN);
        line(graphics, "SESSION CLASS: INTERNAL OPERATIONS", tx, ty + 12, DIM_GREEN);
        line(graphics, "REPORT TIME: " + LocalDateTime.now().format(TIME), tx, ty + 24, DIM_GREEN);
        rule(graphics, tx, ty + 38, panelWidth - 24);

        line(graphics, "[ ANOMALOUS CONTAINMENT TELEMETRY ]", tx, ty + 48, GREEN);
        metric(graphics, "UNCONTAINED SCP SIGNATURES", data.uncontainedScps(), tx, ty + 62,
                panelWidth - 24);
        String condition = data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        metric(graphics, "CONTAINMENT ASSURANCE", condition, tx, ty + 74,
                panelWidth - 24);

        line(graphics, "[ SECURITY INFRASTRUCTURE BUS ]", tx, ty + 94, GREEN);
        metric(graphics, "TESLA NODES ACTIVE", data.activeTeslaGates(), tx, ty + 108,
                panelWidth - 24);
        metric(graphics, "TESLA NODES REGISTERED", data.registeredTeslaGates(), tx, ty + 120,
                panelWidth - 24);
        metric(graphics, "TESLA MANUAL OVERRIDE",
                data.teslaOverride() ? "ENGAGED" : "CLEAR", tx, ty + 132,
                panelWidth - 24);
        metric(graphics, "REMOTE DOOR ENDPOINTS", data.connectedDoors(), tx, ty + 144,
                panelWidth - 24);

        rule(graphics, tx, ty + 164, panelWidth - 24);
        line(graphics, "AUXILIARY DIAGNOSTIC BUS: ONLINE", tx, ty + 174, GREEN);
        line(graphics, "DATA SCOPE: REGISTERED FOUNDATION ASSETS", tx, ty + 186, DIM_GREEN);
        line(graphics, "PRESS ESC TO TERMINATE SESSION", tx, ty + 204, DIM_GREEN);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void metric(GuiGraphics graphics, String label, Object value,
            int x, int y, int width) {
        String left = label + " ";
        String right = String.valueOf(value);
        int dots = Math.max(2, (width - font.width(left) - font.width(right))
                / Math.max(1, font.width(".")));
        line(graphics, left + ".".repeat(dots) + right, x, y, GREEN);
    }

    private void line(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    private void rule(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, DIM_GREEN);
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
'''
write("src/main/java/net/mcreator/scpadditions/client/FacilityDiagnosticsScreen.java", screen)

# Registry additions.
replace("src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModBlocks.java",
        "import net.mcreator.scpadditions.block.SCP079SystemControlBlock;",
        "import net.mcreator.scpadditions.block.SCP079SystemControlBlock;\nimport net.mcreator.scpadditions.block.Scp079AuxiliaryPowerBlock;")
replace("src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModBlocks.java",
        "public static final RegistryObject<Block> SCP_079_SYSTEM_CONTROL = REGISTRY.register(\"scp_079_system_control\", () -> new SCP079SystemControlBlock());",
        "public static final RegistryObject<Block> SCP_079_SYSTEM_CONTROL = REGISTRY.register(\"scp_079_system_control\", () -> new SCP079SystemControlBlock());\n\tpublic static final RegistryObject<Block> SCP_079_AUXILIARY_POWER = REGISTRY.register(\"scp_079_auxiliary_power\", Scp079AuxiliaryPowerBlock::new);")
replace("src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModItems.java",
        "public static final RegistryObject<Item> SCP_079_SYSTEM_CONTROL = block(ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL);",
        "public static final RegistryObject<Item> SCP_079_SYSTEM_CONTROL = block(ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL);\n\tpublic static final RegistryObject<Item> SCP_079_AUXILIARY_POWER = block(ScpAdditionsModBlocks.SCP_079_AUXILIARY_POWER);")

# Add the placeholder block to the facility creative section.
replace("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
        "addExternalCreativeItem(functional, ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL.get().asItem());\n        addExternalCreativeItem(functional, ScpAdditionsModBlocks.SCP_079CONTROLOFF.get().asItem());",
        "addExternalCreativeItem(functional, ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL.get().asItem());\n        addExternalCreativeItem(functional, ScpAdditionsModBlocks.SCP_079_AUXILIARY_POWER.get().asItem());\n        addExternalCreativeItem(functional, ScpAdditionsModBlocks.SCP_079CONTROLOFF.get().asItem());")

# Register and count animated doors, and award exposure for player-driven openings.
replace("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
        "super.onPlace(state, level, pos, oldState, moving);\n            int delay = stage == DoorStage.OPENING || stage == DoorStage.CLOSING",
        "super.onPlace(state, level, pos, oldState, moving);\n            if (!level.isClientSide && level instanceof ServerLevel server\n                    && !(oldState.getBlock() instanceof AnimatedDoorBlock)) {\n                Scp079FacilityAccessManager.registerDoor(server, pos);\n            }\n            int delay = stage == DoorStage.OPENING || stage == DoorStage.CLOSING")
replace("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
        "if (stage == DoorStage.CLOSED) startOpening(server, pos, state, family);\n                else startClosing(server, pos, state, family);",
        "if (stage == DoorStage.CLOSED) {\n                    startOpening(server, pos, state, family);\n                    Scp079FacilityAccessManager.recordActivity(server,\n                            Scp079FacilityAccessManager.Activity.DOOR_OPENED);\n                } else startClosing(server, pos, state, family);")
replace("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
        "        @Override\n        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {\n            return Collections.singletonList(new ItemStack(family().closed().get()));\n        }",
        "        @Override\n        public void onRemove(BlockState state, Level level, BlockPos pos,\n                BlockState newState, boolean moving) {\n            if (!level.isClientSide && level instanceof ServerLevel server\n                    && !(newState.getBlock() instanceof AnimatedDoorBlock)) {\n                Scp079FacilityAccessManager.unregisterDoor(server, pos);\n            }\n            super.onRemove(state, level, pos, newState, moving);\n        }\n\n        @Override\n        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {\n            return Collections.singletonList(new ItemStack(family().closed().get()));\n        }", 1)

# Player button use accelerates discovery only on an opening command.
replace("src/main/java/net/mcreator/scpadditions/facility/DoorButtonIndependentInteractionEvents.java",
        "activateButton(level, pos);",
        "activateButton(level, pos, true);", 1)
replace("src/main/java/net/mcreator/scpadditions/facility/DoorButtonIndependentInteractionEvents.java",
        "public static boolean activateButton(ServerLevel level, BlockPos pos) {\n        if (level == null || pos == null) {",
        "public static boolean activateButton(ServerLevel level, BlockPos pos) {\n        return activateButton(level, pos, false);\n    }\n\n    public static boolean activateButton(ServerLevel level, BlockPos pos,\n            boolean playerInitiated) {\n        if (level == null || pos == null) {")
replace("src/main/java/net/mcreator/scpadditions/facility/DoorButtonIndependentInteractionEvents.java",
        "        ScpAdditionsMod.queueServerWork(TRANSITION_TICKS, () -> {\n            completeTransition(level, pos, endpoint);",
        "        if (playerInitiated && phase == Phase.CLOSED) {\n            Scp079FacilityAccessManager.recordActivity(level,\n                    Scp079FacilityAccessManager.Activity.DOOR_OPENED);\n        }\n\n        ScpAdditionsMod.queueServerWork(TRANSITION_TICKS, () -> {\n            completeTransition(level, pos, endpoint);")
replace("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java",
        "DoorButtonIndependentInteractionEvents.activateButton(server, pos);",
        "DoorButtonIndependentInteractionEvents.activateButton(server, pos, true);")

# Track physical SCP-079 hosts across visual swaps.
for filename in ["Scp079onBlock.java", "Scp079offBlock.java"]:
    path = f"src/main/java/net/mcreator/scpadditions/block/{filename}"
    replace(path,
            "import net.mcreator.scpadditions.procedures.",
            "import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;\nimport net.mcreator.scpadditions.procedures.", 1)
    replace(path,
            "\t\tsuper.onPlace(blockstate, world, pos, oldState, moving);\n\t\tworld.scheduleTick(pos, this, 20);",
            "\t\tsuper.onPlace(blockstate, world, pos, oldState, moving);\n\t\tif (!world.isClientSide && world instanceof ServerLevel server) {\n\t\t\tScp079FacilityAccessManager.registerHost(server, pos);\n\t\t}\n\t\tworld.scheduleTick(pos, this, 20);")
    insertion = "\n\t@Override\n\tpublic void onRemove(BlockState state, Level world, BlockPos pos,\n\t\t\tBlockState newState, boolean moving) {\n\t\tif (!world.isClientSide && world instanceof ServerLevel server\n\t\t\t\t&& newState.getBlock() != ScpAdditionsModBlocks.SCP_079ON.get()\n\t\t\t\t&& newState.getBlock() != ScpAdditionsModBlocks.SCP_079OFF.get()) {\n\t\t\tScp079FacilityAccessManager.unregisterHost(server, pos);\n\t\t}\n\t\tsuper.onRemove(state, world, pos, newState, moving);\n\t}\n"
    replace(path, "\n\t@Override\n\tpublic void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {",
            insertion + "\n\t@Override\n\tpublic void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {")

# Tesla grid depends on auxiliary power, records crossings and configuration changes.
replace("src/main/java/net/mcreator/scpadditions/block/TeslaGateBlock.java",
        "import net.mcreator.scpadditions.procedures.TeslaGateUpdateTickProcedure;",
        "import net.mcreator.scpadditions.procedures.TeslaGateUpdateTickProcedure;\nimport net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;")
replace("src/main/java/net/mcreator/scpadditions/block/TeslaGateBlock.java",
        "if (!level.isClientSide()) {\n            boolean activationQueued",
        "if (!level.isClientSide()) {\n            if (level instanceof ServerLevel server) {\n                Scp079FacilityAccessManager.registerTeslaGate(server, pos);\n            }\n            boolean activationQueued")
replace("src/main/java/net/mcreator/scpadditions/block/TeslaGateBlock.java",
        "private static boolean isEnabled(Level level) {\n        return level.getGameRules().getBoolean(",
        "private static boolean isEnabled(Level level) {\n        return Scp079FacilityAccessManager.isAuxiliaryPowerOnline(level)\n                && (level.getGameRules().getBoolean(")
replace("src/main/java/net/mcreator/scpadditions/block/TeslaGateBlock.java",
        "ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);\n    }",
        "ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE));\n    }")
replace("src/main/java/net/mcreator/scpadditions/block/TeslaGateStructureEvents.java",
        "import net.mcreator.scpadditions.facility.FacilityStructureBreakGuard;",
        "import net.mcreator.scpadditions.facility.FacilityStructureBreakGuard;\nimport net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;")
replace("src/main/java/net/mcreator/scpadditions/block/TeslaGateStructureEvents.java",
        "FacilityStructureBreakGuard.clear(level, controllerPos);\n            TeslaGateStructure.destroyFromCollision",
        "FacilityStructureBreakGuard.clear(level, controllerPos);\n            if (level instanceof net.minecraft.server.level.ServerLevel server) {\n                Scp079FacilityAccessManager.unregisterTeslaGate(server, controllerPos);\n            }\n            TeslaGateStructure.destroyFromCollision")
replace("src/main/java/net/mcreator/scpadditions/block/TeslaGateStructureEvents.java",
        "if (TeslaGateStructure.isController(state)) {\n            FacilityStructureBreakGuard.clear(level, event.getPos());",
        "if (TeslaGateStructure.isController(state)) {\n            if (level instanceof net.minecraft.server.level.ServerLevel server) {\n                Scp079FacilityAccessManager.unregisterTeslaGate(server, event.getPos());\n            }\n            FacilityStructureBreakGuard.clear(level, event.getPos());")
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java",
        "import net.mcreator.scpadditions.facility.Scp079TeslaSuppression;",
        "import net.mcreator.scpadditions.facility.Scp079TeslaSuppression;\nimport net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;\nimport net.minecraft.server.level.ServerPlayer;")
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java",
        "BlockPos gatePos = BlockPos.containing(x, y, z);\n        if (FacilityStructureBreakGuard.isBeingMined",
        "BlockPos gatePos = BlockPos.containing(x, y, z);\n        if (!Scp079FacilityAccessManager.isAuxiliaryPowerOnline(world)) {\n            return false;\n        }\n        if (FacilityStructureBreakGuard.isBeingMined")
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaGateUpdateTickProcedure.java",
        "        if (world instanceof Level level && !level.isClientSide()) {\n            level.playSound",
        "        if (world instanceof ServerLevel server\n                && occupants.stream().anyMatch(ServerPlayer.class::isInstance)) {\n            Scp079FacilityAccessManager.recordActivity(server,\n                    Scp079FacilityAccessManager.Activity.TESLA_TRAVERSAL);\n        }\n        if (world instanceof Level level && !level.isClientSide()) {\n            level.playSound")
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalController.java",
        "import net.mcreator.scpadditions.integration.PlayerItemAccess;",
        "import net.mcreator.scpadditions.integration.PlayerItemAccess;\nimport net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;\nimport net.minecraft.network.chat.Component;")
for method in ["enableTeslaGates", "disableTeslaGates"]:
    anchor = f"\tpublic static void {method}(LevelAccessor world, double x, double y, double z, Player player) {{\n"
    replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalController.java",
            anchor,
            anchor + "\t\tif (!requireAuxiliaryPower(world, player)) return;\n")
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalController.java",
        "\tpublic static void setManualOverride(LevelAccessor world, double x, double y, double z, Player player, boolean enabled) {\n",
        "\tpublic static void setManualOverride(LevelAccessor world, double x, double y, double z, Player player, boolean enabled) {\n\t\tif (!requireAuxiliaryPower(world, player)) return;\n")
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalController.java",
        "\t\tworld.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(true, server(world));\n\t}",
        "\t\tworld.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(true, server(world));\n\t\tScp079FacilityAccessManager.recordActivity(world,\n\t\t\t\tScp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);\n\t}", 1)
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalController.java",
        "\t\tworld.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(false, server(world));\n\t}",
        "\t\tworld.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(false, server(world));\n\t\tScp079FacilityAccessManager.recordActivity(world,\n\t\t\t\tScp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);\n\t}", 1)
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalController.java",
        "\t\tworld.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(enabled, server(world));\n\t}",
        "\t\tworld.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(enabled, server(world));\n\t\tScp079FacilityAccessManager.recordActivity(world,\n\t\t\t\tScp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);\n\t}", 1)
replace("src/main/java/net/mcreator/scpadditions/procedures/TeslaTerminalController.java",
        "\tprivate static MinecraftServer server(LevelAccessor world) {",
        "\tprivate static boolean requireAuxiliaryPower(LevelAccessor world, Player player) {\n\t\tif (Scp079FacilityAccessManager.isAuxiliaryPowerOnline(world)) return true;\n\t\tif (player != null) {\n\t\t\tplayer.displayClientMessage(Component.literal(\n\t\t\t\t\t\"TESLA GRID UNAVAILABLE: AUXILIARY POWER ISOLATED\"), true);\n\t\t}\n\t\treturn false;\n\t}\n\n\tprivate static MinecraftServer server(LevelAccessor world) {")

# Replace gamerule checks with the authoritative physical-access manager.
for path in [
    "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityThreatEvents.java",
    "src/main/java/net/mcreator/scpadditions/facility/Scp079SustainedDoorLocks.java",
    "src/main/java/net/mcreator/scpadditions/scp012/Scp012InfluenceEvents.java",
    "src/main/java/net/mcreator/scpadditions/procedures/Scp079controlonUpdateTickProcedure.java",
    "src/main/java/net/mcreator/scpadditions/procedures/Scp079controloffUpdateTickProcedure.java",
]:
    text = (ROOT / path).read_text(encoding="utf-8")
    if "Scp079FacilityAccessManager" not in text:
        package_end = text.index("\n\n", text.index("package "))
        text = text[:package_end] + "\n\nimport net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;" + text[package_end:]
    text = text.replace("level.getGameRules().getBoolean(\n                ScpAdditionsModGameRules.SCP079CONTROLON)",
                        "Scp079FacilityAccessManager.hasFacilityAccess(level)")
    text = text.replace("world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.SCP079CONTROLON)",
                        "Scp079FacilityAccessManager.hasFacilityAccess(world)")
    (ROOT / path).write_text(text, encoding="utf-8")

# Processing AP only exists after discovery completes, and debug sync includes discovery.
replace("src/main/java/net/mcreator/scpadditions/facility/Scp079ProcessingManager.java",
        "return level != null && level.getGameRules().getBoolean(\n                ScpAdditionsModGameRules.SCP079CONTROLON);",
        "return level != null\n                && Scp079FacilityAccessManager.hasFacilityAccess(level);")
replace("src/main/java/net/mcreator/scpadditions/facility/Scp079ProcessingManager.java",
        "int roundedPower = energyVisible\n                ? Math.round(getPower(player.serverLevel())) : 0;",
        "int roundedPower = energyVisible\n                ? Math.round(getPower(player.serverLevel())) : 0;\n        int roundedDiscovery = energyVisible\n                ? Math.round(Scp079FacilityAccessManager.discoveryProgress(\n                        player.getServer())) : 0;\n        boolean auxiliaryOnline = energyVisible\n                && Scp079FacilityAccessManager.auxiliaryPowerOnline(\n                        player.getServer());\n        boolean hostPresent = energyVisible\n                && Scp079FacilityAccessManager.hasPhysicalScp079(\n                        player.getServer());\n        boolean protocolExposed = energyVisible\n                && Scp079FacilityAccessManager.protocolExposed(\n                        player.getServer());")
replace("src/main/java/net/mcreator/scpadditions/facility/Scp079ProcessingManager.java",
        "ClientSnapshot next = new ClientSnapshot(energyVisible, decisionVisible,\n                active, roundedPower, spawnTimersVisible, roamers,",
        "ClientSnapshot next = new ClientSnapshot(energyVisible, decisionVisible,\n                active, roundedPower, roundedDiscovery, auxiliaryOnline,\n                hostPresent, protocolExposed, spawnTimersVisible, roamers,")
replace("src/main/java/net/mcreator/scpadditions/facility/Scp079ProcessingManager.java",
        "ScpEntityNetwork.syncDebugState(player, energyVisible, active,\n                    roundedPower, spawnTimersVisible, roamers);",
        "ScpEntityNetwork.syncDebugState(player, energyVisible, active,\n                    roundedPower, roundedDiscovery, auxiliaryOnline,\n                    hostPresent, protocolExposed, spawnTimersVisible, roamers);")
replace("src/main/java/net/mcreator/scpadditions/facility/Scp079ProcessingManager.java",
        "private record ClientSnapshot(boolean energyVisible,\n            boolean decisionVisible, boolean active, int roundedPower,\n            boolean spawnTimersVisible, List<RoamerDebugSnapshot> roamers,",
        "private record ClientSnapshot(boolean energyVisible,\n            boolean decisionVisible, boolean active, int roundedPower,\n            int roundedDiscovery, boolean auxiliaryOnline,\n            boolean hostPresent, boolean protocolExposed,\n            boolean spawnTimersVisible, List<RoamerDebugSnapshot> roamers,")
replace("src/main/java/net/mcreator/scpadditions/facility/Scp079ProcessingManager.java",
        "&& roundedPower == other.roundedPower\n                    && spawnTimersVisible == other.spawnTimersVisible",
        "&& roundedPower == other.roundedPower\n                    && roundedDiscovery == other.roundedDiscovery\n                    && auxiliaryOnline == other.auxiliaryOnline\n                    && hostPresent == other.hostPresent\n                    && protocolExposed == other.protocolExposed\n                    && spawnTimersVisible == other.spawnTimersVisible")

# Packet/client HUD sync.
replace("src/main/java/net/mcreator/scpadditions/network/Scp079EnergyPacket.java",
        "private final float energy;\n    private final boolean spawnTimersVisible;",
        "private final float energy;\n    private final float discovery;\n    private final boolean auxiliaryOnline;\n    private final boolean hostPresent;\n    private final boolean protocolExposed;\n    private final boolean spawnTimersVisible;")
replace("src/main/java/net/mcreator/scpadditions/network/Scp079EnergyPacket.java",
        "public Scp079EnergyPacket(boolean energyVisible, boolean active, float energy,\n            boolean spawnTimersVisible, List<RoamerEntry> roamers) {",
        "public Scp079EnergyPacket(boolean energyVisible, boolean active, float energy,\n            float discovery, boolean auxiliaryOnline, boolean hostPresent,\n            boolean protocolExposed, boolean spawnTimersVisible,\n            List<RoamerEntry> roamers) {")
replace("src/main/java/net/mcreator/scpadditions/network/Scp079EnergyPacket.java",
        "this.energy = Mth.clamp(energy, 0.0F, 100.0F);\n        this.spawnTimersVisible",
        "this.energy = Mth.clamp(energy, 0.0F, 100.0F);\n        this.discovery = Mth.clamp(discovery, 0.0F, 100.0F);\n        this.auxiliaryOnline = auxiliaryOnline;\n        this.hostPresent = hostPresent;\n        this.protocolExposed = protocolExposed;\n        this.spawnTimersVisible")
replace("src/main/java/net/mcreator/scpadditions/network/Scp079EnergyPacket.java",
        "buffer.writeFloat(message.energy);\n        buffer.writeBoolean(message.spawnTimersVisible);",
        "buffer.writeFloat(message.energy);\n        buffer.writeFloat(message.discovery);\n        buffer.writeBoolean(message.auxiliaryOnline);\n        buffer.writeBoolean(message.hostPresent);\n        buffer.writeBoolean(message.protocolExposed);\n        buffer.writeBoolean(message.spawnTimersVisible);")
replace("src/main/java/net/mcreator/scpadditions/network/Scp079EnergyPacket.java",
        "float energy = buffer.readFloat();\n        boolean spawnTimersVisible = buffer.readBoolean();",
        "float energy = buffer.readFloat();\n        float discovery = buffer.readFloat();\n        boolean auxiliaryOnline = buffer.readBoolean();\n        boolean hostPresent = buffer.readBoolean();\n        boolean protocolExposed = buffer.readBoolean();\n        boolean spawnTimersVisible = buffer.readBoolean();")
replace("src/main/java/net/mcreator/scpadditions/network/Scp079EnergyPacket.java",
        "return new Scp079EnergyPacket(energyVisible, active, energy,\n                spawnTimersVisible, roamers);",
        "return new Scp079EnergyPacket(energyVisible, active, energy, discovery,\n                auxiliaryOnline, hostPresent, protocolExposed,\n                spawnTimersVisible, roamers);")
replace("src/main/java/net/mcreator/scpadditions/network/Scp079EnergyPacket.java",
        "message.energyVisible, message.active, message.energy,\n                        message.spawnTimersVisible, message.roamers",
        "message.energyVisible, message.active, message.energy,\n                        message.discovery, message.auxiliaryOnline,\n                        message.hostPresent, message.protocolExposed,\n                        message.spawnTimersVisible, message.roamers")

replace("src/main/java/net/mcreator/scpadditions/client/Scp079EnergyClientState.java",
        "private static float energy;\n    private static boolean spawnTimersVisible;",
        "private static float energy;\n    private static float discovery;\n    private static boolean auxiliaryOnline;\n    private static boolean hostPresent;\n    private static boolean protocolExposed;\n    private static boolean spawnTimersVisible;")
replace("src/main/java/net/mcreator/scpadditions/client/Scp079EnergyClientState.java",
        "public static void update(boolean shouldShowEnergy, boolean systemActive,\n            float currentEnergy, boolean shouldShowSpawnTimers,",
        "public static void update(boolean shouldShowEnergy, boolean systemActive,\n            float currentEnergy, float discoveryProgress,\n            boolean isAuxiliaryOnline, boolean hasHost,\n            boolean isProtocolExposed, boolean shouldShowSpawnTimers,")
replace("src/main/java/net/mcreator/scpadditions/client/Scp079EnergyClientState.java",
        "energy = Math.max(0.0F, Math.min(100.0F, currentEnergy));\n        spawnTimersVisible",
        "energy = Math.max(0.0F, Math.min(100.0F, currentEnergy));\n        discovery = Math.max(0.0F, Math.min(100.0F, discoveryProgress));\n        auxiliaryOnline = isAuxiliaryOnline;\n        hostPresent = hasHost;\n        protocolExposed = isProtocolExposed;\n        spawnTimersVisible")
replace("src/main/java/net/mcreator/scpadditions/client/Scp079EnergyClientState.java",
        "public static float energy() {\n        return energy;\n    }",
        "public static float energy() {\n        return energy;\n    }\n\n    public static float discovery() { return discovery; }\n    public static boolean auxiliaryOnline() { return auxiliaryOnline; }\n    public static boolean hostPresent() { return hostPresent; }\n    public static boolean protocolExposed() { return protocolExposed; }")
replace("src/main/java/net/mcreator/scpadditions/client/Scp079EnergyClientState.java",
        "energy = 0.0F;\n        spawnTimersVisible",
        "energy = 0.0F;\n        discovery = 0.0F;\n        auxiliaryOnline = false;\n        hostPresent = false;\n        protocolExposed = false;\n        spawnTimersVisible")

replace("src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
        "ScpAdditionsMod.addNetworkMessage(Scp079DecisionPacket.class,",
        "ScpAdditionsMod.addNetworkMessage(FacilityDiagnosticsPacket.class,\n                FacilityDiagnosticsPacket::encode,\n                FacilityDiagnosticsPacket::decode,\n                FacilityDiagnosticsPacket::handle);\n        ScpAdditionsMod.addNetworkMessage(Scp079DecisionPacket.class,")
replace("src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
        "public static void syncDebugState(ServerPlayer player,\n            boolean energyVisible, boolean active, float energy,\n            boolean spawnTimersVisible,",
        "public static void syncDebugState(ServerPlayer player,\n            boolean energyVisible, boolean active, float energy,\n            float discovery, boolean auxiliaryOnline, boolean hostPresent,\n            boolean protocolExposed, boolean spawnTimersVisible,")
replace("src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
        "new Scp079EnergyPacket(energyVisible, active, energy,\n                        spawnTimersVisible, entries));",
        "new Scp079EnergyPacket(energyVisible, active, energy, discovery,\n                        auxiliaryOnline, hostPresent, protocolExposed,\n                        spawnTimersVisible, entries));")
replace("src/main/java/net/mcreator/scpadditions/network/ScpEntityNetwork.java",
        "public static void syncScp079Decisions(ServerPlayer player,",
        "public static void openFacilityDiagnostics(ServerPlayer player,\n            net.mcreator.scpadditions.facility.Scp079FacilityAccessManager\n                    .DiagnosticSnapshot snapshot) {\n        if (player == null || snapshot == null) return;\n        ScpAdditionsMod.PACKET_HANDLER.send(\n                PacketDistributor.PLAYER.with(() -> player),\n                new FacilityDiagnosticsPacket(snapshot));\n    }\n\n    public static void syncScp079Decisions(ServerPlayer player,")

# Rewrite compact energy panel with an additional discovery bar.
overlay_path = ROOT / "src/main/java/net/mcreator/scpadditions/vitals/client/Scp079EnergyOverlay.java"
overlay = overlay_path.read_text(encoding="utf-8")
overlay = overlay.replace("private static final int ENERGY_WIDTH = 94;\n    private static final int ENERGY_HEIGHT = 32;",
                          "private static final int ENERGY_WIDTH = 126;\n    private static final int ENERGY_HEIGHT = 50;")
start = overlay.index("    private static void renderEnergy(")
end = overlay.index("\n    private static void renderDecisionFeed", start)
new_render = r'''    private static void renderEnergy(GuiGraphics graphics,
            Minecraft minecraft, int screenWidth, int y) {
        boolean active = Scp079EnergyClientState.active();
        float energy = Math.max(0.0F,
                Math.min(100.0F, Scp079EnergyClientState.energy()));
        float discovery = Math.max(0.0F,
                Math.min(100.0F, Scp079EnergyClientState.discovery()));
        int x = screenWidth - ENERGY_WIDTH - MARGIN;

        graphics.fill(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT,
                active ? PANEL : PANEL_INACTIVE);
        border(graphics, x, y, ENERGY_WIDTH, ENERGY_HEIGHT, WHITE);
        ItemStack icon = new ItemStack(ScpAdditionsModItems.SCP_079ON.get());
        graphics.renderItem(icon, x + 6, y + 6);

        draw(graphics, minecraft.font, active ? "AP ONLINE" : "AP OFFLINE",
                x + 28, y + 5, active ? WHITE : MUTED);
        drawBar(graphics, minecraft, "PROCESSING", energy,
                x + 28, y + 16, active ? WHITE : MUTED);

        String state = !Scp079EnergyClientState.auxiliaryOnline()
                ? "AUX ISOLATED"
                : !Scp079EnergyClientState.hostPresent()
                ? "NO PHYSICAL HOST"
                : !Scp079EnergyClientState.protocolExposed()
                ? "PROTOCOL HIDDEN"
                : active ? "ACCESS ACQUIRED" : "DISCOVERY";
        draw(graphics, minecraft.font, state, x + 7, y + 31,
                active ? WHITE : MUTED);
        drawBar(graphics, minecraft, "", discovery,
                x + 7, y + 41, active ? WHITE : MUTED);
    }

    private static void drawBar(GuiGraphics graphics, Minecraft minecraft,
            String label, float value, int x, int y, int fillColor) {
        if (!label.isBlank()) {
            draw(graphics, minecraft.font, label, x, y, WHITE);
        }
        String number = Integer.toString(Math.round(value));
        int numberWidth = minecraft.font.width(styled(number));
        int barX = label.isBlank() ? x : x + 54;
        int barWidth = ENERGY_WIDTH - (barX - (minecraft.screen == null
                ? Minecraft.getInstance().getWindow().getGuiScaledWidth()
                - ENERGY_WIDTH - MARGIN : 0)) - 8 - numberWidth;
        if (label.isBlank()) barWidth = ENERGY_WIDTH - 28;
        barWidth = Math.max(16, Math.min(62, barWidth));
        graphics.fill(barX, y + 2, barX + barWidth, y + 5, TRACK);
        int fill = Math.round(barWidth * value / 100.0F);
        if (fill > 0) graphics.fill(barX, y + 2, barX + fill, y + 5, fillColor);
        draw(graphics, minecraft.font, number, barX + barWidth + 4,
                y - 1, WHITE);
    }
'''
overlay = overlay[:start] + new_render + overlay[end:]
overlay_path.write_text(overlay, encoding="utf-8")

# First actual hostile door decision grants the hidden advancement.
replace("src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityThreatEvents.java",
        "Scp079DecisionLog.record(level, type,\n                Scp079DecisionLog.DecisionOutcome.EXECUTED,\n                action.door().pos(), totalCost, context);\n        return true;",
        "Scp079DecisionLog.record(level, type,\n                Scp079DecisionLog.DecisionOutcome.EXECUTED,\n                action.door().pos(), totalCost, context);\n        if (player != null) {\n            Scp079FacilityAccessManager.awardFirstInterference(player);\n        }\n        return true;")
replace("src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityThreatEvents.java",
        "\"closed SCP-173 ahead away from \"\n                        + player.getGameProfile().getName()\n                        + \" · lock upkeep 1.5 AP/s\");\n        return true;",
        "\"closed SCP-173 ahead away from \"\n                        + player.getGameProfile().getName()\n                        + \" · lock upkeep 1.5 AP/s\");\n        Scp079FacilityAccessManager.awardFirstInterference(player);\n        return true;")

# Legacy redstone procedure no longer grants access or its advancement.
write("src/main/java/net/mcreator/scpadditions/procedures/SCP079SystemControlPProcedure.java", r'''package net.mcreator.scpadditions.procedures;

import net.minecraft.world.level.LevelAccessor;

/** Deprecated compatibility stub: facility access is learned through diagnostics. */
public final class SCP079SystemControlPProcedure {
    private SCP079SystemControlPProcedure() {
    }

    public static void execute(LevelAccessor world, double x, double y,
            double z) {
        // Intentionally empty. Redstone can no longer grant SCP-079 access.
    }
}
''')

# Advancement is hidden until the first real decision affects a player.
adv_path = ROOT / "src/main/resources/data/scp_additions/advancements/scp_079control_achi.json"
adv = json.loads(adv_path.read_text(encoding="utf-8"))
adv["display"]["hidden"] = True
adv_path.write_text(json.dumps(adv, indent=2) + "\n", encoding="utf-8")

# Placeholder assets.
write("src/main/resources/assets/scp_additions/blockstates/scp_079_auxiliary_power.json", '''{
  "variants": {
    "facing=north,powered=false,waterlogged=false": {"model":"scp_additions:block/scp_079_auxiliary_power_off"},
    "facing=east,powered=false,waterlogged=false": {"model":"scp_additions:block/scp_079_auxiliary_power_off","y":90},
    "facing=south,powered=false,waterlogged=false": {"model":"scp_additions:block/scp_079_auxiliary_power_off","y":180},
    "facing=west,powered=false,waterlogged=false": {"model":"scp_additions:block/scp_079_auxiliary_power_off","y":270},
    "facing=north,powered=true,waterlogged=false": {"model":"scp_additions:block/scp_079_auxiliary_power_on"},
    "facing=east,powered=true,waterlogged=false": {"model":"scp_additions:block/scp_079_auxiliary_power_on","y":90},
    "facing=south,powered=true,waterlogged=false": {"model":"scp_additions:block/scp_079_auxiliary_power_on","y":180},
    "facing=west,powered=true,waterlogged=false": {"model":"scp_additions:block/scp_079_auxiliary_power_on","y":270},
    "facing=north,powered=false,waterlogged=true": {"model":"scp_additions:block/scp_079_auxiliary_power_off"},
    "facing=east,powered=false,waterlogged=true": {"model":"scp_additions:block/scp_079_auxiliary_power_off","y":90},
    "facing=south,powered=false,waterlogged=true": {"model":"scp_additions:block/scp_079_auxiliary_power_off","y":180},
    "facing=west,powered=false,waterlogged=true": {"model":"scp_additions:block/scp_079_auxiliary_power_off","y":270},
    "facing=north,powered=true,waterlogged=true": {"model":"scp_additions:block/scp_079_auxiliary_power_on"},
    "facing=east,powered=true,waterlogged=true": {"model":"scp_additions:block/scp_079_auxiliary_power_on","y":90},
    "facing=south,powered=true,waterlogged=true": {"model":"scp_additions:block/scp_079_auxiliary_power_on","y":180},
    "facing=west,powered=true,waterlogged=true": {"model":"scp_additions:block/scp_079_auxiliary_power_on","y":270}
  }
}
''')
write("src/main/resources/assets/scp_additions/models/block/scp_079_auxiliary_power_off.json", '''{"parent":"minecraft:block/cube_all","textures":{"all":"minecraft:block/iron_block"}}\n''')
write("src/main/resources/assets/scp_additions/models/block/scp_079_auxiliary_power_on.json", '''{"parent":"minecraft:block/cube_all","textures":{"all":"minecraft:block/redstone_lamp_on"}}\n''')
write("src/main/resources/assets/scp_additions/models/item/scp_079_auxiliary_power.json", '''{"parent":"scp_additions:block/scp_079_auxiliary_power_off"}\n''')

# Names and advancement text.
lang_path = ROOT / "src/main/resources/assets/scp_additions/lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang["block.scp_additions.scp_079_system_control"] = "Facility Diagnostic Terminal"
lang["block.scp_additions.scp_079_auxiliary_power"] = "Auxiliary Power Unit"
lang["advancements.scp_079control_achi.title"] = "Not Your Decision"
lang["advancements.scp_079control_achi.descr"] = "A facility system acted without your authorization."
lang_path.write_text(json.dumps(lang, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")

# Facility durability coverage.
replace("src/main/java/net/mcreator/scpadditions/facility/FacilityBlockMiningEvents.java",
        "|| path.equals(\"scp_079_system_control\")",
        "|| path.equals(\"scp_079_system_control\")\n                || path.equals(\"scp_079_auxiliary_power\")")

# Changelog.
replace("CHANGELOG.md",
        "## SCP-079\n",
        "## SCP-079\n\n- Replaced redstone-based facility access with a powered Facility Diagnostic Terminal and a placeholder Auxiliary Power Unit;\n- Added a black-and-green Foundation diagnostic interface reporting vague global containment, Tesla-grid, override, and connected-door telemetry;\n- SCP-079 now requires a physical computer in the world, begins hidden protocol discovery only after a powered diagnostic scan, learns faster from door use and Tesla activity, and gains AP regeneration only after completing access discovery;\n- Cutting auxiliary power now erases SCP-079's learned access and disables the diagnostic and Tesla buses, forcing the AI to investigate the system again after restoration;\n- Expanded the SCP-079 developer HUD with a discovery-progress bar and moved the hidden **Not Your Decision** advancement to the first facility decision that actually affects a player;\n")

# Remove temporary integration machinery before the workflow commits.
for path in [
    ROOT / ".github/workflows/apply-scp079-facility-discovery.yml",
    ROOT / "tools/hotfix/APPLY_SCP079_FACILITY_DISCOVERY",
    ROOT / "tools/hotfix/apply_scp079_facility_discovery.py",
]:
    if path.exists():
        path.unlink()
