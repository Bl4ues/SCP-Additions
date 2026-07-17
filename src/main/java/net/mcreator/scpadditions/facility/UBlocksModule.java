package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registry bridge for the older SCP UBlocks resource pack.
 *
 * Registry IDs are owned by scp_additions. The scp_ublocks namespace remains
 * only as a model, texture, sound and translation library.
 */
public final class UBlocksModule {
    public static final String MODID = ScpAdditionsMod.MODID;
    public static final String LEGACY_MODID = "scp_ublocks";
    private static final String LEGACY_WALL_DETAIL_MID = "sl_1_wall_detail_1_mid";
    private static final String LEGACY_WALL_DETAIL_TOP = "sl_1_wall_detail_1_top";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    private static final List<RegistryObject<Item>> CREATIVE_ITEMS = new ArrayList<>();
    private static final List<RegistryObject<Block>> CUTOUT_BLOCKS = new ArrayList<>();

    // Sector 1 structural set. Floor 1 is the static gray surface; Floor 2 owns
    // the visual transition states rendered around neighboring gray tiles.
    public static final RegistryObject<Block> SL_1_FLOOR_1 = registerBlock(
            "sl_1_floor_1", GrayConnectedFloorBlock::new, false);
    public static final RegistryObject<Block> SL_1_FLOOR_2 = registerBlock(
            "sl_1_floor_2", BlueConnectedFloorBlock::new, false);
    public static final RegistryObject<Block> SL1_WALL_BOT = structure("sl1_wall_bot");
    public static final RegistryObject<Block> SL1_WALL_MID = structure("sl1_wall_mid");
    public static final RegistryObject<Block> SL_1_WALL_TOP = structure("sl_1_wall_top");

