package com.bl4ues.scpclassifieddirective.facility.alarm;

import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared 3x3 wall-anchor logic for the Alarm.
 *
 * <p>The Alarm is physically small but may sit on the center, an edge or a
 * corner shared by neighbouring wall cells. The clicked cell remains the
 * controller; invisible helper cells reserve every other touched block so the
 * placement behaves as one physical object instead of allowing blocks to be
 * placed through half of it.</p>
 */
public final class AlarmMountStructure {
    public static final int NEGATIVE = -1;
    public static final int CENTER = 0;
    public static final int POSITIVE = 1;

    private AlarmMountStructure() {
    }

    public static int encodeSlot(int slot) {
        return Math.max(NEGATIVE, Math.min(POSITIVE, slot)) + 1;
    }

    public static int decodeSlot(int encoded) {
        return encoded - 1;
    }

    public static int quantize(double coordinate) {
        if (coordinate < (1.0D / 3.0D)) return NEGATIVE;
        if (coordinate > (2.0D / 3.0D)) return POSITIVE;
        return CENTER;
    }

    /**
     * Returns the click coordinate from left to right on the wall as seen from
     * the room, regardless of world-facing orientation.
     */
    public static double horizontalClick(BlockPlaceContext context,
            Direction facing) {
        BlockPos support = context.getClickedPos()
                .relative(facing.getOpposite());
        Vec3 click = context.getClickLocation();
        Vec3 center = Vec3.atCenterOf(support);
        Direction right = facing.getClockWise();
        return 0.5D + (click.x - center.x) * right.getStepX()
                + (click.z - center.z) * right.getStepZ();
    }

    public static double verticalClick(BlockPlaceContext context,
            Direction facing) {
        BlockPos support = context.getClickedPos()
                .relative(facing.getOpposite());
        return context.getClickLocation().y - support.getY();
    }

    public static Vec3 visualOffset(BlockState state) {
        if (state == null
                || !state.hasProperty(AlarmModule.FACING)
                || !state.hasProperty(AlarmModule.MOUNT_X)
                || !state.hasProperty(AlarmModule.MOUNT_Y)) {
            return Vec3.ZERO;
        }
        int horizontal = decodeSlot(state.getValue(AlarmModule.MOUNT_X));
        int vertical = decodeSlot(state.getValue(AlarmModule.MOUNT_Y));
        Direction right = state.getValue(AlarmModule.FACING).getClockWise();
        return new Vec3(
                right.getStepX() * horizontal * 0.5D,
                vertical * 0.5D,
                right.getStepZ() * horizontal * 0.5D);
    }

    public static List<BlockPos> occupiedPositions(BlockPos controller,
            BlockState state) {
        List<BlockPos> result = new ArrayList<>(4);
        if (controller == null || state == null
                || !state.hasProperty(AlarmModule.FACING)
                || !state.hasProperty(AlarmModule.MOUNT_X)
                || !state.hasProperty(AlarmModule.MOUNT_Y)) {
            return result;
        }

        int horizontal = decodeSlot(state.getValue(AlarmModule.MOUNT_X));
        int vertical = decodeSlot(state.getValue(AlarmModule.MOUNT_Y));
        Direction right = state.getValue(AlarmModule.FACING).getClockWise();

        int[] xs = horizontal == 0 ? new int[] { 0 }
                : new int[] { 0, horizontal };
        int[] ys = vertical == 0 ? new int[] { 0 }
                : new int[] { 0, vertical };

        for (int y : ys) {
            for (int x : xs) {
                result.add(controller.offset(
                        right.getStepX() * x, y,
                        right.getStepZ() * x).immutable());
            }
        }
        return result;
    }

    public static boolean canPlace(LevelReader level, BlockPos controller,
            BlockState state) {
        if (level == null || controller == null || state == null) return false;
        Direction facing = state.getValue(AlarmModule.FACING);
        for (BlockPos cell : occupiedPositions(controller, state)) {
            if (!cell.equals(controller)
                    && !level.getBlockState(cell).canBeReplaced()) {
                return false;
            }
            if (!hasSupport(level, cell, facing)) return false;
        }
        return true;
    }

    public static boolean canSurvive(BlockGetter level, BlockPos controller,
            BlockState state) {
        if (level == null || controller == null || state == null) return false;
        Direction facing = state.getValue(AlarmModule.FACING);
        for (BlockPos cell : occupiedPositions(controller, state)) {
            if (!hasSupport(level, cell, facing)) return false;
            if (cell.equals(controller)) continue;

            BlockState part = level.getBlockState(cell);
            if (!isValidPart(level, cell, part)) return false;
        }
        return true;
    }

    private static boolean hasSupport(BlockGetter level, BlockPos cell,
            Direction facing) {
        BlockPos support = cell.relative(facing.getOpposite());
        BlockState supportState = level.getBlockState(support);
        if (supportState.isFaceSturdy(level, support, facing)) return true;

        // Facility wall models are not always full-cube sturdy. The conceptual
        // top strip of a Blast Door is nevertheless a valid wall mounting area.
        return BlastDoorStructure.topMountController(
                level, support, facing) != null;
    }

