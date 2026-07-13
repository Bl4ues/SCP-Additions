package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Complete SCP Unity block integration.
 *
 * Public registry IDs live under {@code scp_additions}. The original
 * {@code scp_unity_extra_blocks} namespace remains as a resource library, and
 * {@link FacilityLegacyMappings} remaps old block/item IDs when a world loads.
 * Animation frames stay registered for compatibility but only stable endpoints
 * are exposed in the creative tab.
 */
public final class FacilityModule {
    public static final String MODID = ScpAdditionsMod.MODID;
    public static final String LEGACY_MODID = "scp_unity_extra_blocks";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final Map<String, RegistryObject<Block>> BLOCKS_BY_PATH = new LinkedHashMap<>();
    private static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();
    private static final List<RegistryObject<Item>> CREATIVE_ITEMS = new ArrayList<>();
    private static final Map<String, DoorFamily> DOOR_FAMILIES = new LinkedHashMap<>();

    private static final RegistryObject<SoundEvent> UNITY_DOOR_OPENING = sound("unity_door_opening");
    private static final RegistryObject<SoundEvent> UNITY_DOOR_CLOSING = sound("unity_door_closing");
    private static final RegistryObject<SoundEvent> UNITY_DOOR_OPEN = sound("unity_door_open");
    private static final RegistryObject<SoundEvent> UNITY_DOOR_CLOSE = sound("unity_door_close");
    private static final RegistryObject<SoundEvent> UNITY_BATH_OPEN = sound("unity_bath_open");
    private static final RegistryObject<SoundEvent> UNITY_BATH_CLOSE = sound("unity_bath_close");
    private static final RegistryObject<SoundEvent> UNITY_OFFICE_OPEN = sound("unity_office_open");
    private static final RegistryObject<SoundEvent> UNITY_OFFICE_CLOSE = sound("unity_office_close");

    // Architectural pieces.
    public static final RegistryObject<Block> TESLA_BOTTOM = structure("tesla_bottom");
    public static final RegistryObject<Block> TESLA_MID_1 = structure("tesla_mid_1");
    public static final RegistryObject<Block> TESLA_MID_2 = structure("tesla_mid_2");
    public static final RegistryObject<Block> TESLA_BOTTOM_ALT = structure("tesla_bottom_alt");
    public static final RegistryObject<Block> TESLA_TOP_ALT = structure("tesla_top_alt");
    public static final RegistryObject<Block> ARCHIVAL_BOTTOM = structure("archival_bottom");
    public static final RegistryObject<Block> ARCHIVAL_MID = structure("archival_mid");
    public static final RegistryObject<Block> ARCHIVAL_TOP = structure("archival_top");
    public static final RegistryObject<Block> ARCHIVAL_BOT_1 = structure("archival_bot_1");
    public static final RegistryObject<Block> ARCHIVAL_MID_2 = structure("archival_mid_2");
    public static final RegistryObject<Block> OFFICE_BOTTOM = structure("office_bottom");
    public static final RegistryObject<Block> OFFICE_MID = structure("office_mid");
    public static final RegistryObject<Block> OFFICE_TOP = structure("office_top");
    public static final RegistryObject<Block> SKYROOM_BOT_1 = structure("skyroom_bot_1");
    public static final RegistryObject<Block> SKYROOM_BOT_2 = structure("skyroom_bot_2");
    public static final RegistryObject<Block> SKYROOM_MID = structure("skyroom_mid");
    public static final RegistryObject<Block> SKYROOM_TOP_ALT = structure("skyroom_top_alt");
    public static final RegistryObject<Block> SKYROOM_BLOCK = structure("skyroom_block");
    public static final RegistryObject<Block> SECURITY_BOT = structure("security_bot");
    public static final RegistryObject<Block> SECURITY_MID = structure("security_mid");
    public static final RegistryObject<Block> SECURITY_TOP = structure("security_top");

    // Props and lights.
    public static final RegistryObject<Block> ALARM_LAMP = registerBlock("alarm_lamp",
            () -> new AlarmLampBlock(false), true);
    public static final RegistryObject<Block> ALARM_LAMP_ON = registerBlock("alarm_lamp_on",
            () -> new AlarmLampBlock(true), false);
    public static final RegistryObject<Block> WALLLIGHT = registerBlock("walllight",
            () -> new WallLightBlock(false), true);
    public static final RegistryObject<Block> WALLLIGHT_2 = registerBlock("walllight_2",
            () -> new WallLightBlock(true), false);
    public static final RegistryObject<Block> HEATER = registerBlock("heater", HeaterBlock::new, true);
    public static final RegistryObject<Block> SIGN_SUPPORT = registerBlock("sign_support", SignSupportBlock::new, true);
    public static final RegistryObject<Block> TV = registerBlock("tv", TvBlock::new, true);
    public static final RegistryObject<Block> TRASHBIN = registerBlock("trashbin", TrashbinBlock::new, true);

