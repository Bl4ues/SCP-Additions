package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorFamily;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
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
    private static final List<DoorFamily> FAMILIES = List.of(
            FacilityModule.DEFAULT_DOOR,
            FacilityModule.YELLOW_DOOR,
            FacilityModule.BLACK_DOOR,
            FacilityModule.NORMAL_DOOR,
            FacilityModule.LEFT_LOG_DOOR,
            FacilityModule.RIGHT_LOG_DOOR,
            FacilityModule.OFFICE_DOOR,
            FacilityModule.BATH_DOOR,
            FacilityModule.WORKSHOP_DOOR);
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
        if (hit == null || !hit.address().family().directUse()) return;
        DoorAddress address = hit.address();
        if (address.stage() != DoorStage.CLOSED
                && address.stage() != DoorStage.OPEN) return;

        if (address.stage() == DoorStage.CLOSED) {
            start(level, hit.group(), hit.cell(), address.family(), true);
        } else {
            start(level, hit.group(), hit.cell(), address.family(), false);
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

        DoorAddress address = address(group.cells().get(cell));
        if (address == null || !address.family().directUse()) return false;
        if (address.stage() == DoorStage.CLOSED) {
            start(level, group, cell, address.family(), true);
            return true;
        }
        if (address.stage() == DoorStage.OPEN) {
            start(level, group, cell, address.family(), false);
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
            DoorAddress address = address(state);
            if (address == null) continue;
            ServerLevel level = levelById(server, group.dimension());
            if (level == null) continue;

            CellKey key = new CellKey(group.id(), ref.cell());
            if ((address.stage() == DoorStage.OPENING
                    || address.stage() == DoorStage.CLOSING)
                    && !pending.containsKey(key)) {
                pending.put(key, new PendingDoor(group.dimension(),
                        address.family().id(),
                        address.stage() == DoorStage.OPENING,
                        tick + Math.max(1, address.family().frameDelay())));
                continue;
            }
            if (address.family().directUse() || pending.containsKey(key)) {
                continue;
            }
            boolean powered = TransformPowerQuery.powered(level,
                    group.cellCenter(ref.cell()));
            if (address.stage() == DoorStage.CLOSED && powered) {
                start(level, group, ref.cell(), address.family(), true);
            } else if (address.stage() == DoorStage.OPEN && !powered) {
                start(level, group, ref.cell(), address.family(), false);
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
        if (group == null || !group.dimension().equals(level.dimension().location())) {
            return false;
        }
        BlockState current = group.cells().get(key.cell());
        DoorAddress address = address(current);
        if (address == null || !address.family().id().equals(pending.familyId())) {
            return false;
        }
        DoorFamily family = address.family();
        if (pending.opening()) {
            if (address.stage() != DoorStage.OPENING) return false;
            int nextIndex = address.frame() + 1;
            Block next = nextIndex < family.opening().size()
                    ? family.opening().get(nextIndex).get()
                    : family.open().get();
            setState(level, group, key.cell(), copyFacing(current, next), false);
            if (nextIndex >= family.opening().size()) return false;
        } else {
            if (address.stage() != DoorStage.CLOSING) return false;
            int nextIndex = address.frame() + 1;
            Block next = nextIndex < family.closing().size()
                    ? family.closing().get(nextIndex).get()
                    : family.closed().get();
            boolean becameClosed = nextIndex >= family.closing().size();
            setState(level, group, key.cell(), copyFacing(current, next),
                    becameClosed);
            if (becameClosed) return false;
        }
        Map<CellKey, PendingDoor> map = PENDING.get(level.getServer());
        if (map != null) {
            map.put(key, new PendingDoor(level.dimension().location(), family.id(),
                    pending.opening(), tick + Math.max(1, family.frameDelay())));
        }
        return true;
    }

    private static void start(ServerLevel level, TransformGroup group,
            GridPos cell, DoorFamily family, boolean opening) {
        List<net.minecraftforge.registries.RegistryObject<Block>> frames = opening
                ? family.opening() : family.closing();
        if (frames.isEmpty()) return;
        BlockState current = group.cells().get(cell);
        BlockState first = copyFacing(current, frames.get(0).get());
        Vec3 center = group.cellCenter(cell);
        level.playSound(null, center.x, center.y, center.z,
                (opening ? family.openingSound() : family.closingSound()).get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        setState(level, group, cell, first, true);
        CellKey key = new CellKey(group.id(), cell);
        PENDING.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
                .put(key, new PendingDoor(level.dimension().location(), family.id(),
                        opening, level.getServer().getTickCount()
                                + Math.max(1, family.frameDelay())));
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
            TransformConstructionManager.refreshGroupRuntime(
                    level.getServer(), group.id());
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
                DoorAddress address = address(entry.getValue());
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

    private static DoorAddress address(BlockState state) {
        if (state == null || !FacilityModule.isFacilityDoor(state)) return null;
        Block block = state.getBlock();
        for (DoorFamily family : FAMILIES) {
            if (block == family.closed().get()) {
                return new DoorAddress(family, DoorStage.CLOSED, 0);
            }
            if (block == family.open().get()) {
                return new DoorAddress(family, DoorStage.OPEN, 0);
            }
            for (int i = 0; i < family.opening().size(); i++) {
                if (block == family.opening().get(i).get()) {
                    return new DoorAddress(family, DoorStage.OPENING, i);
                }
            }
            for (int i = 0; i < family.closing().size(); i++) {
                if (block == family.closing().get(i).get()) {
                    return new DoorAddress(family, DoorStage.CLOSING, i);
                }
            }
        }
        return null;
    }

    private static BlockState copyFacing(BlockState from, Block target) {
        BlockState next = target.defaultBlockState();
        if (from != null && from.hasProperty(HorizontalDirectionalBlock.FACING)
                && next.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction facing = from.getValue(HorizontalDirectionalBlock.FACING);
            next = next.setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        return next;
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

    private record DoorAddress(DoorFamily family, DoorStage stage, int frame) {
    }

    private record DoorHit(TransformGroup group, GridPos cell,
            DoorAddress address) {
    }
}
