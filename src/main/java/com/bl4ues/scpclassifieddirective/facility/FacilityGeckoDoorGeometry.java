package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.Family;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.Phase;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Exact authored handle geometry shared by prompt anchoring and the interaction
 * outline.
 *
 * <p>The values below are the cubes inside each current .geo.json {@code handle}
 * bone, in Blockbench pixels. Conversion intentionally mirrors GeckoLib 4.4.x:
 * raw X is negated, Y/Z are kept, the model is centred at (0.5, 0, 0.5), bone
 * Y rotations are negated, and FACING is applied around the block centre. This
 * keeps the outline on the actual rendered handle instead of maintaining a
 * second guessed VoxelShape.</p>
 */
public final class FacilityGeckoDoorGeometry {
    public record AuthoredBox(double originX, double originY, double originZ,
            double sizeX, double sizeY, double sizeZ) {
        Vec3 center() {
            return new Vec3(originX + sizeX * 0.5D,
                    originY + sizeY * 0.5D,
                    originZ + sizeZ * 0.5D);
        }
    }

    private FacilityGeckoDoorGeometry() {
    }

    public static List<AuthoredBox> authoredHandleBoxes(Family family) {
        return switch (family) {
            case FACILITY -> FACILITY_HANDLE;
            case LOGISTICS_LEFT -> LEFT_LOGISTICS_HANDLE;
            case LOGISTICS_RIGHT -> RIGHT_LOGISTICS_HANDLE;
            case OFFICE -> OFFICE_HANDLE;
            case BATHROOM -> BATHROOM_HANDLE;
            case WORKSHOP -> WORKSHOP_HANDLE;
        };
    }

    public static Vec3 handleAnchor(BlockState state) {
        Family family = FacilityGeckoDoorModule.family(state);
        if (family == null) return new Vec3(0.5D, 1.0D, 0.5D);

        List<AuthoredBox> boxes = authoredHandleBoxes(family);
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (AuthoredBox box : boxes) {
            minX = Math.min(minX, box.originX());
            minY = Math.min(minY, box.originY());
            minZ = Math.min(minZ, box.originZ());
            maxX = Math.max(maxX, box.originX() + box.sizeX());
            maxY = Math.max(maxY, box.originY() + box.sizeY());
            maxZ = Math.max(maxZ, box.originZ() + box.sizeZ());
        }

        return transformAuthoredHandlePoint(state,
                new Vec3((minX + maxX) * 0.5D,
                        (minY + maxY) * 0.5D,
                        (minZ + maxZ) * 0.5D));
    }

    /**
     * Transform one point from Blockbench handle coordinates into the same
     * block-local coordinates used by GeoBlockRenderer.
     */
    public static Vec3 transformAuthoredHandlePoint(BlockState state,
            Vec3 authoredPixels) {
        Family family = FacilityGeckoDoorModule.family(state);
        if (family == null || authoredPixels == null) return Vec3.ZERO;

        // BakedModelFactory: raw X is mirrored; Y/Z are preserved.
        Vec3 point = new Vec3(-authoredPixels.x / 16.0D,
                authoredPixels.y / 16.0D,
                authoredPixels.z / 16.0D);

        if (family == Family.BATHROOM) {
            point = rotateY(point, bathroomHandlePivot(),
                    Math.toRadians(-180.0D));
        }

        if (FacilityGeckoDoorModule.phase(state) == Phase.OPEN) {
            point = rotateY(point, doorPivot(family), openAngle(family));
        }

        if (family == Family.WORKSHOP) {
            point = rotateY(point, workshopRootPivot(),
                    Math.toRadians(-180.0D));
        }

        point = rotateForFacing(point,
                state.getValue(FacilityGeckoDoorModule.FACING));

        return new Vec3(point.x + 0.5D, point.y, point.z + 0.5D);
    }

    private static Vec3 doorPivot(Family family) {
        double rawX = family == Family.LOGISTICS_RIGHT ? 8.0D : -8.0D;
        return rawPointToModel(new Vec3(rawX, 14.0D, -7.5D));
    }

    private static Vec3 workshopRootPivot() {
        return rawPointToModel(new Vec3(0.0D, 0.0D, -7.5D));
    }

    private static Vec3 bathroomHandlePivot() {
        return rawPointToModel(new Vec3(-4.775D, 17.525D, -7.75D));
    }