    // Sector 1 directional decoration.
    public static final RegistryObject<Block> SL_1_FLOOR_DETAIL_SMALL = directional(
            "sl_1_floor_detail_small", DirectionalShape.FLOOR_DECAL, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_FLOOR_DETAIL_BIG = directional(
            "sl_1_floor_detail_big", DirectionalShape.FLOOR_DECAL, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_WALL_DETAIL_1_BOT = registerBlock(
            "sl_1_wall_detail_1_bot", AdaptiveWallDetailBlock::new, true);
    // Kept registered for old worlds and inventories. New construction uses the
    // adaptive bottom ID, which selects all three visual sections by blockstate.
    public static final RegistryObject<Block> SL_1_WALL_DETAIL_1_MID = directional(
            LEGACY_WALL_DETAIL_MID, DirectionalShape.WALL_DECOR, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_WALL_DETAIL_1_TOP = directional(
            LEGACY_WALL_DETAIL_TOP, DirectionalShape.WALL_DECOR, SoundType.STONE);
    public static final RegistryObject<Block> SL_1_WALL_DETAIL_2 = directional(
            "sl_1_wall_detail_2", DirectionalShape.WALL_DECOR, SoundType.STONE);

    // Sector 2 structural set.
    public static final RegistryObject<Block> SL_2_FLOOR = structure("sl_2_floor");
    public static final RegistryObject<Block> SL_2_WALL_BOT = structure("sl_2_wall_bot");
    public static final RegistryObject<Block> SL_2_WALL_MID = structure("sl_2_wall_mid");
    public static final RegistryObject<Block> SL_2_WALL_TOP = structure("sl_2_wall_top");

    // Props.
    public static final RegistryObject<Block> VENT_OPEN = directional(
            "vent_open", DirectionalShape.VENT, SoundType.METAL);

    private UBlocksModule() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    /** Items are deliberately returned in registration order for tab ordering. */
    public static List<RegistryObject<Item>> creativeItems() {
        return CREATIVE_ITEMS.stream()
                .filter(item -> !isLegacyWallDetailItem(item))
                .toList();
    }

    public static List<RegistryObject<Block>> cutoutBlocks() {
        return Collections.unmodifiableList(CUTOUT_BLOCKS);
    }

    public static RegistryObject<Block> blockByPath(String path) {
        return BLOCKS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    public static RegistryObject<Item> itemByPath(String path) {
        return isLegacyWallDetailPath(path) ? null : registeredItemByPath(path);
    }

    public static RegistryObject<Item> registeredItemByPath(String path) {
        return ITEMS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(path))
                .findFirst()
                .orElse(null);
    }

    private static boolean isLegacyWallDetailItem(RegistryObject<Item> item) {
        return item != null && isLegacyWallDetailPath(item.getId().getPath());
    }

    private static boolean isLegacyWallDetailPath(String path) {
        return LEGACY_WALL_DETAIL_MID.equals(path) || LEGACY_WALL_DETAIL_TOP.equals(path);
    }

    private static RegistryObject<Block> structure(String path) {
        return registerBlock(path, UBlockStructureBlock::new, false);
    }

    private static RegistryObject<Block> directional(String path, DirectionalShape shape, SoundType sound) {
        return registerBlock(path, () -> new UBlockDirectionalBlock(shape, sound), true);
    }

    private static RegistryObject<Block> registerBlock(String path,
            Supplier<? extends Block> factory, boolean cutout) {
        RegistryObject<Block> block = BLOCKS.register(path, factory);
        RegistryObject<Item> item = ITEMS.register(path, () -> isConnectedFloorPath(path)
                ? new ConnectedFloorBlockItem(block.get(), new Item.Properties())
                : new BlockItem(block.get(), new Item.Properties()));
        CREATIVE_ITEMS.add(item);
        if (cutout) {
            CUTOUT_BLOCKS.add(block);
        }
        return block;
    }

    private static boolean isConnectedFloorPath(String path) {
        return "sl_1_floor_1".equals(path) || "sl_1_floor_2".equals(path);
    }

    private static final class ConnectedFloorBlockItem extends BlockItem {
        private ConnectedFloorBlockItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.scp_additions.sl1_connected_floors")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }

    private static class UBlockStructureBlock extends Block {
        private UBlockStructureBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.5F, 10.0F));
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            List<ItemStack> original = super.getDrops(state, builder);
            return original.isEmpty() ? Collections.singletonList(new ItemStack(this)) : original;
        }
    }

    private abstract static class ConnectedFloorBlock extends UBlockStructureBlock {
        private ConnectedFloorBlock() {
            super();
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            if (oldState.getBlock() != state.getBlock()) {
                refreshBlueFloors(level, pos, state);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean movedByPiston) {
            if (state.getBlock() != newState.getBlock()) {
                // onRemove runs before the replacement is fully visible through
                // the level, so pass it explicitly while recalculating neighbors.
                refreshBlueFloors(level, pos, newState);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    private static final class GrayConnectedFloorBlock extends ConnectedFloorBlock {
        private GrayConnectedFloorBlock() {
            super();
        }
    }

    private static final class BlueConnectedFloorBlock extends ConnectedFloorBlock {
        private static final EnumProperty<FloorTransition> TRANSITION =
                EnumProperty.create("transition", FloorTransition.class);

        private BlueConnectedFloorBlock() {
            super();
            registerDefaultState(stateDefinition.any().setValue(TRANSITION, FloorTransition.NONE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TRANSITION);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(TRANSITION,
                    resolveTransition(context.getLevel(), context.getClickedPos(), null, null));
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos currentPos,
                BlockPos neighborPos) {
            FloorTransition transition = resolveTransition(level, currentPos, neighborPos, neighborState);
            return state.getValue(TRANSITION) == transition
                    ? state
                    : state.setValue(TRANSITION, transition);
        }
    }

    private static void refreshBlueFloors(Level level, BlockPos changedPos,
            BlockState replacementState) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos target = changedPos.offset(x, 0, z);
                BlockState targetState = target.equals(changedPos)
                        ? replacementState
                        : level.getBlockState(target);
                if (!(targetState.getBlock() instanceof BlueConnectedFloorBlock)) {
                    continue;
                }

                FloorTransition transition = resolveTransition(
                        level, target, changedPos, replacementState);
                if (targetState.getValue(BlueConnectedFloorBlock.TRANSITION) != transition) {
                    level.setBlock(target,
                            targetState.setValue(BlueConnectedFloorBlock.TRANSITION, transition),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static FloorTransition resolveTransition(BlockGetter level, BlockPos pos,
            BlockPos overriddenPos, BlockState overriddenState) {
        boolean north = isGray(level, pos.north(), overriddenPos, overriddenState);
        boolean east = isGray(level, pos.east(), overriddenPos, overriddenState);
        boolean south = isGray(level, pos.south(), overriddenPos, overriddenState);
        boolean west = isGray(level, pos.west(), overriddenPos, overriddenState);

        int cardinalCount = count(north, east, south, west);
        if (cardinalCount >= 3 || (north && south) || (east && west)) {
            return FloorTransition.FULL;
        }
        if (north && west) return FloorTransition.INNER_NW;
        if (north && east) return FloorTransition.INNER_NE;
        if (south && east) return FloorTransition.INNER_SE;
        if (south && west) return FloorTransition.INNER_SW;
        if (north) return FloorTransition.EDGE_N;
        if (east) return FloorTransition.EDGE_E;
        if (south) return FloorTransition.EDGE_S;
        if (west) return FloorTransition.EDGE_W;

        boolean northWest = isGray(level, pos.north().west(), overriddenPos, overriddenState);
        boolean northEast = isGray(level, pos.north().east(), overriddenPos, overriddenState);
        boolean southEast = isGray(level, pos.south().east(), overriddenPos, overriddenState);
        boolean southWest = isGray(level, pos.south().west(), overriddenPos, overriddenState);

        int diagonalCount = count(northWest, northEast, southEast, southWest);
        if (diagonalCount == 4) return FloorTransition.FULL;
        if (diagonalCount == 3) {
            if (!southEast) return FloorTransition.INNER_NW;
            if (!southWest) return FloorTransition.INNER_NE;
            if (!northWest) return FloorTransition.INNER_SE;
            return FloorTransition.INNER_SW;
        }
        if (diagonalCount == 2) {
            if (northWest && northEast) return FloorTransition.EDGE_N;
            if (northEast && southEast) return FloorTransition.EDGE_E;
            if (southEast && southWest) return FloorTransition.EDGE_S;
            if (southWest && northWest) return FloorTransition.EDGE_W;
            // Dedicated orientation textures still cannot express the two
            // opposite-corner checkerboard cases without a separate pattern.
            return FloorTransition.NONE;
        }
        if (northWest) return FloorTransition.CORNER_NW;
        if (northEast) return FloorTransition.CORNER_NE;
        if (southEast) return FloorTransition.CORNER_SE;
        if (southWest) return FloorTransition.CORNER_SW;
        return FloorTransition.NONE;
    }

    private static boolean isGray(BlockGetter level, BlockPos pos,
            BlockPos overriddenPos, BlockState overriddenState) {
        BlockState state = overriddenPos != null && overriddenPos.equals(pos)
                ? overriddenState
                : level.getBlockState(pos);
        return state != null && state.getBlock() instanceof GrayConnectedFloorBlock;
    }

    private static int count(boolean... values) {
        int result = 0;
        for (boolean value : values) {
            if (value) result++;
        }
        return result;
    }

    private enum FloorTransition implements StringRepresentable {
        NONE("none"),
        CORNER_SW("corner_sw"),
        CORNER_NW("corner_nw"),
        CORNER_NE("corner_ne"),
        CORNER_SE("corner_se"),
        EDGE_W("edge_w"),
        EDGE_N("edge_n"),
        EDGE_E("edge_e"),
        EDGE_S("edge_s"),
        INNER_NW("inner_nw"),
        INNER_NE("inner_ne"),
        INNER_SE("inner_se"),
        INNER_SW("inner_sw"),
        FULL("full");

        private final String serializedName;

        FloorTransition(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    private static final class AdaptiveWallDetailBlock extends HorizontalDirectionalBlock {
        private static final EnumProperty<WallDetailSegment> SEGMENT =
                EnumProperty.create("segment", WallDetailSegment.class);

        private AdaptiveWallDetailBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.5F, 10.0F)
                    .noOcclusion().isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(SEGMENT, WallDetailSegment.BOTTOM));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, SEGMENT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockGetter level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            Direction facing = context.getHorizontalDirection().getOpposite();

            BlockState below = level.getBlockState(pos.below());
            BlockState above = level.getBlockState(pos.above());
            if (below.getBlock() instanceof AdaptiveWallDetailBlock) {
                facing = below.getValue(FACING);
            } else if (above.getBlock() instanceof AdaptiveWallDetailBlock) {
                facing = above.getValue(FACING);
            }

            return defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(SEGMENT, resolveWallDetailSegment(level, pos, facing));
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos currentPos,
                BlockPos neighborPos) {
            if (direction.getAxis() != Direction.Axis.Y) return state;
            WallDetailSegment segment = resolveWallDetailSegment(
                    level, currentPos, state.getValue(FACING));
            return state.getValue(SEGMENT) == segment
                    ? state
                    : state.setValue(SEGMENT, segment);
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
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return DirectionalShape.WALL_DECOR.outline(state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }
    }

    private static WallDetailSegment resolveWallDetailSegment(BlockGetter level,
            BlockPos pos, Direction facing) {
        boolean below = isMatchingWallDetail(level.getBlockState(pos.below()), facing);
        boolean above = isMatchingWallDetail(level.getBlockState(pos.above()), facing);
        if (below && above) return WallDetailSegment.MIDDLE;
        if (below) return WallDetailSegment.TOP;
        return WallDetailSegment.BOTTOM;
    }

    private static boolean isMatchingWallDetail(BlockState state, Direction facing) {
        return state.getBlock() instanceof AdaptiveWallDetailBlock
                && state.getValue(HorizontalDirectionalBlock.FACING) == facing;
    }

    private enum WallDetailSegment implements StringRepresentable {
        BOTTOM("bottom"),
        MIDDLE("middle"),
        TOP("top");

        private final String serializedName;

        WallDetailSegment(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    private static final class UBlockDirectionalBlock extends HorizontalDirectionalBlock {
        private final DirectionalShape shape;

        private UBlockDirectionalBlock(DirectionalShape shape, SoundType sound) {
            super(BlockBehaviour.Properties.of().sound(sound).strength(1.5F, 10.0F)
                    .noOcclusion().isRedstoneConductor((state, level, pos) -> false));
            this.shape = shape;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return shape.outline(state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }
    }

    private enum DirectionalShape {
        FLOOR_DECAL,
        WALL_DECOR,
        VENT;

        private VoxelShape outline(Direction facing) {
            return switch (this) {
                case FLOOR_DECAL -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 0.5D, 16.0D);
                case WALL_DECOR, VENT -> switch (facing) {
                    case NORTH -> Block.box(0.0D, 0.0D, 14.5D, 16.0D, 16.0D, 16.0D);
                    case EAST -> Block.box(0.0D, 0.0D, 0.0D, 1.5D, 16.0D, 16.0D);
                    case SOUTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.5D);
                    case WEST -> Block.box(14.5D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
                    default -> Shapes.block();
                };
            };
        }
    }
}
