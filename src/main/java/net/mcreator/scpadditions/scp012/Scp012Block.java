package net.mcreator.scpadditions.scp012;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** One registry block for one visible SCP-012 containment animation stage. */
public final class Scp012Block extends HorizontalDirectionalBlock {
    /** Four intermediate stages at fifteen ticks each make a three-second cycle. */
    public static final int ANIMATION_STEP_TICKS = 15;

    private final Scp012Stage stage;

    public Scp012Block(Scp012Stage stage) {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(4.0F, 20.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion());
        this.stage = stage;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Scp012Stage stage() {
        return stage;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
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
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && stage.isTransitional()) {
            level.scheduleTick(pos, this, ANIMATION_STEP_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
                     RandomSource random) {
        Scp012Module.advanceAnimation(level, pos, state, stage);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(Scp012Module.SCP_012_ITEM.get()));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
                                  BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return super.updateShape(state, direction, neighborState, level, pos,
                neighborPos);
    }
}
