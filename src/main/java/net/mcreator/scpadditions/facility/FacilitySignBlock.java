package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Wall-mounted editable sign using collision derived from the supplied
 * Blockbench elements. Text-only zero-thickness elements are intentionally not
 * part of the physical shape.
 */
public final class FacilitySignBlock extends BaseEntityBlock
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final Map<SignType, Map<Direction, VoxelShape>> SHAPES =
            buildShapes();

    private final SignType type;

    public FacilitySignBlock(SignType type) {
        super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                .strength(1.0F, 10.0F).noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false));
        this.type = type;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    public SignType type() {
        return type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis() == Direction.Axis.Y) return null;
        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        BlockState state = defaultBlockState()
                .setValue(FACING, clickedFace)
                .setValue(WATERLOGGED, waterlogged);
        return state.canSurvive(context.getLevel(), context.getClickedPos())
                ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state,
            net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return WallMountedSupportEvents.hasWallSupport(
                level, pos, state.getValue(FACING));
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
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return SHAPES.get(type).get(state.getValue(FACING));
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
        return new FacilitySignBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack screwdriver = KeycardReaderInteractionEvents.screwdriver(player);
        if (screwdriver.isEmpty()) return InteractionResult.PASS;

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FacilitySignBlockEntity sign) {
            ScpEntityNetwork.openFacilitySignScreen(serverPlayer, sign);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(this);
    }

    private static Map<SignType, Map<Direction, VoxelShape>> buildShapes() {
        EnumMap<SignType, Map<Direction, VoxelShape>> result =
                new EnumMap<>(SignType.class);
        result.put(SignType.CORE_ROOM, rotations(coreRoomNorthShape()));
        result.put(SignType.DOOR, rotations(doorNorthShape()));
        return Collections.unmodifiableMap(result);
    }

    private static VoxelShape coreRoomNorthShape() {
        return Shapes.or(
                box(-11.0D, 0.75D, 15.5D, 11.0D, 6.25D, 15.75D),
                box(-11.0D, 6.75D, 15.5D, 11.0D, 12.25D, 15.75D),
                box(-11.0D, 12.75D, 15.5D, 11.0D, 18.25D, 15.75D),
                box(-10.0D, 0.25D, 15.75D, -9.0D, 18.75D, 16.0D),
                box(9.0D, 0.25D, 15.75D, 10.0D, 18.75D, 16.0D)).optimize();
    }

    private static VoxelShape doorNorthShape() {
        VoxelShape shape = Shapes.empty();
        // The main housing is a 20×9×2 px cuboid rotated -22.5 degrees around
        // [8, 9.5, 13]. One-pixel slices retain the slope far more accurately
        // than a single oversized bounding box.
        for (int y = 2; y < 11; y++) {
            shape = Shapes.or(shape, rotatedXBox(
                    -2.0D, y, 12.0D, 18.0D, y + 1.0D, 14.0D,
                    -22.5D, 9.5D, 13.0D));
        }
        shape = Shapes.or(shape,
                box(2.0D, 6.5D, 13.25D, 14.0D, 10.5D, 16.0D));
        return shape.optimize();
    }

    private static VoxelShape rotatedXBox(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ, double angleDegrees,
            double originY, double originZ) {
        double radians = Math.toRadians(angleDegrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        double rotatedMinY = Double.POSITIVE_INFINITY;
        double rotatedMinZ = Double.POSITIVE_INFINITY;
        double rotatedMaxY = Double.NEGATIVE_INFINITY;
        double rotatedMaxZ = Double.NEGATIVE_INFINITY;

        for (double y : new double[]{minY, maxY}) {
            for (double z : new double[]{minZ, maxZ}) {
                double offsetY = y - originY;
                double offsetZ = z - originZ;
                double rotatedY = originY + offsetY * cosine - offsetZ * sine;
                double rotatedZ = originZ + offsetY * sine + offsetZ * cosine;
                rotatedMinY = Math.min(rotatedMinY, rotatedY);
                rotatedMinZ = Math.min(rotatedMinZ, rotatedZ);
                rotatedMaxY = Math.max(rotatedMaxY, rotatedY);
                rotatedMaxZ = Math.max(rotatedMaxZ, rotatedZ);
            }
        }
        return box(minX, rotatedMinY, rotatedMinZ,
                maxX, rotatedMaxY, rotatedMaxZ);
    }

    private static Map<Direction, VoxelShape> rotations(VoxelShape north) {
        EnumMap<Direction, VoxelShape> result = new EnumMap<>(Direction.class);
        result.put(Direction.NORTH, north);
        result.put(Direction.EAST, rotateY(north, 1));
        result.put(Direction.SOUTH, rotateY(north, 2));
        result.put(Direction.WEST, rotateY(north, 3));
        return Collections.unmodifiableMap(result);
    }

    private static VoxelShape rotateY(VoxelShape source, int quarterTurns) {
        VoxelShape current = source;
        for (int turn = 0; turn < quarterTurns; turn++) {
            VoxelShape[] rotated = {Shapes.empty()};
            current.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(
                            1.0D - maxZ, minY, minX,
                            1.0D - minZ, maxY, maxX)));
            current = rotated[0].optimize();
        }
        return current;
    }

    public enum SignType {
        CORE_ROOM("core_room", 30, false, true),
        DOOR("door", 40, true, false);

        private final String serializedName;
        private final int maxTextLength;
        private final boolean hasNumbers;
        private final boolean forceUppercase;

        SignType(String serializedName, int maxTextLength,
                boolean hasNumbers, boolean forceUppercase) {
            this.serializedName = serializedName;
            this.maxTextLength = maxTextLength;
            this.hasNumbers = hasNumbers;
            this.forceUppercase = forceUppercase;
        }

        public String serializedName() {
            return serializedName;
        }

        public int maxTextLength() {
            return maxTextLength;
        }

        public boolean hasNumbers() {
            return hasNumbers;
        }

        public boolean forceUppercase() {
            return forceUppercase;
        }

        public static SignType byName(String name) {
            if (name != null) {
                String normalized = name.toLowerCase(Locale.ROOT);
                for (SignType type : values()) {
                    if (type.serializedName.equals(normalized)) return type;
                }
            }
            return CORE_ROOM;
        }
    }
}
