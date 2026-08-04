package net.mcreator.scpadditions.facility;

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
import net.mcreator.scpadditions.roamer.RoamerManager;
import net.mcreator.scpadditions.roamer.RoamerType;
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
                && !data.facilityAccess() && !data.protocolExposed()) {
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
        if (!data.auxiliaryPowerOnline()
                || (data.hosts().isEmpty()
                && (data.protocolExposed() || data.facilityAccess()
                || data.discoveryProgress() > 0.0D))) {
            if (data.protocolExposed() || data.facilityAccess()
                    || data.discoveryProgress() > 0.0D) {
                resetCompromise(server, data);
            }
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
        for (RoamerType type : RoamerType.values()) {
            if (RoamerManager.hasActive(server, type)
                    && !RoamerManager.isContained(server, type)) {
                count++;
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
        if (data.hosts().isEmpty()
                && (data.protocolExposed() || data.facilityAccess()
                || data.discoveryProgress() > 0.0D)) {
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
