package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.block.Scp914Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Physical coordinates and multiblock footprint for the rebuilt SCP-914.
 *
 * <p>Blockbench/Gecko coordinates are authored around the center of the
 * controller block on X/Z and from the controller block floor on Y. Local
 * negative Z is the front/control side of the machine. The transform below is
 * shared by processing volumes, sounds, interaction anchors and structural
 * cells so these systems cannot drift apart.</p>
 */
public final class Scp914Structure {
    private Scp914Structure() {
    }

    private static final int FORWARD_MIN = -3;
    private static final int FORWARD_MAX = 2;
    private static final int SIDE_MIN = -8;
    private static final int SIDE_MAX = 7;
    private static final int Y_MIN = 0;
    private static final int Y_MAX = 2;

    // Centers of the usable chamber volumes, measured from the uploaded model.
    private static final double INTAKE_X = -4.80D;
    private static final double OUTPUT_X = 4.80D;
    private static final double CHAMBER_Y = 1.05D;
    private static final double CHAMBER_Z = 1.50D;

    // Interior volume only. The shell/doors are deliberately outside this AABB.
    private static final double CHAMBER_HALF_X = 0.92D;
    private static final double CHAMBER_MIN_Y = 0.12D;
    private static final double CHAMBER_MAX_Y = 2.18D;
    private static final double CHAMBER_MIN_Z = 0.66D;
    private static final double CHAMBER_MAX_Z = 2.34D;

    // Exact authored pivots for the two physical controls.
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

    /** Converts authored model-space block units to a world-space point. */
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

    /** Entire volume that must be unobstructed before the machine is placed. */
    public static List<BlockPos> requiredCells(BlockPos origin, Direction front) {
        List<BlockPos> result = new ArrayList<>();
        for (int forward = FORWARD_MIN; forward <= FORWARD_MAX; forward++) {
            for (int side = SIDE_MIN; side <= SIDE_MAX; side++) {
                for (int y = Y_MIN; y <= Y_MAX; y++) {
                    result.add(gridCell(origin, front, side, y, forward));
                }
            }
        }
        return result;
    }

    public static List<BlockPos> collectObstructions(Level level, BlockPos origin,
            Direction front) {
        List<BlockPos> blocked = new ArrayList<>();
        for (BlockPos target : requiredCells(origin, front)) {
            if (target.equals(origin)) continue;
            if (!level.getBlockState(target).canBeReplaced()) {
                blocked.add(target.immutable());
            }
        }
        return blocked;
    }

    /** Places hidden reservation/collision cells after the controller succeeds. */
    public static void placeHelpers(Level level, BlockPos origin, Direction front) {
        if (level.isClientSide) return;
        for (int forward = FORWARD_MIN; forward <= FORWARD_MAX; forward++) {
            for (int side = SIDE_MIN; side <= SIDE_MAX; side++) {
                for (int y = Y_MIN; y <= Y_MAX; y++) {
                    BlockPos target = gridCell(origin, front, side, y, forward);
                    if (target.equals(origin)) continue;
                    if (!level.getBlockState(target).canBeReplaced()) continue;

                    BlockState helper;
                    if (isDoorLocal(side, y, forward)) {
                        helper = Scp914Module.SCP_914_DOOR_COLLISION.get()
                                .defaultBlockState();
                    } else if (isSolidLocal(side, y, forward)) {
                        helper = Scp914Module.SCP_914_COLLISION.get()
                                .defaultBlockState();
                    } else {
                        helper = Scp914Module.SCP_914_RESERVATION.get()
                                .defaultBlockState();
                    }
                    level.setBlock(target, helper, 3);
                }
            }
        }
    }

    public static void clearHelpers(Level level, BlockPos origin, Direction front) {
        if (level.isClientSide) return;
        for (BlockPos target : requiredCells(origin, front)) {
            if (target.equals(origin)) continue;
            BlockState state = level.getBlockState(target);
            if (state.is(Scp914Module.SCP_914_RESERVATION.get())
                    || state.is(Scp914Module.SCP_914_COLLISION.get())
                    || state.is(Scp914Module.SCP_914_DOOR_COLLISION.get())) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static boolean isDoorCell(BlockPos origin, Direction front,
            BlockPos candidate) {
        return candidate.equals(gridCell(origin, front, -5, 0, 0))
                || candidate.equals(gridCell(origin, front, -5, 1, 0))
                || candidate.equals(gridCell(origin, front, 5, 0, 0))
                || candidate.equals(gridCell(origin, front, 5, 1, 0));
    }

    private static boolean isDoorLocal(int side, int y, int forward) {
        return (side == -5 || side == 5)
                && forward == 0 && y <= 1;
    }

    /**
     * Coarse collision follows the large body and chamber shells, while the
     * remaining footprint is reservation-only. Chamber interiors and open door
     * paths therefore remain traversable and usable for loose items/entities.
     */
    private static boolean isSolidLocal(int side, int y, int forward) {
        if (side >= -3 && side <= 3 && forward >= -2 && forward <= 0) {
            return true;
        }
        if (side >= -2 && side <= 2 && forward == 1 && y <= 1) {
            return true;
        }
        return chamberWall(side, y, forward, -5)
                || chamberWall(side, y, forward, 5);
    }

    private static boolean chamberWall(int side, int y, int forward, int center) {
        if (Math.abs(side - center) == 1
                && forward >= -3 && forward <= 0) {
            return true;
        }
        if (side == center && forward == -3) return true;
        return side == center && y == 2 && forward >= -2 && forward <= -1;
    }

    private static BlockPos gridCell(BlockPos origin, Direction front,
            int side, int y, int forward) {
        Direction localPositiveX = front.getCounterClockWise();
        return origin.relative(front, forward)
                .relative(localPositiveX, side)
                .above(y);
    }
}
