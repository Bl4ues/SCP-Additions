package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
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

import java.util.Collections;
import java.util.List;

/** Invisible local collision/reservation cells around the large GeckoLib gate. */
public final class TeslaGateCollisionBlock extends Block implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape FULL = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape TOP = Block.box(0, 10, 0, 16, 16, 16);
    private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 1.5, 16);

    public TeslaGateCollisionBlock() {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                .strength(30.0F, 100.0F).requiresCorrectToolForDrops()
                .noOcclusion().pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.LEFT_0)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, WATERLOGGED);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

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
        return switch (part.kind()) {
            case FULL -> FULL;
            case TOP -> TOP;
            case FLOOR -> FLOOR;
            case FRAME -> verticalHalf(state, part.sideOffset());
        };
    }

    private static VoxelShape verticalHalf(BlockState state, int sideOffset) {
        Direction right = state.getValue(FACING).getClockWise();
        Direction outward = sideOffset < 0 ? right.getOpposite() : right;
        return switch (outward) {
            case EAST -> Block.box(8, 0, 0, 16, 16, 16);
            case WEST -> Block.box(0, 0, 0, 8, 16, 16);
            case SOUTH -> Block.box(0, 0, 8, 16, 16, 16);
            case NORTH -> Block.box(0, 0, 0, 16, 16, 8);
            default -> Shapes.empty();
        };
    }

    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return state.getFluidState().isEmpty(); }
    @Override public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) { return 0; }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide) level.scheduleTick(pos, this, 40);
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

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) { return Collections.emptyList(); }
    @Override public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) { return new ItemStack(ScpClassifiedDirectiveModBlocks.TESLA_GATE.get()); }

    public enum Kind { FRAME, FULL, TOP, FLOOR }

    public enum Part implements StringRepresentable {
        LEFT_0("left_0", -1, 0, 0, Kind.FRAME),
        LEFT_1("left_1", -1, 1, 0, Kind.FRAME),
        LEFT_2("left_2", -1, 2, 0, Kind.FRAME),
        LEFT_3("left_3", -1, 3, 0, Kind.FRAME),
        RIGHT_0("right_0", 1, 0, 0, Kind.FRAME),
        RIGHT_1("right_1", 1, 1, 0, Kind.FRAME),
        RIGHT_2("right_2", 1, 2, 0, Kind.FRAME),
        RIGHT_3("right_3", 1, 3, 0, Kind.FRAME),
        // GeckoLib mirrors the model's local X relative to Minecraft block-space.
        // Reserve the cabinet on the same physical side as the rendered mesh.
        CABINET_2_0("cabinet_2_0", -2, 0, 0, Kind.FULL),
        CABINET_2_1("cabinet_2_1", -2, 1, 0, Kind.FULL),
        CABINET_2_2("cabinet_2_2", -2, 2, 0, Kind.FULL),
        CABINET_2_3("cabinet_2_3", -2, 3, 0, Kind.FULL),
        CABINET_3_0("cabinet_3_0", -3, 0, 0, Kind.FULL),
        CABINET_3_1("cabinet_3_1", -3, 1, 0, Kind.FULL),
        CABINET_3_2("cabinet_3_2", -3, 2, 0, Kind.FULL),
        CABINET_3_3("cabinet_3_3", -3, 3, 0, Kind.FULL),
        TOP_CENTER("top_center", 0, 3, 0, Kind.TOP),
        FLOOR_FRONT_LEFT("floor_front_left", -1, 0, -1, Kind.FLOOR),
        FLOOR_FRONT_CENTER("floor_front_center", 0, 0, -1, Kind.FLOOR),
        FLOOR_FRONT_RIGHT("floor_front_right", 1, 0, -1, Kind.FLOOR),
        FLOOR_BACK_LEFT("floor_back_left", -1, 0, 1, Kind.FLOOR),
        FLOOR_BACK_CENTER("floor_back_center", 0, 0, 1, Kind.FLOOR),
        FLOOR_BACK_RIGHT("floor_back_right", 1, 0, 1, Kind.FLOOR);

        private final String name;
        private final int side;
        private final int y;
        private final int forward;
        private final Kind kind;

        Part(String name, int side, int y, int forward, Kind kind) {
            this.name = name;
            this.side = side;
            this.y = y;
            this.forward = forward;
            this.kind = kind;
        }

        public int sideOffset() { return side; }
        public int yOffset() { return y; }
        public int forwardOffset() { return forward; }
        public Kind kind() { return kind; }
        @Override public String getSerializedName() { return name; }
    }
}
