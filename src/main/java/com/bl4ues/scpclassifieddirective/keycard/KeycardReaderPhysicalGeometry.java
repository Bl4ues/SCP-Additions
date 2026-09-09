package com.bl4ues.scpclassifieddirective.keycard;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** Shared physical anchors for the side-offset wall reader models. */
public final class KeycardReaderPhysicalGeometry {
    /* Main reader-body pivots from the authored right/left Blockbench models. */
    private static final Vec3 RIGHT_CENTER = pixels(-2.63D, 1.3716D, 14.6813D);
    private static final Vec3 LEFT_CENTER = pixels(18.67D, 1.3716D, 14.6813D);

    private KeycardReaderPhysicalGeometry() {
    }

    public static Vec3 soundPosition(BlockPos pos, BlockState state) {
        if (pos == null || state == null) return null;
        KeycardReaderLevels.ReaderDescriptor descriptor =
                KeycardReaderLevels.describe(state);
        if (descriptor == null) return null;
        Vec3 local = descriptor.side() == KeycardReaderLevels.Side.RIGHT
                ? RIGHT_CENTER : LEFT_CENTER;
        return localToWorld(pos, local, horizontalFacing(state));
    }

    private static Direction horizontalFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (direction.getAxis().isHorizontal()) return direction;
        }
        return Direction.NORTH;
    }

    private static Vec3 localToWorld(BlockPos pos, Vec3 local,
            Direction facing) {
        double x = local.x - 0.5D;
        double z = local.z - 0.5D;
        Vec3 rotated = switch (facing) {
            case EAST -> new Vec3(-z, local.y, x);
            case SOUTH -> new Vec3(-x, local.y, -z);
            case WEST -> new Vec3(z, local.y, -x);
            default -> new Vec3(x, local.y, z);
        };
        return new Vec3(pos.getX() + rotated.x + 0.5D,
                pos.getY() + rotated.y,
                pos.getZ() + rotated.z + 0.5D);
    }

    private static Vec3 pixels(double x, double y, double z) {
        return new Vec3(x / 16.0D, y / 16.0D, z / 16.0D);
    }
}
