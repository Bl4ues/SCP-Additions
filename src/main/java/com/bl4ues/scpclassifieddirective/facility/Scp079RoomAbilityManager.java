package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative room abilities for playable SCP-079. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079RoomAbilityManager {
    public static final double BLACKOUT_BASE_COST = 40.0D;
    public static final double SURFACE_BLACKOUT_BASE_COST = 80.0D;
    public static final int BLACKOUT_DURATION_TICKS = 200;
    public static final double LOCKDOWN_COST = 100.0D;
    public static final int LOCKDOWN_DURATION_TICKS = 140;
    public static final int LOCKDOWN_COOLDOWN_TICKS = 200;

    private static final Map<MinecraftServer, State> STATES =
            new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Set<LightKey>> SUPPRESSED_LIGHTS =
            new ConcurrentHashMap<>();

    private Scp079RoomAbilityManager() {
    }

    public enum Ability {
        BLACKOUT,
        LOCKDOWN
    }

    public static boolean use(ServerPlayer player, Ability ability) {
        if (player == null || ability == null || player.getServer() == null
                || !Scp079PlayableManager.isController(player)
                || !Scp079PlayableManager.isCameraMode(player)
                || !Scp079FacilityAccessManager.auxiliaryPowerOnline(
                player.getServer())) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        FacilityRoomSnapshot room = currentRoom(level, player.blockPosition());
        if (room == null) return false;
        return switch (ability) {
            case BLACKOUT -> blackout(player, level, room);
            case LOCKDOWN -> lockdown(player, level, room);
        };
    }

    /** SL doubles a single-room Blackout's authored AP cost on Surface. */
    public static double blackoutBaseCost(FacilityRoomSnapshot room) {
        if (room == null) return BLACKOUT_BASE_COST;
        String floor = (room.floorLongLabel() + " " + room.floorShortLabel())
                .toLowerCase(Locale.ROOT);
        return floor.contains("surface")
                ? SURFACE_BLACKOUT_BASE_COST : BLACKOUT_BASE_COST;
    }

    public static boolean isLightSuppressed(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || level.getServer() == null) return false;
        Set<LightKey> suppressed = SUPPRESSED_LIGHTS.get(level.getServer());
        return suppressed != null && suppressed.contains(new LightKey(
                level.dimension(), pos.asLong()));
    }

    /** Lockdown mirrors SL's pre-Tier-5 behavior: AP does not regenerate. */
    public static boolean blocksPowerRegeneration(MinecraftServer server) {
        if (server == null) return false;
        State state = STATES.get(server);
        return state != null && state.lockdownEndsAt > server.getTickCount();
    }

    public static int lockdownCooldownTicks(MinecraftServer server) {
        if (server == null) return 0;
        State state = STATES.get(server);
        return state == null ? 0 : Math.max(0,
                state.nextLockdownAt - server.getTickCount());
    }

    private static boolean blackout(ServerPlayer player, ServerLevel level,
            FacilityRoomSnapshot room) {
        MinecraftServer server = player.getServer();
        State state = STATES.computeIfAbsent(server, ignored -> new State());
        if (state.blackout != null
                && state.blackout.endsAt > server.getTickCount()) {
            return false;
        }

        List<LightTarget> lights = poweredLights(level, room);
        if (lights.isEmpty()) return false;
        double baseCost = blackoutBaseCost(room);
        if (!Scp079PlayerPower.trySpend(level, baseCost)) return false;

        int endsAt = server.getTickCount() + BLACKOUT_DURATION_TICKS;
        ActiveBlackout active = new ActiveBlackout(level.dimension(), room.id(),
                endsAt, List.copyOf(lights));
        state.blackout = active;
        Set<LightKey> suppressed = SUPPRESSED_LIGHTS.computeIfAbsent(server,
                ignored -> ConcurrentHashMap.newKeySet());
        for (LightTarget light : lights) {
            suppressed.add(new LightKey(level.dimension(), light.pos.asLong()));
            forceOff(level, light);
        }
        return true;
    }

    private static boolean lockdown(ServerPlayer player, ServerLevel level,
            FacilityRoomSnapshot room) {
        MinecraftServer server = player.getServer();
        State state = STATES.computeIfAbsent(server, ignored -> new State());
        int now = server.getTickCount();
        if (now < state.nextLockdownAt || state.lockdownEndsAt > now) return false;

        List<BlockPos> doors = roomDoors(level, room);
        if (doors.isEmpty()) return false;
        if (!trySpendExact(level, LOCKDOWN_COST)) return false;

        int affected = 0;
        for (BlockPos door : doors) {
            int closed = HeavyDoorControlPanelAccess.closeConnectedControls(
                    level, door);
            int locked = HeavyDoorControlPanelAccess
                    .temporarilyDenyConnectedControls(level, door,
                            LOCKDOWN_DURATION_TICKS);
            if (closed > 0 || locked > 0) affected++;
        }
        if (affected <= 0) {
            refundExact(level, LOCKDOWN_COST);
            return false;
        }

        state.lockdownEndsAt = now + LOCKDOWN_DURATION_TICKS;
        state.nextLockdownAt = now + LOCKDOWN_COOLDOWN_TICKS;
        return true;
    }

    private static List<LightTarget> poweredLights(ServerLevel level,
            FacilityRoomSnapshot room) {
        List<LightTarget> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (FacilityFloorPatch patch : room.patches()) {
            for (int x = patch.minX(); x <= patch.maxX(); x++) {
                for (int z = patch.minZ(); z <= patch.maxZ(); z++) {
                    for (int y = patch.y() - 1;
                            y <= patch.y() + FacilityRoom.CAMERA_COLUMN_HEIGHT;
                            y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!visited.add(pos.asLong()) || !level.hasChunkAt(pos)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()
                                || state.getLightEmission(level, pos) <= 0) {
                            continue;
                        }
                        boolean powered = level.hasNeighborSignal(pos)
                                || state.hasProperty(BlockStateProperties.POWERED)
                                && state.getValue(BlockStateProperties.POWERED);
                        if (!powered) continue;
                        result.add(new LightTarget(pos.immutable(),
                                state.getBlock()));
                    }
                }
            }
        }
        return result;
    }

    private static List<BlockPos> roomDoors(ServerLevel level,
            FacilityRoomSnapshot room) {
        List<BlockPos> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (FacilityFloorPatch patch : room.patches()) {
            for (int x = patch.minX() - 1; x <= patch.maxX() + 1; x++) {
                for (int z = patch.minZ() - 1; z <= patch.maxZ() + 1; z++) {
                    for (int y = patch.y() - 1;
                            y <= patch.y() + FacilityRoom.CAMERA_COLUMN_HEIGHT;
                            y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!visited.add(pos.asLong()) || !level.hasChunkAt(pos)) {
                            continue;
                        }
                        if (!FacilityModule.isFacilityDoor(
                                level.getBlockState(pos))) continue;
                        if (!HeavyDoorControlPanelAccess.hasControllableInterface(
                                level, pos)) continue;
                        boolean duplicate = result.stream().anyMatch(existing ->
                                existing.distSqr(pos) <= 4.0D);
                        if (!duplicate) result.add(pos.immutable());
                    }
                }
            }
        }
        return result;
    }

    private static FacilityRoomSnapshot currentRoom(ServerLevel level,
            BlockPos pos) {
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

    private static void forceOff(ServerLevel level, LightTarget target) {
        if (!level.hasChunkAt(target.pos)) return;
        BlockState state = level.getBlockState(target.pos);
        if (state.getBlock() != target.block) return;
        BlockState off = state;
        if (off.hasProperty(BlockStateProperties.LIT)
                && off.getValue(BlockStateProperties.LIT)) {
            off = off.setValue(BlockStateProperties.LIT, false);
        }
        if (off.hasProperty(BlockStateProperties.POWERED)
                && off.getValue(BlockStateProperties.POWERED)) {
            off = off.setValue(BlockStateProperties.POWERED, false);
        }
        if (off != state) {
            level.setBlock(target.pos, off, Block.UPDATE_CLIENTS);
        }
    }

    private static void restoreLight(ServerLevel level, LightTarget target) {
        if (!level.hasChunkAt(target.pos)) return;
        BlockState state = level.getBlockState(target.pos);
        if (state.getBlock() != target.block) return;
        state.getBlock().neighborChanged(state, level, target.pos,
                state.getBlock(), target.pos, false);
        level.scheduleTick(target.pos, state.getBlock(), 1);
        level.updateNeighborsAt(target.pos, state.getBlock());
    }

    private static boolean trySpendExact(ServerLevel level, double cost) {
        if (level == null || cost < 0.0D
                || !Scp079PlayableManager.hasController(level.getServer())
                || !Scp079ProcessingManager.isActive(level)) return false;
        double current = Scp079ProcessingManager.getPower(level);
        if (current + 0.0001D < cost) return false;
        Scp079ProcessingSavedData.get(level.getServer())
                .setPower(current - cost);
        Scp079ScreenState.pulse(level.getServer());
        return true;
    }

    private static void refundExact(ServerLevel level, double amount) {
        if (level == null || amount <= 0.0D) return;
        Scp079ProcessingSavedData data = Scp079ProcessingSavedData.get(
                level.getServer());
        data.setPower(Math.min(Scp079ProcessingManager.MAX_POWER,
                Scp079ProcessingManager.getPower(level) + amount));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        State state = STATES.get(server);
        if (state == null) return;
        int now = server.getTickCount();

        ActiveBlackout blackout = state.blackout;
        if (blackout != null) {
            ServerLevel level = server.getLevel(blackout.dimension);
            if (level == null || now >= blackout.endsAt) {
                endBlackout(server, level, blackout);
                state.blackout = null;
            } else {
                for (LightTarget light : blackout.lights) {
                    forceOff(level, light);
                }
            }
        }
        if (state.lockdownEndsAt <= now) state.lockdownEndsAt = 0;
    }

    private static void endBlackout(MinecraftServer server, ServerLevel level,
            ActiveBlackout blackout) {
        Set<LightKey> suppressed = SUPPRESSED_LIGHTS.get(server);
        for (LightTarget light : blackout.lights) {
            if (suppressed != null) {
                suppressed.remove(new LightKey(blackout.dimension,
                        light.pos.asLong()));
            }
            if (level != null) restoreLight(level, light);
        }
        if (suppressed != null && suppressed.isEmpty()) {
            SUPPRESSED_LIGHTS.remove(server, suppressed);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.remove(event.getServer());
        SUPPRESSED_LIGHTS.remove(event.getServer());
    }

    private static final class State {
        private ActiveBlackout blackout;
        private int lockdownEndsAt;
        private int nextLockdownAt;
    }

    private record ActiveBlackout(ResourceKey<Level> dimension, UUID roomId,
            int endsAt, List<LightTarget> lights) {
    }

    private record LightTarget(BlockPos pos, Block block) {
    }

    private record LightKey(ResourceKey<Level> dimension, long pos) {
    }
}
