package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.block.Scp914Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Physical coordinates for the rebuilt SCP-914 model.
 *
 * <p>Blockbench/Gecko coordinates are authored around the center of the
 * controller block on X/Z and from the controller block floor on Y. Local
 * negative Z is the front/control side of the machine. The transform below is
 * shared by processing volumes, sounds and contextual-interaction anchors so
 * those systems cannot slowly drift apart as separate magic offsets.</p>
 */
public final class Scp914Structure {
    private Scp914Structure() {
    }

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
}
