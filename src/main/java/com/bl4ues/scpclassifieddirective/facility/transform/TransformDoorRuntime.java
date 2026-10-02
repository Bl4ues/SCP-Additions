package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;
import com.bl4ues.scpclassifieddirective.facility.HeavyDoorPowerRelay;
import com.bl4ues.scpclassifieddirective.facility.Scp079ActivityPingManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative compatibility layer for SCP:CD animated doors placed on
 * a transformed local grid. The visual frame remains the stored BlockState, so
 * the ordinary block model, transformed collision and save/template systems all
 * see the same state instead of maintaining a parallel display entity.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformDoorRuntime {
    private static final Map<MinecraftServer, Map<CellKey, PendingDoor>> PENDING =
            new WeakHashMap<>();
    private static final Map<MinecraftServer, Integer> LAST_RECOVERY =
            new WeakHashMap<>();
    private static final Map<MinecraftServer, DoorIndex> DOOR_INDEX =
            new WeakHashMap<>();

    private TransformDoorRuntime() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || event.getItemStack().getItem() instanceof BlockItem) {
            return;
        }
        // Rigid transformed doors may be clipped into occupied vanilla cells,
        // where no proxy can exist. Resolve the authored door from the hit
        // location itself so direct-use doors remain functional there too.
        DoorHit hit = nearestDoor(level, event.getHitVec().getLocation(), 4.0D);
        if (hit == null || !hit.address().directUse()) return;
        TransformDoorStateAdapter.Address address = hit.address();
        if (address.stage() != DoorStage.CLOSED
                && address.stage() != DoorStage.OPEN) return;

        if (address.stage() == DoorStage.CLOSED) {
            start(level, hit.group(), hit.cell(), address, true);
        } else {
            start(level, hit.group(), hit.cell(), address, false);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Uses a transformed direct-use door addressed in local grid coordinates.
     * The server revalidates dimension, payload and reach before changing state.
     */
    public static boolean useGroupCell(ServerPlayer player, UUID groupId,
            GridPos cell) {
        if (player == null || groupId == null || cell == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        TransformGroup group = data.group(groupId);
        if (group == null || !group.dimension().equals(
                level.dimension().location())) return false;
        Vec3 center = group.cellCenter(cell);
        if (player.getEyePosition().distanceToSqr(center) > 36.0D) return false;

        TransformDoorStateAdapter.Address address = address(group.cells().get(cell));
        if (address == null || !address.directUse()) return false;
        if (address.stage() == DoorStage.CLOSED) {
            start(level, group, cell, address, true);
            return true;
        }
        if (address.stage() == DoorStage.OPEN) {
            start(level, group, cell, address, false);
            return true;
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        recoverAndDrivePoweredDoors(server, tick);

        Map<CellKey, PendingDoor> pending = PENDING.get(server);
        if (pending == null || pending.isEmpty()) return;
        Iterator<Map.Entry<CellKey, PendingDoor>> iterator =
                pending.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<CellKey, PendingDoor> entry = iterator.next();
            PendingDoor door = entry.getValue();
            if (tick < door.nextTick()) continue;
            ServerLevel level = levelById(server, door.dimension());
            if (level == null || !advance(level, entry.getKey(), door, tick)) {
                iterator.remove();
            }
        }
    }

    private static void recoverAndDrivePoweredDoors(MinecraftServer server,
            int tick) {
        int last = LAST_RECOVERY.getOrDefault(server, Integer.MIN_VALUE / 2);
        if (tick - last < 5) return;
        LAST_RECOVERY.put(server, tick);
        Map<CellKey, PendingDoor> pending = PENDING.computeIfAbsent(server,
                ignored -> new HashMap<>());
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                server);
        for (DoorRef ref : doorRefs(server, data)) {
            TransformGroup group = data.group(ref.groupId());
            if (group == null) continue;
            BlockState state = group.cells().get(ref.cell());
            TransformDoorStateAdapter.Address address = address(state);
            if (address == null) continue;
            ServerLevel level = levelById(server, group.dimension());
            if (level == null) continue;

            CellKey key = new CellKey(group.id(), ref.cell());
            if ((address.stage() == DoorStage.OPENING
                    || address.stage() == DoorStage.CLOSING)
                    && !pending.containsKey(key)) {
                pending.put(key, new PendingDoor(group.dimension(),
                        address.id(),
                        address.stage() == DoorStage.OPENING,
                        tick + TransformDoorStateAdapter.delay(address,
                                address.stage() == DoorStage.OPENING)));
                continue;
            }
            if (address.directUse() || pending.containsKey(key)) {
                continue;
            }
            boolean powered = TransformPowerQuery.powered(level,
                    group, ref.cell());
            // Heavy door controllers are stored in their lower cell. Their
            // upper frame and button positions are real adjacent local cells,
            // not vanilla-world relays when this door is transformed.
            if (!powered && HeavyDoorPowerRelay.isHeavyDoorState(
                    state.getBlock())) {
                for (int up = 1; up <= 2 && !powered; up++) {
                    powered = TransformPowerQuery.powered(level, group,
                            ref.cell().offset(0, up, 0));
                }
            }
            if (!powered
                    && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                powered = TransformPowerQuery.doorPanelPowered(level, group,
                        ref.cell(), state.getValue(HorizontalDirectionalBlock.FACING),
                        HeavyDoorPowerRelay.isHeavyDoorState(state.getBlock()));
            }
            if (address.stage() == DoorStage.CLOSED && powered) {
                start(level, group, ref.cell(), address, true);
            } else if (address.stage() == DoorStage.OPEN && !powered) {
                start(level, group, ref.cell(), address, false);
            }
        }
    }

    private static List<DoorRef> doorRefs(MinecraftServer server,
            TransformConstructionSavedData data) {
        long revision = data.revision();
        DoorIndex cached = DOOR_INDEX.get(server);
        if (cached != null && cached.revision() == revision) {
            return cached.refs();
        }

        java.util.ArrayList<DoorRef> refs = new java.util.ArrayList<>();
        for (TransformGroup group : data.groups()) {
            for (Map.Entry<GridPos, BlockState> entry : group.cells().entrySet()) {
                if (address(entry.getValue()) != null) {
                    refs.add(new DoorRef(group.id(), entry.getKey()));
                }
            }
        }
        List<DoorRef> immutable = List.copyOf(refs);
        DOOR_INDEX.put(server, new DoorIndex(revision, immutable));
        return immutable;
    }

    private static boolean advance(ServerLevel level, CellKey key,
            PendingDoor pending, int tick) {
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        TransformGroup group = data.group(key.groupId());
        if (group == null || !group.dimension().equals(
                level.dimension().location())) return false;
        BlockState current = group.cells().get(key.cell());
        TransformDoorStateAdapter.Address address =
                TransformDoorStateAdapter.address(current);
        if (address == null || !address.id().equals(pending.familyId())) {
            return false;
        }
        TransformDoorStateAdapter.Advance advanced =
                TransformDoorStateAdapter.advance(current, address,
                        pending.opening());
        if (advanced == null) return false;
        boolean becameClosed = advanced.done() && !pending.opening();
        setState(level, group, key.cell(), advanced.state(), becameClosed);
        if (advanced.done()) return false;
        TransformDoorStateAdapter.Address next =
                TransformDoorStateAdapter.address(advanced.state());
        if (next == null) return false;
        Map<CellKey, PendingDoor> map = PENDING.get(level.getServer());
        if (map != null) {
            map.put(key, new PendingDoor(level.dimension().location(),
                    next.id(), pending.opening(),
                    tick + TransformDoorStateAdapter.delay(next,
                            pending.opening())));
        }
        return true;
    }

    private static void start(ServerLevel level, TransformGroup group,
            GridPos cell, TransformDoorStateAdapter.Address address,
            boolean opening) {
        BlockState current = group.cells().get(cell);
        BlockState first = TransformDoorStateAdapter.begin(current, address,
                opening);
        if (first == current) return;
        Vec3 center = group.cellCenter(cell);
        Scp079ActivityPingManager.emitDoorAt(level, center);
        TransformDoorStateAdapter.playSound(level, center, address, opening);
        setState(level, group, cell, first, true);
        TransformDoorStateAdapter.Address next =
                TransformDoorStateAdapter.address(first);
        if (next == null) return;
        CellKey key = new CellKey(group.id(), cell);
        PENDING.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
                .put(key, new PendingDoor(level.dimension().location(),
                        next.id(), opening,
                        level.getServer().getTickCount()
                                + TransformDoorStateAdapter.delay(next,
                                        opening)));
    }

    private static void setState(ServerLevel level, TransformGroup group,
            GridPos cell, BlockState state, boolean refreshCollision) {
        TransformGroup next = group.withCell(cell, state);
        TransformConstructionSavedData.get(level.getServer()).putGroupState(next);
        TransformConstructionNetwork.broadcastGroupCell(level, group.id(), cell,
                state);
        boolean passabilityChanged = FacilityModule.isFacilityDoor(state)
                && FacilityModule.isFacilityDoor(group.cells().get(cell))
                && FacilityModule.isDoorPassable(state)
                != FacilityModule.isDoorPassable(group.cells().get(cell));
        if (refreshCollision || passabilityChanged) {
            TransformConstructionManager.refreshGroupCellRuntime(
                    level.getServer(), group.id(), cell);
        }
        if (passabilityChanged) {
            TransformConstructionManager.refreshOpenDoorNeighbours(
                    level.getServer(), group.id(), cell);
        }
    }

    public static synchronized void structuralCellChanged(
            MinecraftServer server, UUID groupId, GridPos cell) {
        DoorIndex index = DOOR_INDEX.get(server);
        if (server == null || index == null || groupId == null || cell == null) {
            return;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<DoorRef> refs = new java.util.ArrayList<>(index.refs());
        refs.removeIf(ref -> ref.groupId().equals(groupId)
                && ref.cell().equals(cell));
        TransformGroup group = data.group(groupId);
        if (group != null && address(group.cells().get(cell)) != null) {
            refs.add(new DoorRef(groupId, cell));
        }
        DOOR_INDEX.put(server, new DoorIndex(data.revision(),
                List.copyOf(refs)));
    }

    public static synchronized void structuralGroupChanged(
            MinecraftServer server, UUID groupId) {
        DoorIndex index = DOOR_INDEX.get(server);
        if (server == null || index == null || groupId == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<DoorRef> refs = new java.util.ArrayList<>(index.refs());
        refs.removeIf(ref -> ref.groupId().equals(groupId));
        TransformGroup group = data.group(groupId);
        if (group != null) {
            for (Map.Entry<GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (address(entry.getValue()) != null) {
                    refs.add(new DoorRef(groupId, entry.getKey()));
                }
            }
        }
        DOOR_INDEX.put(server, new DoorIndex(data.revision(),
                List.copyOf(refs)));
    }

    public static synchronized void acknowledgeStructuralRevision(
            MinecraftServer server) {
        DoorIndex index = DOOR_INDEX.get(server);
        if (server == null || index == null) return;
        long revision = TransformConstructionSavedData.get(server).revision();
        if (index.revision() != revision) {
            DOOR_INDEX.put(server, new DoorIndex(revision, index.refs()));
        }
    }

    private static DoorHit nearestDoor(ServerLevel level, Vec3 world,
            double maxDistanceSqr) {
        DoorHit best = null;
        double bestDistance = maxDistanceSqr;
        for (TransformGroup group : TransformConstructionManager.groups(level)) {
            Vec3 local = TransformMath.worldToLocal(group.origin(), world,
                    group.rotationX(), group.rotationY(), group.rotationZ());
            for (Map.Entry<GridPos, BlockState> entry : group.cells().entrySet()) {
                TransformDoorStateAdapter.Address address = address(entry.getValue());
                if (address == null) continue;
                GridPos cell = entry.getKey();
                double distance = local.distanceToSqr(
                        new Vec3(cell.x(), cell.y(), cell.z()));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new DoorHit(group, cell, address);
                }
            }
        }
        return best;
    }

    private static TransformDoorStateAdapter.Address address(
            BlockState state) {
        return TransformDoorStateAdapter.address(state);
    }

    private static ServerLevel levelById(MinecraftServer server,
            ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) return level;
        }
        return null;
    }

    private record DoorRef(UUID groupId, GridPos cell) {
    }

    private record DoorIndex(long revision, List<DoorRef> refs) {
    }

    private record CellKey(UUID groupId, GridPos cell) {
    }

    private record PendingDoor(ResourceLocation dimension, String familyId,
            boolean opening, int nextTick) {
    }


    private record DoorHit(TransformGroup group, GridPos cell,
            TransformDoorStateAdapter.Address address) {
    }
}
