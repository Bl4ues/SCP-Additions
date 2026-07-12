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
 * Public registry IDs now live under {@code scp_additions}. The original
 * {@code scp_unity_extra_blocks} asset namespace remains as a resource library,
 * and {@link FacilityLegacyMappings} remaps old block/item IDs when a world is
 * loaded. Animation frames stay registered for world compatibility but only
 * stable endpoints are exposed in the creative tab.
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

    // Architectural pieces already present in the first migration batch.
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
    public static final RegistryObject<Block> HEATER = prop("heater", true, false);
    public static final RegistryObject<Block> SIGN_SUPPORT = prop("sign_support", true, true);
    public static final RegistryObject<Block> TV = prop("tv", true, false);
    public static final RegistryObject<Block> TRASHBIN = prop("trashbin", true, false);

    // Button states. Only LOCKED and CLOSED are public items.
    public static final RegistryObject<Block> BUTTON_LOCKED = registerButton("button_locked", ButtonState.LOCKED, true);
    public static final RegistryObject<Block> BUTTON_CLOSED = registerButton("button_closed", ButtonState.CLOSED, true);
    public static final RegistryObject<Block> BUTTON_OPENING = registerButton("button_opening", ButtonState.OPENING, false);
    public static final RegistryObject<Block> BUTTON_OPEN = registerButton("button_open", ButtonState.OPEN, false);
    public static final RegistryObject<Block> BUTTON_CLOSING = registerButton("button_closing", ButtonState.CLOSING, false);

    // Door families. Frame registry names match the standalone mod exactly.
    public static final DoorFamily DEFAULT_DOOR = door("default",
            "default_door", numbered("default_door_", 1, 13), "default_door_open",
            descending("default_clos_", 13, 1), 1, false,
            UNITY_DOOR_OPENING, UNITY_DOOR_CLOSING);
    public static final DoorFamily YELLOW_DOOR = door("yellow",
            "yellow_closed", numbered("yellow_", 1, 13), "yellow_open",
            descending("yellow_c_", 13, 1), 1, false,
            UNITY_DOOR_OPENING, UNITY_DOOR_CLOSING);
    public static final DoorFamily BLACK_DOOR = door("black",
            "black_closed", numbered("black_", 1, 13), "black_open",
            descending("black_c_", 13, 1), 1, false,
            UNITY_DOOR_OPENING, UNITY_DOOR_CLOSING);
    public static final DoorFamily NORMAL_DOOR = door("normal",
            "normal_door", numbered("ndoor_", 1, 4), "door_open",
            numbered("door_c_", 1, 3), 5, true,
            UNITY_DOOR_OPEN, UNITY_DOOR_CLOSE);
    public static final DoorFamily LEFT_LOG_DOOR = door("left_logistics",
            "left_log_door", numbered("left_log_door_", 1, 4), "left_log_door_open",
            numbered("left_log_clo_", 1, 3), 5, true,
            UNITY_DOOR_OPEN, UNITY_DOOR_CLOSE);
    public static final DoorFamily RIGHT_LOG_DOOR = door("right_logistics",
            "right_log_door", numbered("right_log_door_", 1, 4), "right_log_door_open",
            List.of("right_log_clos_1", "right_log_clos_2", "right_clos_3"), 5, true,
            UNITY_DOOR_OPEN, UNITY_DOOR_CLOSE);
    public static final DoorFamily OFFICE_DOOR = door("office",
            "office_door", numbered("office_door_", 1, 4), "office_door_open",
            numbered("office_c_", 1, 3), 5, true,
            UNITY_OFFICE_OPEN, UNITY_OFFICE_CLOSE);
    public static final DoorFamily BATH_DOOR = door("bathroom",
            "bath_door", numbered("bath_door_", 1, 3), "bath_door_open",
            numbered("bath_c_", 1, 3), 5, true,
            UNITY_BATH_OPEN, UNITY_BATH_CLOSE);
    public static final DoorFamily WORKSHOP_DOOR = door("workshop",
            "ws_dclosed", numbered("ws_", 1, 4), "ws_open",
            List.of("w_sc_1", "w_sc_2", "wsc_3"), 5, true,
            UNITY_DOOR_OPEN, UNITY_DOOR_CLOSE);

    public static final RegistryObject<CreativeModeTab> SCP_UNITY_BLOCKS = TABS.register("scp_unity_blocks", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("item_group.scp_additions.scp_unity_blocks"))
                    .icon(() -> new ItemStack(TESLA_BOTTOM.get()))
                    .displayItems((parameters, output) -> CREATIVE_ITEMS.forEach(item -> output.accept(item.get())))
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

    private static RegistryObject<SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, path)));
    }

    private static RegistryObject<Block> structure(String path) {
        return registerBlock(path, FacilityStructureBlock::new, true);
    }

    private static RegistryObject<Block> prop(String path, boolean directional, boolean noCollision) {
        return registerBlock(path,
                () -> directional ? new DirectionalPropBlock(noCollision) : new FacilityStructureBlock(), true);
    }

    private static RegistryObject<Block> registerButton(String path, ButtonState state, boolean publicItem) {
        return registerBlock(path, () -> new DoorButtonBlock(state), publicItem);
    }

    private static RegistryObject<Block> registerBlock(String path, Supplier<? extends Block> factory, boolean publicItem) {
        RegistryObject<Block> block = BLOCKS.register(path, factory);
        RegistryObject<Item> item = ITEMS.register(path, () -> new BlockItem(block.get(), new Item.Properties()));
        BLOCKS_BY_PATH.put(path, block);
        ITEMS_BY_PATH.put(path, item);
        if (publicItem) {
            CREATIVE_ITEMS.add(item);
        }
        return block;
    }

    private static DoorFamily door(String id, String closedPath, List<String> openingPaths,
            String openPath, List<String> closingPaths, int frameDelay, boolean directUse,
            RegistryObject<SoundEvent> openingSound, RegistryObject<SoundEvent> closingSound) {
        RegistryObject<Block> closed = registerBlock(closedPath,
                () -> new AnimatedDoorBlock(id, DoorStage.CLOSED, 0), true);
        List<RegistryObject<Block>> opening = new ArrayList<>();
        for (int i = 0; i < openingPaths.size(); i++) {
            final int frame = i;
            opening.add(registerBlock(openingPaths.get(i),
                    () -> new AnimatedDoorBlock(id, DoorStage.OPENING, frame), false));
        }
        RegistryObject<Block> open = registerBlock(openPath,
                () -> new AnimatedDoorBlock(id, DoorStage.OPEN, 0), false);
        List<RegistryObject<Block>> closing = new ArrayList<>();
        for (int i = 0; i < closingPaths.size(); i++) {
            final int frame = i;
            closing.add(registerBlock(closingPaths.get(i),
                    () -> new AnimatedDoorBlock(id, DoorStage.CLOSING, frame), false));
        }
        DoorFamily family = new DoorFamily(id, closed, List.copyOf(opening), open, List.copyOf(closing),
                frameDelay, directUse, openingSound, closingSound);
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

    private static VoxelShape doorShape(Direction facing) {
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
        BlockState replacement = buttonFor(target).get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
        level.setBlock(pos, replacement, Block.UPDATE_ALL);

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

    private static class DirectionalPropBlock extends HorizontalDirectionalBlock {
        private final boolean noCollision;

        private DirectionalPropBlock(boolean noCollision) {
            super(properties(noCollision));
            this.noCollision = noCollision;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        private static BlockBehaviour.Properties properties(boolean noCollision) {
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                    .sound(SoundType.METAL).strength(1.0F, 10.0F).noOcclusion();
            return noCollision ? properties.noCollission() : properties;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return noCollision ? Shapes.empty() : super.getVisualShape(state, level, pos, context);
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
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            level.scheduleTick(pos, this, 1);
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                BlockPos neighborPos, boolean moving) {
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
        public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level,
                BlockPos pos, Player player) {
            return new ItemStack(ALARM_LAMP.get());
        }
    }

    private static final class WallLightBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
        private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
        private final boolean upper;

        private WallLightBlock(boolean upper) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.0F, 10.0F)
                    .lightLevel(state -> 15).noOcclusion().noCollission());
            this.upper = upper;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER);
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(WATERLOGGED, waterlogged);
        }

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

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (!upper && !level.isClientSide && level.getBlockState(pos.above()).canBeReplaced()) {
                level.setBlock(pos.above(), WALLLIGHT_2.get().defaultBlockState()
                        .setValue(FACING, state.getValue(FACING)), Block.UPDATE_ALL);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
            if (state.getBlock() != newState.getBlock()) {
                BlockPos other = upper ? pos.below() : pos.above();
                Block expected = upper ? WALLLIGHT.get() : WALLLIGHT_2.get();
                if (level.getBlockState(other).is(expected)) level.removeBlock(other, false);
            }
            super.onRemove(state, level, pos, newState, moving);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return upper ? Collections.emptyList() : Collections.singletonList(new ItemStack(WALLLIGHT.get()));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level,
                BlockPos pos, Player player) {
            return new ItemStack(WALLLIGHT.get());
        }
    }

    private static final class AnimatedDoorBlock extends HorizontalDirectionalBlock {
        private final String familyId;
        private final DoorStage stage;
        private final int frame;

        private AnimatedDoorBlock(String familyId, DoorStage stage, int frame) {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.0F, 10.0F)
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
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
            return switch (stage) {
                case OPEN -> true;
                case CLOSED -> false;
                case OPENING -> frame >= Math.max(1, family.opening().size() / 2);
                case CLOSING -> frame < Math.max(1, family.closing().size() / 2);
            };
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return doorShape(state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                CollisionContext context) {
            return passable() ? Shapes.empty() : doorShape(state.getValue(FACING));
        }

        @Override
        public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Shapes.empty();
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            int delay = stage == DoorStage.OPENING || stage == DoorStage.CLOSING ? family().frameDelay() : 1;
            level.scheduleTick(pos, this, delay);
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                BlockPos neighborPos, boolean moving) {
            if (stage == DoorStage.CLOSED || stage == DoorStage.OPEN) level.scheduleTick(pos, this, 1);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                InteractionHand hand, BlockHitResult hit) {
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
                    if (!family.directUse() && !doorPowered(level, pos)) startClosing(level, pos, state, family);
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

        private static void startOpening(ServerLevel level, BlockPos pos, BlockState state, DoorFamily family) {
            if (family.opening().isEmpty()) return;
            level.playSound(null, pos, family.openingSound().get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, copyFacing(state, family.opening().get(0).get()), Block.UPDATE_ALL);
        }

        private static void startClosing(ServerLevel level, BlockPos pos, BlockState state, DoorFamily family) {
            if (family.closing().isEmpty()) return;
            level.playSound(null, pos, family.closingSound().get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.setBlock(pos, copyFacing(state, family.closing().get(0).get()), Block.UPDATE_ALL);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            return Collections.singletonList(new ItemStack(family().closed().get()));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level,
                BlockPos pos, Player player) {
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
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return isSignalSource(state) ? 15 : 0;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level.isClientSide) return;
            Direction facing = state.getValue(FACING);
            BlockPos pairPos = pos.relative(facing.getOpposite(), 2);
            BlockState pairState = level.getBlockState(pairPos);
            if (!isDoorButton(pairState.getBlock()) && pairState.canBeReplaced()) {
                level.setBlock(pairPos, buttonFor(buttonState).get().defaultBlockState()
                        .setValue(FACING, facing.getOpposite()), Block.UPDATE_ALL);
            }
            if (buttonState == ButtonState.OPENING || buttonState == ButtonState.CLOSING) {
                level.scheduleTick(pos, this, 22);
            }
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                InteractionHand hand, BlockHitResult hit) {
            if (buttonState != ButtonState.CLOSED && buttonState != ButtonState.OPEN) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide && level instanceof ServerLevel server) {
                setButtonPair(server, pos,
                        buttonState == ButtonState.CLOSED ? ButtonState.OPENING : ButtonState.CLOSING);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (buttonState == ButtonState.OPENING) setButtonPair(level, pos, ButtonState.OPEN);
            else if (buttonState == ButtonState.CLOSING) setButtonPair(level, pos, ButtonState.CLOSED);
        }

        @Override
        public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            Block drop = buttonState == ButtonState.LOCKED ? BUTTON_LOCKED.get() : BUTTON_CLOSED.get();
            return Collections.singletonList(new ItemStack(drop));
        }

        @Override
        public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level,
                BlockPos pos, Player player) {
            return new ItemStack(buttonState == ButtonState.LOCKED ? BUTTON_LOCKED.get() : BUTTON_CLOSED.get());
        }
    }
}
