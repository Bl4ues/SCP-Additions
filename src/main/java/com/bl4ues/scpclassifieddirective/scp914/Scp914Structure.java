package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.block.Scp914Block;
import com.bl4ues.scpclassifieddirective.block.Scp914PartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Physical coordinates and semantic multiblock footprint for rebuilt SCP-914. */
public final class Scp914Structure {
    private Scp914Structure() {
    }

    private static final double INTAKE_X = -4.80D;
    private static final double OUTPUT_X = 4.80D;
    private static final double CHAMBER_Y = 1.05D;
    private static final double CHAMBER_Z = 1.50D;
    private static final double CHAMBER_HALF_X = 0.92D;
    private static final double CHAMBER_MIN_Y = 0.12D;
    private static final double CHAMBER_MAX_Y = 2.18D;
    private static final double CHAMBER_MIN_Z = 0.66D;
    private static final double CHAMBER_MAX_Z = 2.34D;

    public static final double DIAL_X = 0.0D;
    public static final double DIAL_Y = 20.04D / 16.0D;
    public static final double DIAL_Z = -8.25D / 16.0D;
    public static final double WIND_KEY_X = 0.0D;
    public static final double WIND_KEY_Y = 14.5D / 16.0D;
    public static final double WIND_KEY_Z = -9.075D / 16.0D;

    public static Direction facing(BlockState state) {
        return state.hasProperty(Scp914Block.FACING)
                ? state.getValue(Scp914Block.FACING) : Direction.NORTH;
    }

    public static Vec3 localToWorld(BlockPos origin, Direction front,
            double localX, double localY, double localZ) {
        Direction rightFromViewer = front.getCounterClockWise();
        Direction back = front.getOpposite();
        return new Vec3(
                origin.getX() + 0.5D
                        + rightFromViewer.getStepX() * localX
                        + back.getStepX() * localZ,
                origin.getY() + localY,
                origin.getZ() + 0.5D
                        + rightFromViewer.getStepZ() * localX
                        + back.getStepZ() * localZ);
    }

    public static Vec3 dialAnchor(BlockPos origin, Direction front) {
        return localToWorld(origin, front, DIAL_X, DIAL_Y, DIAL_Z);
    }

    public static Vec3 windKeyAnchor(BlockPos origin, Direction front) {
        return localToWorld(origin, front, WIND_KEY_X, WIND_KEY_Y, WIND_KEY_Z);
    }

