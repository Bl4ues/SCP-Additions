package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.network.Scp079ActivityPingNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts sparse facility-device activity into short visual map hints for a
 * human SCP-079 operator. It deliberately reports interactions, not movement,
 * so the map never becomes a live personnel radar.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079ActivityPingManager {
    private static final long SAME_DEVICE_DEBOUNCE_TICKS = 8L;
    private static final Map<DeviceKey, Long> LAST_PING =
            new ConcurrentHashMap<>();

    private Scp079ActivityPingManager() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (isFacilityActivityDevice(level.getBlockState(pos))) {
            emit(level, pos);
        }
    }

    /**
     * Covers redstone/mob/automation-driven activations that do not pass through
     * a player right-click. Debouncing prevents animated doors and reader chains
     * from creating a storm of rings for one physical action.
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (isFacilityActivityDevice(event.getState())) {
            emit(level, event.getPos());
        }
    }

    public static void emit(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || level.getServer() == null) return;
        ServerPlayer operator = Scp079PlayableManager.controller(level.getServer());
        if (operator == null || !Scp079PlayableManager.isController(operator)) return;

        FacilityRoomSnapshot room = roomAt(level, pos);
        if (room == null) return;
        long now = level.getGameTime();
        DeviceKey key = new DeviceKey(level.dimension().location(), pos.asLong());
        Long previous = LAST_PING.put(key, now);
        if (previous != null && now - previous < SAME_DEVICE_DEBOUNCE_TICKS) {
            return;
        }

        Scp079ActivityPingNetwork.send(operator, level.dimension().location(),
                room.id(), pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    private static FacilityRoomSnapshot roomAt(ServerLevel level, BlockPos pos) {
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (room.containsColumn(pos)) return room;
        }
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (Scp079RoomInteractionPolicy.withinExpandedFloor(room, pos, 1)) {
                return room;
            }
        }
        return null;
    }

    private static boolean isFacilityActivityDevice(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (FacilityModule.isFacilityDoor(state)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(
                id.getNamespace())) return false;
        String path = id.getPath();
        return path.contains("button")
                || path.contains("reader")
                || path.contains("terminal")
                || path.contains("elevator")
                || path.contains("tesla")
                || path.contains("decon")
                || path.contains("speaker")
                || path.contains("camera")
                || path.contains("checkpoint");
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LAST_PING.clear();
    }

    private record DeviceKey(ResourceLocation dimension, long pos) {
    }
}