    // Button states. Only LOCKED and CLOSED are public items.
    public static final RegistryObject<Block> BUTTON_LOCKED = registerButton("button_locked", ButtonState.LOCKED, true);
    public static final RegistryObject<Block> BUTTON_CLOSED = registerButton("button_closed", ButtonState.CLOSED, true);
    public static final RegistryObject<Block> BUTTON_OPENING = registerButton("button_opening", ButtonState.OPENING, false);
    public static final RegistryObject<Block> BUTTON_OPEN = registerButton("button_open", ButtonState.OPEN, false);
    public static final RegistryObject<Block> BUTTON_CLOSING = registerButton("button_closing", ButtonState.CLOSING, false);

    // Door collision indices reproduce the standalone classes. Heavy doors
    // become passable at opening frame 10 and solid again at closing frame 9.
    // Smaller doors stay solid throughout their transition and become passable
    // only at the fully-open endpoint.
    public static final DoorFamily DEFAULT_DOOR = door("default",
            "default_door", numbered("default_door_", 1, 13), "default_door_open",
            descending("default_clos_", 13, 1), 1, false, 9, 4, SoundType.METAL,
            UNITY_DOOR_OPENING, UNITY_DOOR_CLOSING);
    public static final DoorFamily YELLOW_DOOR = door("yellow",
            "yellow_closed", numbered("yellow_", 1, 13), "yellow_open",
            descending("yellow_c_", 13, 1), 1, false, 9, 4, SoundType.METAL,
            UNITY_DOOR_OPENING, UNITY_DOOR_CLOSING);
    public static final DoorFamily BLACK_DOOR = door("black",
            "black_closed", numbered("black_", 1, 13), "black_open",
            descending("black_c_", 13, 1), 1, false, 9, 4, SoundType.METAL,
            UNITY_DOOR_OPENING, UNITY_DOOR_CLOSING);
    public static final DoorFamily NORMAL_DOOR = door("normal",
            "normal_door", numbered("ndoor_", 1, 4), "door_open",
            numbered("door_c_", 1, 3), 5, true, 4, 0, SoundType.WOOD,
            UNITY_DOOR_OPEN, UNITY_DOOR_CLOSE);
    public static final DoorFamily LEFT_LOG_DOOR = door("left_logistics",
            "left_log_door", numbered("left_log_door_", 1, 4), "left_log_door_open",
            numbered("left_log_clo_", 1, 3), 5, true, 4, 0, SoundType.WOOD,
            UNITY_DOOR_OPEN, UNITY_DOOR_CLOSE);
    public static final DoorFamily RIGHT_LOG_DOOR = door("right_logistics",
            "right_log_door", numbered("right_log_door_", 1, 4), "right_log_door_open",
            List.of("right_log_clos_1", "right_log_clos_2", "right_clos_3"),
            5, true, 4, 0, SoundType.WOOD, UNITY_DOOR_OPEN, UNITY_DOOR_CLOSE);
    public static final DoorFamily OFFICE_DOOR = door("office",
            "office_door", numbered("office_door_", 1, 4), "office_door_open",
            numbered("office_c_", 1, 3), 5, true, 4, 0, SoundType.WOOD,
            UNITY_OFFICE_OPEN, UNITY_OFFICE_CLOSE);
    public static final DoorFamily BATH_DOOR = door("bathroom",
            "bath_door", numbered("bath_door_", 1, 3), "bath_door_open",
            numbered("bath_c_", 1, 3), 5, true, 3, 0, SoundType.WOOD,
            UNITY_BATH_OPEN, UNITY_BATH_CLOSE);
    public static final DoorFamily WORKSHOP_DOOR = door("workshop",
            "ws_dclosed", numbered("ws_", 1, 4), "ws_open",
            List.of("w_sc_1", "w_sc_2", "wsc_3"), 5, true, 4, 0, SoundType.WOOD,
            UNITY_DOOR_OPEN, UNITY_DOOR_CLOSE);