    /**
     * The clicked fourth-height Blast Door strip accepts only the upper row of
     * the 3x3 Alarm anchors. The horizontal choice remains left/center/right.
     */
    public static boolean blastDoorPlacementAllowed(BlockGetter level,
            BlockPos controller, BlockState state) {
        BlockPos blastDoor = blastDoorController(level, controller, state);
        if (blastDoor == null) return true;
        return decodeSlot(state.getValue(AlarmModule.MOUNT_Y)) == POSITIVE;
    }

    @Nullable
    public static BlockPos blastDoorController(BlockGetter level,
            BlockPos controller, BlockState state) {
        if (level == null || controller == null || state == null
                || !state.hasProperty(AlarmModule.FACING)) {
            return null;
        }
        Direction facing = state.getValue(AlarmModule.FACING);
        BlockPos support = controller.relative(facing.getOpposite());
        return BlastDoorStructure.topMountController(
                level, support, facing);
    }

    public static boolean placeParts(Level level, BlockPos controller,
            BlockState state) {
        if (!canPlace(level, controller, state)) return false;

        Direction facing = state.getValue(AlarmModule.FACING);
        Direction right = facing.getClockWise();
        for (BlockPos cell : occupiedPositions(controller, state)) {
            if (cell.equals(controller)) continue;

            int dx = (cell.getX() - controller.getX()) * right.getStepX()
                    + (cell.getZ() - controller.getZ()) * right.getStepZ();
            int dy = cell.getY() - controller.getY();
            BlockState part = AlarmModule.PART.get().defaultBlockState()
                    .setValue(AlarmModule.FACING, facing)
                    .setValue(AlarmModule.PART_X, encodeSlot(dx))
                    .setValue(AlarmModule.PART_Y, encodeSlot(dy));
            level.setBlock(cell, part, Block.UPDATE_ALL);
        }
        return true;
    }

    public static void ensureParts(Level level, BlockPos controller,
            BlockState state) {
        if (level == null || controller == null || state == null) return;
        Direction facing = state.getValue(AlarmModule.FACING);
        Direction right = facing.getClockWise();

        for (BlockPos cell : occupiedPositions(controller, state)) {
            if (cell.equals(controller)) continue;
            BlockState existing = level.getBlockState(cell);
            if (isValidPart(level, cell, existing)) continue;
            if (!existing.canBeReplaced()) continue;

            int dx = (cell.getX() - controller.getX()) * right.getStepX()
                    + (cell.getZ() - controller.getZ()) * right.getStepZ();
            int dy = cell.getY() - controller.getY();
            level.setBlock(cell, AlarmModule.PART.get().defaultBlockState()
                    .setValue(AlarmModule.FACING, facing)
                    .setValue(AlarmModule.PART_X, encodeSlot(dx))
                    .setValue(AlarmModule.PART_Y, encodeSlot(dy)),
                    Block.UPDATE_ALL);
        }
    }

    public static void removeParts(Level level, BlockPos controller,
            BlockState controllerState) {
        if (level == null || controller == null || controllerState == null) {
            return;
        }
        for (BlockPos cell : occupiedPositions(controller, controllerState)) {
            if (cell.equals(controller)) continue;
            BlockState state = level.getBlockState(cell);
            if (AlarmModule.isPart(state)
                    && controllerPosition(cell, state).equals(controller)) {
                level.setBlock(cell,
                        state.getFluidState().isEmpty()
                                ? net.minecraft.world.level.block.Blocks.AIR
                                        .defaultBlockState()
                                : state.getFluidState().createLegacyBlock(),
                        Block.UPDATE_ALL);
            }
        }
    }

    public static boolean isValidPart(BlockGetter level, BlockPos pos,
            BlockState state) {
        if (!AlarmModule.isPart(state)) return false;
        BlockPos controller = controllerPosition(pos, state);
        BlockState controllerState = level.getBlockState(controller);
        if (!AlarmModule.isController(controllerState)) return false;

        Direction facing = controllerState.getValue(AlarmModule.FACING);
        if (state.getValue(AlarmModule.FACING) != facing) return false;

        int dx = decodeSlot(state.getValue(AlarmModule.PART_X));
        int dy = decodeSlot(state.getValue(AlarmModule.PART_Y));
        if (dx == 0 && dy == 0) return false;

        return occupiedPositions(controller, controllerState).contains(pos);
    }

    public static BlockPos controllerPosition(BlockPos partPos,
            BlockState partState) {
        Direction facing = partState.getValue(AlarmModule.FACING);
        Direction right = facing.getClockWise();
        int dx = decodeSlot(partState.getValue(AlarmModule.PART_X));
        int dy = decodeSlot(partState.getValue(AlarmModule.PART_Y));
        return partPos.offset(
                -right.getStepX() * dx, -dy,
                -right.getStepZ() * dx);
    }

    public static boolean hasNeighborSignal(Level level, BlockPos controller,
            BlockState state) {
        for (BlockPos pos : occupiedPositions(controller, state)) {
            if (level.hasNeighborSignal(pos)) return true;
        }
        return false;
    }

    public static boolean isControllerCell(BlockState state) {
        return AlarmModule.isController(state);
    }
}
