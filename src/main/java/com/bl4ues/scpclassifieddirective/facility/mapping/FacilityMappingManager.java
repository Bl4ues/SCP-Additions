package com.bl4ues.scpclassifieddirective.facility.mapping;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityMappingNetwork;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
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

    private FacilityMappingManager() {
    }

    public static boolean canEdit(ServerPlayer player) {
        return player != null && player.isCreative();
    }

    public static void beginSelection(ServerPlayer player, BlockPos pos) {
        if (!canEdit(player) || pos == null || !holdingTool(player)) return;
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
                    List.of(patch), "Unnamed Room", station);
        } else {
            target = overlapping.get(0).withAddedPatch(patch);
            for (int index = 1; index < overlapping.size(); index++) {
                FacilityRoom merged = overlapping.get(index);
                target = target.merge(merged);
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
        boolean removed = map != null && map.remove(player.getUUID()) != null;
        FacilityMappingNetwork.sendSelectionState(player, null);
        player.displayClientMessage(Component.literal(removed
                ? "Room-floor selection cancelled."
                : "No room-floor selection is active."), true);
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

    /** A future camera belongs to the authored room floor beneath its column. */
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
    }

    private static void broadcast(ServerLevel level) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.DIMENSION.with(level::dimension),
                FacilityMappingNetwork.roomSync(level.dimension().location(),
                        roomSnapshots(level)));
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
}
