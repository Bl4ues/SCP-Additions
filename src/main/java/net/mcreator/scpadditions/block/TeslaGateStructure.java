package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

/**
 * Placement, validation and teardown rules for the Tesla Gate's invisible
 * collision shell.
 */
public final class TeslaGateStructure {
    private TeslaGateStructure() {
    }

    public static boolean canPlace(Level level, BlockPos controllerPos, Direction facing) {
        if (!level.getWorldBorder().isWithinBounds(controllerPos)
                || !level.getBlockState(controllerPos).canBeReplaced()) {
            return false;
        }

        for (TeslaGateCollisionBlock.Part part : TeslaGateCollisionBlock.Part.values()) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            if (!level.getWorldBorder().isWithinBounds(partPos)
                    || !level.getBlockState(partPos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    public static boolean placeCollisionParts(Level level, BlockPos controllerPos, Direction facing) {
        for (TeslaGateCollisionBlock.Part part : TeslaGateCollisionBlock.Part.values()) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            if (!level.getBlockState(partPos).canBeReplaced()) {
                return false;
            }
        }

        for (TeslaGateCollisionBlock.Part part : TeslaGateCollisionBlock.Part.values()) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            level.setBlock(partPos, collisionState(level, partPos, facing, part), Block.UPDATE_ALL);
        }
        return true;
    }

    public static void ensureCollisionParts(Level level, BlockPos controllerPos, Direction facing) {
        if (!isController(level.getBlockState(controllerPos))) {
            return;
        }

        for (TeslaGateCollisionBlock.Part part : TeslaGateCollisionBlock.Part.values()) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            BlockState current = level.getBlockState(partPos);
            if (current.getBlock() == TeslaGateStructureBlocks.collision()
                    && current.getValue(TeslaGateCollisionBlock.FACING) == facing
                    && current.getValue(TeslaGateCollisionBlock.PART) == part) {
                continue;
            }
            if (current.canBeReplaced()) {
                level.setBlock(partPos, collisionState(level, partPos, facing, part), Block.UPDATE_ALL);
            }
        }
    }

    public static void removeCollisionParts(Level level, BlockPos controllerPos, BlockState controllerState) {
        if (!controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);

        for (TeslaGateCollisionBlock.Part part : TeslaGateCollisionBlock.Part.values()) {
            BlockPos partPos = partPosition(controllerPos, facing, part);
            BlockState partState = level.getBlockState(partPos);
            if (partState.getBlock() == TeslaGateStructureBlocks.collision()
                    && controllerPosition(partPos, partState).equals(controllerPos)) {
                clearBlock(level, partPos, partState);
            }
        }
    }

    public static void destroyFromCollision(Level level, BlockPos partPos, BlockState partState,
            boolean dropGate) {
        BlockPos controllerPos = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controllerPos);

        if (!isController(controllerState)) {
            clearBlock(level, partPos, partState);
            return;
        }

        removeCollisionParts(level, controllerPos, controllerState);
        if (dropGate) {
            Block.popResource(level, controllerPos,
                    new ItemStack(ScpAdditionsModBlocks.TESLA_GATE.get()));
        }
        clearBlock(level, controllerPos, controllerState);
    }

    public static boolean isValidCollisionPart(Level level, BlockPos partPos, BlockState partState) {
        if (partState.getBlock() != TeslaGateStructureBlocks.collision()) {
            return false;
        }

        BlockPos controllerPos = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!isController(controllerState)
                || !controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false;
        }

        Direction facing = partState.getValue(TeslaGateCollisionBlock.FACING);
        return controllerState.getValue(HorizontalDirectionalBlock.FACING) == facing
                && partPosition(controllerPos, facing,
                        partState.getValue(TeslaGateCollisionBlock.PART)).equals(partPos);
    }

    public static BlockPos partPosition(BlockPos controllerPos, Direction facing,
            TeslaGateCollisionBlock.Part part) {
        Direction right = facing.getClockWise();
        return controllerPos.offset(
                right.getStepX() * part.sideOffset(),
                part.yOffset(),
                right.getStepZ() * part.sideOffset());
    }

    public static BlockPos controllerPosition(BlockPos partPos, BlockState partState) {
        Direction facing = partState.getValue(TeslaGateCollisionBlock.FACING);
        TeslaGateCollisionBlock.Part part = partState.getValue(TeslaGateCollisionBlock.PART);
        Direction right = facing.getClockWise();
        return partPos.offset(
                -right.getStepX() * part.sideOffset(),
                -part.yOffset(),
                -right.getStepZ() * part.sideOffset());
    }

    public static boolean isController(BlockState state) {
        Block block = state.getBlock();
        return block == ScpAdditionsModBlocks.TESLA_GATE.get()
                || block == ScpAdditionsModBlocks.TESLA_ACTIVE.get()
                || block == ScpAdditionsModBlocks.TESLA_RECHARGE.get()
                || block == ScpAdditionsModBlocks.TESLA_ACTIVE_2.get()
                || block == ScpAdditionsModBlocks.TESLA_ACTIVE_3.get()
                || block == ScpAdditionsModBlocks.TESLA_ACTIVE_4.get();
    }

    public static void clearBlock(Level level, BlockPos pos, BlockState state) {
        BlockState replacement = state.getFluidState().isEmpty()
                ? Blocks.AIR.defaultBlockState()
                : state.getFluidState().createLegacyBlock();
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private static BlockState collisionState(Level level, BlockPos pos, Direction facing,
            TeslaGateCollisionBlock.Part part) {
        return TeslaGateStructureBlocks.collision().defaultBlockState()
                .setValue(TeslaGateCollisionBlock.FACING, facing)
                .setValue(TeslaGateCollisionBlock.PART, part)
                .setValue(TeslaGateCollisionBlock.WATERLOGGED,
                        level.getFluidState(pos).getType() == Fluids.WATER);
    }
}
