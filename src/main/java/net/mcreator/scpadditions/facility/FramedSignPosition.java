package net.mcreator.scpadditions.facility;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.Vec3;

/** Horizontal placement of the wide glass-backed sign relative to its block. */
public enum FramedSignPosition implements StringRepresentable {
    LEFT("left", 1, 8.0F),
    CENTER("center", 0, 0.0F),
    RIGHT("right", -1, -8.0F);

    private static final double THIRD_REGION_THRESHOLD = 1.0D / 6.0D;

    private final String serializedName;
    private final int sideOffset;
    private final float modelOffsetPixels;

    FramedSignPosition(String serializedName, int sideOffset,
            float modelOffsetPixels) {
        this.serializedName = serializedName;
        this.sideOffset = sideOffset;
        this.modelOffsetPixels = modelOffsetPixels;
    }

    public int sideOffset() {
        return sideOffset;
    }

    public float modelOffsetBlocks() {
        return modelOffsetPixels / 16.0F;
    }

    public FramedSignPosition opposite() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            default -> CENTER;
        };
    }

    /**
     * Divides the clicked wall block into left, centre and right thirds from the
     * player's view of that wall. The basis deliberately matches keycard-reader
     * placement, so every wall orientation feels identical.
     */
    public static FramedSignPosition fromPlacement(BlockPlaceContext context,
            Direction facing) {
        Direction screenLeft = facing.getClockWise();
        Vec3 center = Vec3.atCenterOf(context.getClickedPos());
        Vec3 offset = context.getClickLocation().subtract(center);
        double coordinate = offset.x * screenLeft.getStepX()
                + offset.z * screenLeft.getStepZ();
        if (coordinate > THIRD_REGION_THRESHOLD) return LEFT;
        if (coordinate < -THIRD_REGION_THRESHOLD) return RIGHT;
        return CENTER;
    }

    /** Preserves the physical side when a structure is mirrored. */
    public static FramedSignPosition mirror(FramedSignPosition position,
            Direction facing, Mirror mirror) {
        if (position == CENTER || mirror == Mirror.NONE) return position;
        Direction mirroredFacing = mirror.mirror(facing);
        Direction mirroredOldLeft = mirror.mirror(facing.getClockWise());
        return mirroredOldLeft == mirroredFacing.getClockWise()
                ? position : position.opposite();
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
