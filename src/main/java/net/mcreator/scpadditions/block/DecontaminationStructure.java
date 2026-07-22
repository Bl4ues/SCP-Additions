package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.mcreator.scpadditions.facility.FacilityStructureBreakGuard;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.procedures.DecontaminationCheckpointController;

/**
 * Placement, validation and teardown rules for the decontamination checkpoint's
 * invisible collision shell.
 */
public final class DecontaminationStructure {
    private static final int MIN_OFFSET = -1;
    private static final int MAX_OFFSET = 1;

    private DecontaminationStructure() {
    }

    public static boolean canPlace(Level level, BlockPos controllerPos,
            Direction facing, BlockPos allowedOccupiedPos) {
        if (!level.getWorldBorder().isWithinBounds(controllerPos)
                || (!controllerPos.equals(allowedOccupiedPos)
                && !level.getBlockState(controllerPos).canBeReplaced())) {
            return false;
        }

        for (int offsetX = MIN_OFFSET; offsetX <= MAX_OFFSET; offsetX++) {
            for (int offsetY = MIN_OFFSET; offsetY <= MAX_OFFSET; offsetY++) {
                for (int offsetZ = MIN_OFFSET; offsetZ <= MAX_OFFSET; offsetZ++) {
                    if (isControllerOffset(offsetX, offsetY, offsetZ)
                            || !DecontaminationShapeHelper.hasStructurePart(
                            facing, offsetX, offsetY, offsetZ)) {
                        continue;
                    }
                    BlockPos partPos = controllerPos.offset(
                            offsetX, offsetY, offsetZ);
                    if (!level.getWorldBorder().isWithinBounds(partPos)
                            || (!partPos.equals(allowedOccupiedPos)
                            && !level.getBlockState(partPos).canBeReplaced())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean placeCollisionParts(Level level,
            BlockPos controllerPos, Direction facing) {
        for (int offsetX = MIN_OFFSET; offsetX <= MAX_OFFSET; offsetX++) {
            for (int offsetY = MIN_OFFSET; offsetY <= MAX_OFFSET; offsetY++) {
                for (int offsetZ = MIN_OFFSET; offsetZ <= MAX_OFFSET; offsetZ++) {
                    if (isControllerOffset(offsetX, offsetY, offsetZ)
                            || !DecontaminationShapeHelper.hasStructurePart(
                            facing, offsetX, offsetY, offsetZ)) {
                        continue;
                    }
                    BlockPos partPos = controllerPos.offset(
                            offsetX, offsetY, offsetZ);
                    BlockState current = level.getBlockState(partPos);
                    if (isMatchingCollisionPart(current, facing,
                            offsetX, offsetY, offsetZ)) {
                        continue;
                    }
                    if (!current.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }

        ensureCollisionParts(level, controllerPos, facing);
        return true;
    }

    public static void ensureCollisionParts(Level level,
            BlockPos controllerPos, Direction facing) {
        if (!isController(level.getBlockState(controllerPos))) {
            return;
        }

        for (int offsetX = MIN_OFFSET; offsetX <= MAX_OFFSET; offsetX++) {
            for (int offsetY = MIN_OFFSET; offsetY <= MAX_OFFSET; offsetY++) {
                for (int offsetZ = MIN_OFFSET; offsetZ <= MAX_OFFSET; offsetZ++) {
                    if (isControllerOffset(offsetX, offsetY, offsetZ)
                            || !DecontaminationShapeHelper.hasStructurePart(
                            facing, offsetX, offsetY, offsetZ)) {
                        continue;
                    }
                    BlockPos partPos = controllerPos.offset(
                            offsetX, offsetY, offsetZ);
                    BlockState current = level.getBlockState(partPos);
                    if (isMatchingCollisionPart(current, facing,
                            offsetX, offsetY, offsetZ)) {
                        continue;
                    }
                    if (current.canBeReplaced()) {
                        level.setBlock(partPos, collisionState(level, partPos,
                                facing, offsetX, offsetY, offsetZ),
                                Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    public static void removeCollisionParts(Level level,
            BlockPos controllerPos, BlockState controllerState) {
        Direction facing = controllerState.hasProperty(
                HorizontalDirectionalBlock.FACING)
                ? controllerState.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;

        for (int offsetX = MIN_OFFSET; offsetX <= MAX_OFFSET; offsetX++) {
            for (int offsetY = MIN_OFFSET; offsetY <= MAX_OFFSET; offsetY++) {
                for (int offsetZ = MIN_OFFSET; offsetZ <= MAX_OFFSET; offsetZ++) {
                    if (isControllerOffset(offsetX, offsetY, offsetZ)) {
                        continue;
                    }
                    BlockPos partPos = controllerPos.offset(
                            offsetX, offsetY, offsetZ);
                    BlockState partState = level.getBlockState(partPos);
                    if (partState.getBlock()
                            == DecontaminationStructureBlocks.collision()
                            && controllerPosition(partPos, partState)
                            .equals(controllerPos)) {
                        clearBlock(level, partPos, partState);
                    }
                }
            }
        }
    }

    public static void destroyFromCollision(Level level, BlockPos partPos,
            BlockState partState, boolean dropCheckpoint) {
        BlockPos controllerPos = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!isController(controllerState)) {
            clearBlock(level, partPos, partState);
            return;
        }

        removeCollisionParts(level, controllerPos, controllerState);
        DecontaminationCheckpointController.forget(level, controllerPos);
        FacilityStructureBreakGuard.clear(level, controllerPos);
        if (dropCheckpoint) {
            Block.popResource(level, controllerPos,
                    new ItemStack(ScpAdditionsModBlocks.DECON_OPEN.get()));
        }
        clearBlock(level, controllerPos, controllerState);
    }

    public static boolean isValidCollisionPart(BlockGetter level,
            BlockPos partPos, BlockState partState) {
        if (partState.getBlock()
                != DecontaminationStructureBlocks.collision()) {
            return false;
        }

        int offsetX = partState.getValue(
                DecontaminationCollisionBlock.OFFSET_X);
        int offsetY = partState.getValue(
                DecontaminationCollisionBlock.OFFSET_Y);
        int offsetZ = partState.getValue(
                DecontaminationCollisionBlock.OFFSET_Z);
        if (isControllerOffset(offsetX, offsetY, offsetZ)) {
            return false;
        }

        BlockPos controllerPos = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!isController(controllerState)
                || !controllerState.hasProperty(
                HorizontalDirectionalBlock.FACING)) {
            return false;
        }

        Direction facing = partState.getValue(
                DecontaminationCollisionBlock.FACING);
        return controllerState.getValue(HorizontalDirectionalBlock.FACING)
                == facing
                && DecontaminationShapeHelper.hasStructurePart(facing,
                offsetX, offsetY, offsetZ)
                && controllerPos.offset(offsetX, offsetY, offsetZ)
                .equals(partPos);
    }

    public static BlockPos controllerPosition(BlockPos partPos,
            BlockState partState) {
        return partPos.offset(
                -partState.getValue(DecontaminationCollisionBlock.OFFSET_X),
                -partState.getValue(DecontaminationCollisionBlock.OFFSET_Y),
                -partState.getValue(DecontaminationCollisionBlock.OFFSET_Z));
    }

    public static boolean isController(BlockState state) {
        Block block = state.getBlock();
        return block == ScpAdditionsModBlocks.DECON_OPEN.get()
                || block == ScpAdditionsModBlocks.DECON_CLOSED.get()
                || block == ScpAdditionsModBlocks.DECON_OPEN_RELOAD.get();
    }

    public static boolean isClosedController(BlockState state) {
        return state.is(ScpAdditionsModBlocks.DECON_CLOSED.get());
    }

    public static void clearBlock(Level level, BlockPos pos,
            BlockState state) {
        BlockState replacement = state.getFluidState().isEmpty()
                ? Blocks.AIR.defaultBlockState()
                : state.getFluidState().createLegacyBlock();
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private static boolean isMatchingCollisionPart(BlockState state,
            Direction facing, int offsetX, int offsetY, int offsetZ) {
        return state.getBlock() == DecontaminationStructureBlocks.collision()
                && state.getValue(DecontaminationCollisionBlock.FACING)
                == facing
                && state.getValue(DecontaminationCollisionBlock.OFFSET_X)
                == offsetX
                && state.getValue(DecontaminationCollisionBlock.OFFSET_Y)
                == offsetY
                && state.getValue(DecontaminationCollisionBlock.OFFSET_Z)
                == offsetZ;
    }

    private static BlockState collisionState(Level level, BlockPos pos,
            Direction facing, int offsetX, int offsetY, int offsetZ) {
        return DecontaminationStructureBlocks.collision()
                .defaultBlockState()
                .setValue(DecontaminationCollisionBlock.FACING, facing)
                .setValue(DecontaminationCollisionBlock.OFFSET_X, offsetX)
                .setValue(DecontaminationCollisionBlock.OFFSET_Y, offsetY)
                .setValue(DecontaminationCollisionBlock.OFFSET_Z, offsetZ)
                .setValue(DecontaminationCollisionBlock.WATERLOGGED,
                        level.getFluidState(pos).getType() == Fluids.WATER);
    }

    private static boolean isControllerOffset(int offsetX, int offsetY,
            int offsetZ) {
        return offsetX == 0 && offsetY == 0 && offsetZ == 0;
    }

}
