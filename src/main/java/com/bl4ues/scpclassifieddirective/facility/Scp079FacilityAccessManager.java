package com.bl4ues.scpclassifieddirective.facility;

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
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.Scp079AuxiliaryPowerBlock;
import com.bl4ues.scpclassifieddirective.block.TeslaGateStructure;
import com.bl4ues.scpclassifieddirective.roamer.RoamerManager;
import com.bl4ues.scpclassifieddirective.roamer.RoamerType;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Global, deliberately simple facility bus used by diagnostics and SCP-079.
 * Nothing is wired by radius or site ID: registered facility blocks share one
 * world-level auxiliary channel.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079FacilityAccessManager {
    private static final double PASSIVE_DISCOVERY_PER_SECOND = 0.025D;
    private static final double SCAN_DISCOVERY = 8.0D;
    private static final long CACHE_PURGE_LOCKOUT_TICKS =
            5L * 60L * 20L;

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

    public static int cachePurgeCooldownTicks(MinecraftServer server) {
        if (server == null) return 0;
        Scp079FacilityAccessSavedData data = data(server);
        long remaining = data.cachePurgeLockoutUntilGameTime()
                - server.overworld().getGameTime();
        if (remaining <= 0L) {
            if (data.cachePurgeLockoutUntilGameTime() != 0L) {
                data.setCachePurgeLockoutUntilGameTime(0L);
            }
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    private static boolean cachePurgeLocked(MinecraftServer server) {
        return cachePurgeCooldownTicks(server) > 0;
    }

    public static int activeAuxiliaryGenerators(MinecraftServer server) {
        return server == null ? 0
                : data(server).poweredAuxiliaryUnits().size();
    }

    private static void refreshAuxiliaryBus(MinecraftServer server,
            Scp079FacilityAccessSavedData data) {
        if (server == null) return;
        boolean online = !data.poweredAuxiliaryUnits().isEmpty();
        boolean changed = data.auxiliaryPowerOnline() != online;
        data.setAuxiliaryPowerOnline(online);
        if (changed && online) {
            wakeTeslaGates(server, data);
        }
        applyAuxiliaryControlState(server, data);
    }

    private static void applyAuxiliaryControlState(MinecraftServer server,
            Scp079FacilityAccessSavedData data) {
        boolean expected = data.auxiliaryPowerOnline()
                && data.facilityAccess() && !data.hosts().isEmpty()
                && !cachePurgeLocked(server);
        boolean current = server.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.SCP079CONTROLON);
        if (current != expected) {
            server.getGameRules().getRule(
                    ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)
                    .set(expected, server);
        }
        if (expected) {
            Scp079ProcessingManager.onControlEnabled(server.overworld());
        } else {
            Scp079ProcessingManager.onControlDisabled(server.overworld());
        }
    }

    public static DiagnosticSnapshot performDiagnosticScan(
            ServerPlayer player) {
        return diagnosticSnapshot(player, true);
    }

    public static DiagnosticSnapshot currentDiagnosticSnapshot(
            ServerPlayer player) {
        return diagnosticSnapshot(player, false);
    }

    private static DiagnosticSnapshot diagnosticSnapshot(ServerPlayer player,
            boolean exposeProtocol) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null) return DiagnosticSnapshot.EMPTY;
        Scp079FacilityAccessSavedData data = data(server);
        pruneLoadedPositions(server, data);

        if (exposeProtocol && data.auxiliaryPowerOnline()
                && !cachePurgeLocked(server)
                && !data.hosts().isEmpty() && !data.facilityAccess()
                && !data.protocolExposed()) {
            data.setProtocolExposed(true);
            addDiscovery(server, data, SCAN_DISCOVERY);
        }

        int uncontained = countUncontainedScps(server);
        boolean override = server.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE);
        boolean configured = server.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.TESLAGATEON);
        int totalGates = data.teslaGates().size();
        int activeGates = data.auxiliaryPowerOnline()
                && (configured || override) ? totalGates : 0;
        // A learned remote session remains cached even while its physical
        // host is absent; only an explicit terminal purge clears the advisory.
        boolean unusualNetworkActivity = data.facilityAccess();
        return new DiagnosticSnapshot(uncontained, activeGates, totalGates,
                override, data.doors().size(), data.auxiliaryPowerOnline(),
                cachePurgeCooldownTicks(server), unusualNetworkActivity);
    }

    public static void resetRemoteSession(ServerPlayer actor) {
        MinecraftServer server = actor == null ? null : actor.getServer();
        if (server == null) return;
        Scp079FacilityAccessSavedData data = data(server);
        if (!data.auxiliaryPowerOnline()) {
            actor.displayClientMessage(Component.literal(
                    "CACHE PURGE UNAVAILABLE: AUXILIARY POWER OFFLINE"),
                    true);
            return;
        }
        int remaining = cachePurgeCooldownTicks(server);
        if (remaining > 0) {
            actor.displayClientMessage(Component.literal(
                    "REMOTE SESSION CACHE LOCKOUT: "
                            + ((remaining + 19) / 20) + "s"), true);
            return;
        }
        resetCompromise(server, data);
        data.setCachePurgeLockoutUntilGameTime(
                server.overworld().getGameTime()
                        + CACHE_PURGE_LOCKOUT_TICKS);
        actor.displayClientMessage(Component.literal(
                "REMOTE SESSION CACHE: PURGE COMPLETE"), true);
    }

    public static void recordActivity(LevelAccessor level, Activity activity) {
        MinecraftServer server = server(level);
        if (server == null || activity == null) return;
        Scp079FacilityAccessSavedData data = data(server);
        if (!data.auxiliaryPowerOnline() || cachePurgeLocked(server)
                || !data.protocolExposed() || data.hosts().isEmpty()
                || data.facilityAccess()) {
            return;
        }
        addDiscovery(server, data, activity.discovery);
    }

    public static void registerHost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        MinecraftServer server = level.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        if (data.hosts().add(tracked(level, pos))) data.markChanged();
        if (data.facilityAccess() && data.auxiliaryPowerOnline()) {
            server.getGameRules().getRule(
                    ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)
                    .set(true, server);
            Scp079ProcessingManager.onControlEnabled(server.overworld());
        }
    }

    public static void unregisterHost(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        MinecraftServer server = level.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        if (data.hosts().remove(tracked(level, pos))) data.markChanged();
        if (data.hosts().isEmpty()) {
            server.getGameRules().getRule(
                    ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)
                    .set(false, server);
            Scp079ProcessingManager.onControlDisabled(server.overworld());
        }
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

    public static void registerAuxiliaryUnit(ServerLevel level, BlockPos pos,
            boolean powered) {
        if (level == null || pos == null) return;
        MinecraftServer server = level.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        Scp079FacilityAccessSavedData.TrackedPosition tracked =
                tracked(level, pos);
        boolean changed = data.auxiliaryUnits().add(tracked);
        if (powered) {
            changed |= data.poweredAuxiliaryUnits().add(tracked);
        } else {
            changed |= data.poweredAuxiliaryUnits().remove(tracked);
        }
        if (changed) data.markChanged();
        refreshAuxiliaryBus(server, data);
    }

    public static void updateAuxiliaryUnitPower(ServerLevel level,
            BlockPos pos, boolean powered) {
        if (level == null || pos == null) return;
        MinecraftServer server = level.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        Scp079FacilityAccessSavedData.TrackedPosition tracked =
                tracked(level, pos);
        boolean changed = data.auxiliaryUnits().add(tracked);
        if (powered) {
            changed |= data.poweredAuxiliaryUnits().add(tracked);
        } else {
            changed |= data.poweredAuxiliaryUnits().remove(tracked);
        }
        if (changed) data.markChanged();
        refreshAuxiliaryBus(server, data);
    }

    public static void unregisterAuxiliaryUnit(ServerLevel level,
            BlockPos pos) {
        if (level == null || pos == null) return;
        MinecraftServer server = level.getServer();
        Scp079FacilityAccessSavedData data = data(server);
        Scp079FacilityAccessSavedData.TrackedPosition tracked =
                tracked(level, pos);
        boolean changed = data.auxiliaryUnits().remove(tracked);
        changed |= data.poweredAuxiliaryUnits().remove(tracked);
        if (changed) data.markChanged();
        refreshAuxiliaryBus(server, data);
    }

    public static void awardFirstInterference(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;
        Advancement advancement = player.getServer().getAdvancements()
                .getAdvancement(new ResourceLocation(
                        "scp_classified_directive:scp_079control_achi"));
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
        refreshAuxiliaryBus(server, data);
        if (cachePurgeLocked(server)) {
            if (data.protocolExposed() || data.facilityAccess()
                    || data.discoveryProgress() > 0.0D) {
                resetCompromise(server, data);
            }
            if (server.getGameRules().getBoolean(
                    ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)) {
                server.getGameRules().getRule(
                        ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)
                        .set(false, server);
            }
            Scp079ProcessingManager.onControlDisabled(server.overworld());
            return;
        }

        if (data.auxiliaryPowerOnline() && data.protocolExposed()
                && !data.hosts().isEmpty() && !data.facilityAccess()) {
            addDiscovery(server, data, PASSIVE_DISCOVERY_PER_SECOND);
        }

        boolean expectedRule = data.auxiliaryPowerOnline()
                && data.facilityAccess() && !data.hosts().isEmpty();
        boolean currentRule = server.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.SCP079CONTROLON);
        if (currentRule != expectedRule) {
            server.getGameRules().getRule(
                    ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)
                    .set(expectedRule, server);
            if (expectedRule) {
                Scp079ProcessingManager.onControlEnabled(server.overworld());
            } else {
                Scp079ProcessingManager.onControlDisabled(server.overworld());
            }
        }

        // Force lazy AP regeneration/decay to advance even when no debug HUD
        // or SCP action happens to query the processing manager.
        Scp079ProcessingManager.getPower(server.overworld());
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
        if (cachePurgeLocked(server)) return;
        data.setDiscoveryProgress(data.discoveryProgress() + amount);
        if (data.discoveryProgress() >= 100.0D && !data.facilityAccess()) {
            data.setDiscoveryProgress(100.0D);
            data.setFacilityAccess(true);
            server.getGameRules().getRule(
                    ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)
                    .set(true, server);
            Scp079ProcessingManager.onControlEnabled(server.overworld());
        }
    }

    private static void resetCompromise(MinecraftServer server,
            Scp079FacilityAccessSavedData data) {
        data.setProtocolExposed(false);
        data.setDiscoveryProgress(0.0D);
        data.setFacilityAccess(false);
        server.getGameRules().getRule(
                ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)
                .set(false, server);
        Scp079ProcessingManager.resetPower(server.overworld());
    }

    private static int countUncontainedScps(MinecraftServer server) {
        int count = 0;
        for (RoamerType type : RoamerType.values()) {
            if (RoamerManager.isOperationallyUncontained(server, type)) {
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
        removeChunkEntries(data.poweredAuxiliaryUnits(), dimension,
                chunkX, chunkZ);

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
                                == ScpClassifiedDirectiveModBlocks.SCP_079_AUXILIARY_POWER.get()) {
                            data.auxiliaryUnits().add(tracked);
                            if (state.hasProperty(
                                    Scp079AuxiliaryPowerBlock.POWERED)
                                    && state.getValue(
                                    Scp079AuxiliaryPowerBlock.POWERED)) {
                                data.poweredAuxiliaryUnits().add(tracked);
                            }
                        }
                    }
                }
            }
        }
        data.markChanged();
        // ChunkEvent.Load can fire while this chunk is still being promoted.
        // Do not refresh the global auxiliary bus here: that path can inspect
        // tracked Tesla gates and other world positions, re-entering the chunk
        // pipeline. The existing once-per-second server tick refreshes the bus
        // immediately after startup in a safe phase.
    }

    private static boolean isTracked(BlockState state) {
        Block block = state.getBlock();
        return isScp079(block) || FacilityModule.isFacilityDoor(state)
                || TeslaGateStructure.isController(state)
                || block == ScpClassifiedDirectiveModBlocks.SCP_079_AUXILIARY_POWER.get();
    }

    private static boolean isScp079(Block block) {
        return block == ScpClassifiedDirectiveModBlocks.SCP_079ON.get()
                || block == ScpClassifiedDirectiveModBlocks.SCP_079OFF.get();
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
                        == ScpClassifiedDirectiveModBlocks.SCP_079_AUXILIARY_POWER.get());
        changed |= prune(server, data.poweredAuxiliaryUnits(), state ->
                state.getBlock()
                        == ScpClassifiedDirectiveModBlocks.SCP_079_AUXILIARY_POWER.get()
                        && state.hasProperty(
                        Scp079AuxiliaryPowerBlock.POWERED)
                        && state.getValue(
                        Scp079AuxiliaryPowerBlock.POWERED));
        if (changed) data.markChanged();
        refreshAuxiliaryBus(server, data);
        if (data.hosts().isEmpty()) {
            server.getGameRules().getRule(
                    ScpClassifiedDirectiveModGameRules.SCP079CONTROLON)
                    .set(false, server);
            Scp079ProcessingManager.onControlDisabled(server.overworld());
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
            boolean teslaOverride, int connectedDoors,
            boolean auxiliaryPowerOnline,
            int cachePurgeCooldownTicks,
            boolean unusualNetworkActivity) {
        public static final DiagnosticSnapshot EMPTY =
                new DiagnosticSnapshot(0, 0, 0, false, 0, false, 0, false);
    }
}
