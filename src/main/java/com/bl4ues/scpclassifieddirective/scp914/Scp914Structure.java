package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.block.Scp914Block;
import com.bl4ues.scpclassifieddirective.block.Scp914PartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Physical coordinates, footprint, ownership and teardown for rebuilt SCP-914. */
public final class Scp914Structure {
    private static final int MIN_SIDE = -6;
    private static final int MAX_SIDE = 6;
    private static final int MIN_FORWARD = -3;
    private static final int MAX_FORWARD = 2;
    private static final int MAX_HEIGHT = 2;

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

    private Scp914Structure() {
    }

    public static Direction facing(BlockState state) {
        return state.hasProperty(Scp914Block.FACING)
                ? state.getValue(Scp914Block.FACING) : Direction.NORTH;
    }

    public static boolean isController(BlockState state) {
        return state.is(Scp914Module.SCP_914.get());
    }

    public static boolean isHelper(BlockState state) {
        return state.is(Scp914Module.SCP_914_RESERVATION.get())
                || state.is(Scp914Module.SCP_914_COLLISION.get())
                || state.is(Scp914Module.SCP_914_DOOR_COLLISION.get());
    }

    public static Vec3 localToWorld(BlockPos origin, Direction front,
            double localX, double localY, double localZ) {
        Direction localPositiveX = front.getCounterClockWise();
        Direction back = front.getOpposite();
        return new Vec3(
                origin.getX() + 0.5D
                        + localPositiveX.getStepX() * localX
                        + back.getStepX() * localZ,
                origin.getY() + localY,
                origin.getZ() + 0.5D
                        + localPositiveX.getStepZ() * localX
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

    /**
     * Complete 13-block-wide, three-block-high authored envelope. It includes
     * the open door swing but deliberately contains no cells below placement Y,
     * so normal facility floor blocks may remain beneath both booths.
     */
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
        Set<BlockPos> blocked = new LinkedHashSet<>();
        for (PartPlacement placement : placements(origin, front)) {
            if (!level.getWorldBorder().isWithinBounds(placement.pos())
                    || !level.getBlockState(placement.pos()).canBeReplaced()) {
                blocked.add(placement.pos().immutable());
            }
        }
        return List.copyOf(blocked);
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
            if (!level.setBlock(placement.pos(), helper, Block.UPDATE_ALL)) {
                for (BlockPos pos : placed) {
                    BlockState state = level.getBlockState(pos);
                    if (isHelper(state)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_ALL);
                    }
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
                level.setBlock(placement.pos(), Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
        clearLegacyBoothFloorHelpers(level, origin, front);
    }

    private static void clearLegacyBoothFloorHelpers(Level level,
            BlockPos origin, Direction front) {
        for (int centerSide : new int[] {-5, 5}) {
            for (int side = centerSide - 1; side <= centerSide + 1; side++) {
                for (int forward = -2; forward <= -1; forward++) {
                    BlockPos pos = gridCell(origin, front, side, -1, forward);
                    if (isHelper(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    public static void destroyFromHelper(Level level, BlockPos helperPos,
            BlockState helperState, boolean dropMachine) {
        BlockPos controller = findController(level, helperPos, helperState);
        if (controller == null) {
            if (isHelper(level.getBlockState(helperPos))) {
                level.setBlock(helperPos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
            return;
        }
        destroyController(level, controller, dropMachine);
    }

    public static void destroyController(Level level, BlockPos origin,
            boolean dropMachine) {
        BlockState controller = level.getBlockState(origin);
        if (!isController(controller)) return;
        Direction front = facing(controller);
        clearHelpers(level, origin, front);
        if (dropMachine) {
            Block.popResource(level, origin,
                    new ItemStack(Scp914Module.SCP_914_ITEM.get()));
        }
        level.setBlock(origin, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /**
     * Helpers intentionally store semantic shape rather than controller offsets.
     * Breaking a structure is rare, so ownership can be resolved cheaply by trying
     * the finite authored 13x3x6 envelope. This avoids thousands of block states.
     */
    @Nullable
    public static BlockPos findController(BlockGetter level, BlockPos helperPos,
            BlockState helperState) {
        if (!isHelper(helperState)
                || !helperState.hasProperty(Scp914PartBlock.FACING)
                || !helperState.hasProperty(Scp914PartBlock.ROLE)) {
            return null;
        }
        Direction front = helperState.getValue(Scp914PartBlock.FACING);
        Direction localPositiveX = front.getCounterClockWise();
        for (int side = MIN_SIDE; side <= MAX_SIDE; side++) {
            for (int forward = MIN_FORWARD; forward <= MAX_FORWARD; forward++) {
                for (int y = 0; y <= MAX_HEIGHT; y++) {
                    BlockPos candidate = helperPos
                            .relative(localPositiveX, -side)
                            .relative(front, -forward)
                            .below(y);
                    BlockState candidateState = level.getBlockState(candidate);
                    if (!isController(candidateState)
                            || facing(candidateState) != front) {
                        continue;
                    }
                    if (isExpectedHelper(candidate, front, helperPos,
                            helperState)) {
                        return candidate.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isExpectedHelper(BlockPos origin, Direction front,
            BlockPos helperPos, BlockState helperState) {
        for (PartPlacement placement : placements(origin, front)) {
            if (!placement.pos().equals(helperPos)
                    || placement.role() != helperState.getValue(Scp914PartBlock.ROLE)) {
                continue;
            }
            return switch (placement.kind()) {
                case RESERVATION -> helperState.is(Scp914Module.SCP_914_RESERVATION.get());
                case SOLID -> helperState.is(Scp914Module.SCP_914_COLLISION.get());
                case DOOR -> helperState.is(Scp914Module.SCP_914_DOOR_COLLISION.get());
            };
        }
        return false;
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
        addCentralBody(result, origin, front);
        addConnector(result, origin, front, -3);
        addConnector(result, origin, front, 3);
        addCabin(result, origin, front, -5);
        addCabin(result, origin, front, 5);
        return List.copyOf(result.values());
    }

    private static void addCentralBody(Map<BlockPos, PartPlacement> result,
            BlockPos origin, Direction front) {
        for (int side = -2; side <= 2; side++) {
            for (int y = 0; y <= 2; y++) {
                put(result, origin, front, side, y, 0,
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.BODY_FRONT);
                put(result, origin, front, side, y, -2,
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.BODY_BACK);
            }
        }
        for (int y = 0; y <= 2; y++) {
            put(result, origin, front, -2, y, -1,
                    Scp914PartBlock.Kind.SOLID,
                    Scp914PartBlock.Role.BODY_SIDE_NEG);
            put(result, origin, front, 2, y, -1,
                    Scp914PartBlock.Kind.SOLID,
                    Scp914PartBlock.Role.BODY_SIDE_POS);
        }
        for (int side = -1; side <= 1; side++) {
            put(result, origin, front, side, 0, -1,
                    Scp914PartBlock.Kind.SOLID,
                    Scp914PartBlock.Role.BODY_FLOOR);
            put(result, origin, front, side, 1, -1,
                    Scp914PartBlock.Kind.RESERVATION,
                    Scp914PartBlock.Role.RESERVED);
            put(result, origin, front, side, 2, -1,
                    Scp914PartBlock.Kind.SOLID,
                    Scp914PartBlock.Role.BODY_ROOF);
        }
    }

    private static void addConnector(Map<BlockPos, PartPlacement> result,
            BlockPos origin, Direction front, int side) {
        for (int forward = -2; forward <= 0; forward++) {
            for (int y = 0; y <= 2; y++) {
                put(result, origin, front, side, y, forward,
                        Scp914PartBlock.Kind.RESERVATION,
                        Scp914PartBlock.Role.FRAME);
            }
        }
    }

    private static void addCabin(Map<BlockPos, PartPlacement> result,
            BlockPos origin, Direction front, int centerSide) {
        int negativeWall = centerSide - 1;
        int positiveWall = centerSide + 1;

        for (int forward = -2; forward <= -1; forward++) {
            for (int y = 0; y <= 2; y++) {
                put(result, origin, front, negativeWall, y, forward,
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.CABIN_SIDE_NEG);
                put(result, origin, front, positiveWall, y, forward,
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.CABIN_SIDE_POS);
            }
            put(result, origin, front, centerSide, 2, forward,
                    Scp914PartBlock.Kind.SOLID,
                    Scp914PartBlock.Role.CABIN_ROOF);
            for (int y = 0; y <= 1; y++) {
                put(result, origin, front, centerSide, y, forward,
                        Scp914PartBlock.Kind.RESERVATION,
                        Scp914PartBlock.Role.RESERVED);
            }
        }

        for (int side = centerSide - 1; side <= centerSide + 1; side++) {
            for (int y = 0; y <= 2; y++) {
                put(result, origin, front, side, y, -3,
                        Scp914PartBlock.Kind.SOLID,
                        Scp914PartBlock.Role.CABIN_BACK);
                put(result, origin, front, side, y, 0,
                        Scp914PartBlock.Kind.DOOR,
                        closedDoorRole(side, y));
            }
        }

        int outerSide = centerSide < 0 ? centerSide - 1 : centerSide + 1;
        for (int forward = 1; forward <= 2; forward++) {
            for (int y = 0; y <= 2; y++) {
                put(result, origin, front, outerSide, y, forward,
                        Scp914PartBlock.Kind.DOOR,
                        openDoorRole(outerSide, forward, y));
            }
        }
    }

    private static Scp914PartBlock.Role closedDoorRole(int side, int y) {
        return switch (side) {
            case -6 -> switch (y) {
                case 0 -> Scp914PartBlock.Role.DOOR_N6_LOWER;
                case 1 -> Scp914PartBlock.Role.DOOR_N6_UPPER;
                default -> Scp914PartBlock.Role.DOOR_N6_TOP;
            };
            case -5 -> switch (y) {
                case 0 -> Scp914PartBlock.Role.DOOR_N5_LOWER;
                case 1 -> Scp914PartBlock.Role.DOOR_N5_UPPER;
                default -> Scp914PartBlock.Role.DOOR_N5_TOP;
            };
            case -4 -> switch (y) {
                case 0 -> Scp914PartBlock.Role.DOOR_N4_LOWER;
                case 1 -> Scp914PartBlock.Role.DOOR_N4_UPPER;
                default -> Scp914PartBlock.Role.DOOR_N4_TOP;
            };
            case 4 -> switch (y) {
                case 0 -> Scp914PartBlock.Role.DOOR_P4_LOWER;
                case 1 -> Scp914PartBlock.Role.DOOR_P4_UPPER;
                default -> Scp914PartBlock.Role.DOOR_P4_TOP;
            };
            case 5 -> switch (y) {
                case 0 -> Scp914PartBlock.Role.DOOR_P5_LOWER;
                case 1 -> Scp914PartBlock.Role.DOOR_P5_UPPER;
                default -> Scp914PartBlock.Role.DOOR_P5_TOP;
            };
            case 6 -> switch (y) {
                case 0 -> Scp914PartBlock.Role.DOOR_P6_LOWER;
                case 1 -> Scp914PartBlock.Role.DOOR_P6_UPPER;
                default -> Scp914PartBlock.Role.DOOR_P6_TOP;
            };
            default -> throw new IllegalArgumentException(
                    "Invalid SCP-914 closed door side " + side);
        };
    }

    private static Scp914PartBlock.Role openDoorRole(int side, int forward, int y) {
        if (side == -6) {
            if (forward == 1) return switch (y) {
                case 0 -> Scp914PartBlock.Role.DOOR_N_OPEN1_LOWER;
                case 1 -> Scp914PartBlock.Role.DOOR_N_OPEN1_UPPER;
                default -> Scp914PartBlock.Role.DOOR_N_OPEN1_TOP;
            };
            return switch (y) {
                case 0 -> Scp914PartBlock.Role.DOOR_N_OPEN2_LOWER;
                case 1 -> Scp914PartBlock.Role.DOOR_N_OPEN2_UPPER;
                default -> Scp914PartBlock.Role.DOOR_N_OPEN2_TOP;
            };
        }
        if (forward == 1) return switch (y) {
            case 0 -> Scp914PartBlock.Role.DOOR_P_OPEN1_LOWER;
            case 1 -> Scp914PartBlock.Role.DOOR_P_OPEN1_UPPER;
            default -> Scp914PartBlock.Role.DOOR_P_OPEN1_TOP;
        };
        return switch (y) {
            case 0 -> Scp914PartBlock.Role.DOOR_P_OPEN2_LOWER;
            case 1 -> Scp914PartBlock.Role.DOOR_P_OPEN2_UPPER;
            default -> Scp914PartBlock.Role.DOOR_P_OPEN2_TOP;
        };
    }

    private static void put(Map<BlockPos, PartPlacement> result,
            BlockPos origin, Direction front, int side, int y, int forward,
            Scp914PartBlock.Kind kind, Scp914PartBlock.Role role) {
        BlockPos pos = gridCell(origin, front, side, y, forward);
        if (!pos.equals(origin)) {
            result.put(pos.immutable(), new PartPlacement(pos.immutable(), kind, role));
        }
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
