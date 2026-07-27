package net.mcreator.scpadditions.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Separate Tesla Gate sensing and visible-arc discharge footprints. */
public final class TeslaGateVolume {
    private static final double ARC_SIDE_MIN = -6.0D / 16.0D;
    private static final double ARC_SIDE_MAX = 22.0D / 16.0D;
    private static final double ARC_Y_MIN = -15.0D / 16.0D;
    private static final double ARC_Y_MAX = 27.0D / 16.0D;
    private static final double ARC_DEPTH_MIN = 0.0D;
    private static final double ARC_DEPTH_MAX = 1.0D;

    private TeslaGateVolume() {
    }

    /** Broad 3x3x3 sensor used only to decide when the gate should activate. */
    public static AABB at(double x, double y, double z) {
        BlockPos controller = BlockPos.containing(x, y, z);
        return new AABB(controller).inflate(1.0D);
    }

    /**
     * Exact bounds of the model's shock element. Damage is limited to the
     * visible opening and rotates with the gate while the broad sensor remains
     * unchanged.
     */
    public static AABB lethalArcAt(LevelAccessor world, BlockPos controller) {
        BlockState state = world.getBlockState(controller);
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
        return lethalArcAt(controller, facing);
    }

    static AABB lethalArcAt(BlockPos controller, Direction facing) {
        double minY = controller.getY() + ARC_Y_MIN;
        double maxY = controller.getY() + ARC_Y_MAX;
        if (facing.getAxis() == Direction.Axis.X) {
            return new AABB(
                    controller.getX() + ARC_DEPTH_MIN, minY,
                    controller.getZ() + ARC_SIDE_MIN,
                    controller.getX() + ARC_DEPTH_MAX, maxY,
                    controller.getZ() + ARC_SIDE_MAX);
        }
        return new AABB(
                controller.getX() + ARC_SIDE_MIN, minY,
                controller.getZ() + ARC_DEPTH_MIN,
                controller.getX() + ARC_SIDE_MAX, maxY,
                controller.getZ() + ARC_DEPTH_MAX);
    }

    public static boolean intersects(Entity entity, AABB volume) {
        return entity != null && entity.isAlive()
                && entity.getBoundingBox().intersects(volume);
    }
}
