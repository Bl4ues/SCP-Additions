package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.DecontaminationStructure;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.network.Scp079ActivityPingNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
        if (isFacilityActivityDevice(level, pos, level.getBlockState(pos))) {
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
        if (isFacilityActivityDevice(level, event.getPos(), event.getState())) {
            emit(level, event.getPos());
        }
    }

    public static void emit(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        emitAt(level, pos, pos, pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    /**
     * Reports an actual decontamination cycle at the physical centre of the
     * chamber model. The checkpoint's two permanently powered doors are
     * intentionally excluded from generic activity pings.
     */
    public static void emitDecontaminationCycle(ServerLevel level,
            BlockPos controllerPos, Direction facing) {
        if (level == null || controllerPos == null || facing == null) return;
        Vec3 center = DecontaminationStructure.chamberBox(
                controllerPos, facing).getCenter();
        emitAt(level, BlockPos.containing(center), controllerPos,
                center.x, center.z);
    }

    private static void emitAt(ServerLevel level, BlockPos roomProbe,
            BlockPos debouncePos, double x, double z) {
        if (level == null || roomProbe == null || debouncePos == null
                || level.getServer() == null) {
            return;
        }
        ServerPlayer operator = Scp079PlayableManager.controller(level.getServer());
        if (operator == null || !Scp079PlayableManager.isController(operator)) return;

        FacilityRoomSnapshot room = roomAt(level, roomProbe);
        if (room == null) return;
        long now = level.getGameTime();
        DeviceKey key = new DeviceKey(level.dimension().location(),
                debouncePos.asLong());
        Long previous = LAST_PING.put(key, now);
        if (previous != null && now - previous < SAME_DEVICE_DEBOUNCE_TICKS) {
            return;
        }

        Scp079ActivityPingNetwork.send(operator, level.dimension().location(),
                room.id(), x, z);
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

    private static boolean isFacilityActivityDevice(ServerLevel level,
            BlockPos pos, BlockState state) {
        if (state == null || state.isAir()) return false;

        // Decontamination is one composite machine. Its entrance/exit doors are
        // deliberately held open by redstone while idle, so their neighbour
        // updates are not meaningful activity. The machine emits one explicit
        // ping only when a player actually starts a cycle.
        if (DecontaminationStructure.isController(state)
                || DecontaminationStructure.isOwnedDoor(level, pos, state)) {
            return false;
        }

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && ScpClassifiedDirectiveMod.MODID.equals(
                id.getNamespace())) {
            String path = id.getPath();
            if (path.contains("decon") || path.contains("decontamination")) {
                return false;
            }
        }

        if (FacilityModule.isFacilityDoor(state)) return true;
        if (id == null || !ScpClassifiedDirectiveMod.MODID.equals(
                id.getNamespace())) return false;
        String path = id.getPath();
        return path.contains("button")
                || path.contains("reader")
                || path.contains("terminal")
                || path.contains("elevator")
                || path.contains("tesla")
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
