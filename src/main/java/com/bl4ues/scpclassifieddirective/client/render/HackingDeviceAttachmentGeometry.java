package com.bl4ues.scpclassifieddirective.client.render;

import com.bl4ues.scpclassifieddirective.facility.ObjectContainmentUnitModule;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * One geometry definition for attached-device rendering, its physical screen and
 * camera focus. Values come directly from the authored Blockbench pivots.
 */
public final class HackingDeviceAttachmentGeometry {
    /** User-authored back/head contact point used to seat the device on a reader. */
    private static final Vec3 DEVICE_CONTACT = pixels(0.35D, 7.8009D, 1.4402D);

    /**
     * Exact screen center after the screen cube's -22.5 degree X rotation and
     * the body's 180 degree Y rotation. The model itself owns the black CRT plane;
     * this frame is used only for text and camera geometry.
     */
    private static final Vec3 SCREEN_CENTER = pixels(
            -0.03D, 7.41817334D, 1.16496434D);
    private static final Vec3 SCREEN_RIGHT = new Vec3(-1.0D, 0.0D, 0.0D);
    private static final Vec3 SCREEN_UP = new Vec3(
            0.0D, 0.9238795325D, 0.3826834324D);
    /**
     * Viewer-side CRT normal. The attachment contact is on the back of the head
     * at positive local Z, so the visible CRT faces the opposite half-space.
     * This sign is shared by rendering, seating animation and camera focus.
     */
    private static final Vec3 SCREEN_OUTWARD = new Vec3(
            0.0D, 0.3826834324D, -0.9238795325D);

    public static final double SCREEN_WIDTH = 2.5D / 16.0D;
    public static final double SCREEN_HEIGHT = 1.5D / 16.0D;
    public static final double FOCUS_DISTANCE = 0.230D;

    private HackingDeviceAttachmentGeometry() {
    }

    public static Attachment resolve(BlockPos pos, BlockState state) {
        if (pos == null || state == null) return null;
        KeycardReaderLevels.ReaderDescriptor reader =
                KeycardReaderLevels.describe(state);
        boolean ocu = state.is(ObjectContainmentUnitModule.UNIT.get());
        if (reader == null && !ocu) return null;

        Direction facing = horizontalFacing(state);
        Vec3 localTarget;
        if (ocu) {
            // Same physical keycard-reader center used by the OCU context prompt.
            localTarget = new Vec3(
                    0.5D - 9.625D / 16.0D,
                    13.28094476D / 16.0D,
                    0.5D + 0.90481263D / 16.0D);
        } else if (reader.side() == KeycardReaderLevels.Side.RIGHT) {
            // Exact Blockbench attachment pivot supplied for right readers.
            localTarget = pixels(-2.65D, 1.05D, 14.2D);
        } else {
            // Exact Blockbench attachment pivot supplied for left readers.
            localTarget = pixels(18.85D, 1.05D, 14.2D);
        }

        Vec3 worldTarget = localToWorld(pos, localTarget, facing);
        Vec3 contactVector = rotateHorizontal(DEVICE_CONTACT, facing);
        Vec3 origin = worldTarget.subtract(contactVector);

        Vec3 screenCenter = origin.add(rotateHorizontal(SCREEN_CENTER, facing));
        Vec3 right = rotateHorizontalVector(SCREEN_RIGHT, facing).normalize();
        Vec3 up = rotateHorizontalVector(SCREEN_UP, facing).normalize();
        Vec3 outward = rotateHorizontalVector(SCREEN_OUTWARD, facing).normalize();
        PhysicalBlockScreenGeometry.Frame frame =
                new PhysicalBlockScreenGeometry.Frame(screenCenter, right, up,
                        outward, SCREEN_WIDTH, SCREEN_HEIGHT);
        return new Attachment(origin, worldTarget, facing, frame, ocu);
    }

    public static float modelYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static Direction horizontalFacing(BlockState state) {
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (facing.getAxis().isHorizontal()) return facing;
        }
        return Direction.NORTH;
    }

    private static Vec3 localToWorld(BlockPos pos, Vec3 local,
            Direction facing) {
        Vec3 rotated = rotateHorizontalAroundCenter(local, facing);
        return new Vec3(pos.getX() + rotated.x,
                pos.getY() + rotated.y, pos.getZ() + rotated.z);
    }

    private static Vec3 rotateHorizontalAroundCenter(Vec3 value,
            Direction facing) {
        double x = value.x - 0.5D;
        double z = value.z - 0.5D;
        Vec3 rotated = rotateXZ(x, value.y, z, facing);
        return new Vec3(rotated.x + 0.5D, rotated.y,
                rotated.z + 0.5D);
    }

    private static Vec3 rotateHorizontal(Vec3 value, Direction facing) {
        return rotateXZ(value.x, value.y, value.z, facing);
    }

    private static Vec3 rotateHorizontalVector(Vec3 value, Direction facing) {
        return rotateXZ(value.x, value.y, value.z, facing);
    }

    private static Vec3 rotateXZ(double x, double y, double z,
            Direction facing) {
        return switch (facing) {
            case EAST -> new Vec3(-z, y, x);
            case SOUTH -> new Vec3(-x, y, -z);
            case WEST -> new Vec3(z, y, -x);
            default -> new Vec3(x, y, z);
        };
    }

    private static Vec3 pixels(double x, double y, double z) {
        return new Vec3(x / 16.0D, y / 16.0D, z / 16.0D);
    }

    public record Attachment(Vec3 modelOrigin, Vec3 contactPoint,
            Direction facing, PhysicalBlockScreenGeometry.Frame screen,
            boolean objectContainmentUnit) {
    }
}
