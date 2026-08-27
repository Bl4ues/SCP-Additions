package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Invisible floor/wall/ceiling cells belonging to one Decontamination controller. */
public final class DecontaminationCollisionBlock extends Block
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty SIDE = IntegerProperty.create("side", 0, 2);
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 0, 4);
    public static final IntegerProperty FORWARD = IntegerProperty.create("forward", 1, 5);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // Exact floor-grille bounds from the authored vent 2/3 cubes.
    private static final double VENT_MIN_X = -12.0D;
    private static final double VENT_MAX_X = 12.0D;
    private static final double VENT_2_MIN_Z = 14.1D;
    private static final double VENT_2_MAX_Z = 38.1D;
    private static final double VENT_3_MIN_Z = 57.95D;
    private static final double VENT_3_MAX_Z = 81.95D;

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
                .setValue(SIDE, encodeSide(0))
                .setValue(HEIGHT, encodeHeight(-1))
                .setValue(FORWARD, 1)
                .setValue(WATERLOGGED, false));
    }

    public static int encodeSide(int side) {
        if (side < -1 || side > 1) {
            throw new IllegalArgumentException("side=" + side);
        }
        return side + 1;
    }

    public static int decodeSide(int stored) {
        return stored - 1;
    }

    public static int encodeHeight(int height) {
        if (height < -1 || height > 3) {
            throw new IllegalArgumentException("height=" + height);
        }
        return height + 1;
    }

    public static int decodeHeight(int stored) {
        return stored - 1;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, HEIGHT, FORWARD, WATERLOGGED);
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
        if (!DecontaminationStructure.isValidCollisionPart(level, pos, state)) {
            return Shapes.empty();
        }
        return DecontaminationShapeHelper.localShape(
                state.getValue(FACING),
                decodeSide(state.getValue(SIDE)),
                decodeHeight(state.getValue(HEIGHT)),
                state.getValue(FORWARD));
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level,
            BlockPos pos, @Nullable Entity entity) {
        // Keep the existing structural floor sound everywhere else. The two
        // authored 24x24 floor grilles get a sharper metallic copper step so
        // sound follows the visible vent geometry rather than whole block rows.
        if (entity != null && decodeHeight(state.getValue(HEIGHT)) == -1
                && isStandingOnFloorVent(state, pos, entity)) {
            return SoundType.COPPER;
        }
        return super.getSoundType(state, level, pos, entity);
    }

    private static boolean isStandingOnFloorVent(BlockState state, BlockPos pos,
            Entity entity) {
        BlockPos controller = DecontaminationStructure.controllerPosition(pos, state);
        BlockPos origin = DecontaminationStructure.structureOrigin(controller);
        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();

        double dx = entity.getX() - (origin.getX() + 0.5D);
        double dz = entity.getZ() - (origin.getZ() + 0.5D);
        double modelX = (dx * right.getStepX() + dz * right.getStepZ()) * 16.0D;
        double modelZ = (dx * forward.getStepX() + dz * forward.getStepZ()) * 16.0D;

        if (modelX < VENT_MIN_X || modelX > VENT_MAX_X) return false;
        return (modelZ >= VENT_2_MIN_Z && modelZ <= VENT_2_MAX_Z)
                || (modelZ >= VENT_3_MIN_Z && modelZ <= VENT_3_MAX_Z);
    }

    /**
     * The center floor helper immediately inside each endpoint doubles as the
     * checkpoint's invisible redstone relay. No extra side placeholder is
     * required, so map builders retain the wall cells beside both doors.
     */
    @Override
    public boolean isSignalSource(BlockState state) {
        return DecontaminationStructure.isDoorPowerPart(state);
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos,
            Direction direction) {
        return DecontaminationStructure.ownedDoorPowerSignal(level, pos, state);
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
        if (!level.isClientSide) level.scheduleTick(pos, this, 40);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos,
            RandomSource random) {
        if (!DecontaminationStructure.isValidCollisionPart(level, pos, state)) {
            DecontaminationStructure.clearBlock(level, pos, state);
            return;
        }
        BlockPos controller = DecontaminationStructure.controllerPosition(pos, state);
        BlockState controllerState = level.getBlockState(controller);
        DecontaminationStructure.ensureStructure(level, controller,
                controllerState.getValue(HorizontalDirectionalBlock.FACING));
        level.scheduleTick(pos, this, 40);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(ScpClassifiedDirectiveModBlocks.DECON_OPEN.get());
    }
}
