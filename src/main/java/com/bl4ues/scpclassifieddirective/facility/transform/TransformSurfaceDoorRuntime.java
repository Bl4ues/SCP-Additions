package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorFamily;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;
import com.bl4ues.scpclassifieddirective.facility.Scp079ActivityPingManager;
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

/** Animated SCP:CD door compatibility for rigid attachments on curved walls. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformSurfaceDoorRuntime {
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

    private TransformSurfaceDoorRuntime() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(
                        TransformConstructionModule.getProxy())
                || event.getItemStack().getItem() instanceof BlockItem) return;
        DoorHit hit = nearestDoor(level, event.getHitVec().getLocation(), 4.0D);
        if (hit == null || !hit.address().family().directUse()) return;
        DoorAddress address = hit.address();
        if (address.stage() != DoorStage.CLOSED
                && address.stage() != DoorStage.OPEN) return;
        start(level, hit.surface(), hit.slot(), hit.normalSign(), address.family(),
                address.stage() == DoorStage.CLOSED);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Direct-use Surface doors are addressed by the authored parametric slot,
     * not by the vanilla proxy cell behind the curved wall.
     */
    public static boolean useSurfaceSlot(ServerPlayer player, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
        return useSurfaceDoor(player, surfaceId, slot, 0);
    }

    public static boolean useSurfaceOverlay(ServerPlayer player, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
        return useSurfaceDoor(player, surfaceId, slot,
                normalSign < 0 ? -1 : 1);
    }

    private static boolean useSurfaceDoor(ServerPlayer player, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
        if (player == null || surfaceId == null || slot == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface.SurfaceAttachment attachment =
                attachment(surface, slot, normalSign);
        if (attachment == null) return false;
        DoorAddress address = address(attachment.state());
        if (address == null || !address.family().directUse()) return false;
        if (player.getEyePosition().distanceToSqr(
                center(surface, slot, normalSign)) > 36.0D) return false;
        if (address.stage() == DoorStage.CLOSED
                || address.stage() == DoorStage.OPEN) {
            start(level, surface, slot, normalSign, address.family(),
                    address.stage() == DoorStage.CLOSED);
        }
        return true;
    }

    private static ConstructionSurface.SurfaceAttachment attachment(
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            int normalSign) {
        return normalSign == 0 ? surface.attachments().get(slot)
                : surface.overlay(slot, normalSign);
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
            ConstructionSurface surface = data.surface(ref.surfaceId());
            if (surface == null) continue;
            ConstructionSurface.SurfaceAttachment attachment =
                    attachment(surface, ref.slot(), ref.normalSign());
            if (attachment == null) continue;
            DoorAddress address = address(attachment.state());
            if (address == null) continue;
            ServerLevel level = levelById(server, surface.dimension());
            if (level == null) continue;

            CellKey key = new CellKey(surface.id(), ref.slot(), ref.normalSign());
            if ((address.stage() == DoorStage.OPENING
                    || address.stage() == DoorStage.CLOSING)
                    && !pending.containsKey(key)) {
                pending.put(key, new PendingDoor(surface.dimension(),
                        address.family().id(),
                        address.stage() == DoorStage.OPENING,
                        tick + Math.max(1, address.family().frameDelay())));
                continue;
            }
            if (address.family().directUse() || pending.containsKey(key)) {
                continue;
            }
            boolean powered = TransformPowerQuery.powered(level,
                    surface, ref.slot());
            if (address.stage() == DoorStage.CLOSED && powered) {
                start(level, surface, ref.slot(), ref.normalSign(),
                        address.family(), true);
            } else if (address.stage() == DoorStage.OPEN && !powered) {
                start(level, surface, ref.slot(), ref.normalSign(),
                        address.family(), false);
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
        for (ConstructionSurface surface : data.surfaces()) {
            collectDoorRefs(refs, surface);
        }
        List<DoorRef> immutable = List.copyOf(refs);
        DOOR_INDEX.put(server, new DoorIndex(revision, immutable));
        return immutable;
    }

    private static void collectDoorRefs(List<DoorRef> refs,
            ConstructionSurface surface) {
        for (Map.Entry<ConstructionSurface.SurfaceSlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.attachments().entrySet()) {
            if (address(entry.getValue().state()) != null) {
                refs.add(new DoorRef(surface.id(), entry.getKey(), 0));
            }
        }
        for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.overlays().entrySet()) {
            if (address(entry.getValue().state()) != null) {
                refs.add(new DoorRef(surface.id(), entry.getKey().slot(),
                        entry.getKey().normalSign() < 0 ? -1 : 1));
            }
        }
    }

    private static void collectSlotDoorRefs(List<DoorRef> refs,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot) {
        for (int sign : new int[]{0, -1, 1}) {
            ConstructionSurface.SurfaceAttachment current =
                    attachment(surface, slot, sign);
            if (current != null && address(current.state()) != null) {
                refs.add(new DoorRef(surface.id(), slot, sign));
            }
        }
    }

    private static boolean advance(ServerLevel level, CellKey key,
            PendingDoor pending, int tick) {
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        ConstructionSurface surface = data.surface(key.surfaceId());
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface.SurfaceAttachment attachment =
                attachment(surface, key.slot(), key.normalSign());
        if (attachment == null) return false;
        BlockState current = attachment.state();
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
            setState(level, surface, key.slot(), key.normalSign(),
                    attachment, copyFacing(current, next), false);
            if (nextIndex >= family.opening().size()) return false;
        } else {
            if (address.stage() != DoorStage.CLOSING) return false;
            int nextIndex = address.frame() + 1;
            Block next = nextIndex < family.closing().size()
                    ? family.closing().get(nextIndex).get()
                    : family.closed().get();
            boolean becameClosed = nextIndex >= family.closing().size();
            setState(level, surface, key.slot(), key.normalSign(),
                    attachment, copyFacing(current, next), becameClosed);
            if (becameClosed) return false;
        }
        Map<CellKey, PendingDoor> map = PENDING.get(level.getServer());
        if (map != null) {
            map.put(key, new PendingDoor(level.dimension().location(), family.id(),
                    pending.opening(), tick + Math.max(1, family.frameDelay())));
        }
        return true;
    }

    private static void start(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            DoorFamily family, boolean opening) {
        List<net.minecraftforge.registries.RegistryObject<Block>> frames = opening
                ? family.opening() : family.closing();
        if (frames.isEmpty()) return;
        ConstructionSurface.SurfaceAttachment attachment =
                attachment(surface, slot, normalSign);
        if (attachment == null) return;
        BlockState current = attachment.state();
        BlockState first = copyFacing(current, frames.get(0).get());
        Vec3 center = center(surface, slot, normalSign);
        Scp079ActivityPingManager.emitDoorAt(level, center);
        level.playSound(null, center.x, center.y, center.z,
                (opening ? family.openingSound() : family.closingSound()).get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        setState(level, surface, slot, normalSign, attachment, first, true);
        CellKey key = new CellKey(surface.id(), slot, normalSign);
        PENDING.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
                .put(key, new PendingDoor(level.dimension().location(), family.id(),
                        opening, level.getServer().getTickCount()
                                + Math.max(1, family.frameDelay())));
    }

    private static void setState(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            ConstructionSurface.SurfaceAttachment previous, BlockState state,
            boolean refreshCollision) {
        ConstructionSurface next;
        if (normalSign == 0) {
            next = surface.withAttachment(slot, state, previous.deform());
            TransformConstructionNetwork.broadcastSurfaceSlot(level, surface.id(),
                    slot, state, previous.deform());
        } else {
            next = surface.withOverlay(slot, normalSign, state,
                    previous.deform());
            TransformConstructionNetwork.broadcastSurfaceOverlay(level,
                    surface.id(), slot, normalSign, state, previous.deform());
        }
        TransformConstructionSavedData.get(level.getServer()).putSurfaceState(next);
        boolean passabilityChanged = FacilityModule.isFacilityDoor(state)
                && FacilityModule.isFacilityDoor(previous.state())
                && FacilityModule.isDoorPassable(state)
                != FacilityModule.isDoorPassable(previous.state());
        if (refreshCollision || passabilityChanged) {
            TransformConstructionManager.refreshSurfaceSlotRuntime(
                    level.getServer(), surface.id(), slot);
        }
    }

    public static synchronized void structuralSlotChanged(
            MinecraftServer server, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
        DoorIndex index = DOOR_INDEX.get(server);
        if (server == null || index == null || surfaceId == null || slot == null) {
            return;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<DoorRef> refs = new java.util.ArrayList<>(index.refs());
        refs.removeIf(ref -> ref.surfaceId().equals(surfaceId)
                && ref.slot().equals(slot));
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface != null) collectSlotDoorRefs(refs, surface, slot);
        DOOR_INDEX.put(server, new DoorIndex(data.revision(), List.copyOf(refs)));
    }

    public static synchronized void structuralSurfaceChanged(
            MinecraftServer server, UUID surfaceId) {
        DoorIndex index = DOOR_INDEX.get(server);
        if (server == null || index == null || surfaceId == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<DoorRef> refs = new java.util.ArrayList<>(index.refs());
        refs.removeIf(ref -> ref.surfaceId().equals(surfaceId));
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface != null) collectDoorRefs(refs, surface);
        DOOR_INDEX.put(server, new DoorIndex(data.revision(), List.copyOf(refs)));
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
        for (ConstructionSurface surface
                : TransformConstructionManager.surfaces(level)) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                DoorAddress address = address(entry.getValue().state());
                if (address == null) continue;
                Vec3 center = center(surface, entry.getKey(), 0);
                double distance = center.distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new DoorHit(surface, entry.getKey(), 0, address);
                }
            }
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                DoorAddress address = address(entry.getValue().state());
                if (address == null) continue;
                int sign = entry.getKey().normalSign() < 0 ? -1 : 1;
                Vec3 center = center(surface, entry.getKey().slot(), sign);
                double distance = center.distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new DoorHit(surface, entry.getKey().slot(),
                            sign, address);
                }
            }
        }
        return best;
    }

    private static Vec3 center(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
        return TransformSurfaceGeometry.cellCenter(surface, slot,
                normalSign == 0 ? TransformSurfaceGeometry.MAIN_SIDE
                        : normalSign, normalSign != 0);
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

    private record DoorRef(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
    }

    private record DoorIndex(long revision, List<DoorRef> refs) {
    }

    private record CellKey(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
    }

    private record PendingDoor(ResourceLocation dimension, String familyId,
            boolean opening, int nextTick) {
    }

    private record DoorAddress(DoorFamily family, DoorStage stage, int frame) {
    }

    private record DoorHit(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            DoorAddress address) {
    }
}
