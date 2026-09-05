package com.bl4ues.scpclassifieddirective.procedures;

import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Sensor and discharge volumes for the wider, floor-anchored Tesla Gate. */
public final class TeslaGateVolume {
    // The old three-block-deep sensor let a sprinting player cross the lethal
    // plane before the alarm's fixed discharge cue completed. The physical arc
    // stays one block thick; only the warning/arming volume reaches farther out.
    private static final double SENSOR_HALF_DEPTH = 2.75D; // 5.5 blocks deep
    private static final double ARC_HALF_DEPTH = 0.5D;     // one block thick
    private static final double ARC_HALF_WIDTH = 1.10D;
    private static final double Y_MIN = 0.05D;
    private static final double Y_MAX = 3.65D;
    private static final double MOTION_QUERY_MARGIN = 2.0D;

    private TeslaGateVolume() {
    }

    public static AABB sensorAt(LevelAccessor world, BlockPos controller) {
        return oriented(controller, facing(world, controller), SENSOR_HALF_DEPTH);
    }

    public static AABB lethalArcAt(LevelAccessor world, BlockPos controller) {
        return oriented(controller, facing(world, controller), ARC_HALF_DEPTH);
    }

    static AABB lethalArcAt(BlockPos controller, Direction facing) {
        return oriented(controller, facing, ARC_HALF_DEPTH);
    }

    private static Direction facing(LevelAccessor world, BlockPos controller) {
        BlockState state = world.getBlockState(controller);
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }

    private static AABB oriented(BlockPos controller, Direction facing,
            double halfDepth) {
        double centerX = controller.getX() + 0.5D;
        double centerZ = controller.getZ() + 0.5D;
        double minY = controller.getY() + Y_MIN;
        double maxY = controller.getY() + Y_MAX;
        if (facing.getAxis() == Direction.Axis.X) {
            return new AABB(centerX - halfDepth, minY,
                    centerZ - ARC_HALF_WIDTH, centerX + halfDepth, maxY,
                    centerZ + ARC_HALF_WIDTH);
        }
        return new AABB(centerX - ARC_HALF_WIDTH, minY,
                centerZ - halfDepth, centerX + ARC_HALF_WIDTH, maxY,
                centerZ + halfDepth);
    }

    public static AABB motionCandidates(AABB volume) {
        return volume.inflate(MOTION_QUERY_MARGIN);
    }

    public static boolean intersects(Entity entity, AABB volume) {
        return isPhysicalTeslaTarget(entity)
                && entity.getBoundingBox().intersects(volume);
    }

    public static boolean intersectsOrCrossed(Entity entity, AABB volume) {
        if (!isPhysicalTeslaTarget(entity)) return false;
        if (entity.getBoundingBox().intersects(volume)) return true;
        double halfWidth = Math.max(0.01D, entity.getBbWidth() * 0.5D);
        double halfHeight = Math.max(0.01D, entity.getBbHeight() * 0.5D);
        AABB target = volume.inflate(halfWidth, halfHeight, halfWidth);
        Vec3 previous = new Vec3(entity.xo, entity.yo + halfHeight, entity.zo);
        Vec3 current = new Vec3(entity.getX(), entity.getY() + halfHeight,
                entity.getZ());
        return target.contains(previous) || target.contains(current)
                || target.clip(previous, current).isPresent();
    }

    /**
     * Playable SCP-079 uses a spectator player only as an internal network/camera
     * anchor. It is not a physical body and must never arm, cross, or be damaged
     * by a Tesla Gate while that role is active.
     */
    private static boolean isPhysicalTeslaTarget(Entity entity) {
        if (entity == null || !entity.isAlive()) return false;
        return !(entity instanceof ServerPlayer player
                && Scp079PlayableManager.isController(player));
    }
}
