
package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.entity.Scp173Entity;

import javax.annotation.Nullable;

/** Decorative folding caution sign with eight placement orientations. */
public final class WetFloorBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);

    private static final VoxelShape NORTH_SOUTH = box(5.0D, 0.0D, 3.0D,
            11.0D, 16.0D, 13.0D);
    private static final VoxelShape EAST_WEST = box(3.0D, 0.0D, 5.0D,
            13.0D, 16.0D, 11.0D);
    private static final VoxelShape DIAGONAL = box(4.0D, 0.0D, 4.0D,
            12.0D, 16.0D, 12.0D);

    public WetFloorBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(0.8F)
                .noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WetFloorBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!canStandOn(context.getLevel(), context.getClickedPos())) return null;
        int rotation = Mth.floor((context.getRotation() + 180.0F + 22.5F) / 45.0F) & 7;
        return defaultBlockState().setValue(ROTATION, rotation);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canStandOn(level, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
            LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !canStandOn(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        int rotation = state.getValue(ROTATION);
        if ((rotation & 1) != 0) return DIAGONAL;
        return (rotation & 2) == 0 ? NORTH_SOUTH : EAST_WEST;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        // The authored sign occupies most of one navigation cell even though it
        // is only a light decorative prop. Minecraft's node grid consequently
        // treats several otherwise open arrangements as sealed, and SCP-173's
        // collision-safe snap movement repeatedly stages against the oversized
        // bounding prism. The statue ignores only this prop's physical shape;
        // players and every other entity retain the normal model-matched
        // collision and selection box.
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Scp173Entity) {
            return Shapes.empty();
        }
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        int offset = switch (rotation) {
            case CLOCKWISE_90 -> 2;
            case CLOCKWISE_180 -> 4;
            case COUNTERCLOCKWISE_90 -> 6;
            default -> 0;
        };
        return state.setValue(ROTATION, (state.getValue(ROTATION) + offset) & 7);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        int value = state.getValue(ROTATION);
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(ROTATION, (-value) & 7);
            case FRONT_BACK -> state.setValue(ROTATION, (4 - value) & 7);
            default -> state;
        };
    }

    private static boolean canStandOn(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
