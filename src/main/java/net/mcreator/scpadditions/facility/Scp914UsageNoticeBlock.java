package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Static decorative SCP-914 notice using the Sign Support frame. */
public final class Scp914UsageNoticeBlock extends BaseEntityBlock
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    public Scp914UsageNoticeBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.GLASS)
                .strength(1.0F, 10.0F).noOcclusion().randomTicks()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis() == Direction.Axis.Y) return null;
        if (!FacilityLargePropStructure.canPlace(context.getLevel(),
                context.getClickedPos(),
                FacilityLargePropStructure.Kind.SCP_914_NOTICE, clickedFace)) {
            return null;
        }
        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        BlockState state = defaultBlockState().setValue(FACING, clickedFace)
                .setValue(WATERLOGGED, waterlogged);
        return state.canSurvive(context.getLevel(), context.getClickedPos())
                ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state,
            net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return WallMountedSupportEvents.hasLargePropWallSupport(level, pos,
                FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                state.getValue(FACING));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return FacilityLargePropStructure.controllerShape(
                FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level,
            BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Scp914UsageNoticeBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (level.isClientSide || oldState.getBlock() == this) return;
        Direction facing = state.getValue(FACING);
        if (!FacilityLargePropStructure.placeParts(level, pos,
                FacilityLargePropStructure.Kind.SCP_914_NOTICE, facing)) {
            level.destroyBlock(pos, true);
            return;
        }
        level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        FacilityLargePropStructure.ensureParts(level, pos,
                FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                state.getValue(FACING));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        FacilityLargePropStructure.ensureParts(level, pos,
                FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                state.getValue(FACING));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock() && !level.isClientSide) {
            FacilityLargePropStructure.removeParts(level, pos,
                    FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                    state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(this);
    }
}
