package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Invisible, locally bounded selection/collision cell belonging to a large
 * facility prop. It has no item and never drops independently.
 */
public final class FacilityPropPartBlock extends Block
        implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING =
            BlockStateProperties.FACING;
    public static final EnumProperty<Part> PART =
            EnumProperty.create("part", Part.class);
    public static final BooleanProperty WATERLOGGED =
            BlockStateProperties.WATERLOGGED;

    public FacilityPropPartBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(1.0F, 10.0F)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.SIGN_NEAR)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, CollisionContext context) {
        return FacilityLargePropStructure.partShape(
                state.getValue(PART), state.getValue(FACING));
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
        if (!FacilityLargePropStructure.isValidPart(level, pos, state)) {
            FacilityLargePropStructure.clearBlock(level, pos, state);
            return;
        }

        Part part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        BlockPos controller = FacilityLargePropStructure.controllerPosition(
                pos, state);
        BlockState controllerState = level.getBlockState(controller);
        FacilityLargePropStructure.ensureParts(level, controller,
                part.kind(), facing,
                FacilityLargePropStructure.position(controllerState,
                        part.kind()));
        level.scheduleTick(pos, this, 40);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        Part part = state.getValue(PART);
        if (part.kind() != FacilityLargePropStructure.Kind.SIGN_SUPPORT
                || KeycardReaderInteractionEvents.screwdriver(player).isEmpty()) {
            return InteractionResult.PASS;
        }
        BlockPos controller = FacilityLargePropStructure.controllerPosition(
                pos, state);
        if (!FacilityLargePropStructure.isValidPart(level, pos, state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(controller)
                        instanceof ScpSignSupportBlockEntity sign) {
            ScpEntityNetwork.openScpSignScreen(serverPlayer, sign);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state,
            LootParams.Builder builder) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target,
            BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(FacilityLargePropStructure.controllerBlock(
                state.getValue(PART).kind()));
    }

    public enum Part implements StringRepresentable {
        // Legacy cells remain decodable so existing worlds load cleanly. They
        // are no longer generated and self-remove during their validation tick.
        SIGN_NEAR("sign_near",
                FacilityLargePropStructure.Kind.SIGN_SUPPORT, 0, -1,
                null, true),
        SIGN_FAR("sign_far",
                FacilityLargePropStructure.Kind.SIGN_SUPPORT, 1, -1,
                null, true),
        NOTICE_NEAR("notice_near",
                FacilityLargePropStructure.Kind.SCP_914_NOTICE, 0, -1,
                null, true),
        NOTICE_FAR("notice_far",
                FacilityLargePropStructure.Kind.SCP_914_NOTICE, 1, -1,
                null, true),

        SIGN_LEFT("sign_left",
                FacilityLargePropStructure.Kind.SIGN_SUPPORT, 1, 0,
                FramedSignPosition.LEFT, false),
        SIGN_RIGHT("sign_right",
                FacilityLargePropStructure.Kind.SIGN_SUPPORT, -1, 0,
                FramedSignPosition.RIGHT, false),
        NOTICE_LEFT("notice_left",
                FacilityLargePropStructure.Kind.SCP_914_NOTICE, 1, 0,
                FramedSignPosition.LEFT, false),
        NOTICE_RIGHT("notice_right",
                FacilityLargePropStructure.Kind.SCP_914_NOTICE, -1, 0,
                FramedSignPosition.RIGHT, false),
        CONSTRUCTION_LEFT("construction_left",
                FacilityLargePropStructure.Kind.UNDER_CONSTRUCTION_NOTICE,
                1, 0, FramedSignPosition.LEFT, false),
        CONSTRUCTION_RIGHT("construction_right",
                FacilityLargePropStructure.Kind.UNDER_CONSTRUCTION_NOTICE,
                -1, 0, FramedSignPosition.RIGHT, false),

        TV_LEFT_LOWER("tv_left_lower",
                FacilityLargePropStructure.Kind.TV, -1, -1,
                null, false),
        TV_CENTER_LOWER("tv_center_lower",
                FacilityLargePropStructure.Kind.TV, 0, -1,
                null, false),
        TV_RIGHT_LOWER("tv_right_lower",
                FacilityLargePropStructure.Kind.TV, 1, -1,
                null, false),
        TV_LEFT_CURRENT("tv_left_current",
                FacilityLargePropStructure.Kind.TV, -1, 0,
                null, false),
        TV_RIGHT_CURRENT("tv_right_current",
                FacilityLargePropStructure.Kind.TV, 1, 0,
                null, false);

        private final String serializedName;
        private final FacilityLargePropStructure.Kind kind;
        private final int sideOffset;
        private final int rowOffset;
        private final FramedSignPosition position;
        private final boolean legacy;

        Part(String serializedName, FacilityLargePropStructure.Kind kind,
                int sideOffset, int rowOffset,
                FramedSignPosition position, boolean legacy) {
            this.serializedName = serializedName;
            this.kind = kind;
            this.sideOffset = sideOffset;
            this.rowOffset = rowOffset;
            this.position = position;
            this.legacy = legacy;
        }

        public FacilityLargePropStructure.Kind kind() {
            return kind;
        }

        public int sideOffset() {
            return sideOffset;
        }

        public int rowOffset() {
            return rowOffset;
        }

        public FramedSignPosition position() {
            return position;
        }

        public boolean legacy() {
            return legacy;
        }

        public static List<Part> forKind(
                FacilityLargePropStructure.Kind kind) {
            return Arrays.stream(values())
                    .filter(part -> part.kind == kind && !part.legacy)
                    .toList();
        }

        public static List<Part> forKind(
                FacilityLargePropStructure.Kind kind,
                FramedSignPosition position) {
            return Arrays.stream(values())
                    .filter(part -> part.kind == kind && !part.legacy)
                    .filter(part -> kind == FacilityLargePropStructure.Kind.TV
                            || part.position == position)
                    .toList();
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
