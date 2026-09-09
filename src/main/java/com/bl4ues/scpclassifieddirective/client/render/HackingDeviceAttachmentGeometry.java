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
 * camera focus. Values come directly from the authored Blockbench geometry.
 */
public final class HackingDeviceAttachmentGeometry {
    /** User-authored back/head contact point used to seat the device on a reader. */
    private static final Vec3 DEVICE_CONTACT = pixels(0.35D, 7.8009D, 1.4402D);

    /*
     * The visible screen is the actual 2.5 x 1.5 zero-thickness cube inside the
     * `screen` bone, not the bone pivot. Its cube centre is
     * [-0.35, 7.60, -1.05], rotated -22.5 degrees around
     * [-0.35, 7.725, -0.55], then inherited through the body's 180-degree Y
     * rotation. That produces the real rendered centre below.
     */
    private static final Vec3 SCREEN_CENTER = pixels(
            -0.03D, 7.41817334D, 1.16496434D);
    private static final Vec3 SCREEN_RIGHT = new Vec3(-1.0D, 0.0D, 0.0D);
    private static final Vec3 SCREEN_UP = new Vec3(
            0.0D, 0.9238795325D, 0.3826834324D);

    /* RIGHT x UP: the authored visible CRT face. */
    private static final Vec3 SCREEN_OUTWARD = new Vec3(
            0.0D, 0.3826834324D, -0.9238795325D);

    /*
     * Centre of the OCU reader's visible upper surface. The old attachment point
     * was authored at y=13.85 before the reader's +35 degree X rotation, inside
     * the reader thickness. Moving only this target to y=14.45 leaves the device
     * orientation/camera untouched while giving its body 0.05 px clearance over
     * the actual top face at y=14.4.
     */
    private static final Vec3 OCU_READER_SURFACE = centeredPixels(
            -9.625D, 13.7724359891D, 1.2489584952D);

    public static final double SCREEN_WIDTH = 2.5D / 16.0D;
    public static final double SCREEN_HEIGHT = 1.5D / 16.0D;
    public static final double FOCUS_DISTANCE = 0.36D;

    private HackingDeviceAttachmentGeometry() {
    }

    /** Exact CRT frame in the Hacking Device model's own rendered coordinates. */
    public static PhysicalBlockScreenGeometry.Frame localScreenFrame() {
        return new PhysicalBlockScreenGeometry.Frame(
                SCREEN_CENTER, SCREEN_RIGHT, SCREEN_UP, SCREEN_OUTWARD,
                SCREEN_WIDTH, SCREEN_HEIGHT);
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
            localTarget = OCU_READER_SURFACE;
        } else if (reader.side() == KeycardReaderLevels.Side.RIGHT) {
            localTarget = pixels(-2.65D, 1.05D, 14.2D);
        } else {
            localTarget = pixels(18.85D, 1.05D, 14.2D);
        }

        Vec3 worldTarget = localToWorld(pos, localTarget, facing);
        Vec3 contactVector = rotateHorizontalVector(DEVICE_CONTACT, facing);
        Vec3 origin = worldTarget.subtract(contactVector);

        Vec3 screenCenter = origin.add(
                rotateHorizontalVector(SCREEN_CENTER, facing));
        Vec3 right = rotateHorizontalVector(SCREEN_RIGHT, facing).normalize();
        Vec3 up = rotateHorizontalVector(SCREEN_UP, facing).normalize();
        Vec3 outward = rotateHorizontalVector(SCREEN_OUTWARD, facing).normalize();
        PhysicalBlockScreenGeometry.Frame frame =
                new PhysicalBlockScreenGeometry.Frame(screenCenter, right, up,
                        outward, SCREEN_WIDTH, SCREEN_HEIGHT);

        // Keep the current Attachment API so renderer/audio callers remain stable.
        // The pre-regression placement had no extra OCU pitch; seating followed
        // the same visible-screen normal used by the camera.
        return new Attachment(origin, worldTarget, facing, 0.0F, outward,
                frame, ocu);
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

    /** Converts centred Gecko model X/Z coordinates into block-local space. */
    private static Vec3 centeredPixels(double x, double y, double z) {
        return new Vec3(0.5D + x / 16.0D,
                y / 16.0D,
                0.5D + z / 16.0D);
    }

    public record Attachment(Vec3 modelOrigin, Vec3 contactPoint,
            Direction facing, float pitchDegrees, Vec3 mountOutward,
            PhysicalBlockScreenGeometry.Frame screen,
            boolean objectContainmentUnit) {
    }
}
