package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.block.entity.Scp902BlockEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlockEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Shared GeckoLib-backed behavior for the legacy closed/open SCP-902 ids. */
abstract class Scp902BlockBase extends BaseEntityBlock
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    /*
     * These bounds follow the authored GeckoLib geometry instead of the old
     * placeholder footprint. The open form includes the translated lid, which
     * extends the model farther along its local X axis.
     */
    private static final VoxelShape CLOSED_NORTH =
            Block.box(3.825D, 0.0D, 7.0D, 9.55D, 1.65D, 9.0D);
    private static final VoxelShape CLOSED_SOUTH =
            Block.box(6.45D, 0.0D, 7.0D, 12.175D, 1.65D, 9.0D);
    private static final VoxelShape CLOSED_EAST =
            Block.box(7.0D, 0.0D, 3.825D, 9.0D, 1.65D, 9.55D);
    private static final VoxelShape CLOSED_WEST =
            Block.box(7.0D, 0.0D, 6.45D, 9.0D, 1.65D, 12.175D);

    private static final VoxelShape OPEN_NORTH =
            Block.box(3.825D, 0.0D, 7.0D, 11.85D, 1.65D, 9.0D);
    private static final VoxelShape OPEN_SOUTH =
            Block.box(4.15D, 0.0D, 7.0D, 12.175D, 1.65D, 9.0D);
    private static final VoxelShape OPEN_EAST =
            Block.box(7.0D, 0.0D, 3.825D, 9.0D, 1.65D, 11.85D);
    private static final VoxelShape OPEN_WEST =
            Block.box(7.0D, 0.0D, 4.15D, 9.0D, 1.65D, 12.175D);

    protected Scp902BlockBase() {
        super(BlockBehaviour.Properties.of()
                .ignitedByLava()
                .sound(SoundType.WOOD)
                .strength(30.0F, 10.0F)
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Scp902BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
            BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type,
                ScpClassifiedDirectiveModBlockEntities.SCP_902.get(),
                Scp902BlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Scp902BlockEntity box)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return box.handleUse(player);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, waterlogged);
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        boolean open = state.is(ScpClassifiedDirectiveModBlocks.SCP_902_OPEN.get());
        return switch (state.getValue(FACING)) {
            case SOUTH -> open ? OPEN_SOUTH : CLOSED_SOUTH;
            case EAST -> open ? OPEN_EAST : CLOSED_EAST;
            case WEST -> open ? OPEN_WEST : CLOSED_WEST;
            default -> open ? OPEN_NORTH : CLOSED_NORTH;
        };
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
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level,
            BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state,
            BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }
}
