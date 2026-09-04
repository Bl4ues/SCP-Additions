package com.bl4ues.scpclassifieddirective.safezone;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.safezone.network.SafeZoneNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server authority for selection, persistence, editing and zone queries. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SafeZoneManager {
    public static final long MAX_SELECTION_VOLUME = 262_144L;
    private static final int MAX_SELECTION_SPAN = 256;
    private static final Map<MinecraftServer, Map<UUID, PendingSelection>>
            PENDING_SELECTIONS = new WeakHashMap<>();

    private SafeZoneManager() {
    }

    public static boolean canEdit(ServerPlayer player) {
        return player != null && player.isCreative()
                && player.canUseGameMasterBlocks();
    }

    public static void beginSelection(ServerPlayer player, BlockPos pos) {
        if (!canEdit(player) || pos == null
                || !holdingTool(player)) return;
        pending(player.getServer()).put(player.getUUID(),
                new PendingSelection(player.level().dimension(),
                        pos.immutable()));
        SafeZoneNetwork.sendSelectionState(player, pos);
        player.displayClientMessage(Component.literal(
                "Safe Zone first corner selected. Right-click the opposite corner."),
                true);
    }

    public static void completeSelection(ServerPlayer player, BlockPos pos) {
        if (!canEdit(player) || pos == null) return;
        PendingSelection pending = pending(player.getServer())
                .remove(player.getUUID());
        if (pending == null || !pending.dimension()
                .equals(player.level().dimension())) {
            SafeZoneNetwork.sendSelectionState(player, null);
            player.displayClientMessage(Component.literal(
                    "No Safe Zone selection is active."), true);
            return;
        }

        BlockPos min = minimum(pending.first(), pos);
        BlockPos max = maximum(pending.first(), pos);
        long volume = volume(min, max);
        if (volume <= 0L || volume > MAX_SELECTION_VOLUME
                || span(min.getX(), max.getX()) > MAX_SELECTION_SPAN
                || span(min.getY(), max.getY()) > MAX_SELECTION_SPAN
                || span(min.getZ(), max.getZ()) > MAX_SELECTION_SPAN) {
            SafeZoneNetwork.sendSelectionState(player, null);
            player.displayClientMessage(Component.literal(
                    "Safe Zone is too large. Maximum: 256 blocks per axis and 262,144 blocks total."),
                    true);
            return;
        }

        if (!(player.level() instanceof ServerLevel level)) return;
        SafeZoneTrack.Detection detection = SafeZoneTrack.detect(level,
                min, max);
        String automatic = detection.track() == null
                ? "" : detection.track().id();
        SafeZone zone = new SafeZone(UUID.randomUUID(),
                level.dimension().location(), min, max,
                detection.track() != null, "",
                automatic);
        SafeZoneSavedData.get(level.getServer()).put(zone);
        SafeZoneNetwork.sendSelectionState(player, null);
        broadcast(level);

        Component feedback = detection.track() == null
                ? Component.literal("Safe Zone created.")
                : Component.literal("Safe Zone created with "
                        + detection.track().displayName() + ".");
        player.displayClientMessage(feedback, true);
    }

    public static void cancelSelection(ServerPlayer player) {
        if (!canEdit(player)) return;
        boolean removed = pending(player.getServer())
                .remove(player.getUUID()) != null;
        SafeZoneNetwork.sendSelectionState(player, null);
        player.displayClientMessage(Component.literal(removed
                ? "Safe Zone selection cancelled."
                : "No Safe Zone selection is active."), true);
    }

    public static boolean openEditor(ServerPlayer player, BlockPos pos) {
        if (!canEdit(player) || pos == null
                || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        SafeZone zone = findAt(level, pos);
        if (zone == null) {
            player.displayClientMessage(Component.literal(
                    "No Safe Zone exists at this position."), true);
            return false;
        }

        SafeZoneTrack.Detection detection = SafeZoneTrack.detect(level,
                zone.min(), zone.max());
        String detectedId = detection.track() == null
                ? "" : detection.track().id();
        boolean newlyDetected = !zone.hasAutomaticTrack()
                && !detectedId.isEmpty();
        SafeZone refreshed = zone.withAutomaticTrack(detectedId,
                newlyDetected);
        if (!refreshed.equals(zone)) {
            SafeZoneSavedData.get(level.getServer()).put(refreshed);
            broadcast(level);
        }
        SafeZoneNetwork.openEditor(player, refreshed);
        return true;
    }

    public static void update(ServerPlayer player, UUID id,
            boolean musicEnabled, String trackId) {
        if (!canEdit(player) || id == null
                || !(player.level() instanceof ServerLevel level)) return;
        SafeZoneSavedData data = SafeZoneSavedData.get(level.getServer());
        SafeZone zone = data.get(id);
        if (zone == null || !zone.isIn(level.dimension())) return;
        data.put(zone.withMusic(musicEnabled, trackId));
        broadcast(level);
        player.displayClientMessage(Component.literal("Safe Zone updated."),
                true);
    }

    public static void delete(ServerPlayer player, UUID id) {
        if (!canEdit(player) || id == null
                || !(player.level() instanceof ServerLevel level)) return;
        SafeZoneSavedData data = SafeZoneSavedData.get(level.getServer());
        SafeZone zone = data.get(id);
        if (zone == null || !zone.isIn(level.dimension())) return;
        if (data.delete(id)) {
            broadcast(level);
            player.displayClientMessage(Component.literal("Safe Zone deleted."),
                    true);
        }
    }

    public static boolean isInside(Entity entity) {
        if (entity == null || entity.level().isClientSide
                || !(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        return intersects(level, entity.getBoundingBox());
    }

    public static boolean isInside(Player player) {
        return player != null && isInside((Entity) player);
    }

    public static boolean contains(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        for (SafeZone zone : zones(level)) {
            if (zone.contains(level.dimension(), pos)) return true;
        }
        return false;
    }

    public static boolean intersects(ServerLevel level, AABB box) {
        if (level == null || box == null) return false;
        return findIntersecting(level, box) != null;
    }

    public static SafeZone findIntersecting(ServerLevel level, AABB box) {
        if (level == null || box == null) return null;
        return zones(level).stream()
                .filter(zone -> zone.intersects(level.dimension(), box))
                .min(Comparator.comparingLong(SafeZone::volume)
                        .thenComparing(zone -> zone.id().toString()))
                .orElse(null);
    }

    public static SafeZone findAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        return zones(level).stream()
                .filter(zone -> zone.contains(level.dimension(), pos))
                .min(Comparator.comparingLong(SafeZone::volume)
                        .thenComparing(zone -> zone.id().toString()))
                .orElse(null);
    }

    public static List<SafeZone> zones(ServerLevel level) {
        if (level == null) return List.of();
        ResourceKey<Level> dimension = level.dimension();
        List<SafeZone> zones = new ArrayList<>();
        for (SafeZone zone : SafeZoneSavedData.get(level.getServer()).all()) {
            if (zone.isIn(dimension)) zones.add(zone);
        }
        return List.copyOf(zones);
    }

    public static void sync(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        SafeZoneNetwork.sendZones(player, level.dimension().location(),
                zones(level));
    }

    private static void broadcast(ServerLevel level) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.DIMENSION.with(level::dimension),
                SafeZoneNetwork.zoneSync(level.dimension().location(),
                        zones(level)));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        Map<UUID, PendingSelection> selections = PENDING_SELECTIONS.get(server);
        if (selections != null) selections.remove(event.getEntity().getUUID());
    }

    private static boolean holdingTool(ServerPlayer player) {
        return player.getMainHandItem().is(
                ScpClassifiedDirectiveModItems.SAFE_ZONE_TOOL.get());
    }

    private static Map<UUID, PendingSelection> pending(MinecraftServer server) {
        return PENDING_SELECTIONS.computeIfAbsent(server,
                ignored -> new HashMap<>());
    }

    private static BlockPos minimum(BlockPos first, BlockPos second) {
        return new BlockPos(Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()));
    }

    private static BlockPos maximum(BlockPos first, BlockPos second) {
        return new BlockPos(Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()));
    }

    private static long volume(BlockPos min, BlockPos max) {
        return (long) span(min.getX(), max.getX())
                * span(min.getY(), max.getY())
                * span(min.getZ(), max.getZ());
    }

    private static int span(int min, int max) {
        return max - min + 1;
    }

    private record PendingSelection(ResourceKey<Level> dimension,
            BlockPos first) {
    }
}
