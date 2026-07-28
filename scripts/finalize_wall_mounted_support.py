from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"{label}: expected source block not found in {path}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# Font providers resolve TTF locations relative to assets/<namespace>/font.
replace_once(
    "src/main/resources/assets/scp_additions/font/noto_sans_bold.json",
    '"file": "scp_additions:font/noto_sans_bold.ttf"',
    '"file": "scp_additions:noto_sans_bold.ttf"',
    "Noto Sans provider path",
)

# Editable SCP Sign: require all wall cells and open the editor after placement.
scp_sign = "src/main/java/net/mcreator/scpadditions/facility/ScpSignSupportBlock.java"
replace_once(
    scp_sign,
    "import net.minecraft.world.entity.player.Player;\n",
    "import net.minecraft.world.entity.LivingEntity;\n"
    "import net.minecraft.world.entity.player.Player;\n",
    "SCP Sign LivingEntity import",
)
replace_once(
    scp_sign,
    """        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(FACING, clickedFace)
                .setValue(WATERLOGGED, waterlogged);
    }
""",
    """        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        BlockState state = defaultBlockState().setValue(FACING, clickedFace)
                .setValue(WATERLOGGED, waterlogged);
        return state.canSurvive(context.getLevel(), context.getClickedPos())
                ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state,
            net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return WallMountedSupportEvents.hasLargePropWallSupport(level, pos,
                FacilityLargePropStructure.Kind.SIGN_SUPPORT,
                state.getValue(FACING));
    }
""",
    "SCP Sign placement support",
)
replace_once(
    scp_sign,
    """    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
""",
    """    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos)
                        instanceof ScpSignSupportBlockEntity sign) {
            ScpEntityNetwork.openScpSignScreen(serverPlayer, sign);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
""",
    "SCP Sign automatic editor",
)

# Static 914 notice uses the same support footprint.
notice = "src/main/java/net/mcreator/scpadditions/facility/Scp914UsageNoticeBlock.java"
replace_once(
    notice,
    """        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(FACING, clickedFace)
                .setValue(WATERLOGGED, waterlogged);
    }
""",
    """        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        BlockState state = defaultBlockState().setValue(FACING, clickedFace)
                .setValue(WATERLOGGED, waterlogged);
        return state.canSurvive(context.getLevel(), context.getClickedPos())
                ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state,
            net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return WallMountedSupportEvents.hasLargePropWallSupport(level, pos,
                FacilityLargePropStructure.Kind.SCP_914_NOTICE,
                state.getValue(FACING));
    }
""",
    "SCP-914 Notice placement support",
)

# Editable facility signs sit directly against their wall cell.
facility_sign = "src/main/java/net/mcreator/scpadditions/facility/FacilitySignBlock.java"
replace_once(
    facility_sign,
    """        boolean waterlogged = context.getLevel().getFluidState(
                context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(FACING, clickedFace)
                .setValue(WATERLOGGED, waterlogged);
    }
""",
    """        boolean waterlogged = context.getLevel().getFluidState(
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
""",
    "Facility Sign placement support",
)
replace_once(
    facility_sign,
    """        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
""",
    """        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (!state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
""",
    "Facility Sign support loss",
)

module = "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java"

# Every ordinary one-cell wall prop now shares the Water Faucet contract.
replace_once(
    module,
    """    private abstract static class WallMountedWaterloggedPropBlock
            extends HorizontalWaterloggedPropBlock {
        protected WallMountedWaterloggedPropBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            if (clickedFace.getAxis() == Direction.Axis.Y) return null;
            boolean waterlogged = context.getLevel().getFluidState(
                    context.getClickedPos()).getType() == Fluids.WATER;
            return defaultBlockState().setValue(FACING, clickedFace)
                    .setValue(WATERLOGGED, waterlogged);
        }
    }
""",
    """    private abstract static class WallMountedWaterloggedPropBlock
            extends HorizontalWaterloggedPropBlock {
        protected WallMountedWaterloggedPropBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            if (clickedFace.getAxis() == Direction.Axis.Y) return null;
            boolean waterlogged = context.getLevel().getFluidState(
                    context.getClickedPos()).getType() == Fluids.WATER;
            BlockState state = defaultBlockState().setValue(FACING, clickedFace)
                    .setValue(WATERLOGGED, waterlogged);
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            return WallMountedSupportEvents.hasWallSupport(
                    level, pos, state.getValue(FACING));
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighbor, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            BlockState updated = super.updateShape(state, direction, neighbor,
                    level, pos, neighborPos);
            return state.canSurvive(level, pos) ? updated
                    : Blocks.AIR.defaultBlockState();
        }
    }
""",
    "Shared wall prop support contract",
)

