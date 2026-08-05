package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;

import java.util.Collections;
import java.util.List;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Redstone-fed auxiliary generator for the global SCiPNET facility bus. */
public final class Scp079AuxiliaryPowerBlock extends HorizontalDirectionalBlock
        implements SimpleWaterloggedBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    private static final double CHIMNEY_X = 1.5D / 16.0D;
    private static final double CHIMNEY_Y = 24.5D / 16.0D;
    private static final double CHIMNEY_Z = 18.6D / 16.0D;

    public Scp079AuxiliaryPowerBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                .strength(30.0F, 100.0F).noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,
            BlockState> builder) {
        builder.add(FACING, POWERED, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(FACING,
                        context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, level.hasNeighborSignal(pos))
                .setValue(WATERLOGGED,
                        level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
            Block neighborBlock, BlockPos neighborPos, boolean moving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos,
                moving);
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED)) return;

        BlockState updated = state.setValue(POWERED, powered);
        level.setBlock(pos, updated, Block.UPDATE_ALL);
        if (level instanceof ServerLevel server) {
            Scp079FacilityAccessManager.updateAuxiliaryUnitPower(server, pos,
                    powered);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos,
            RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (!state.getValue(POWERED) || state.getValue(WATERLOGGED)
                || random.nextInt(3) != 0) {
            return;
        }

        double[] rotated = rotateLocal(CHIMNEY_X, CHIMNEY_Z,
                state.getValue(FACING));
        level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + rotated[0],
                pos.getY() + CHIMNEY_Y,
                pos.getZ() + rotated[1],
                (random.nextDouble() - 0.5D) * 0.006D,
                0.020D + random.nextDouble() * 0.016D,
                (random.nextDouble() - 0.5D) * 0.006D);
    }

    private static double[] rotateLocal(double x, double z,
            Direction facing) {
        return switch (facing) {
            case EAST -> new double[] {1.0D - z, x};
            case SOUTH -> new double[] {1.0D - x, 1.0D - z};
            case WEST -> new double[] {z, 1.0D - x};
            default -> new double[] {x, z};
        };
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide && level instanceof ServerLevel server
                && oldState.getBlock() != this) {
            Scp079FacilityAccessManager.registerAuxiliaryUnit(server, pos,
                    state.getValue(POWERED));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean moving) {
        if (!level.isClientSide && level instanceof ServerLevel server
                && newState.getBlock() != this) {
            Scp079FacilityAccessManager.unregisterAuxiliaryUnit(server, pos);
        }
        super.onRemove(state, level, pos, newState, moving);
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

    // BEGIN GENERATED MODEL COLLISION
    private static final VoxelShape MODEL_SHAPE_NORTH = Shapes.or(
            Block.box(0.0D, 0.0D, -3.0D, 16.0D, 2.0D, 32.0D),
            Block.box(1.0D, 2.0D, -2.0D, 4.0D, 23.0D, -1.0D),
            Block.box(12.0D, 2.0D, -2.0D, 15.0D, 23.0D, -1.0D),
            Block.box(-4.0D, 2.0D, 6.0D, 20.0D, 26.0D, 32.0D),
            Block.box(-5.0D, 2.0D, 14.0D, -4.0D, 17.0D, 23.0D),
            Block.box(0.0D, 6.0D, -5.0D, 16.0D, 22.0D, -2.0D),
            Block.box(0.0D, 6.0D, -2.0D, 1.0D, 23.0D, 6.0D),
            Block.box(4.0D, 6.0D, -2.0D, 12.0D, 22.0D, 6.0D),
            Block.box(15.0D, 6.0D, -2.0D, 16.0D, 23.0D, 6.0D),
            Block.box(1.0D, 6.0D, -1.0D, 4.0D, 23.0D, 6.0D),
            Block.box(12.0D, 6.0D, -1.0D, 15.0D, 23.0D, 6.0D),
            Block.box(-2.0D, 9.0D, -2.0D, 0.0D, 23.0D, 6.0D),
            Block.box(16.0D, 9.0D, -2.0D, 18.0D, 23.0D, 6.0D),
            Block.box(4.0D, 22.0D, -2.0D, 7.0D, 23.0D, 6.0D),
            Block.box(9.0D, 22.0D, -2.0D, 12.0D, 23.0D, 6.0D));
    private static final VoxelShape MODEL_SHAPE_EAST = Shapes.or(
            Block.box(-16.0D, 0.0D, 0.0D, 19.0D, 2.0D, 16.0D),
            Block.box(17.0D, 2.0D, 1.0D, 18.0D, 23.0D, 4.0D),
            Block.box(17.0D, 2.0D, 12.0D, 18.0D, 23.0D, 15.0D),
            Block.box(-16.0D, 2.0D, -4.0D, 10.0D, 26.0D, 20.0D),
            Block.box(-7.0D, 2.0D, -5.0D, 2.0D, 17.0D, -4.0D),
            Block.box(18.0D, 6.0D, 0.0D, 21.0D, 22.0D, 16.0D),
            Block.box(10.0D, 6.0D, 0.0D, 18.0D, 23.0D, 1.0D),
            Block.box(10.0D, 6.0D, 4.0D, 18.0D, 22.0D, 12.0D),
            Block.box(10.0D, 6.0D, 15.0D, 18.0D, 23.0D, 16.0D),
            Block.box(10.0D, 6.0D, 1.0D, 17.0D, 23.0D, 4.0D),
            Block.box(10.0D, 6.0D, 12.0D, 17.0D, 23.0D, 15.0D),
            Block.box(10.0D, 9.0D, -2.0D, 18.0D, 23.0D, 0.0D),
            Block.box(10.0D, 9.0D, 16.0D, 18.0D, 23.0D, 18.0D),
            Block.box(10.0D, 22.0D, 4.0D, 18.0D, 23.0D, 7.0D),
            Block.box(10.0D, 22.0D, 9.0D, 18.0D, 23.0D, 12.0D));
    private static final VoxelShape MODEL_SHAPE_SOUTH = Shapes.or(
            Block.box(0.0D, 0.0D, -16.0D, 16.0D, 2.0D, 19.0D),
            Block.box(12.0D, 2.0D, 17.0D, 15.0D, 23.0D, 18.0D),
            Block.box(1.0D, 2.0D, 17.0D, 4.0D, 23.0D, 18.0D),
            Block.box(-4.0D, 2.0D, -16.0D, 20.0D, 26.0D, 10.0D),
            Block.box(20.0D, 2.0D, -7.0D, 21.0D, 17.0D, 2.0D),
            Block.box(0.0D, 6.0D, 18.0D, 16.0D, 22.0D, 21.0D),
            Block.box(15.0D, 6.0D, 10.0D, 16.0D, 23.0D, 18.0D),
            Block.box(4.0D, 6.0D, 10.0D, 12.0D, 22.0D, 18.0D),
            Block.box(0.0D, 6.0D, 10.0D, 1.0D, 23.0D, 18.0D),
            Block.box(12.0D, 6.0D, 10.0D, 15.0D, 23.0D, 17.0D),
            Block.box(1.0D, 6.0D, 10.0D, 4.0D, 23.0D, 17.0D),
            Block.box(16.0D, 9.0D, 10.0D, 18.0D, 23.0D, 18.0D),
            Block.box(-2.0D, 9.0D, 10.0D, 0.0D, 23.0D, 18.0D),
            Block.box(9.0D, 22.0D, 10.0D, 12.0D, 23.0D, 18.0D),
            Block.box(4.0D, 22.0D, 10.0D, 7.0D, 23.0D, 18.0D));
    private static final VoxelShape MODEL_SHAPE_WEST = Shapes.or(
            Block.box(-3.0D, 0.0D, 0.0D, 32.0D, 2.0D, 16.0D),
            Block.box(-2.0D, 2.0D, 12.0D, -1.0D, 23.0D, 15.0D),
            Block.box(-2.0D, 2.0D, 1.0D, -1.0D, 23.0D, 4.0D),
            Block.box(6.0D, 2.0D, -4.0D, 32.0D, 26.0D, 20.0D),
            Block.box(14.0D, 2.0D, 20.0D, 23.0D, 17.0D, 21.0D),
            Block.box(-5.0D, 6.0D, 0.0D, -2.0D, 22.0D, 16.0D),
            Block.box(-2.0D, 6.0D, 15.0D, 6.0D, 23.0D, 16.0D),
            Block.box(-2.0D, 6.0D, 4.0D, 6.0D, 22.0D, 12.0D),
            Block.box(-2.0D, 6.0D, 0.0D, 6.0D, 23.0D, 1.0D),
            Block.box(-1.0D, 6.0D, 12.0D, 6.0D, 23.0D, 15.0D),
            Block.box(-1.0D, 6.0D, 1.0D, 6.0D, 23.0D, 4.0D),
            Block.box(-2.0D, 9.0D, 16.0D, 6.0D, 23.0D, 18.0D),
            Block.box(-2.0D, 9.0D, -2.0D, 6.0D, 23.0D, 0.0D),
            Block.box(-2.0D, 22.0D, 9.0D, 6.0D, 23.0D, 12.0D),
            Block.box(-2.0D, 22.0D, 4.0D, 6.0D, 23.0D, 7.0D));

    private static VoxelShape modelShape(Direction facing) {
        return switch (facing) {
            case EAST -> MODEL_SHAPE_EAST;
            case SOUTH -> MODEL_SHAPE_SOUTH;
            case WEST -> MODEL_SHAPE_WEST;
            default -> MODEL_SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return modelShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return modelShape(state.getValue(FACING));
    }
    // END GENERATED MODEL COLLISION (auxiliary generator)
    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Redstone-powered Auxiliary Power Unit."));
        tooltip.add(Component.literal("Each active unit supplies 0.1 AP/s to SCiPNET."));
        super.appendHoverText(stack, level, tooltip, flag);
    }
    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }
}
