package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.facility.FacilityStructureBreakGuard;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.procedures.DecontaminationCheckpointController;

import java.util.Collections;
import java.util.List;

/**
 * Common controller behavior for all visual states of a decontamination
 * checkpoint. Collision outside the controller cell is delegated to invisible
 * structure parts.
 */
public abstract class AbstractDecontaminationBlock extends Block
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    protected AbstractDecontaminationBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(20.0F, 30.0F)
                .lightLevel(state -> 6)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    protected abstract boolean isClosedState();

    protected boolean raisesOnInitialPlacement() {
        return false;
    }

    protected void controllerPlaced(BlockState state, Level level,
            BlockPos pos, BlockState oldState, boolean moving) {
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return DecontaminationShapeHelper.localShape(
                state.getValue(FACING), isClosedState(), 0, 0, 0);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return DecontaminationShapeHelper.localShape(
                state.getValue(FACING), isClosedState(), 0, 0, 0);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos placementPos = context.getClickedPos();
        BlockPos controllerPos = placementPos.above();
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!context.getLevel().getBlockState(placementPos.below())
                .isFaceSturdy(context.getLevel(), placementPos.below(),
                        Direction.UP)
                || !DecontaminationStructure.canPlace(context.getLevel(),
                controllerPos, facing, placementPos)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, context.getLevel()
                        .getFluidState(placementPos).getType() == Fluids.WATER);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING,
                rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER,
                    Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos,
                neighborPos);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level,
            BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level,
            BlockPos pos) {
        return 0;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ScpAdditionsModBlocks.DECON_OPEN.get());
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level,
            BlockPos pos, Player player) {
        if (player.getInventory().getSelected().getItem()
                instanceof PickaxeItem pickaxe) {
            return pickaxe.getTier().getLevel() >= 1;
        }
        return false;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        List<ItemStack> original = super.getDrops(state, builder);
        return original.isEmpty()
                ? Collections.singletonList(new ItemStack(
                ScpAdditionsModBlocks.DECON_OPEN.get()))
                : original;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        if (raisesOnInitialPlacement()
                && !DecontaminationStructure.isController(oldState)
                && tryRaiseOnPlacement(state, level, pos, oldState, moving)) {
            return;
        }

        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide) {
            DecontaminationStructure.ensureCollisionParts(level, pos,
                    state.getValue(FACING));
            controllerPlaced(state, level, pos, oldState, moving);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean moving) {
        if (!level.isClientSide
                && !DecontaminationStructure.isController(newState)) {
            DecontaminationStructure.removeCollisionParts(level, pos, state);
            DecontaminationCheckpointController.forget(level, pos);
            FacilityStructureBreakGuard.clear(level, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    protected final void ensureStructure(Level level, BlockPos pos,
            BlockState state) {
        if (!level.isClientSide) {
            DecontaminationStructure.ensureCollisionParts(level, pos,
                    state.getValue(FACING));
        }
    }

    private boolean tryRaiseOnPlacement(BlockState state, Level level,
            BlockPos pos, BlockState oldState, boolean moving) {
        if (moving || level.isClientSide || oldState.getBlock() == this
                || level.getBlockState(pos.below()).getBlock() == this
                || !level.getBlockState(pos.below()).isFaceSturdy(level,
                pos.below(), Direction.UP)) {
            return false;
        }

        Direction facing = state.getValue(FACING);
        BlockPos raisedPos = pos.above();
        if (!DecontaminationStructure.canPlace(level, raisedPos, facing, pos)) {
            return false;
        }

        level.setBlock(raisedPos, state, Block.UPDATE_ALL);
        level.setBlock(pos, state.getValue(WATERLOGGED)
                ? Blocks.WATER.defaultBlockState()
                : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (!DecontaminationStructure.placeCollisionParts(level, raisedPos,
                facing)) {
            BlockState raisedState = level.getBlockState(raisedPos);
            DecontaminationStructure.removeCollisionParts(level, raisedPos,
                    raisedState);
            DecontaminationStructure.clearBlock(level, raisedPos, raisedState);
        }
        return true;
    }
}