    private static double openAngle(Family family) {
        return Math.toRadians(family == Family.LOGISTICS_RIGHT
                || family == Family.WORKSHOP ? 100.0D : -100.0D);
    }

    private static Vec3 rawPointToModel(Vec3 raw) {
        return new Vec3(-raw.x / 16.0D,
                raw.y / 16.0D, raw.z / 16.0D);
    }

    /**
     * Match JOML/Mojang positive Y rotation used by GeckoLib.
     */
    private static Vec3 rotateY(Vec3 point, Vec3 pivot, double angle) {
        double dx = point.x - pivot.x;
        double dz = point.z - pivot.z;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(
                pivot.x + dx * cos + dz * sin,
                point.y,
                pivot.z - dx * sin + dz * cos);
    }

    /**
     * GeoBlockRenderer.rotateBlock: NORTH=0, SOUTH=180, WEST=+90, EAST=-90.
     * Coordinates here are still model-relative, before the +0.5 centre offset.
     */
    private static Vec3 rotateForFacing(Vec3 point, Direction facing) {
        return switch (facing) {
            case SOUTH -> new Vec3(-point.x, point.y, -point.z);
            case WEST -> new Vec3(point.z, point.y, -point.x);
            case EAST -> new Vec3(-point.z, point.y, point.x);
            default -> point;
        };
    }

    private static AuthoredBox box(double ox, double oy, double oz,
            double sx, double sy, double sz) {
        return new AuthoredBox(ox, oy, oz, sx, sy, sz);
    }

    private static final List<AuthoredBox> FACILITY_HANDLE = List.of(
            box(5.65D, 16.5D, -9.0D, 0.75D, 0.75D, 1.0D),
            box(5.65D, 16.5D, -7.0D, 0.75D, 0.75D, 1.0D),
            box(3.65D, 16.5D, -6.0D, 2.75D, 0.75D, 0.5D),
            box(3.65D, 16.5D, -9.5D, 2.75D, 0.75D, 0.5D));

    private static final List<AuthoredBox> OFFICE_HANDLE = List.of(
            box(6.65D, 16.5D, -9.0D, 0.75D, 0.75D, 1.0D),
            box(6.65D, 16.5D, -7.0D, 0.75D, 0.75D, 1.0D),
            box(4.65D, 16.5D, -6.0D, 2.75D, 0.75D, 0.5D),
            box(4.65D, 16.5D, -9.5D, 2.75D, 0.75D, 0.5D));

    private static final List<AuthoredBox> LEFT_LOGISTICS_HANDLE = List.of(
            box(6.15D, 16.75D, -9.0D, 0.75D, 0.75D, 1.0D),
            box(6.15D, 16.75D, -7.0D, 0.75D, 0.75D, 1.0D),
            box(4.15D, 16.75D, -6.0D, 2.75D, 0.75D, 0.5D),
            box(4.15D, 16.75D, -9.5D, 2.75D, 0.75D, 0.5D));

    private static final List<AuthoredBox> RIGHT_LOGISTICS_HANDLE = List.of(
            box(-6.9D, 16.75D, -9.0D, 0.75D, 0.75D, 1.0D),
            box(-6.9D, 16.75D, -7.0D, 0.75D, 0.75D, 1.0D),
            box(-6.9D, 16.75D, -6.0D, 2.75D, 0.75D, 0.5D),
            box(-6.9D, 16.75D, -9.5D, 2.75D, 0.75D, 0.5D));

    private static final List<AuthoredBox> BATHROOM_HANDLE = List.of(
            box(-5.15D, 14.9D, -9.0D, 0.75D, 0.75D, 1.0D),
            box(-5.15D, 14.9D, -9.5D, 0.75D, 5.25D, 0.5D),
            box(-5.15D, 19.4D, -9.0D, 0.75D, 0.75D, 1.0D));

    private static final List<AuthoredBox> WORKSHOP_HANDLE = List.of(
            box(6.65D, 16.75D, -9.0D, 0.75D, 0.75D, 1.0D),
            box(6.65D, 16.75D, -7.0D, 0.75D, 0.75D, 1.0D),
            box(4.65D, 16.75D, -6.0D, 2.75D, 0.75D, 0.5D),
            box(4.65D, 16.75D, -9.5D, 2.75D, 0.75D, 0.5D));
}
