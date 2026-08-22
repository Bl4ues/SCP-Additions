package com.bl4ues.scpclassifieddirective.facility;

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
import java.util.List;
import java.util.Map;

/**
 * Local selection/collision shells for props whose authored models cross block
 * boundaries. The visible model remains on its controller; invisible local
 * parts expose only the portion contained in each neighboring block cell.
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
        UNDER_CONSTRUCTION_NOTICE,
        TV;

        public boolean framedSign() {
            return this != TV;
        }
    }

    public static VoxelShape controllerShape(Kind kind, Direction facing) {
        return controllerShape(kind, facing, FramedSignPosition.CENTER);
    }

    public static VoxelShape controllerShape(Kind kind, Direction facing,
            FramedSignPosition position) {
        return localShape(kind, facing, normalizedPosition(kind, position),
                BlockPos.ZERO);
    }

    public static VoxelShape partShape(FacilityPropPartBlock.Part part,
            Direction facing) {
        if (part.legacy()) return Shapes.empty();
        FramedSignPosition position = normalizedPosition(part.kind(),
                part.position());
        BlockPos relative = relativePosition(part, facing);
        return localShape(part.kind(), facing, position, relative);
    }

    public static boolean canPlace(Level level, BlockPos controllerPos,
            Kind kind, Direction facing) {
        return canPlace(level, controllerPos, kind, facing,
                FramedSignPosition.CENTER);
    }

    public static boolean canPlace(Level level, BlockPos controllerPos,
            Kind kind, Direction facing, FramedSignPosition position) {
        for (FacilityPropPartBlock.Part part : partsFor(kind, position)) {
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
        return placeParts(level, controllerPos, kind, facing,
                FramedSignPosition.CENTER);
    }

    public static boolean placeParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing, FramedSignPosition position) {
        if (!canPlace(level, controllerPos, kind, facing, position)) {
            return false;
        }
        ensureParts(level, controllerPos, kind, facing, position);
        return true;
    }

    public static void ensureParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing) {
        BlockState controllerState = level.getBlockState(controllerPos);
        ensureParts(level, controllerPos, kind, facing,
                position(controllerState, kind));
    }

    public static void ensureParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing, FramedSignPosition position) {
        if (!isController(level.getBlockState(controllerPos), kind)) return;
        FramedSignPosition normalized = normalizedPosition(kind, position);

        // Remove a stale side cell when commands, structure rotation, or an old
        // world changes the placement variant without recreating the block.
        for (FacilityPropPartBlock.Part possible :
                FacilityPropPartBlock.Part.forKind(kind)) {
            if (partsFor(kind, normalized).contains(possible)) continue;
            BlockPos possiblePos = partPosition(controllerPos, facing, possible);
            BlockState possibleState = level.getBlockState(possiblePos);
            if (isExpectedPart(possibleState, possiblePos, controllerPos,
                    facing, possible)) {
                clearBlock(level, possiblePos, possibleState);
            }
        }

        for (FacilityPropPartBlock.Part part : partsFor(kind, normalized)) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            BlockState current = level.getBlockState(partPos);
            if (isExpectedPart(current, partPos, controllerPos, facing, part)) {
                continue;
            }
            if (current.canBeReplaced()) {
                level.setBlock(partPos, partState(level, partPos, facing, part),
                        Block.UPDATE_ALL);
            }
        }
    }

    public static void removeParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing) {
        removeParts(level, controllerPos, kind, facing,
                FramedSignPosition.CENTER);
    }

    public static void removeParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing, FramedSignPosition ignoredPosition) {
        // Remove every current variant, not merely the state being destroyed.
        // This also cleans obsolete side cells left by old worlds.
        for (FacilityPropPartBlock.Part part :
                FacilityPropPartBlock.Part.forKind(kind)) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            BlockState state = level.getBlockState(partPos);
            if (isExpectedPart(state, partPos, controllerPos, facing, part)) {
                clearBlock(level, partPos, state);
            }
        }
        removeLegacyParts(level, controllerPos, kind, facing);
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
                || controllerFacing(controllerState) != facing
                || !partPosition(controllerPos, facing, part).equals(partPos)) {
            clearBlock(level, partPos, partState);
            return;
        }

        removeParts(level, controllerPos, kind, facing,
                position(controllerState, kind));
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
        if (part.legacy()) return false;
        Direction facing = partState.getValue(FacilityPropPartBlock.FACING);
        BlockPos controllerPos = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controllerPos);
        FramedSignPosition position = position(controllerState, part.kind());
        return isController(controllerState, part.kind())
                && controllerFacing(controllerState) == facing
                && partsFor(part.kind(), position).contains(part)
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
            case UNDER_CONSTRUCTION_NOTICE ->
                    AreaUnderConstructionSignModule.BLOCK.get();
            case TV -> FacilityModule.TV.get();
        };
    }

    public static FramedSignPosition position(BlockState state, Kind kind) {
        if (!kind.framedSign() || state == null
                || !state.hasProperty(AbstractFramedSignBlock.POSITION)) {
            return FramedSignPosition.CENTER;
        }
        return state.getValue(AbstractFramedSignBlock.POSITION);
    }

    private static Direction controllerFacing(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static boolean isController(BlockState state, Kind kind) {
        return state != null && state.getBlock() == controllerBlock(kind);
    }

    private static List<FacilityPropPartBlock.Part> partsFor(Kind kind,
            FramedSignPosition position) {
        return FacilityPropPartBlock.Part.forKind(kind,
                normalizedPosition(kind, position));
    }

    private static FramedSignPosition normalizedPosition(Kind kind,
            FramedSignPosition position) {
        return kind.framedSign() && position != null
                ? position : FramedSignPosition.CENTER;
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

    private static void removeLegacyParts(Level level, BlockPos controllerPos,
            Kind kind, Direction facing) {
        for (FacilityPropPartBlock.Part part :
                FacilityPropPartBlock.Part.values()) {
            if (!part.legacy() || part.kind() != kind) continue;
            BlockPos partPos = partPosition(controllerPos, facing, part);
            BlockState state = level.getBlockState(partPos);
            if (state.getBlock() == FacilityModule.FACILITY_PROP_PART.get()
                    && state.getValue(FacilityPropPartBlock.PART) == part) {
                clearBlock(level, partPos, state);
            }
        }
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
            FramedSignPosition position, BlockPos relative) {
        return LOCAL_SHAPES.getOrDefault(new ShapeKey(kind, facing, position,
                        relative.getX(), relative.getY(), relative.getZ()),
                Shapes.empty());
    }

    private static Map<ShapeKey, VoxelShape> buildLocalShapes() {
        Map<ShapeKey, VoxelShape> result = new HashMap<>();

        for (FramedSignPosition position : FramedSignPosition.values()) {
            VoxelShape north = framedSignNorthShape(position);
            EnumMap<Direction, VoxelShape> rotations = rotations(north);
            for (Kind kind : List.of(Kind.SIGN_SUPPORT,
                    Kind.SCP_914_NOTICE, Kind.UNDER_CONSTRUCTION_NOTICE)) {
                for (Map.Entry<Direction, VoxelShape> entry :
                        rotations.entrySet()) {
                    split(result, kind, entry.getKey(), position,
                            entry.getValue());
                }
            }
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

        for (Map.Entry<Direction, VoxelShape> entry : tvShapes.entrySet()) {
            split(result, Kind.TV, entry.getKey(),
                    FramedSignPosition.CENTER, entry.getValue());
        }
        return Map.copyOf(result);
    }

    /** Raised one block from the legacy model and horizontally selected. */
    private static VoxelShape framedSignNorthShape(
            FramedSignPosition position) {
        double shift = position.modelOffsetBlocks() * 16.0D;
        return Block.box(0.2D + shift, 2.65D, 13.5D,
                15.7D + shift, 13.35D, 16.0D);
    }

    private static EnumMap<Direction, VoxelShape> rotations(
            VoxelShape north) {
        EnumMap<Direction, VoxelShape> result =
                new EnumMap<>(Direction.class);
        result.put(Direction.NORTH, north);
        result.put(Direction.EAST, rotateY(north, 1));
        result.put(Direction.SOUTH, rotateY(north, 2));
        result.put(Direction.WEST, rotateY(north, 3));
        return result;
    }

    private static VoxelShape rotateY(VoxelShape source, int quarterTurns) {
        VoxelShape current = source;
        for (int turn = 0; turn < quarterTurns; turn++) {
            VoxelShape[] rotated = {Shapes.empty()};
            current.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(
                            1.0D - maxZ, minY, minX,
                            1.0D - minZ, maxY, maxX)));
            current = rotated[0].optimize();
        }
        return current;
    }

    private static void split(Map<ShapeKey, VoxelShape> target, Kind kind,
            Direction facing, FramedSignPosition position,
            VoxelShape source) {
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

                        ShapeKey key = new ShapeKey(kind, facing, position,
                                x, y, z);
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
            FramedSignPosition position, int x, int y, int z) {
    }
}
