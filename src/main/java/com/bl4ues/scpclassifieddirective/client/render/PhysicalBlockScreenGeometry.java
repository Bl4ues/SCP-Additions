package com.bl4ues.scpclassifieddirective.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry helper for interfaces painted onto real block models. It assumes the
 * authored model's unrotated front faces NORTH, matching vanilla blockstate
 * rotation conventions. Future terminals can reuse the same camera/render math
 * instead of growing another bespoke floating GUI.
 */
public final class PhysicalBlockScreenGeometry {
    private PhysicalBlockScreenGeometry() { }

    public static Frame fromNorthFacing(BlockPos pos, Direction facing,
            double localCenterX, double localCenterY, double localCenterZ,
            double tiltDegrees, double width, double height) {
        Direction horizontal = facing.getAxis().isHorizontal()
                ? facing : Direction.NORTH;
        Vec3 front = new Vec3(horizontal.getStepX(), 0.0D,
                horizontal.getStepZ());
        // Local +X after rotating the north-authored model to its block facing.
        Vec3 modelRight = new Vec3(-horizontal.getStepZ(), 0.0D,
                horizontal.getStepX());
        // A viewer standing in front sees the opposite horizontal direction as
        // screen-right. Keeping this explicit prevents mirrored UI textures.
        Vec3 viewerRight = modelRight.scale(-1.0D);

        double forwardOffset = 0.5D - localCenterZ;
        Vec3 center = new Vec3(pos.getX() + 0.5D,
                pos.getY() + localCenterY, pos.getZ() + 0.5D)
                .add(front.scale(forwardOffset))
                .add(modelRight.scale(localCenterX - 0.5D));

        double radians = Math.toRadians(tiltDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vec3 outward = new Vec3(front.x * cos, sin, front.z * cos)
                .normalize();
        Vec3 up = new Vec3(-front.x * sin, cos, -front.z * sin)
                .normalize();
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
