package com.bl4ues.scpclassifieddirective.facility.mapping;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityMappingNetwork;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server authority for authored room floors used by surveillance systems. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilityMappingManager {
    public static final long MAX_PATCH_AREA = 65_536L;
    public static final int MAX_PATCH_SPAN = 512;
    private static final Map<MinecraftServer, Map<UUID, PendingSelection>>
            PENDING = new WeakHashMap<>();
    private static final Map<MinecraftServer, Map<UUID, PendingCameraLink>>
            CAMERA_LINKS = new WeakHashMap<>();

    private FacilityMappingManager() {
    }

    public static boolean canEdit(ServerPlayer player) {
        return player != null && player.isCreative();
    }

    public static void beginSelection(ServerPlayer player, BlockPos pos) {
        if (!canEdit(player) || pos == null || !holdingTool(player)) return;
        Map<UUID, PendingCameraLink> cameraLinks = CAMERA_LINKS.get(
                player.getServer());
        if (cameraLinks != null) cameraLinks.remove(player.getUUID());
        FacilityMappingNetwork.sendCameraLinkState(player, null);
        PENDING.computeIfAbsent(player.getServer(), ignored -> new HashMap<>())
                .put(player.getUUID(), new PendingSelection(
                        player.level().dimension(), pos.immutable()));
        FacilityMappingNetwork.sendSelectionState(player, pos);
        player.displayClientMessage(Component.literal(
                "Room floor first corner selected. Right-click the opposite floor corner."),
                true);
    }

    public static void completeSelection(ServerPlayer player, BlockPos pos) {
        if (!canEdit(player) || pos == null) return;
        Map<UUID, PendingSelection> pendingMap = PENDING.get(player.getServer());
        PendingSelection pending = pendingMap == null ? null
                : pendingMap.remove(player.getUUID());
        FacilityMappingNetwork.sendSelectionState(player, null);
        if (pending == null || !pending.dimension()
                .equals(player.level().dimension())) {
            player.displayClientMessage(Component.literal(
                    "No room-floor selection is active."), true);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;

        FacilityFloorPatch patch = FacilityFloorPatch.between(
                pending.first(), pos);
        int xSpan = patch.maxX() - patch.minX() + 1;
        int zSpan = patch.maxZ() - patch.minZ() + 1;
        if (patch.area() <= 0L || patch.area() > MAX_PATCH_AREA
                || xSpan > MAX_PATCH_SPAN || zSpan > MAX_PATCH_SPAN) {
            player.displayClientMessage(Component.literal(
                    "Mapped floor area is too large. Maximum: 512 blocks per horizontal axis and 65,536 floor blocks per selection."),
                    true);
            return;
        }

        FacilityMappingSavedData data = FacilityMappingSavedData.get(
                level.getServer());
        ResourceLocation dimension = level.dimension().location();
        List<FacilityRoom> overlapping = data.rooms().stream()
                .filter(room -> room.dimension().equals(dimension)
                        && room.overlaps(patch))
                .toList();

        FacilityRoom target;
        if (overlapping.isEmpty()) {
            BlockPos station = FacilityFloorStationIndex.nearest(level,
                    patch.y());
            target = new FacilityRoom(UUID.randomUUID(), dimension,
                    List.of(patch), "", station);
        } else {
            target = overlapping.get(0).withAddedPatch(patch);
            for (int index = 1; index < overlapping.size(); index++) {
                FacilityRoom merged = overlapping.get(index);
                target = target.merge(merged);
                data.rebindRoom(merged.id(), target.id());
                data.deleteRoom(merged.id());
            }
        }
        data.putRoom(target);
        broadcast(level);
        player.displayClientMessage(Component.literal(overlapping.isEmpty()
                ? "Room floor mapped. Shift + right-click it to name the room and choose its floor."
                : "Mapped floor area added to the existing room."), true);
    }

    public static void cancelSelection(ServerPlayer player) {
        if (!canEdit(player)) return;
        Map<UUID, PendingSelection> map = PENDING.get(player.getServer());
        boolean removedFloor = map != null
                && map.remove(player.getUUID()) != null;
        Map<UUID, PendingCameraLink> cameraMap = CAMERA_LINKS.get(
                player.getServer());
        boolean removedCamera = cameraMap != null
                && cameraMap.remove(player.getUUID()) != null;
        FacilityMappingNetwork.sendSelectionState(player, null);
        FacilityMappingNetwork.sendCameraLinkState(player, null);
        player.displayClientMessage(Component.literal(
                removedFloor || removedCamera
                        ? "Facility mapping selection cancelled."
                        : "No mapping selection is active."), true);
    }

    public static void beginCameraLink(ServerPlayer player, BlockPos cameraPos) {
        if (!canEdit(player) || cameraPos == null || !holdingTool(player)
                || !(player.level() instanceof ServerLevel level)) return;
        FacilityCameraDefinition camera = FacilitySurveillanceRegistry.cameraAt(
                level, cameraPos);
        if (camera == null) {
            player.displayClientMessage(Component.literal(
                    "No surveillance camera is registered at this block."), true);
            return;
        }
        Map<UUID, PendingSelection> floorMap = PENDING.get(player.getServer());
        if (floorMap != null) floorMap.remove(player.getUUID());
        FacilityMappingNetwork.sendSelectionState(player, null);
        CAMERA_LINKS.computeIfAbsent(player.getServer(),
                ignored -> new HashMap<>()).put(player.getUUID(),
                new PendingCameraLink(level.dimension(), camera.id()));
        FacilityMappingNetwork.sendCameraLinkState(player, camera.id());
        player.displayClientMessage(Component.literal(
                "Camera selected. Left-click any block inside the mapped room that should own this camera."), true);
    }

    public static void completeCameraLink(ServerPlayer player,
            BlockPos roomProbe) {
        if (!canEdit(player) || roomProbe == null
                || !(player.level() instanceof ServerLevel level)) return;
        Map<UUID, PendingCameraLink> pendingMap = CAMERA_LINKS.get(
                player.getServer());
        PendingCameraLink pending = pendingMap == null ? null
                : pendingMap.get(player.getUUID());
        if (pending == null || !pending.dimension().equals(level.dimension())) {
            FacilityMappingNetwork.sendCameraLinkState(player, null);
            player.displayClientMessage(Component.literal(
                    "No camera association selection is active."), true);
            return;
        }
        FacilityCameraDefinition camera = FacilitySurveillanceRegistry.camera(
                level, pending.cameraId());
        if (camera == null) {
            pendingMap.remove(player.getUUID());
            FacilityMappingNetwork.sendCameraLinkState(player, null);
            player.displayClientMessage(Component.literal(
                    "The selected camera no longer exists."), true);
            return;
        }

        FacilityRoom room = roomForPosition(level, roomProbe);
        if (room == null) {
            player.displayClientMessage(Component.literal(
                    "No mapped room exists at that block. Camera selection remains active."), true);
            return;
        }

        FacilityMappingSavedData.get(level.getServer()).bindCamera(
                camera.id(), room.id());
        pendingMap.remove(player.getUUID());
        FacilityMappingNetwork.sendCameraLinkState(player, null);
        broadcast(level);
        String target = room.name().isBlank()
                ? room.id().toString().substring(0, 8) : room.name();
        player.displayClientMessage(Component.literal(
                "Camera manually associated with " + target + "."), true);
    }

    public static boolean detachCamera(ServerPlayer player, BlockPos cameraPos) {
        if (!canEdit(player) || cameraPos == null
                || !(player.level() instanceof ServerLevel level)) return false;
        FacilityCameraDefinition camera = FacilitySurveillanceRegistry.cameraAt(
                level, cameraPos);
        if (camera == null) return false;
        FacilityMappingSavedData.get(level.getServer()).detachCamera(camera.id());
        Map<UUID, PendingCameraLink> cameraMap = CAMERA_LINKS.get(
                player.getServer());
        if (cameraMap != null) cameraMap.remove(player.getUUID());
        FacilityMappingNetwork.sendCameraLinkState(player, null);
        broadcast(level);
        player.displayClientMessage(Component.literal(
                "Camera detached from automatic room association. Left-click it to choose a mapped room."), true);
        return true;
    }

    public static boolean openEditor(ServerPlayer player, BlockPos pos) {
        if (!canEdit(player) || pos == null
                || !(player.level() instanceof ServerLevel level)) return false;
        FacilityRoom room = findFloorAt(level, pos);
        if (room == null) {
            player.displayClientMessage(Component.literal(
                    "No mapped room floor exists at this position."), true);
            return false;
        }
        FacilityFloorStationIndex.refreshLoaded(level);
        FacilityMappingNetwork.openEditor(player, snapshot(level, room),
                FacilityFloorStationIndex.configured(level));
        return true;
    }

    public static void updateRoom(ServerPlayer player, UUID roomId,
            String name, BlockPos stationPos) {
        if (!canEdit(player) || roomId == null
                || !(player.level() instanceof ServerLevel level)) return;
        FacilityMappingSavedData data = FacilityMappingSavedData.get(
                level.getServer());
        FacilityRoom room = data.room(roomId);
        if (room == null || !room.dimension().equals(
                level.dimension().location())) return;
        BlockPos validatedStation = stationPos != null
                && FacilityFloorStationIndex.contains(level, stationPos)
                ? stationPos : null;
        data.putRoom(room.withMetadata(name, validatedStation));
        broadcast(level);
        player.displayClientMessage(Component.literal("Mapped room updated."),
                true);
    }

    public static void deleteRoom(ServerPlayer player, UUID roomId) {
        if (!canEdit(player) || roomId == null
                || !(player.level() instanceof ServerLevel level)) return;
        FacilityMappingSavedData data = FacilityMappingSavedData.get(
                level.getServer());
        FacilityRoom room = data.room(roomId);
        if (room == null || !room.dimension().equals(
                level.dimension().location())) return;
        if (data.deleteRoom(roomId)) {
            data.clearBindingsForRoom(roomId);
            broadcast(level);
            player.displayClientMessage(Component.literal("Mapped room deleted."),
                    true);
        }
    }

    public static FacilityRoom findFloorAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        ResourceLocation dimension = level.dimension().location();
        return FacilityMappingSavedData.get(level.getServer()).rooms().stream()
                .filter(room -> room.dimension().equals(dimension)
                        && room.containsFloor(pos))
                .min(Comparator.comparingLong(FacilityRoom::authoredArea))
                .orElse(null);
    }

    /** A world position belongs to the nearest authored floor beneath its column. */
    public static FacilityRoom roomForPosition(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        ResourceLocation dimension = level.dimension().location();
        return FacilityMappingSavedData.get(level.getServer()).rooms().stream()
                .filter(room -> room.dimension().equals(dimension)
                        && room.containsColumn(pos))
                .min(Comparator.comparingInt(
                        (FacilityRoom room) -> verticalDistance(room, pos))
                        .thenComparingLong(FacilityRoom::authoredArea))
                .orElse(null);
    }

    /**
     * Resolves one camera to exactly one logical room. Manual mappings win,
     * explicit detachment suppresses fallback, and untouched cameras choose the
     * nearest authored floor instead of depending on room iteration order.
     */
    public static FacilityRoom roomForCamera(ServerLevel level,
            FacilityCameraDefinition camera) {
        if (level == null || camera == null) return null;
        FacilityMappingSavedData data = FacilityMappingSavedData.get(
                level.getServer());
        FacilityMappingSavedData.CameraRoomBinding binding =
                data.cameraBinding(camera.id());
        if (binding != null) {
            if (binding.detached()) return null;
            FacilityRoom bound = data.room(binding.roomId());
            return bound != null && bound.dimension().equals(
                    level.dimension().location()) ? bound : null;
        }

        ResourceLocation dimension = level.dimension().location();
        BlockPos eye = BlockPos.containing(camera.eyePosition());
        FacilityRoom contained = data.rooms().stream()
                .filter(room -> room.dimension().equals(dimension)
                        && room.containsColumn(eye))
                .min(Comparator.comparingInt(
                        (FacilityRoom room) -> verticalDistance(room, eye))
                        .thenComparingLong(FacilityRoom::authoredArea))
                .orElse(null);
        if (contained != null) return contained;

        return data.rooms().stream()
                .filter(room -> room.dimension().equals(dimension)
                        && withinExpandedFloor(room, camera.anchorPos(), 1))
                .min(Comparator.comparingInt(
                        (FacilityRoom room) -> verticalDistance(
                                room, camera.anchorPos()))
                        .thenComparingLong(FacilityRoom::authoredArea))
                .orElse(null);
    }

    public static FacilityRoomSnapshot roomSnapshotForCamera(ServerLevel level,
            FacilityCameraDefinition camera) {
        FacilityRoom room = roomForCamera(level, camera);
        return room == null ? null : snapshot(level, room);
    }

    public static List<FacilityCameraMappingSnapshot> cameraMappings(
            ServerLevel level) {
        if (level == null) return List.of();
        FacilityMappingSavedData data = FacilityMappingSavedData.get(
                level.getServer());
        ResourceLocation dimension = level.dimension().location();
        return FacilitySurveillanceSavedData.get(level.getServer()).all().stream()
                .filter(raw -> raw.dimension().equals(dimension))
                .map(raw -> FacilitySurveillanceRegistry.camera(level, raw.id()))
                .filter(camera -> camera != null)
                .map(camera -> {
                    FacilityMappingSavedData.CameraRoomBinding binding =
                            data.cameraBinding(camera.id());
                    FacilityRoom room = roomForCamera(level, camera);
                    boolean manual = binding != null && !binding.detached();
                    boolean detached = binding != null && binding.detached();
                    return new FacilityCameraMappingSnapshot(camera.id(),
                            camera.anchorPos(),
                            room == null ? null : room.id(),
                            manual, detached);
                })
                .sorted(Comparator.comparingLong(snapshot ->
                        snapshot.anchorPos().asLong()))
                .toList();
    }

    public static void cameraRegistryChanged(ServerLevel level) {
        if (level != null) broadcastCameraMappings(level);
    }

    public static void cameraRemoved(ServerLevel level, UUID cameraId) {
        if (level == null || cameraId == null) return;
        FacilityMappingSavedData.get(level.getServer())
                .clearCameraBinding(cameraId);
        broadcastCameraMappings(level);
    }

    public static List<FacilityRoomSnapshot> roomSnapshots(ServerLevel level) {
        if (level == null) return List.of();
        ResourceLocation dimension = level.dimension().location();
        return FacilityMappingSavedData.get(level.getServer()).rooms().stream()
                .filter(room -> room.dimension().equals(dimension))
                .map(room -> snapshot(level, room)).toList();
    }

    public static void sync(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        FacilityMappingNetwork.sendRooms(player, level.dimension().location(),
                roomSnapshots(level));
        FacilityMappingNetwork.sendCameraMappings(player,
                level.dimension().location(), cameraMappings(level));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        Map<UUID, PendingSelection> map = PENDING.get(server);
        if (map != null) map.remove(event.getEntity().getUUID());
        Map<UUID, PendingCameraLink> cameraMap = CAMERA_LINKS.get(server);
        if (cameraMap != null) cameraMap.remove(event.getEntity().getUUID());
    }

    private static void broadcast(ServerLevel level) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.DIMENSION.with(level::dimension),
                FacilityMappingNetwork.roomSync(level.dimension().location(),
                        roomSnapshots(level)));
        broadcastCameraMappings(level);
    }

    private static void broadcastCameraMappings(ServerLevel level) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.DIMENSION.with(level::dimension),
                FacilityMappingNetwork.cameraMappingSync(
                        level.dimension().location(), cameraMappings(level)));
    }

    private static FacilityRoomSnapshot snapshot(ServerLevel level,
            FacilityRoom room) {
        FacilityMappingSavedData.TrackedStation station = room.floorStation()
                == null ? null : FacilityFloorStationIndex.indexed(
                level, room.floorStation());
        String longLabel = station == null || station.longLabel().isBlank()
                ? "Unassigned Floor" : station.longLabel();
        String shortLabel = station == null || station.shortLabel().isBlank()
                ? "UNASSIGNED" : station.shortLabel();
        return new FacilityRoomSnapshot(room.id(), room.dimension(),
                room.patches(), room.name(), room.floorStation(),
                longLabel, shortLabel);
    }

    private static boolean withinExpandedFloor(FacilityRoom room,
            BlockPos pos, int border) {
        if (room == null || pos == null) return false;
        int extra = Math.max(0, border);
        for (FacilityFloorPatch patch : room.patches()) {
            if (pos.getX() < patch.minX() - extra
                    || pos.getX() > patch.maxX() + extra
                    || pos.getZ() < patch.minZ() - extra
                    || pos.getZ() > patch.maxZ() + extra) continue;
            if (pos.getY() >= patch.y() - 1
                    && pos.getY() <= patch.y()
                    + FacilityRoom.CAMERA_COLUMN_HEIGHT) return true;
        }
        return false;
    }

    private static int verticalDistance(FacilityRoom room, BlockPos pos) {
        int best = Integer.MAX_VALUE;
        for (FacilityFloorPatch patch : room.patches()) {
            if (pos.getX() >= patch.minX() && pos.getX() <= patch.maxX()
                    && pos.getZ() >= patch.minZ() && pos.getZ() <= patch.maxZ()) {
                best = Math.min(best, Math.abs(pos.getY() - patch.y()));
            }
        }
        return best;
    }

    private static boolean holdingTool(ServerPlayer player) {
        return player.getMainHandItem().is(FacilityMappingItems.getTool());
    }

    private record PendingSelection(ResourceKey<Level> dimension,
            BlockPos first) {
    }

    private record PendingCameraLink(ResourceKey<Level> dimension,
            UUID cameraId) {
    }
}
