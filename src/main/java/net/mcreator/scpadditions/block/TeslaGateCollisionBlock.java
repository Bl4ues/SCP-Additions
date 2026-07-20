package net.mcreator.scpadditions.block;

import net.minecraft.world.level.LevelReader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
 * Invisible physical pieces surrounding a Tesla Gate. The model remains on the
 * controller block; these parts only supply accurate, local collision.
 */
public final class TeslaGateCollisionBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape TOP_SLAB = Block.box(0, 8, 0, 16, 16, 16);

    public TeslaGateCollisionBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(30.0F, 100.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.LEFT_BOTTOM)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return collisionShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return collisionShape(state);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    private static VoxelShape collisionShape(BlockState state) {
        Part part = state.getValue(PART);
        if (part == Part.TOP_CENTER) {
            return TOP_SLAB;
        }

        Direction right = state.getValue(FACING).getClockWise();
        Direction outward = part.sideOffset() < 0 ? right.getOpposite() : right;
        VoxelShape vertical = verticalHalf(outward);
        return part.yOffset() == 1 ? Shapes.or(vertical, TOP_SLAB) : vertical;
    }

    private static VoxelShape verticalHalf(Direction outward) {
        return switch (outward) {
            case EAST -> Block.box(8, 0, 0, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, 8, 16, 16);
            case SOUTH -> Block.box(0, 0, 8, 16, 16, 16);
            case NORTH -> Block.box(0, 0, 0, 16, 16, 8);
            default -> Shapes.empty();
        };
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
            boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 40);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!TeslaGateStructure.isValidCollisionPart(level, pos, state)) {
            TeslaGateStructure.clearBlock(level, pos, state);
            return;
        }

        BlockPos controller = TeslaGateStructure.controllerPosition(pos, state);
        BlockState controllerState = level.getBlockState(controller);
        TeslaGateStructure.ensureCollisionParts(level, controller,
                controllerState.getValue(HorizontalDirectionalBlock.FACING));
        level.scheduleTick(pos, this, 40);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(ScpAdditionsModBlocks.TESLA_GATE.get());
    }

    public enum Part implements StringRepresentable {
        LEFT_BOTTOM("left_bottom", -1, -1),
        LEFT_MIDDLE("left_middle", -1, 0),
        LEFT_TOP("left_top", -1, 1),
        TOP_CENTER("top_center", 0, 1),
        RIGHT_TOP("right_top", 1, 1),
        RIGHT_MIDDLE("right_middle", 1, 0),
        RIGHT_BOTTOM("right_bottom", 1, -1);

        private final String serializedName;
        private final int sideOffset;
        private final int yOffset;

        Part(String serializedName, int sideOffset, int yOffset) {
            this.serializedName = serializedName;
            this.sideOffset = sideOffset;
            this.yOffset = yOffset;
        }

        public int sideOffset() {
            return sideOffset;
        }

        public int yOffset() {
            return yOffset;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
