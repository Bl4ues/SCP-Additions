package com.bl4ues.scpclassifieddirective.facility.elevator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/** Discovers vertical elevator columns and keeps their generated pieces coherent. */
public final class CoreRoomElevatorManager {
    private static final ThreadLocal<Boolean> REBUILDING =
            ThreadLocal.withInitial(() -> false);

    private CoreRoomElevatorManager() {
    }

    static boolean isRebuilding() {
        return REBUILDING.get();
    }

    public record ColumnLayout(int x, int z, Direction facing,
            List<BlockPos> stations, @Nullable BlockPos pulley,
            boolean unobstructed) {
        public ColumnLayout {
            stations = List.copyOf(stations);
            pulley = pulley == null ? null : pulley.immutable();
        }

        public boolean complete() {
            return !stations.isEmpty() && pulley != null && unobstructed;
        }

        public int[] floorHeights() {
            return stations.stream().mapToInt(BlockPos::getY).toArray();
        }

        public OptionalInt stationIndex(BlockPos stationPos) {
            for (int i = 0; i < stations.size(); i++) {
                if (stations.get(i).equals(stationPos)) {
                    return OptionalInt.of(i);
                }
            }
            return OptionalInt.empty();
        }
    }

    @Nullable
    public static Direction findPulleyFacing(Level level, BlockPos pulleyPos) {
        Direction facing = null;
        boolean found = false;
        for (int y = level.getMinBuildHeight();
                y < level.getMaxBuildHeight(); y++) {
            BlockPos candidate = new BlockPos(pulleyPos.getX(), y,
                    pulleyPos.getZ());
            BlockState state = level.getBlockState(candidate);
            if (!state.is(CoreRoomElevatorModule.STATION.get())) continue;
            if (y >= pulleyPos.getY()) return null;
            Direction stationFacing = state.getValue(
                    CoreRoomElevatorModule.FACING);
            if (facing == null) facing = stationFacing;
            else if (facing != stationFacing) return null;
            found = true;
        }
        return found ? facing : null;
    }

