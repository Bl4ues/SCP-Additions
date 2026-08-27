package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.block.entity.DecontaminationBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.FacilityStructureBreakGuard;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.procedures.DecontaminationCheckpointController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Shared GeckoLib controller behavior for all legacy visual state IDs. */
public abstract class AbstractDecontaminationBlock extends BaseEntityBlock
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected AbstractDecontaminationBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(20.0F, 30.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    protected abstract boolean isClosedState();

    protected void controllerPlaced(BlockState state, Level level,
            BlockPos pos, BlockState oldState, boolean moving) {
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecontaminationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                ScpClassifiedDirectiveModBlockEntities.DECONTAMINATION.get(),
                DecontaminationBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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

    /** The visible body is large; this local cell remains its break target. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return DecontaminationShapeHelper.controllerSelectionShape();
    }

    /** Physical collision belongs to the owned floor/wall/ceiling helpers. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // BlockPlaceContext already resolves the actual target cell. The
        // controller stays there; the authored floor lives one block below and
        // must be cleared rather than causing the structure to auto-raise.
        BlockPos controllerPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!DecontaminationStructure.hasPlacementSupport(
                context.getLevel(), controllerPos, facing)
                || !DecontaminationStructure.canPlace(context.getLevel(),
                controllerPos, facing, controllerPos)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(WATERLOGGED, context.getLevel()
                        .getFluidState(controllerPos).getType() == Fluids.WATER);
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
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ScpClassifiedDirectiveModBlocks.DECON_OPEN.get());
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level,
            BlockPos pos, Player player) {
        return player.getInventory().getSelected().getItem()
                instanceof PickaxeItem pickaxe
                && pickaxe.getTier().getLevel() >= 1;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        List<ItemStack> original = super.getDrops(state, builder);
        return original.isEmpty()
                ? Collections.singletonList(new ItemStack(
                ScpClassifiedDirectiveModBlocks.DECON_OPEN.get()))
                : original;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level.isClientSide) return;

        Direction facing = state.getValue(FACING);
        if (!DecontaminationStructure.isController(oldState)) {
            if (!DecontaminationStructure.placeStructure(level, pos, facing)) {
                DecontaminationStructure.removeStructureParts(level, pos, state);
                DecontaminationStructure.clearBlock(level, pos, state);
                return;
            }
        } else {
            DecontaminationStructure.ensureStructure(level, pos, facing);
        }
        controllerPlaced(state, level, pos, oldState, moving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean moving) {
        if (!level.isClientSide
                && !DecontaminationStructure.isController(newState)) {
            DecontaminationStructure.removeStructureParts(level, pos, state);
            DecontaminationCheckpointController.forget(level, pos);
            FacilityStructureBreakGuard.clear(level, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    protected final void ensureStructure(Level level, BlockPos pos,
            BlockState state) {
        if (!level.isClientSide) {
            DecontaminationStructure.ensureStructure(level, pos,
                    state.getValue(FACING));
        }
    }
}
