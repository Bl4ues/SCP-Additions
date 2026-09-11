package com.bl4ues.scpclassifieddirective.facility.blastdoor;

import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.LeftDoorButtons;
import com.bl4ues.scpclassifieddirective.facility.MirroredDoorButtons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Placement, ownership, redstone relay and model-derived collision for a Blast Door. */
public final class BlastDoorStructure {
    public static final int MIN_SIDE = -2;
    public static final int MAX_SIDE = 2;
    public static final int MAX_HEIGHT = 3;

    private BlastDoorStructure() {
    }

    public static boolean canPlace(Level level, BlockPos controller,
            Direction facing) {
        return collectObstructions(level, controller, facing).isEmpty();
    }

    public static List<BlockPos> collectObstructions(Level level,
            BlockPos controller, Direction facing) {
        Set<BlockPos> blockers = new LinkedHashSet<>();
        if (!level.getWorldBorder().isWithinBounds(controller)
                || !level.getBlockState(controller).canBeReplaced()) {
            blockers.add(controller.immutable());
        }
        for (PartAddress part : parts()) {
            BlockPos pos = partPosition(controller, facing,
                    part.side(), part.height());
            if (!level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).canBeReplaced()) {
                blockers.add(pos.immutable());
            }
        }
        return List.copyOf(blockers);
    }

    public static boolean placeParts(Level level, BlockPos controller,
            Direction facing) {
        for (PartAddress part : parts()) {
            BlockPos pos = partPosition(controller, facing,
                    part.side(), part.height());
            if (!level.getBlockState(pos).canBeReplaced()) return false;
        }
        for (PartAddress part : parts()) {
            BlockPos pos = partPosition(controller, facing,
                    part.side(), part.height());
            level.setBlock(pos, partState(level, pos, facing, part),
                    Block.UPDATE_ALL);
        }
        return true;
    }

    public static void ensureParts(Level level, BlockPos controller,
            Direction facing) {
        if (!BlastDoorModule.isController(level.getBlockState(controller))) {
            return;
        }
        for (PartAddress part : parts()) {
            BlockPos pos = partPosition(controller, facing,
                    part.side(), part.height());
            BlockState state = level.getBlockState(pos);
            if (BlastDoorModule.isPart(state)
                    && state.getValue(BlastDoorModule.FACING) == facing
                    && decodeSide(state) == part.side()
                    && state.getValue(BlastDoorModule.HEIGHT) == part.height()
                    && controllerPosition(pos, state).equals(controller)) {
                continue;
            }
            if (state.canBeReplaced()) {
                level.setBlock(pos, partState(level, pos, facing, part),
                        Block.UPDATE_ALL);
            }
        }
    }

    public static void removeParts(Level level, BlockPos controller,
            BlockState controllerState) {
        if (!controllerState.hasProperty(BlastDoorModule.FACING)) return;
        Direction facing = controllerState.getValue(BlastDoorModule.FACING);
        for (PartAddress part : parts()) {
            BlockPos pos = partPosition(controller, facing,
                    part.side(), part.height());
            BlockState state = level.getBlockState(pos);
            if (BlastDoorModule.isPart(state)
                    && controllerPosition(pos, state).equals(controller)) {
                clearBlock(level, pos, state);
            }
        }
    }

    public static void destroyFromPart(Level level, BlockPos partPos,
            BlockState partState, boolean dropDoor) {
        BlockPos controller = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controller);
        if (!BlastDoorModule.isController(controllerState)) {
            clearBlock(level, partPos, partState);
            return;
        }
        removeParts(level, controller, controllerState);
        if (dropDoor) {
            Block.popResource(level, controller,
                    new ItemStack(BlastDoorModule.ITEM.get()));
        }
        clearBlock(level, controller, controllerState);
    }

    public static boolean isValidPart(BlockGetter level, BlockPos partPos,
            BlockState state) {
        if (!BlastDoorModule.isPart(state)) return false;
        int side = decodeSide(state);
        int height = state.getValue(BlastDoorModule.HEIGHT);
        if (!isPartAddress(side, height)) return false;

        Direction facing = state.getValue(BlastDoorModule.FACING);
        BlockPos controller = controllerPosition(partPos, state);
        BlockState controllerState = level.getBlockState(controller);
        return BlastDoorModule.isController(controllerState)
                && controllerState.getValue(BlastDoorModule.FACING) == facing
                && partPosition(controller, facing, side, height)
                .equals(partPos);
    }

    public static BlockPos partPosition(BlockPos controller, Direction facing,
            int side, int height) {
        Direction right = facing.getClockWise();
        return controller.offset(right.getStepX() * side, height,
                right.getStepZ() * side);
    }

    public static BlockPos controllerPosition(BlockPos partPos,
            BlockState state) {
        Direction facing = state.getValue(BlastDoorModule.FACING);
        Direction right = facing.getClockWise();
        int side = decodeSide(state);
        int height = state.getValue(BlastDoorModule.HEIGHT);
        return partPos.offset(-right.getStepX() * side, -height,
                -right.getStepZ() * side);
    }

    public static BlockPos mimicSource(BlockPos controller, Direction facing,
            boolean rightSide, boolean upperLayer) {
        return partPosition(controller, facing, rightSide ? 3 : -3,
                upperLayer ? 3 : 2);
    }

    public static boolean hasNeighborSignal(Level level,
            BlockPos controller) {
        BlockState controllerState = level.getBlockState(controller);
        if (!BlastDoorModule.isController(controllerState)) return false;
        Direction facing = controllerState.getValue(BlastDoorModule.FACING);
        for (BlockPos pos : structurePositions(controller, facing)) {
            if (level.hasNeighborSignal(pos)) return true;
        }
        return false;
    }

    /**
     * A physical redstone component anywhere on the multiblock is enough to
     * connect the door to SCP-079. It need not currently be powered.
     */
    public static boolean hasRedstoneConnection(Level level,
            BlockPos controller) {
        BlockState controllerState = level.getBlockState(controller);
        if (!BlastDoorModule.isController(controllerState)) return false;
        Direction facing = controllerState.getValue(BlastDoorModule.FACING);
        List<BlockPos> positions = structurePositions(controller, facing);
        Set<BlockPos> owned = new HashSet<>(positions);

        // First handle the exact vanilla-door case: any part of the
        // multiblock can receive power directly, including from below.
        for (BlockPos pos : positions) {
            if (level.hasNeighborSignal(pos)) return true;
        }

        // Facility buttons/readers are commonly mounted on the wall or frame
        // immediately beside a large doorway rather than directly touching
        // the controller cell. Search a tight two-block shell around the
        // structure for a real redstone source/wire. This keeps the door
        // hackable while such a physical control is installed, even when the
        // control is currently unpowered.
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos pos : positions) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 2) {
                            continue;
                        }
                        BlockPos candidate = pos.offset(dx, dy, dz);
                        if (owned.contains(candidate)
                                || !visited.add(candidate)
                                || !level.hasChunkAt(candidate)) {
                            continue;
                        }
                        BlockState neighbor = level.getBlockState(candidate);
                        Block neighborBlock = neighbor.getBlock();
                        if (neighbor.isSignalSource()
                                || neighbor.is(Blocks.REDSTONE_WIRE)
                                || isFacilityControlPanel(neighborBlock)) {
                            return true;
                        }
                        for (Direction direction : Direction.values()) {
                            if (level.getSignal(candidate,
                                    direction.getOpposite()) > 0
                                    || level.getDirectSignal(candidate,
                                    direction.getOpposite()) > 0) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isFacilityControlPanel(Block block) {
        // Functional Unity-style door buttons are not signal sources while
        // visually CLOSED, even though pressing them is exactly what powers a
        // door. Treat every functional state as a persistent physical control
        // connection so SCP-079 gets its hacking icon before the player presses
        // the panel.
        return block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get()
                || LeftDoorButtons.isFunctional(block)
                || MirroredDoorButtons.isFunctional(block);
    }

    public static List<BlockPos> structurePositions(BlockPos controller,
            Direction facing) {
        List<BlockPos> result = new ArrayList<>();
        result.add(controller.immutable());
        for (PartAddress part : parts()) {
            result.add(partPosition(controller, facing,
                    part.side(), part.height()));
        }
        return result;
    }

    /**
     * Intersects the authored door/frame envelope with the addressed multiblock
     * cell. The moving slab uses the same 33-pixel lift and easing clock as the
     * GeckoLib animation, so collision and SCP-173 vision follow the actual door.
     */
    public static VoxelShape shapeAt(BlockGetter level, BlockPos pos,
            BlockState state) {
        BlockPos controller;
        int side;
        int height;
        Direction facing;
        BlockState controllerState;

        if (BlastDoorModule.isController(state)) {
            controller = pos;
            side = 0;
            height = 0;
            facing = state.getValue(BlastDoorModule.FACING);
            controllerState = state;
        } else if (BlastDoorModule.isPart(state)) {
            side = decodeSide(state);
            height = state.getValue(BlastDoorModule.HEIGHT);
            facing = state.getValue(BlastDoorModule.FACING);
            controller = controllerPosition(pos, state);
            controllerState = level.getBlockState(controller);
            if (!BlastDoorModule.isController(controllerState)) {
                return Shapes.empty();
            }
        } else {
            return Shapes.empty();
        }

        double lift = 0.0D;
        if (level.getBlockEntity(controller)
                instanceof BlastDoorModule.BlastDoorBlockEntity door) {
            lift = door.doorLiftPixels();
        } else {
            BlastDoorModule.Phase phase =
                    controllerState.getValue(BlastDoorModule.PHASE);
            lift = phase == BlastDoorModule.Phase.OPEN
                    || phase == BlastDoorModule.Phase.CLOSING ? 33.0D : 0.0D;
        }

        VoxelShape canonical = Shapes.empty();
        canonical = addModelBox(canonical, side, height,
                -32.5D, 32.5D, 3.75D + lift, 47.5D + lift,
                -7.75D, 7.75D);
        canonical = addModelBox(canonical, side, height,
                -40.0D, -32.5D, 0.0D, 40.0D, -8.0D, 8.0D);
        canonical = addModelBox(canonical, side, height,
                32.5D, 40.0D, 0.0D, 40.0D, -8.0D, 8.0D);
        canonical = addModelBox(canonical, side, height,
                -40.0D, 40.0D, 40.0D, 60.0D, -8.0D, 8.0D);
        canonical = addModelBox(canonical, side, height,
                -32.5D, 32.5D, 0.0D, 1.5D, -7.75D, 7.75D);

        return rotateFromNorth(canonical, facing);
    }

    private static VoxelShape addModelBox(VoxelShape shape, int side,
            int height, double modelMinX, double modelMaxX,
            double modelMinY, double modelMaxY,
            double modelMinZ, double modelMaxZ) {
        double globalMinX = 8.0D + modelMinX;
        double globalMaxX = 8.0D + modelMaxX;
        double globalMinY = modelMinY;
        double globalMaxY = modelMaxY;
        double globalMinZ = 8.0D + modelMinZ;
        double globalMaxZ = 8.0D + modelMaxZ;

        double cellMinX = side * 16.0D;
        double cellMaxX = cellMinX + 16.0D;
        double cellMinY = height * 16.0D;
        double cellMaxY = cellMinY + 16.0D;

        double minX = Math.max(globalMinX, cellMinX);
        double maxX = Math.min(globalMaxX, cellMaxX);
        double minY = Math.max(globalMinY, cellMinY);
        double maxY = Math.min(globalMaxY, cellMaxY);
        double minZ = Math.max(globalMinZ, 0.0D);
        double maxZ = Math.min(globalMaxZ, 16.0D);

        if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
            return shape;
        }

        VoxelShape next = Block.box(
                minX - cellMinX, minY - cellMinY, minZ,
                maxX - cellMinX, maxY - cellMinY, maxZ);
        return Shapes.or(shape, next);
    }

    private static VoxelShape rotateFromNorth(VoxelShape shape,
            Direction facing) {
        if (facing == Direction.NORTH || shape.isEmpty()) return shape;
        VoxelShape result = Shapes.empty();
        for (AABB box : shape.toAabbs()) {
            double x1 = box.minX * 16.0D;
            double x2 = box.maxX * 16.0D;
            double y1 = box.minY * 16.0D;
            double y2 = box.maxY * 16.0D;
            double z1 = box.minZ * 16.0D;
            double z2 = box.maxZ * 16.0D;
            VoxelShape rotated = switch (facing) {
                case EAST -> Block.box(16.0D - z2, y1, x1,
                        16.0D - z1, y2, x2);
                case SOUTH -> Block.box(16.0D - x2, y1, 16.0D - z2,
                        16.0D - x1, y2, 16.0D - z1);
                case WEST -> Block.box(z1, y1, 16.0D - x2,
                        z2, y2, 16.0D - x1);
                default -> Block.box(x1, y1, z1, x2, y2, z2);
            };
            result = Shapes.or(result, rotated);
        }
        return result;
    }

    private static BlockState partState(Level level, BlockPos pos,
            Direction facing, PartAddress part) {
        return BlastDoorModule.PART.get().defaultBlockState()
                .setValue(BlastDoorModule.FACING, facing)
                .setValue(BlastDoorModule.SIDE, encodeSide(part.side()))
                .setValue(BlastDoorModule.HEIGHT, part.height())
                .setValue(BlastDoorModule.WATERLOGGED,
                        level.getFluidState(pos).getType() == Fluids.WATER);
    }

    public static int encodeSide(int side) {
        return side + 3;
    }

    public static int decodeSide(BlockState state) {
        return state.getValue(BlastDoorModule.SIDE) - 3;
    }

    public static boolean isPartAddress(int side, int height) {
        if (side < MIN_SIDE || side > MAX_SIDE
                || height < 0 || height > MAX_HEIGHT) {
            return false;
        }
        return !(side == 0 && height == 0);
    }

    private static List<PartAddress> parts() {
        List<PartAddress> result = new ArrayList<>();
        for (int height = 0; height <= MAX_HEIGHT; height++) {
            for (int side = MIN_SIDE; side <= MAX_SIDE; side++) {
                if (isPartAddress(side, height)) {
                    result.add(new PartAddress(side, height));
                }
            }
        }
        return result;
    }

    public static void clearBlock(Level level, BlockPos pos, BlockState state) {
        BlockState replacement = state.getFluidState().isEmpty()
                ? Blocks.AIR.defaultBlockState()
                : state.getFluidState().createLegacyBlock();
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private record PartAddress(int side, int height) {
    }
}
