package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorFamily;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;
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
        start(level, hit.surface(), hit.slot(), address.family(),
                address.stage() == DoorStage.CLOSED);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
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
        for (ConstructionSurface surface : data.surfaces()) {
            ServerLevel level = levelById(server, surface.dimension());
            if (level == null) continue;
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                DoorAddress address = address(entry.getValue().state());
                if (address == null) continue;
                CellKey key = new CellKey(surface.id(), entry.getKey());
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
                Vec3 center = center(surface, entry.getKey());
                boolean powered = TransformPowerQuery.powered(level, center);
                if (address.stage() == DoorStage.CLOSED && powered) {
                    start(level, surface, entry.getKey(), address.family(), true);
                } else if (address.stage() == DoorStage.OPEN && !powered) {
                    start(level, surface, entry.getKey(), address.family(), false);
                }
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
                surface.attachments().get(key.slot());
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
            setState(level, surface, key.slot(), attachment,
                    copyFacing(current, next), false);
            if (nextIndex >= family.opening().size()) return false;
        } else {
            if (address.stage() != DoorStage.CLOSING) return false;
            int nextIndex = address.frame() + 1;
            Block next = nextIndex < family.closing().size()
                    ? family.closing().get(nextIndex).get()
                    : family.closed().get();
            boolean becameClosed = nextIndex >= family.closing().size();
            setState(level, surface, key.slot(), attachment,
                    copyFacing(current, next), becameClosed);
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
            ConstructionSurface.SurfaceSlot slot, DoorFamily family,
            boolean opening) {
        List<net.minecraftforge.registries.RegistryObject<Block>> frames = opening
                ? family.opening() : family.closing();
        if (frames.isEmpty()) return;
        ConstructionSurface.SurfaceAttachment attachment =
                surface.attachments().get(slot);
        if (attachment == null) return;
        BlockState current = attachment.state();
        BlockState first = copyFacing(current, frames.get(0).get());
        Vec3 center = center(surface, slot);
        level.playSound(null, center.x, center.y, center.z,
                (opening ? family.openingSound() : family.closingSound()).get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        setState(level, surface, slot, attachment, first, true);
        CellKey key = new CellKey(surface.id(), slot);
        PENDING.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
                .put(key, new PendingDoor(level.dimension().location(), family.id(),
                        opening, level.getServer().getTickCount()
                                + Math.max(1, family.frameDelay())));
    }

    private static void setState(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment previous, BlockState state,
            boolean refreshCollision) {
        ConstructionSurface next = surface.withAttachment(slot, state,
                previous.deform());
        TransformConstructionSavedData.get(level.getServer()).putSurfaceState(next);
        TransformConstructionNetwork.broadcastSurfaceSlot(level, surface.id(), slot,
                state, previous.deform());
        if (refreshCollision) TransformConstructionManager.refresh(level.getServer());
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
                Vec3 center = center(surface, entry.getKey());
                double distance = center.distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new DoorHit(surface, entry.getKey(), address);
                }
            }
        }
        return best;
    }

    private static Vec3 center(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        return surface.gridPoint(u, v)
                .add(surface.gridNormal(u, v).scale(0.5D));
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

    private record CellKey(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
    }

    private record PendingDoor(ResourceLocation dimension, String familyId,
            boolean opening, int nextTick) {
    }

    private record DoorAddress(DoorFamily family, DoorStage stage, int frame) {
    }

    private record DoorHit(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, DoorAddress address) {
    }
}