# Wall Light is a two-cell wall prop. Both visual cells require wall backing.
replace_once(
    module,
    """    private static final class WallLightBlock extends HorizontalWaterloggedPropBlock {
        private final boolean upper;

        private WallLightBlock(boolean upper) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).lightLevel(state -> 15));
            this.upper = upper;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
""",
    """    private static final class WallLightBlock extends WallMountedWaterloggedPropBlock {
        private final boolean upper;

        private WallLightBlock(boolean upper) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).lightLevel(state -> 15));
            this.upper = upper;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            if (state == null || upper) return state;
            BlockPos upperPos = context.getClickedPos().above();
            if (!context.getLevel().getBlockState(upperPos).canBeReplaced()) {
                return null;
            }
            Direction facing = state.getValue(FACING);
            return WallMountedSupportEvents.hasWallSupport(
                    context.getLevel(), upperPos, facing) ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            return upper ? level.getBlockState(pos.below()).is(WALLLIGHT.get())
                    : super.canSurvive(state, level, pos);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
""",
    "Wall Light support contract",
)
replace_once(
    module,
    """                level.setBlock(pos.above(), WALLLIGHT_2.get().defaultBlockState()
                        .setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
""",
    """                boolean upperWaterlogged = level.getFluidState(pos.above())
                        .getType() == Fluids.WATER;
                level.setBlock(pos.above(), WALLLIGHT_2.get().defaultBlockState()
                        .setValue(FACING, state.getValue(FACING))
                        .setValue(WATERLOGGED, upperWaterlogged), Block.UPDATE_ALL);
""",
    "Wall Light upper waterlogging",
)

# TV is wall-only; its authored structure spans five visible wall cells.
replace_once(
    module,
    "private static final class TvBlock extends DirectionalBlock {",
    "private static final class TvBlock extends HorizontalDirectionalBlock {",
    "TV horizontal block type",
)
replace_once(
    module,
    """        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            if (!FacilityLargePropStructure.canPlace(context.getLevel(),
                    context.getClickedPos(),
                    FacilityLargePropStructure.Kind.TV, clickedFace)) {
                return null;
            }
            return defaultBlockState().setValue(FACING, clickedFace);
        }
""",
    """        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            if (clickedFace.getAxis() == Direction.Axis.Y
                    || !FacilityLargePropStructure.canPlace(context.getLevel(),
                    context.getClickedPos(),
                    FacilityLargePropStructure.Kind.TV, clickedFace)) {
                return null;
            }
            BlockState state = defaultBlockState().setValue(FACING, clickedFace);
            return state.canSurvive(context.getLevel(), context.getClickedPos())
                    ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            return WallMountedSupportEvents.hasLargePropWallSupport(level, pos,
                    FacilityLargePropStructure.Kind.TV,
                    state.getValue(FACING));
        }
""",
    "TV wall placement",
)

# Public button fallback follows the clicked wall rather than player yaw.
replace_once(
    module,
    """        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getHorizontalDirection().getOpposite());
        }
""",
    """        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            return clickedFace.getAxis() == Direction.Axis.Y ? null
                    : defaultBlockState().setValue(FACING, clickedFace);
        }
""",
    "Door Button clicked-face fallback",
)

# TV no longer uses the six-way property when resolving multiblock controllers.
structure = "src/main/java/net/mcreator/scpadditions/facility/FacilityLargePropStructure.java"
replace_once(
    structure,
    """    private static Direction controllerFacing(BlockState state, Kind kind) {
        return kind == Kind.TV
                ? state.getValue(FacilityPropPartBlock.FACING)
                : state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
""",
    """    private static Direction controllerFacing(BlockState state, Kind kind) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
""",
    "TV multiblock facing property",
)

# Door buttons and readers reject non-sturdy surfaces at placement time.
button_events = "src/main/java/net/mcreator/scpadditions/facility/DoorButtonPlacementEvents.java"
replace_once(
    button_events,
    """        if (clickedFace.getAxis() == Direction.Axis.Y) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
""",
    """        if (clickedFace.getAxis() == Direction.Axis.Y
                || !player.level().getBlockState(hit.getBlockPos()).isFaceSturdy(
                        player.level(), hit.getBlockPos(), clickedFace)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
""",
    "Door Button sturdy wall placement",
)

reader_item = "src/main/java/net/mcreator/scpadditions/item/OffsetKeycardReaderItem.java"
replace_once(
    reader_item,
    """        Direction readerFacing = context.getClickedFace();
        Direction screenLeft = readerFacing.getClockWise();
""",
    """        Direction readerFacing = context.getClickedFace();
        BlockPos supportPos = context.getClickedPos();
        if (!context.getLevel().getBlockState(supportPos).isFaceSturdy(
                context.getLevel(), supportPos, readerFacing)) {
            return InteractionResult.FAIL;
        }
        Direction screenLeft = readerFacing.getClockWise();
""",
    "Keycard Reader sturdy wall placement",
)
