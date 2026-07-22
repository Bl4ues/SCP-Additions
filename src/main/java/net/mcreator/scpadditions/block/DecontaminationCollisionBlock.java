package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

import java.util.Collections;
import java.util.List;

/**
 * Invisible block-local pieces making up a decontamination checkpoint. The
 * visible model stays on its controller; these blocks provide precise collision
 * and forward breaking to that controller.
 */
public final class DecontaminationCollisionBlock extends Block
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty OFFSET_X = IntegerProperty.create(
            "offset_x", 0, 2);
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create(
            "offset_y", 0, 2);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create(
            "offset_z", 0, 2);
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    public DecontaminationCollisionBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(20.0F, 30.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_X, encodeOffset(-1))
                .setValue(OFFSET_Y, encodeOffset(-1))
                .setValue(OFFSET_Z, encodeOffset(-1))
                .setValue(WATERLOGGED, false));
    }

    public static int encodeOffset(int offset) {
        if (offset < -1 || offset > 1) {
            throw new IllegalArgumentException(
                    "Decontamination offset must be between -1 and 1: "
                            + offset);
        }
        return offset + 1;
    }

    public static int decodeOffset(int storedOffset) {
        return storedOffset - 1;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_X, OFFSET_Y, OFFSET_Z, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return localShape(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return localShape(state, level, pos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static VoxelShape localShape(BlockState state, BlockGetter level,
            BlockPos pos) {
        if (!DecontaminationStructure.isValidCollisionPart(
                level, pos, state)) {
            return Shapes.empty();
        }
        BlockState controllerState = level.getBlockState(
                DecontaminationStructure.controllerPosition(pos, state));
        return DecontaminationShapeHelper.localShape(
                state.getValue(FACING),
                DecontaminationStructure.isClosedController(controllerState),
                decodeOffset(state.getValue(OFFSET_X)),
                decodeOffset(state.getValue(OFFSET_Y)),
                decodeOffset(state.getValue(OFFSET_Z)));
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
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING,
                rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
            BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 40);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        if (!DecontaminationStructure.isValidCollisionPart(
                level, pos, state)) {
            DecontaminationStructure.clearBlock(level, pos, state);
            return;
        }
        BlockPos controller = DecontaminationStructure.controllerPosition(
                pos, state);
        BlockState controllerState = level.getBlockState(controller);
        DecontaminationStructure.ensureCollisionParts(level, controller,
                controllerState.getValue(HorizontalDirectionalBlock.FACING));
        level.scheduleTick(pos, this, 40);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ScpAdditionsModBlocks.DECON_OPEN.get());
    }
}