    public static BlockPos findStationSnap(Level level, BlockPos desired,
            Direction facing) {
        BlockPos best = desired;
        int bestDistance = Integer.MAX_VALUE;
        boolean ambiguous = false;
        Set<BlockPos> columns = new HashSet<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                int columnX = desired.getX() + dx;
                int columnZ = desired.getZ() + dz;
                for (int y = level.getMinBuildHeight();
                        y < level.getMaxBuildHeight(); y++) {
                    BlockPos candidate = new BlockPos(columnX, y, columnZ);
                    BlockState state = level.getBlockState(candidate);
                    if (!state.is(CoreRoomElevatorModule.STATION.get())
                            || state.getValue(CoreRoomElevatorModule.FACING)
                            != facing) continue;
                    int spacing = Math.abs(y - desired.getY());
                    if (spacing < CoreRoomElevatorModule.MIN_FLOOR_SPACING
                            || spacing > CoreRoomElevatorModule.MAX_FLOOR_SPACING) {
                        continue;
                    }
                    BlockPos column = new BlockPos(columnX, desired.getY(),
                            columnZ);
                    if (!columns.add(column)) continue;
                    int distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        best = column;
                        bestDistance = distance;
                        ambiguous = false;
                    } else if (distance == bestDistance && !best.equals(column)) {
                        ambiguous = true;
                    }
                }
            }
        }
        return ambiguous ? desired : best;
    }

    public static boolean isValidStationPlacement(Level level, BlockPos pos,
            Direction facing) {
        List<Integer> heights = new ArrayList<>();
        heights.add(pos.getY());
        for (int y = level.getMinBuildHeight();
                y < level.getMaxBuildHeight(); y++) {
            if (y == pos.getY()) continue;
            BlockState state = level.getBlockState(new BlockPos(
                    pos.getX(), y, pos.getZ()));
            if (!state.is(CoreRoomElevatorModule.STATION.get())) continue;
            if (state.getValue(CoreRoomElevatorModule.FACING) != facing) {
                return false;
            }
            heights.add(y);
        }
        heights.sort(Integer::compareTo);
        for (int index = 0; index + 1 < heights.size(); index++) {
            int spacing = heights.get(index + 1) - heights.get(index);
            if (spacing < CoreRoomElevatorModule.MIN_FLOOR_SPACING
                    || spacing > CoreRoomElevatorModule.MAX_FLOOR_SPACING) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasStationInDirection(Level level,
            BlockPos stationPos, ElevatorFoundation.TravelDirection direction) {
        if (direction == ElevatorFoundation.TravelDirection.NONE) return false;
        BlockState origin = level.getBlockState(stationPos);
        if (!origin.is(CoreRoomElevatorModule.STATION.get())) return false;
        Direction facing = origin.getValue(CoreRoomElevatorModule.FACING);
        int sign = direction.step();
        for (int distance = CoreRoomElevatorModule.MIN_FLOOR_SPACING;
                distance <= CoreRoomElevatorModule.MAX_FLOOR_SPACING;
                distance++) {
            BlockState candidate = level.getBlockState(stationPos.offset(
                    0, sign * distance, 0));
            if (candidate.is(CoreRoomElevatorModule.STATION.get())
                    && candidate.getValue(CoreRoomElevatorModule.FACING)
                    == facing) return true;
        }
        return false;
    }

    public static ColumnLayout discover(ServerLevel level, int x, int z) {
        List<BlockPos> stations = new ArrayList<>();
        List<BlockPos> pulleys = new ArrayList<>();
        Direction facing = null;
        boolean orientationValid = true;
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(CoreRoomElevatorModule.STATION.get())) {
                Direction found = state.getValue(CoreRoomElevatorModule.FACING);
                if (facing == null) facing = found;
                else if (facing != found) orientationValid = false;
                stations.add(pos);
            } else if (state.is(CoreRoomElevatorModule.PULLEY.get())) {
                Direction found = state.getValue(CoreRoomElevatorModule.FACING);
                if (facing == null) facing = found;
                else if (facing != found) orientationValid = false;
                pulleys.add(pos);
            }
        }
        stations.sort(Comparator.comparingInt(BlockPos::getY));
        pulleys.sort(Comparator.comparingInt(BlockPos::getY));
        for (int index = 0; index + 1 < stations.size(); index++) {
            int spacing = stations.get(index + 1).getY()
                    - stations.get(index).getY();
            if (spacing < CoreRoomElevatorModule.MIN_FLOOR_SPACING
                    || spacing > CoreRoomElevatorModule.MAX_FLOOR_SPACING) {
                orientationValid = false;
            }
        }
        if (facing == null) facing = Direction.NORTH;

        BlockPos pulley = null;
        if (!stations.isEmpty()) {
            int minimumPulleyY = stations.get(stations.size() - 1).getY()
                    + CoreRoomElevatorModule.STATION_HEIGHT_BLOCKS;
            for (BlockPos candidate : pulleys) {
                if (candidate.getY() >= minimumPulleyY) {
                    pulley = candidate;
                    break;
                }
            }
        }
        return new ColumnLayout(x, z, facing, stations, pulley,
                orientationValid);
    }

    public static void rebuildColumn(ServerLevel level, BlockPos reference,
            @Nullable Entity notifier) {
        if (Boolean.TRUE.equals(REBUILDING.get())) return;
        REBUILDING.set(true);
        try {
            int x = reference.getX();
            int z = reference.getZ();
            removeGeneratedBeams(level, x, z);
            ColumnLayout discovered = discover(level, x, z);
            boolean unobstructed = discovered.unobstructed();
            if (!discovered.stations().isEmpty()) {
                List<BlockPos> stations = discovered.stations();
                for (int i = 0; i + 1 < stations.size(); i++) {
                    BlockPos lower = stations.get(i);
                    BlockPos upper = stations.get(i + 1);
                    int start = lower.getY()
                            + CoreRoomElevatorModule.STATION_HEIGHT_BLOCKS;
                    int end = upper.getY();
                    if (end < start) unobstructed = false;
                    unobstructed &= fillBeams(level, x, z, start, end,
                            discovered.facing());
                }
                if (discovered.pulley() != null) {
                    BlockPos highest = stations.get(stations.size() - 1);
                    int start = highest.getY()
                            + CoreRoomElevatorModule.STATION_HEIGHT_BLOCKS;
                    int end = discovered.pulley().getY();
                    if (end < start) unobstructed = false;
                    unobstructed &= fillBeams(level, x, z, start, end,
                            discovered.facing());
                }
            }

            ColumnLayout layout = new ColumnLayout(x, z, discovered.facing(),
                    discovered.stations(), discovered.pulley(), unobstructed);
            synchronizeCarriage(level, layout);
            if (notifier instanceof ServerPlayer player) {
                reportLayout(player, layout);
            }
        } finally {
            REBUILDING.set(false);
        }
    }

    private static void reportLayout(ServerPlayer player, ColumnLayout layout) {
        if (layout.stations().isEmpty()) return;
        if (layout.pulley() == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.scp_classified_directive.elevator_needs_pulley")
                    .withStyle(ChatFormatting.YELLOW));
        } else if (!layout.unobstructed()) {
            player.sendSystemMessage(Component.translatable(
                    "message.scp_classified_directive.elevator_path_obstructed")
                    .withStyle(ChatFormatting.RED));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.scp_classified_directive.elevator_linked",
                    layout.stations().size())
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    private static void removeGeneratedBeams(ServerLevel level, int x, int z) {
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(CoreRoomElevatorModule.BEAMS.get())
                    && state.getValue(CoreRoomElevatorModule.GENERATED)) {
                level.removeBlock(pos, false);
            }
        }
    }

    private static boolean fillBeams(ServerLevel level, int x, int z,
            int startY, int endY, Direction facing) {
        boolean clear = true;
        for (int y = startY; y < endY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(CoreRoomElevatorModule.BEAMS.get())) {
                boolean generated = state.getValue(CoreRoomElevatorModule.GENERATED);
                if (!generated && state.getValue(CoreRoomElevatorModule.FACING) != facing) {
                    clear = false;
                    continue;
                }
                if (generated && state.getValue(CoreRoomElevatorModule.FACING) != facing) {
                    level.setBlock(pos, state.setValue(
                            CoreRoomElevatorModule.FACING, facing), 3);
                }
                if (!CoreRoomElevatorModule.placeBeamParts(level, pos, facing,
                        null)) {
                    clear = false;
                }
                continue;
            }
            if (!(state.isAir() || state.canBeReplaced())) {
                clear = false;
                continue;
            }
            level.setBlock(pos, CoreRoomElevatorModule.BEAMS.get()
                    .defaultBlockState()
                    .setValue(CoreRoomElevatorModule.FACING, facing)
                    .setValue(CoreRoomElevatorModule.GENERATED, true), 3);
            if (!CoreRoomElevatorModule.placeBeamParts(level, pos, facing,
                    null)) {
                level.removeBlock(pos, false);
                clear = false;
            }
        }
        return clear;
    }

    private static AABB columnEntityArea(ServerLevel level, int x, int z) {
        return new AABB(x - 2.5D, level.getMinBuildHeight(), z - 2.5D,
                x + 3.5D, level.getMaxBuildHeight(), z + 3.5D);
    }

    public static List<CoreRoomElevatorCarriageEntity> findCarriages(
            ServerLevel level, int x, int z) {
        return level.getEntitiesOfClass(CoreRoomElevatorCarriageEntity.class,
                columnEntityArea(level, x, z),
                carriage -> carriage.matchesColumn(x, z));
    }

    @Nullable
    public static CoreRoomElevatorCarriageEntity findCarriage(
            ServerLevel level, int x, int z) {
        List<CoreRoomElevatorCarriageEntity> found = findCarriages(level, x, z);
        return found.isEmpty() ? null : found.get(0);
    }

    private static void synchronizeCarriage(ServerLevel level,
            ColumnLayout layout) {
        List<CoreRoomElevatorCarriageEntity> existing = findCarriages(level,
                layout.x(), layout.z());
        if (!layout.complete()) {
            existing.forEach(entity -> entity.discard());
            return;
        }

        CoreRoomElevatorCarriageEntity carriage;
        if (existing.isEmpty()) {
            carriage = CoreRoomElevatorModule.CARRIAGE.get().create(level);
            if (carriage == null) return;
            BlockPos first = layout.stations().get(0);
            carriage.setPos(layout.x() + 0.5D, first.getY(),
                    layout.z() + 0.5D);
            level.addFreshEntity(carriage);
        } else {
            carriage = existing.get(0);
            for (int i = 1; i < existing.size(); i++) {
                existing.get(i).discard();
            }
        }
        carriage.applyLayout(layout);
    }

    public static boolean requestFromStation(ServerLevel level,
            BlockPos stationPos, ElevatorFoundation.TravelDirection direction,
            ServerPlayer player) {
        ColumnLayout layout = discover(level, stationPos.getX(),
                stationPos.getZ());
        CoreRoomElevatorCarriageEntity carriage = findCarriage(level,
                stationPos.getX(), stationPos.getZ());
        OptionalInt stationIndex = layout.stationIndex(stationPos);
        if (!layout.complete() || carriage == null || stationIndex.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.scp_classified_directive.elevator_incomplete")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        int requestedFloor = stationIndex.getAsInt();
        if (carriage.isAtFloorHeight(stationPos.getY())) {
            return false;
        }
        return carriage.requestFromStation(requestedFloor, direction, player);
    }

    public static CoreRoomElevatorModule.DoorVisualState visualStateForStation(
            ServerLevel level, BlockPos stationPos) {
        CoreRoomElevatorCarriageEntity carriage = findCarriage(level,
                stationPos.getX(), stationPos.getZ());
        if (carriage == null || !carriage.isAtFloorHeight(stationPos.getY())) {
            return CoreRoomElevatorModule.DoorVisualState.CLOSED;
        }
        return switch (carriage.phase()) {
            case IDLE_OPEN -> CoreRoomElevatorModule.DoorVisualState.OPEN;
            case DOOR_OPENING -> CoreRoomElevatorModule.DoorVisualState.OPENING;
            case DOOR_CLOSING -> CoreRoomElevatorModule.DoorVisualState.OPEN;
            case STATION_CLOSING -> CoreRoomElevatorModule.DoorVisualState.CLOSING;
            default -> CoreRoomElevatorModule.DoorVisualState.CLOSED;
        };
    }

    public static boolean isColumnStillValid(ServerLevel level,
            CoreRoomElevatorCarriageEntity carriage) {
        ColumnLayout layout = discover(level, carriage.columnX(),
                carriage.columnZ());
        if (!layout.complete()) return false;
        if (layout.pulley() == null
                || !layout.pulley().equals(carriage.controllerPos())) return false;
        Set<Integer> expected = new HashSet<>();
        layout.stations().forEach(pos -> expected.add(pos.getY()));
        for (int floor : carriage.floorHeights()) {
            if (!expected.contains(floor)) return false;
        }
        return expected.size() == carriage.floorHeights().length;
    }
}
