package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Local selection/collision shell for props whose authored model crosses block
 * boundaries. Minecraft only ray-traces the block cell currently traversed by
 * the player's view, so one extended shape on the controller cannot make the
 * out-of-cell parts selectable. The visible model remains on the controller;
 * invisible local parts expose only the portion that lies in each occupied
 * cell.
 */
public final class FacilityLargePropStructure {
    private static final double EPSILON = 1.0E-7D;
    private static final Map<ShapeKey, VoxelShape> LOCAL_SHAPES =
            buildLocalShapes();

    private FacilityLargePropStructure() {
    }

    public enum Kind {
        SIGN_SUPPORT,
        SCP_914_NOTICE,
        TV
    }

    public static VoxelShape controllerShape(Kind kind, Direction facing) {
        return localShape(kind, facing, BlockPos.ZERO);
    }

    public static VoxelShape partShape(FacilityPropPartBlock.Part part,
            Direction facing) {
        BlockPos relative = relativePosition(part, facing);
        return localShape(part.kind(), facing, relative);
    }

    public static boolean canPlace(Level level, BlockPos controllerPos,
            Kind kind, Direction facing) {
        for (FacilityPropPartBlock.Part part :
                FacilityPropPartBlock.Part.forKind(kind)) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            if (!level.getWorldBorder().isWithinBounds(partPos)
                    || !level.getBlockState(partPos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    public static boolean placeParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing) {
        if (!canPlace(level, controllerPos, kind, facing)) {
            return false;
        }

        for (FacilityPropPartBlock.Part part :
                FacilityPropPartBlock.Part.forKind(kind)) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            level.setBlock(partPos, partState(level, partPos, facing, part),
                    Block.UPDATE_ALL);
        }
        return true;
    }

    public static void ensureParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing) {
        if (!isController(level.getBlockState(controllerPos), kind)) {
            return;
        }

        for (FacilityPropPartBlock.Part part :
                FacilityPropPartBlock.Part.forKind(kind)) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            BlockState current = level.getBlockState(partPos);
            if (isExpectedPart(current, partPos, controllerPos, facing, part)) {
                continue;
            }
            if (current.canBeReplaced()) {
                level.setBlock(partPos,
                        partState(level, partPos, facing, part),
                        Block.UPDATE_ALL);
            }
        }
    }

    public static void removeParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing) {
        for (FacilityPropPartBlock.Part part :
                FacilityPropPartBlock.Part.forKind(kind)) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            BlockState state = level.getBlockState(partPos);
            if (isExpectedPart(state, partPos, controllerPos, facing, part)) {
                clearBlock(level, partPos, state);
            }
        }
    }

    public static void destroyFromPart(Level level, BlockPos partPos,
            BlockState partState, boolean dropController) {
        FacilityPropPartBlock.Part part =
                partState.getValue(FacilityPropPartBlock.PART);
        Direction facing = partState.getValue(FacilityPropPartBlock.FACING);
        Kind kind = part.kind();
        BlockPos controllerPos = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controllerPos);

        if (!isController(controllerState, kind)
                || controllerFacing(controllerState, kind) != facing
                || !partPosition(controllerPos, facing, part)
                        .equals(partPos)) {
            clearBlock(level, partPos, partState);
            return;
        }

        removeParts(level, controllerPos, kind, facing);
        if (dropController) {
            Block.popResource(level, controllerPos,
                    new ItemStack(controllerBlock(kind)));
        }
        clearBlock(level, controllerPos, controllerState);
    }

    public static boolean isValidPart(Level level, BlockPos partPos,
            BlockState partState) {
        if (partState.getBlock() != FacilityModule.FACILITY_PROP_PART.get()) {
            return false;
        }

        FacilityPropPartBlock.Part part =
                partState.getValue(FacilityPropPartBlock.PART);
        Direction facing = partState.getValue(FacilityPropPartBlock.FACING);
        BlockPos controllerPos = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controllerPos);
        return isController(controllerState, part.kind())
                && controllerFacing(controllerState, part.kind()) == facing
                && partPosition(controllerPos, facing, part).equals(partPos);
    }

    public static BlockPos partPosition(BlockPos controllerPos,
            Direction facing, FacilityPropPartBlock.Part part) {
        return controllerPos.offset(relativePosition(part, facing));
    }

    public static BlockPos controllerPosition(BlockPos partPos,
            BlockState partState) {
        FacilityPropPartBlock.Part part =
                partState.getValue(FacilityPropPartBlock.PART);
        Direction facing = partState.getValue(FacilityPropPartBlock.FACING);
        BlockPos relative = relativePosition(part, facing);
        return partPos.offset(-relative.getX(), -relative.getY(),
                -relative.getZ());
    }

    public static Block controllerBlock(Kind kind) {
        return switch (kind) {
            case SIGN_SUPPORT -> FacilityModule.SIGN_SUPPORT.get();
            case SCP_914_NOTICE -> FacilityModule.SCP_914_USAGE_NOTICE.get();
            case TV -> FacilityModule.TV.get();
        };
    }

    private static Direction controllerFacing(BlockState state, Kind kind) {
        return kind == Kind.TV
                ? state.getValue(FacilityPropPartBlock.FACING)
                : state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static boolean isController(BlockState state, Kind kind) {
        return state.getBlock() == controllerBlock(kind);
    }

    private static boolean isExpectedPart(BlockState state, BlockPos partPos,
            BlockPos controllerPos, Direction facing,
            FacilityPropPartBlock.Part expected) {
        return state.getBlock() == FacilityModule.FACILITY_PROP_PART.get()
                && state.getValue(FacilityPropPartBlock.FACING) == facing
                && state.getValue(FacilityPropPartBlock.PART) == expected
                && controllerPosition(partPos, state).equals(controllerPos);
    }

    private static BlockState partState(Level level, BlockPos pos,
            Direction facing, FacilityPropPartBlock.Part part) {
        return FacilityModule.FACILITY_PROP_PART.get().defaultBlockState()
                .setValue(FacilityPropPartBlock.FACING, facing)
                .setValue(FacilityPropPartBlock.PART, part)
                .setValue(FacilityPropPartBlock.WATERLOGGED,
                        level.getFluidState(pos).getType() == Fluids.WATER);
    }

    static void clearBlock(Level level, BlockPos pos, BlockState state) {
        BlockState replacement = state.getFluidState().isEmpty()
                ? Blocks.AIR.defaultBlockState()
                : state.getFluidState().createLegacyBlock();
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private static BlockPos relativePosition(FacilityPropPartBlock.Part part,
            Direction facing) {
        Direction sideAxis = facing.getAxis() == Direction.Axis.Y
                ? Direction.EAST : facing.getClockWise();
        Direction rowAxis = facing.getAxis() == Direction.Axis.Y
                ? Direction.SOUTH : Direction.UP;
        int rowOffset = part.rowOffset();
        if (part.kind() == Kind.TV && facing == Direction.DOWN) {
            rowOffset = -rowOffset;
        }
        return BlockPos.ZERO.relative(sideAxis, part.sideOffset())
                .relative(rowAxis, rowOffset);
    }

    private static VoxelShape localShape(Kind kind, Direction facing,
            BlockPos relative) {
        return LOCAL_SHAPES.getOrDefault(
                new ShapeKey(kind, facing, relative.getX(),
                        relative.getY(), relative.getZ()),
                Shapes.empty());
    }

    private static Map<ShapeKey, VoxelShape> buildLocalShapes() {
        Map<ShapeKey, VoxelShape> result = new HashMap<>();
        EnumMap<Direction, VoxelShape> signShapes =
                new EnumMap<>(Direction.class);
        signShapes.put(Direction.NORTH,
                Block.box(8.2D, -13.35D, 13.5D,
                        23.7D, -2.65D, 16.0D));
        signShapes.put(Direction.EAST,
                Block.box(0.0D, -13.35D, 8.2D,
                        2.5D, -2.65D, 23.7D));
        signShapes.put(Direction.SOUTH,
                Block.box(-7.7D, -13.35D, 0.0D,
                        7.8D, -2.65D, 2.5D));
        signShapes.put(Direction.WEST,
                Block.box(13.5D, -13.35D, -7.7D,
                        16.0D, -2.65D, 7.8D));

        for (Map.Entry<Direction, VoxelShape> entry :
                signShapes.entrySet()) {
            shareFullShape(result, Kind.SIGN_SUPPORT, entry.getKey(),
                    entry.getValue());
            shareFullShape(result, Kind.SCP_914_NOTICE, entry.getKey(),
                    entry.getValue());
        }

        EnumMap<Direction, VoxelShape> tvShapes =
                new EnumMap<>(Direction.class);
        tvShapes.put(Direction.NORTH,
                Block.box(-16.0D, -13.05D, 13.5D,
                        32.0D, 13.3D, 16.0D));
        tvShapes.put(Direction.EAST,
                Block.box(0.0D, -13.05D, -16.0D,
                        2.5D, 13.3D, 32.0D));
        tvShapes.put(Direction.SOUTH,
                Block.box(-16.0D, -13.05D, 0.0D,
                        32.0D, 13.3D, 2.5D));
        tvShapes.put(Direction.WEST,
                Block.box(13.5D, -13.05D, -16.0D,
                        16.0D, 13.3D, 32.0D));
        tvShapes.put(Direction.UP,
                Block.box(-16.0D, 0.0D, -13.05D,
                        32.0D, 2.5D, 13.3D));
        tvShapes.put(Direction.DOWN,
                Block.box(-16.0D, 13.5D, 2.7D,
                        32.0D, 16.0D, 29.05D));

        for (Map.Entry<Direction, VoxelShape> entry :
                tvShapes.entrySet()) {
            split(result, Kind.TV, entry.getKey(), entry.getValue());
        }
        return Map.copyOf(result);
    }

    /**
     * The controller and both occupied sign cells expose the same world-space
     * shape. The local coordinates differ, but the selection outline remains
     * visually continuous when the cursor crosses the invisible multiblock seam.
     */
    private static void shareFullShape(Map<ShapeKey, VoxelShape> target,
            Kind kind, Direction facing, VoxelShape source) {
        target.put(new ShapeKey(kind, facing, 0, 0, 0), source.optimize());
        for (FacilityPropPartBlock.Part part :
                FacilityPropPartBlock.Part.forKind(kind)) {
            BlockPos relative = relativePosition(part, facing);
            VoxelShape local = source.move(-relative.getX(),
                    -relative.getY(), -relative.getZ()).optimize();
            target.put(new ShapeKey(kind, facing, relative.getX(),
                    relative.getY(), relative.getZ()), local);
        }
    }

    private static void split(Map<ShapeKey, VoxelShape> target, Kind kind,
            Direction facing, VoxelShape source) {
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            int firstX = (int) Math.floor(minX + EPSILON);
            int firstY = (int) Math.floor(minY + EPSILON);
            int firstZ = (int) Math.floor(minZ + EPSILON);
            int lastX = (int) Math.ceil(maxX - EPSILON) - 1;
            int lastY = (int) Math.ceil(maxY - EPSILON) - 1;
            int lastZ = (int) Math.ceil(maxZ - EPSILON) - 1;

            for (int x = firstX; x <= lastX; x++) {
                for (int y = firstY; y <= lastY; y++) {
                    for (int z = firstZ; z <= lastZ; z++) {
                        double localMinX = Math.max(minX, x) - x;
                        double localMinY = Math.max(minY, y) - y;
                        double localMinZ = Math.max(minZ, z) - z;
                        double localMaxX = Math.min(maxX, x + 1.0D) - x;
                        double localMaxY = Math.min(maxY, y + 1.0D) - y;
                        double localMaxZ = Math.min(maxZ, z + 1.0D) - z;
                        if (localMaxX - localMinX <= EPSILON
                                || localMaxY - localMinY <= EPSILON
                                || localMaxZ - localMinZ <= EPSILON) {
                            continue;
                        }

                        ShapeKey key = new ShapeKey(kind, facing, x, y, z);
                        VoxelShape local = Shapes.box(localMinX, localMinY,
                                localMinZ, localMaxX, localMaxY, localMaxZ);
                        target.merge(key, local, Shapes::or);
                    }
                }
            }
        });
        target.replaceAll((key, shape) -> shape.optimize());
    }

    private record ShapeKey(Kind kind, Direction facing,
            int x, int y, int z) {
    }
}
