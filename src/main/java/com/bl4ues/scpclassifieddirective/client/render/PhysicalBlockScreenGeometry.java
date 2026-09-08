package com.bl4ues.scpclassifieddirective.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry helper for interfaces painted onto real block models. It assumes the
 * authored model's unrotated front faces NORTH, matching vanilla blockstate
 * rotation conventions. Physical monitors can also apply their own local yaw
 * before the block-facing rotation, which is useful for deliberately angled CRTs.
 */
public final class PhysicalBlockScreenGeometry {
    private PhysicalBlockScreenGeometry() { }

    public static Frame fromNorthFacing(BlockPos pos, Direction facing,
            double localCenterX, double localCenterY, double localCenterZ,
            double tiltDegrees, double width, double height) {
        return fromNorthFacing(pos, facing, localCenterX, localCenterY,
                localCenterZ, 0.0D, tiltDegrees, width, height);
    }

    public static Frame fromNorthFacing(BlockPos pos, Direction facing,
            double localCenterX, double localCenterY, double localCenterZ,
            double yawDegrees, double tiltDegrees, double width,
            double height) {
        Direction horizontal = facing.getAxis().isHorizontal()
                ? facing : Direction.NORTH;
        Vec3 front = new Vec3(horizontal.getStepX(), 0.0D,
                horizontal.getStepZ());
        // Local +X after rotating the north-authored model to its block facing.
        Vec3 modelRight = new Vec3(-horizontal.getStepZ(), 0.0D,
                horizontal.getStepX());

        double forwardOffset = 0.5D - localCenterZ;
        Vec3 center = new Vec3(pos.getX() + 0.5D,
                pos.getY() + localCenterY, pos.getZ() + 0.5D)
                .add(front.scale(forwardOffset))
                .add(modelRight.scale(localCenterX - 0.5D));

        double yaw = Math.toRadians(yawDegrees);
        Vec3 horizontalOutward = front.scale(Math.cos(yaw))
                .add(modelRight.scale(Math.sin(yaw))).normalize();
        // Screen-right from the viewer's side of the panel. At zero yaw this is
        // exactly the former -modelRight vector, preserving Tesla CRT mapping.
        Vec3 viewerRight = new Vec3(horizontalOutward.z, 0.0D,
                -horizontalOutward.x).normalize();

        double tilt = Math.toRadians(tiltDegrees);
        double cos = Math.cos(tilt);
        double sin = Math.sin(tilt);
        Vec3 outward = horizontalOutward.scale(cos)
                .add(0.0D, sin, 0.0D).normalize();
        Vec3 up = horizontalOutward.scale(-sin)
                .add(0.0D, cos, 0.0D).normalize();
        return new Frame(center, viewerRight, up, outward, width, height);
    }

    public record Frame(Vec3 center, Vec3 right, Vec3 up, Vec3 outward,
            double width, double height) {
        public Vec3 point(double normalizedX, double normalizedY,
                double normalOffset) {
            return center
                    .add(right.scale(normalizedX * width))
                    .add(up.scale(normalizedY * height))
                    .add(outward.scale(normalOffset));
        }
    }
}