    public static Vec3 machineSoundCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, 0.0D, 1.25D, 1.05D);
    }

    public static Vec3 intakeCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, INTAKE_X, CHAMBER_Y, CHAMBER_Z);
    }

    public static Vec3 outputCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, OUTPUT_X, CHAMBER_Y, CHAMBER_Z);
    }

    public static Vec3 intakeDoorCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, INTAKE_X, 1.20D, 0.42D);
    }

    public static Vec3 outputDoorCenter(BlockPos origin, Direction front) {
        return localToWorld(origin, front, OUTPUT_X, 1.20D, 0.42D);
    }

    public static AABB intakeArea(BlockPos origin, Direction front) {
        return chamberArea(origin, front, INTAKE_X);
    }

    public static AABB outputArea(BlockPos origin, Direction front) {
        return chamberArea(origin, front, OUTPUT_X);
    }

    private static AABB chamberArea(BlockPos origin, Direction front,
            double centerX) {
        Vec3 a = localToWorld(origin, front,
                centerX - CHAMBER_HALF_X, CHAMBER_MIN_Y, CHAMBER_MIN_Z);
        Vec3 b = localToWorld(origin, front,
                centerX + CHAMBER_HALF_X, CHAMBER_MAX_Y, CHAMBER_MAX_Z);
        return new AABB(
                Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
                Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    /** Includes the stepped model footprint plus the six floor cells below each cabin. */
    public static List<BlockPos> requiredCells(BlockPos origin, Direction front) {
        List<BlockPos> result = new ArrayList<>();
        result.add(origin.immutable());
        for (PartPlacement placement : placements(origin, front)) {
            result.add(placement.pos());
        }
        return result;
    }

    public static List<BlockPos> collectObstructions(Level level, BlockPos origin,
            Direction front) {
        List<BlockPos> blocked = new ArrayList<>();
        for (PartPlacement placement : placements(origin, front)) {
            if (!level.getBlockState(placement.pos()).canBeReplaced()) {
                blocked.add(placement.pos().immutable());
            }
        }
        return blocked;
    }

    /** All helpers are preverified before any cell is written. */
    public static void placeHelpers(Level level, BlockPos origin, Direction front) {
        if (level.isClientSide) return;
        List<PartPlacement> expected = placements(origin, front);
        for (PartPlacement placement : expected) {
            if (!level.getBlockState(placement.pos()).canBeReplaced()) return;
        }

        List<BlockPos> placed = new ArrayList<>();
        for (PartPlacement placement : expected) {
            BlockState helper = stateFor(placement, front);
            if (!level.setBlock(placement.pos(), helper, 3)) {
                for (BlockPos pos : placed) {
                    BlockState state = level.getBlockState(pos);
                    if (isHelper(state)) level.setBlock(pos,
                            Blocks.AIR.defaultBlockState(), 3);
                }
                return;
            }
            placed.add(placement.pos());
        }
    }

    public static void clearHelpers(Level level, BlockPos origin, Direction front) {
        if (level.isClientSide) return;
        for (PartPlacement placement : placements(origin, front)) {
            BlockState state = level.getBlockState(placement.pos());
            if (isHelper(state)) {
                level.setBlock(placement.pos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static boolean isDoorCell(BlockPos origin, Direction front,
            BlockPos candidate) {
        for (PartPlacement placement : placements(origin, front)) {
            if (placement.kind() == Scp914PartBlock.Kind.DOOR
                    && placement.pos().equals(candidate)) return true;
        }
        return false;
    }

    private static boolean isHelper(BlockState state) {
        return state.is(Scp914Module.SCP_914_RESERVATION.get())
                || state.is(Scp914Module.SCP_914_COLLISION.get())
                || state.is(Scp914Module.SCP_914_DOOR_COLLISION.get());
    }

    private static BlockState stateFor(PartPlacement placement, Direction front) {
        BlockState state = switch (placement.kind()) {
            case RESERVATION -> Scp914Module.SCP_914_RESERVATION.get().defaultBlockState();
            case SOLID -> Scp914Module.SCP_914_COLLISION.get().defaultBlockState();
            case DOOR -> Scp914Module.SCP_914_DOOR_COLLISION.get().defaultBlockState();
        };
        return state.setValue(Scp914PartBlock.FACING, front)
                .setValue(Scp914PartBlock.ROLE, placement.role());
    }

    private static List<PartPlacement> placements(BlockPos origin, Direction front) {
        Map<BlockPos, PartPlacement> result = new LinkedHashMap<>();

        // Reserve only cells actually covered by the model. The previous implementation
        // reserved one 16x6x3 cuboid, which made a large amount of visibly empty space
        // unusable. The stair-step footprint follows the silhouette of the center body,
        // both chambers and the two angled outer wings.
        addReservationPrism(result, origin, front, -2, 2, -2, 0, 0, 2);
        addReservationPrism(result, origin, front, -3, -3, -1, 0, 0, 2);
        addReservationPrism(result, origin, front, 3, 3, -1, 0, 0, 2);
        addReservationPrism(result, origin, front, -6, -4, -3, 0, 0, 2);
        addReservationPrism(result, origin, front, 4, 6, -3, 0, 0, 2);
        addReservationPrism(result, origin, front, -7, -7, -1, 1, 0, 2);
        addReservationPrism(result, origin, front, 7, 7, -1, 1, 0, 2);
        addReservationPrism(result, origin, front, -8, -8, 0, 2, 0, 2);
        addReservationPrism(result, origin, front, 8, 8, 0, 2, 0, 2);

        for (int side = -2; side <= 2; side++) {
            for (int forward = -2; forward <= -1; forward++) {
                for (int y = 0; y <= 2; y++) {
                    put(result, gridCell(origin, front, side, y, forward),
                            Scp914PartBlock.Kind.SOLID,
                            Scp914PartBlock.Role.BODY);
                }
            }
            for (int y = 0; y <= 2; y++) {
                BlockPos pos = gridCell(origin, front, side, y, 0);
                if (!pos.equals(origin)) put(result, pos,
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.BODY_FRONT);
            }
        }

        addCabin(result, origin, front, -5);
        addCabin(result, origin, front, 5);
        return List.copyOf(result.values());
    }

    private static void addReservationPrism(Map<BlockPos, PartPlacement> result,
            BlockPos origin, Direction front, int sideMin, int sideMax,
            int forwardMin, int forwardMax, int yMin, int yMax) {
        for (int side = sideMin; side <= sideMax; side++) {
            for (int forward = forwardMin; forward <= forwardMax; forward++) {
                for (int y = yMin; y <= yMax; y++) {
                    BlockPos pos = gridCell(origin, front, side, y, forward);
                    if (!pos.equals(origin)) put(result, pos,
                            Scp914PartBlock.Kind.RESERVATION,
                            Scp914PartBlock.Role.RESERVED);
                }
            }
        }
    }

    private static void addCabin(Map<BlockPos, PartPlacement> result,
            BlockPos origin, Direction front, int centerSide) {
        for (int side = centerSide - 1; side <= centerSide + 1; side++) {
            for (int forward = -2; forward <= -1; forward++) {
                put(result, gridCell(origin, front, side, -1, forward),
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.FLOOR);
            }
        }

        for (int wallSide : new int[] {centerSide - 1, centerSide + 1}) {
            for (int forward = -2; forward <= -1; forward++) {
                for (int y = 0; y <= 1; y++) {
                    put(result, gridCell(origin, front, wallSide, y, forward),
                            Scp914PartBlock.Kind.SOLID,
                            Scp914PartBlock.Role.CABIN_SIDE);
                }
            }
        }
        for (int side = centerSide - 1; side <= centerSide + 1; side++) {
            for (int y = 0; y <= 2; y++) {
                put(result, gridCell(origin, front, side, y, -3),
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.CABIN_BACK);
            }
            for (int forward = -2; forward <= -1; forward++) {
                put(result, gridCell(origin, front, side, 2, forward),
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.CABIN_ROOF);
            }
        }

        for (int side = centerSide - 1; side <= centerSide + 1; side++) {
            put(result, gridCell(origin, front, side, 0, 0),
                    Scp914PartBlock.Kind.DOOR, doorRole(side, 0));
            put(result, gridCell(origin, front, side, 1, 0),
                    Scp914PartBlock.Kind.DOOR, doorRole(side, 1));
        }
    }

    private static Scp914PartBlock.Role doorRole(int side, int y) {
        return switch (side) {
            case -6 -> y == 0 ? Scp914PartBlock.Role.DOOR_N6_LOWER
                    : Scp914PartBlock.Role.DOOR_N6_UPPER;
            case -5 -> y == 0 ? Scp914PartBlock.Role.DOOR_N5_LOWER
                    : Scp914PartBlock.Role.DOOR_N5_UPPER;
            case -4 -> y == 0 ? Scp914PartBlock.Role.DOOR_N4_LOWER
                    : Scp914PartBlock.Role.DOOR_N4_UPPER;
            case 4 -> y == 0 ? Scp914PartBlock.Role.DOOR_P4_LOWER
                    : Scp914PartBlock.Role.DOOR_P4_UPPER;
            case 5 -> y == 0 ? Scp914PartBlock.Role.DOOR_P5_LOWER
                    : Scp914PartBlock.Role.DOOR_P5_UPPER;
            case 6 -> y == 0 ? Scp914PartBlock.Role.DOOR_P6_LOWER
                    : Scp914PartBlock.Role.DOOR_P6_UPPER;
            default -> throw new IllegalArgumentException("Invalid SCP-914 door side " + side);
        };
    }

    private static void put(Map<BlockPos, PartPlacement> result, BlockPos pos,
            Scp914PartBlock.Kind kind, Scp914PartBlock.Role role) {
        result.put(pos.immutable(), new PartPlacement(pos.immutable(), kind, role));
    }

    private static BlockPos gridCell(BlockPos origin, Direction front,
            int side, int y, int forward) {
        Direction localPositiveX = front.getCounterClockWise();
        return origin.relative(front, forward)
                .relative(localPositiveX, side)
                .above(y);
    }

    private record PartPlacement(BlockPos pos, Scp914PartBlock.Kind kind,
            Scp914PartBlock.Role role) {
    }
}