    public static final RegistryObject<CreativeModeTab> SCP_UNITY_BLOCKS = TABS.register("scp_unity_blocks", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("item_group.scp_additions.scp_unity_blocks"))
                    .icon(() -> new ItemStack(TESLA_BOTTOM.get()))
                    .displayItems((parameters, output) ->
                            creativeItemsInDisplayOrder().forEach(item -> output.accept(item.get())))
                    .withSearchBar()
                    .build());

    private FacilityModule() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }

    public static RegistryObject<Block> blockByPath(String path) {
        return BLOCKS_BY_PATH.get(path);
    }

    public static RegistryObject<Item> itemByPath(String path) {
        return ITEMS_BY_PATH.get(path);
    }

    public static boolean isFacilityDoor(BlockState state) {
        return state != null && state.getBlock() instanceof AnimatedDoorBlock;
    }

    public static boolean isDoorPassable(BlockState state) {
        return state != null && state.getBlock() instanceof AnimatedDoorBlock door && door.passable();
    }

    public static boolean isWindowedDoor(BlockState state) {
        if (state == null || !(state.getBlock() instanceof AnimatedDoorBlock door)) return false;
        return "office".equals(door.familyId);
    }

    /**
     * Visual blocking is deliberately independent from render occlusion and
     * path collision. Every animation frame remains visually transparent so an
     * observed SCP-173 cannot exploit the closing animation; only the fully
     * closed endpoint uses the model-derived opaque geometry.
     */
    public static VoxelShape doorVisualOcclusionShape(BlockState state) {
        if (state == null || !(state.getBlock() instanceof AnimatedDoorBlock door)
                || door.stage == DoorStage.OPENING || door.stage == DoorStage.CLOSING
                || door.passable()) {
            return Shapes.empty();
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return door.family().directUse()
                ? FacilityDoorShapes.visualOcclusionShape(door.familyId, facing)
                : heavyDoorShape(facing);
    }

    /**
     * Stable, curated order for the public facility tab. Registration order is
     * intentionally not used here because animation states and compatibility
     * entries are interleaved with the public endpoints.
     */
    private static List<RegistryObject<Item>> creativeItemsInDisplayOrder() {
        List<RegistryObject<Item>> ordered = new ArrayList<>();

        // Frequently used facility props.
        addFacilityCreativeItem(ordered, "walllight");
        addFacilityCreativeItem(ordered, "heater");
        addFacilityCreativeItem(ordered, "sign_support");
        addFacilityCreativeItem(ordered, "tv");
        addFacilityCreativeItem(ordered, "trashbin");

        // Public closed endpoints for every door family, preserving the
        // original family order from this module.
        addFacilityCreativeItem(ordered, "default_door");
        addFacilityCreativeItem(ordered, "yellow_closed");
        addFacilityCreativeItem(ordered, "black_closed");
        addFacilityCreativeItem(ordered, "normal_door");
        addFacilityCreativeItem(ordered, "left_log_door");
        addFacilityCreativeItem(ordered, "right_log_door");
        addFacilityCreativeItem(ordered, "office_door");
        addFacilityCreativeItem(ordered, "bath_door");
        addFacilityCreativeItem(ordered, "ws_dclosed");

        // Main SL1 navigation and wall-detail pieces.
        addUBlockCreativeItem(ordered, "sl_1_floor_detail_small");
        addUBlockCreativeItem(ordered, "sl_1_floor_detail_big");
        addUBlockCreativeItem(ordered, "sl_1_wall_detail_1_bot");
        addUBlockCreativeItem(ordered, "sl_1_wall_detail_1_mid");
        addUBlockCreativeItem(ordered, "sl_1_wall_detail_1_top");
        addUBlockCreativeItem(ordered, "sl_1_wall_detail_2");

        // Preserve the relative order of everything else and suppress entries
        // already placed above.
        UBlocksModule.creativeItems().forEach(item -> addUnique(ordered, item));
        CREATIVE_ITEMS.forEach(item -> addUnique(ordered, item));
        return ordered;
    }

    private static void addFacilityCreativeItem(List<RegistryObject<Item>> ordered, String path) {
        RegistryObject<Item> item = ITEMS_BY_PATH.get(path);
        if (item != null) addUnique(ordered, item);
    }

    private static void addUBlockCreativeItem(List<RegistryObject<Item>> ordered, String path) {
        RegistryObject<Item> item = UBlocksModule.itemByPath(path);
        if (item != null) addUnique(ordered, item);
    }

    private static void addUnique(List<RegistryObject<Item>> ordered, RegistryObject<Item> item) {
        if (!ordered.contains(item)) ordered.add(item);
    }

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, path)));
    }

    private static RegistryObject<Block> structure(String path) {
        return registerBlock(path, FacilityStructureBlock::new, true);
    }

    private static RegistryObject<Block> registerButton(String path, ButtonState state, boolean publicItem) {
        return registerBlock(path, () -> new DoorButtonBlock(state), publicItem);
    }

    private static RegistryObject<Block> registerBlock(String path,
            Supplier<? extends Block> factory, boolean publicItem) {
        RegistryObject<Block> block = BLOCKS.register(path, factory);
        RegistryObject<Item> item = ITEMS.register(path,
                () -> new BlockItem(block.get(), new Item.Properties()));
        BLOCKS_BY_PATH.put(path, block);
        ITEMS_BY_PATH.put(path, item);
        if (publicItem) CREATIVE_ITEMS.add(item);
        return block;
    }

    private static DoorFamily door(String id, String closedPath, List<String> openingPaths,
            String openPath, List<String> closingPaths, int frameDelay, boolean directUse,
            int openingPassableFrame, int closingSolidFrame, SoundType soundType,
            RegistryObject<SoundEvent> openingSound, RegistryObject<SoundEvent> closingSound) {
        RegistryObject<Block> closed = registerBlock(closedPath,
                () -> new AnimatedDoorBlock(id, DoorStage.CLOSED, 0, soundType), true);

        List<RegistryObject<Block>> opening = new ArrayList<>();
        for (int i = 0; i < openingPaths.size(); i++) {
            final int frame = i;
            opening.add(registerBlock(openingPaths.get(i),
                    () -> new AnimatedDoorBlock(id, DoorStage.OPENING, frame, soundType), false));
        }

        RegistryObject<Block> open = registerBlock(openPath,
                () -> new AnimatedDoorBlock(id, DoorStage.OPEN, 0, soundType), false);

        List<RegistryObject<Block>> closing = new ArrayList<>();
        for (int i = 0; i < closingPaths.size(); i++) {
            final int frame = i;
            closing.add(registerBlock(closingPaths.get(i),
                    () -> new AnimatedDoorBlock(id, DoorStage.CLOSING, frame, soundType), false));
        }

        DoorFamily family = new DoorFamily(id, closed, List.copyOf(opening), open,
                List.copyOf(closing), frameDelay, directUse, openingPassableFrame,
                closingSolidFrame, openingSound, closingSound);
        DOOR_FAMILIES.put(id, family);
        return family;
    }

    private static List<String> numbered(String prefix, int first, int last) {
        List<String> result = new ArrayList<>();
        for (int i = first; i <= last; i++) result.add(prefix + i);
        return result;
    }

    private static List<String> descending(String prefix, int first, int last) {
        List<String> result = new ArrayList<>();
        for (int i = first; i >= last; i--) result.add(prefix + i);
        return result;
    }

    private static BlockState copyFacing(BlockState from, Block target) {
        BlockState result = target.defaultBlockState();
        if (from.hasProperty(HorizontalDirectionalBlock.FACING)
                && result.hasProperty(HorizontalDirectionalBlock.FACING)) {
            result = result.setValue(HorizontalDirectionalBlock.FACING,
                    from.getValue(HorizontalDirectionalBlock.FACING));
        }
        if (from.hasProperty(BlockStateProperties.WATERLOGGED)
                && result.hasProperty(BlockStateProperties.WATERLOGGED)) {
            result = result.setValue(BlockStateProperties.WATERLOGGED,
                    from.getValue(BlockStateProperties.WATERLOGGED));
        }
        return result;
    }

    private static boolean doorPowered(Level level, BlockPos pos) {
        return level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
    }

    private static VoxelShape heavyDoorShape(Direction facing) {
        return facing.getAxis() == Direction.Axis.X
                ? Block.box(4.75D, 0.0D, 0.0D, 11.25D, 32.0D, 16.0D)
                : Block.box(0.0D, 0.0D, 4.75D, 16.0D, 32.0D, 11.25D);
    }

    private static boolean isDoorButton(Block block) {
        return block == BUTTON_LOCKED.get() || block == BUTTON_CLOSED.get()
                || block == BUTTON_OPENING.get() || block == BUTTON_OPEN.get()
                || block == BUTTON_CLOSING.get();
    }

    private static RegistryObject<Block> buttonFor(ButtonState state) {
        return switch (state) {
            case LOCKED -> BUTTON_LOCKED;
            case CLOSED -> BUTTON_CLOSED;
            case OPENING -> BUTTON_OPENING;
            case OPEN -> BUTTON_OPEN;
            case CLOSING -> BUTTON_CLOSING;
        };
    }

    private static void setButtonPair(ServerLevel level, BlockPos pos, ButtonState target) {
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof DoorButtonBlock)) return;
        Direction facing = current.getValue(HorizontalDirectionalBlock.FACING);
        level.setBlock(pos, buttonFor(target).get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);

        BlockPos pairPos = pos.relative(facing.getOpposite(), 2);
        BlockState pairState = level.getBlockState(pairPos);
        if (isDoorButton(pairState.getBlock())) {
            level.setBlock(pairPos, buttonFor(target).get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing.getOpposite()), Block.UPDATE_ALL);
        }
    }

    public enum DoorStage {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    public enum ButtonState {
        LOCKED,
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    public record DoorFamily(String id, RegistryObject<Block> closed,
            List<RegistryObject<Block>> opening, RegistryObject<Block> open,
            List<RegistryObject<Block>> closing, int frameDelay, boolean directUse,
            int openingPassableFrame, int closingSolidFrame,
            RegistryObject<SoundEvent> openingSound, RegistryObject<SoundEvent> closingSound) {
    }

    private static final class FacilityStructureBlock extends Block {
        private FacilityStructureBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.0F, 10.0F));
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            List<ItemStack> original = super.getDrops(state, builder);
            return original.isEmpty() ? Collections.singletonList(new ItemStack(this)) : original;
        }
    }

    private abstract static class HorizontalWaterloggedPropBlock extends HorizontalDirectionalBlock
            implements SimpleWaterloggedBlock {
        protected static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

        protected HorizontalWaterloggedPropBlock(BlockBehaviour.Properties properties) {
            super(properties.noOcclusion().isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                    .setValue(WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(WATERLOGGED, waterlogged);
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
        public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            if (state.getValue(WATERLOGGED)) {
                level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
            return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }
    }

    private static final class AlarmLampBlock extends Block {
        private final boolean on;

        private AlarmLampBlock(boolean on) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.0F, 10.0F)
                    .lightLevel(state -> on ? 15 : 0).noOcclusion());
            this.on = on;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            level.scheduleTick(pos, this, 1);
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos,
                Block neighbor, BlockPos neighborPos, boolean moving) {
            level.scheduleTick(pos, this, 1);
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean powered = level.hasNeighborSignal(pos);
            if (powered != on) {
                Block target = powered ? ALARM_LAMP_ON.get() : ALARM_LAMP.get();
                level.setBlock(pos, target.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(ALARM_LAMP.get()));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(ALARM_LAMP.get());
        }
    }

    private static final class WallLightBlock extends HorizontalWaterloggedPropBlock {
        private final boolean upper;

        private WallLightBlock(boolean upper) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).lightLevel(state -> 15));
            this.upper = upper;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            Direction facing = state.getValue(FACING);
            if (upper) {
                return switch (facing) {
                    case NORTH -> Shapes.or(box(5, 0, 14.6, 11, 2.5, 16.6),
                            box(10, -0.01, 15, 12, 2.51, 17), box(4, -0.01, 15, 6, 2.51, 17));
                    case EAST -> Shapes.or(box(-0.6, 0, 5, 1.4, 2.5, 11),
                            box(-1, -0.01, 10, 1, 2.51, 12), box(-1, -0.01, 4, 1, 2.51, 6));
                    case WEST -> Shapes.or(box(14.6, 0, 5, 16.6, 2.5, 11),
                            box(15, -0.01, 4, 17, 2.51, 6), box(15, -0.01, 10, 17, 2.51, 12));
                    default -> Shapes.or(box(5, 0, -0.6, 11, 2.5, 1.4),
                            box(4, -0.01, -1, 6, 2.51, 1), box(10, -0.01, -1, 12, 2.51, 1));
                };
            }
            return switch (facing) {
                case NORTH -> Shapes.or(box(10, 13.49, 15, 12, 16.01, 17),
                        box(5, 13.5, 14.6, 11, 16, 16.6), box(4, 13.49, 15, 6, 16.01, 17));
                case EAST -> Shapes.or(box(-1, 13.49, 10, 1, 16.01, 12),
                        box(-0.6, 13.5, 5, 1.4, 16, 11), box(-1, 13.49, 4, 1, 16.01, 6));
                case WEST -> Shapes.or(box(15, 13.49, 4, 17, 16.01, 6),
                        box(14.6, 13.5, 5, 16.6, 16, 11), box(15, 13.49, 10, 17, 16.01, 12));
                default -> Shapes.or(box(4, 13.49, -1, 6, 16.01, 1),
                        box(5, 13.5, -0.6, 11, 16, 1.4), box(10, 13.49, -1, 12, 16.01, 1));
            };
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (!upper && !level.isClientSide && level.getBlockState(pos.above()).canBeReplaced()) {
                level.setBlock(pos.above(), WALLLIGHT_2.get().defaultBlockState()
                        .setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (state.getBlock() != newState.getBlock()) {
                BlockPos other = upper ? pos.below() : pos.above();
                Block expected = upper ? WALLLIGHT.get() : WALLLIGHT_2.get();
                if (level.getBlockState(other).is(expected)) level.removeBlock(other, false);
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return upper ? Collections.emptyList()
                    : Collections.singletonList(new ItemStack(WALLLIGHT.get()));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(WALLLIGHT.get());
        }
    }

    private static final class HeaterBlock extends HorizontalWaterloggedPropBlock {
        private HeaterBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).lightLevel(state -> 5));
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(6, 0, 6, 10, 1, 10), box(6, 14.9, 6, 10, 15.9, 10),
                        box(6.35, 1, 7.6, 8.45, 13.75, 9.7), box(6, 13.45, 5.6, 10, 14.95, 10.5),
                        box(7.65, 1, 6.5, 10.65, 13.75, 8.5));
                case EAST -> Shapes.or(box(6, 0, 6, 10, 1, 10), box(6, 14.9, 6, 10, 15.9, 10),
                        box(6.3, 1, 6.35, 8.4, 13.75, 8.45), box(5.5, 13.45, 6, 10.4, 14.95, 10),
                        box(7.5, 1, 7.65, 9.5, 13.75, 10.65));
                case WEST -> Shapes.or(box(6, 0, 6, 10, 1, 10), box(6, 14.9, 6, 10, 15.9, 10),
                        box(7.6, 1, 7.55, 9.7, 13.75, 9.65), box(5.6, 13.45, 6, 10.5, 14.95, 10),
                        box(6.5, 1, 5.35, 8.5, 13.75, 8.35));
                default -> Shapes.or(box(6, 0, 6, 10, 1, 10), box(6, 14.9, 6, 10, 15.9, 10),
                        box(7.55, 1, 6.3, 9.65, 13.75, 8.4), box(6, 13.45, 5.5, 10, 14.95, 10.4),
                        box(5.35, 1, 7.5, 8.35, 13.75, 9.5));
            };
        }
    }

    private static final class SignSupportBlock extends HorizontalWaterloggedPropBlock {
        private SignSupportBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.GLASS)
                    .strength(1.0F, 10.0F).noCollission());
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(8.2, -3.75, 15.55, 8.7, -3.25, 16.8),
                        box(22.8, -3.75, 15.55, 23.3, -3.25, 16.8),
                        box(22.8, -12.55, 15.55, 23.3, -12.05, 16.8),
                        box(8.2, -12.55, 15.55, 8.7, -12.05, 16.8),
                        box(8.2, -13.35, 15.7, 23.7, -12.85, 15.9),
                        box(8.2, -3.15, 15.7, 23.7, -2.65, 15.9));
                case EAST -> Shapes.or(box(-0.8, -3.75, 8.2, 0.45, -3.25, 8.7),
                        box(-0.8, -3.75, 22.8, 0.45, -3.25, 23.3),
                        box(-0.8, -12.55, 22.8, 0.45, -12.05, 23.3),
                        box(-0.8, -12.55, 8.2, 0.45, -12.05, 8.7),
                        box(0.1, -13.35, 8.2, 0.3, -12.85, 23.7),
                        box(0.1, -3.15, 8.2, 0.3, -2.65, 23.7));
                case WEST -> Shapes.or(box(15.55, -3.75, 7.3, 16.8, -3.25, 7.8),
                        box(15.55, -3.75, -7.3, 16.8, -3.25, -6.8),
                        box(15.55, -12.55, -7.3, 16.8, -12.05, -6.8),
                        box(15.55, -12.55, 7.3, 16.8, -12.05, 7.8),
                        box(15.7, -13.35, -7.7, 15.9, -12.85, 7.8),
                        box(15.7, -3.15, -7.7, 15.9, -2.65, 7.8));
                default -> Shapes.or(box(7.3, -3.75, -0.8, 7.8, -3.25, 0.45),
                        box(-7.3, -3.75, -0.8, -6.8, -3.25, 0.45),
                        box(-7.3, -12.55, -0.8, -6.8, -12.05, 0.45),
                        box(7.3, -12.55, -0.8, 7.8, -12.05, 0.45),
                        box(-7.7, -13.35, 0.1, 7.8, -12.85, 0.3),
                        box(-7.7, -3.15, 0.1, 7.8, -2.65, 0.3));
            };
        }
    }

    private static final class TvBlock extends DirectionalBlock {
        private TvBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).noCollission().noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING,
                    context.getNearestLookingDirection().getOpposite());
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
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return switch (state.getValue(FACING)) {
                case NORTH -> Shapes.or(box(-16, -13.05, 15.25, 32, -12.3, 17.75),
                        box(-16, 12.55, 15.25, 32, 13.3, 17.75),
                        box(31.225, -12.275, 15.25, 31.975, 12.625, 17.75),
                        box(-15.975, -12.375, 15.25, -15.225, 12.625, 17.75));
                case EAST -> Shapes.or(box(-1.75, -13.05, -16, 0.75, -12.3, 32),
                        box(-1.75, 12.55, -16, 0.75, 13.3, 32),
                        box(-1.75, -12.275, 31.225, 0.75, 12.625, 31.975),
                        box(-1.75, -12.375, -15.975, 0.75, 12.625, -15.225));
                case WEST -> Shapes.or(box(15.25, -13.05, -16, 17.75, -12.3, 32),
                        box(15.25, 12.55, -16, 17.75, 13.3, 32),
                        box(15.25, -12.275, -15.975, 17.75, 12.625, -15.225),
                        box(15.25, -12.375, 31.225, 17.75, 12.625, 31.975));
                case UP -> Shapes.or(box(-16, -1.75, -13.05, 32, 0.75, -12.3),
                        box(-16, -1.75, 12.55, 32, 0.75, 13.3),
                        box(31.225, -1.75, -12.275, 31.975, 0.75, 12.625),
                        box(-15.975, -1.75, -12.375, -15.225, 0.75, 12.625));
                case DOWN -> Shapes.or(box(-16, 15.25, 28.3, 32, 17.75, 29.05),
                        box(-16, 15.25, 2.7, 32, 17.75, 3.45),
                        box(31.225, 15.25, 3.375, 31.975, 17.75, 28.275),
                        box(-15.975, 15.25, 3.375, -15.225, 17.75, 28.375));
                default -> Shapes.or(box(-16, -13.05, -1.75, 32, -12.3, 0.75),
                        box(-16, 12.55, -1.75, 32, 13.3, 0.75),
                        box(-15.975, -12.275, -1.75, -15.225, 12.625, 0.75),
                        box(31.225, -12.375, -1.75, 31.975, 12.625, 0.75));
            };
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }
    }

    private static final class TrashbinBlock extends HorizontalDirectionalBlock {
        private TrashbinBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).noOcclusion());
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
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return state.getValue(FACING).getAxis() == Direction.Axis.X
                    ? box(5.5, 0, 4.25, 10.5, 12, 11.75)
                    : box(4.25, 0, 5.5, 11.75, 12, 10.5);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(this));
        }
    }

    private static final class AnimatedDoorBlock extends HorizontalDirectionalBlock {
        private final String familyId;
        private final DoorStage stage;
        private final int frame;

        private AnimatedDoorBlock(String familyId, DoorStage stage, int frame, SoundType soundType) {
            super(BlockBehaviour.Properties.of().sound(soundType).strength(1.0F, 10.0F)
                    .noOcclusion().isRedstoneConductor((state, level, pos) -> false));
            this.familyId = familyId;
            this.stage = stage;
            this.frame = frame;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        private DoorFamily family() {
            return DOOR_FAMILIES.get(familyId);
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

        private boolean passable() {
            DoorFamily family = family();
            if (family.directUse()) return stage != DoorStage.CLOSED;
            return switch (stage) {
                case OPEN -> true;
                case CLOSED -> false;
                case OPENING -> frame >= family.openingPassableFrame();
                case CLOSING -> frame < family.closingSolidFrame();
            };
        }

        private boolean manualDoorUsesOpenShape() {
            DoorFamily family = family();
            return switch (stage) {
                case CLOSED -> false;
                case OPEN -> true;
                case OPENING -> frame * 2 >= Math.max(1, family.opening().size() - 1);
                case CLOSING -> frame * 2 < Math.max(1, family.closing().size() - 1);
            };
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            Direction facing = state.getValue(FACING);
            return family().directUse()
                    ? FacilityDoorShapes.shape(familyId, manualDoorUsesOpenShape(), facing)
                    : heavyDoorShape(facing);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            if (passable()) return Shapes.empty();
            Direction facing = state.getValue(FACING);
            return family().directUse()
                    ? FacilityDoorShapes.shape(familyId, false, facing)
                    : heavyDoorShape(facing);
        }

        @Override
        public boolean isPathfindable(BlockState state, BlockGetter level,
                BlockPos pos, PathComputationType type) {
            return passable();
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            int delay = stage == DoorStage.OPENING || stage == DoorStage.CLOSING
                    ? family().frameDelay() : 1;
            level.scheduleTick(pos, this, delay);
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos,
                Block neighbor, BlockPos neighborPos, boolean moving) {
            if (stage == DoorStage.CLOSED || stage == DoorStage.OPEN) {
                level.scheduleTick(pos, this, 1);
            }
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            DoorFamily family = family();
            if (!family.directUse() || (stage != DoorStage.CLOSED && stage != DoorStage.OPEN)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide && level instanceof ServerLevel server) {
                if (stage == DoorStage.CLOSED) startOpening(server, pos, state, family);
                else startClosing(server, pos, state, family);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            DoorFamily family = family();
            switch (stage) {
                case CLOSED -> {
                    if (doorPowered(level, pos)) startOpening(level, pos, state, family);
                }
                case OPEN -> {
                    if (!family.directUse() && !doorPowered(level, pos)) {
                        startClosing(level, pos, state, family);
                    }
                }
                case OPENING -> {
                    Block next = frame + 1 < family.opening().size()
                            ? family.opening().get(frame + 1).get() : family.open().get();
                    level.setBlock(pos, copyFacing(state, next), Block.UPDATE_ALL);
                }
                case CLOSING -> {
                    Block next = frame + 1 < family.closing().size()
                            ? family.closing().get(frame + 1).get() : family.closed().get();
                    level.setBlock(pos, copyFacing(state, next), Block.UPDATE_ALL);
                }
            }
        }

        private static void startOpening(ServerLevel level, BlockPos pos,
                BlockState state, DoorFamily family) {
            if (family.opening().isEmpty()) return;
            level.playSound(null, pos, family.openingSound().get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, copyFacing(state, family.opening().get(0).get()),
                    Block.UPDATE_ALL);
        }

        private static void startClosing(ServerLevel level, BlockPos pos,
                BlockState state, DoorFamily family) {
            if (family.closing().isEmpty()) return;
            level.playSound(null, pos, family.closingSound().get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, copyFacing(state, family.closing().get(0).get()),
                    Block.UPDATE_ALL);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(family().closed().get()));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(family().closed().get());
        }
    }

    private static final class DoorButtonBlock extends HorizontalDirectionalBlock {
        private final ButtonState buttonState;

        private DoorButtonBlock(ButtonState buttonState) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.0F, 10.0F)
                    .noCollission().noOcclusion().isRedstoneConductor((state, level, pos) -> false));
            this.buttonState = buttonState;
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
            return switch (state.getValue(FACING)) {
                case NORTH -> Block.box(-4.2D, -2.66D, 14.2D, -0.9D, 2.64D, 16.0D);
                case EAST -> Block.box(0.0D, -2.66D, -4.2D, 1.8D, 2.64D, -0.9D);
                case SOUTH -> Block.box(16.9D, -2.66D, 0.0D, 20.2D, 2.64D, 1.8D);
                case WEST -> Block.box(14.2D, -2.66D, 16.9D, 16.0D, 2.64D, 20.2D);
                default -> Shapes.empty();
            };
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return buttonState == ButtonState.OPENING || buttonState == ButtonState.OPEN;
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level,
                BlockPos pos, Direction direction) {
            return isSignalSource(state) ? 15 : 0;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level.isClientSide) return;

            if (buttonState == ButtonState.OPENING || buttonState == ButtonState.CLOSING) {
                level.scheduleTick(pos, this, 22);
            }
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos,
                Player player, InteractionHand hand, BlockHitResult hit) {
            if (buttonState != ButtonState.CLOSED && buttonState != ButtonState.OPEN) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide && level instanceof ServerLevel server) {
                DoorButtonIndependentInteractionEvents.activateButton(server, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void tick(BlockState state, ServerLevel level,
                BlockPos pos, RandomSource random) {
            if (buttonState == ButtonState.OPENING) {
                setButtonPair(level, pos, ButtonState.OPEN);
            } else if (buttonState == ButtonState.CLOSING) {
                setButtonPair(level, pos, ButtonState.CLOSED);
            }
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            Block drop = buttonState == ButtonState.LOCKED
                    ? BUTTON_LOCKED.get() : BUTTON_CLOSED.get();
            return Collections.singletonList(new ItemStack(drop));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target,
                BlockGetter level, BlockPos pos, Player player) {
            return new ItemStack(buttonState == ButtonState.LOCKED
                    ? BUTTON_LOCKED.get() : BUTTON_CLOSED.get());
        }
    }
}
