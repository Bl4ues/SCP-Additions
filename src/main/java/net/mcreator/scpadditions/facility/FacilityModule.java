package net.mcreator.scpadditions.facility;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.UnifiedReaderItems;

import javax.annotation.Nullable;
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
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
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
    public static final RegistryObject<Block> WALLLIGHT = registerBlock("walllight",
            () -> new WallLightBlock(false), true);
    public static final RegistryObject<Block> WALLLIGHT_2 = registerBlock("walllight_2",
            () -> new WallLightBlock(true), false);
    public static final RegistryObject<Block> HEATER = registerBlock("heater", HeaterBlock::new, true);
    public static final RegistryObject<Block> EMERGENCY_BUTTON = registerBlock(
            "emergency_button", EmergencyButtonBlock::new, true);
    public static final RegistryObject<Block> FIRE_EXTINGUISHER = registerBlock(
            "fire_extinguisher", FireExtinguisherBlock::new, true);
    public static final RegistryObject<Block> SIGN_SUPPORT = registerBlock("sign_support", ScpSignSupportBlock::new, true);
    public static final RegistryObject<Block> CORE_ROOM_SIGN = registerSign(
            "core_room_sign", FacilitySignBlock.SignType.CORE_ROOM);
    public static final RegistryObject<Block> DOOR_SIGN = registerSign(
            "door_sign", FacilitySignBlock.SignType.DOOR);
    public static final RegistryObject<Block> TV = registerBlock("tv", TvBlock::new, true);
    public static final RegistryObject<Block> TRASHBIN = registerBlock("trashbin", TrashbinBlock::new, true);
    public static final RegistryObject<Block> WET_FLOOR = registerWetFloor();
    public static final RegistryObject<Block> WATER_FAUCET = registerBlock(
            "water_faucet", WaterFaucetBlock::new, true);
    public static final RegistryObject<Block> FACILITY_PROP_PART =
            BLOCKS.register("facility_prop_part",
                    FacilityPropPartBlock::new);
    public static final RegistryObject<BlockEntityType<WetFloorBlockEntity>>
            WET_FLOOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "wet_floor", () -> BlockEntityType.Builder.of(
                            WetFloorBlockEntity::new, WET_FLOOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<FacilitySignBlockEntity>>
            FACILITY_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "facility_sign", () -> BlockEntityType.Builder.of(
                            FacilitySignBlockEntity::new,
                            CORE_ROOM_SIGN.get(), DOOR_SIGN.get()).build(null));
    public static final RegistryObject<BlockEntityType<ScpSignSupportBlockEntity>>
            SCP_SIGN_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
                    "scp_sign_support", () -> BlockEntityType.Builder.of(
                            ScpSignSupportBlockEntity::new,
                            SIGN_SUPPORT.get()).build(null));

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

    public static final RegistryObject<CreativeModeTab> SCP_FACILITY_BLOCKS =
            TABS.register("scp_unity_blocks", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_additions.scp_facility_blocks"))
                            .icon(() -> new ItemStack(TESLA_BOTTOM.get()))
                            .displayItems((parameters, output) ->
                                    creativeItemsInDisplayOrder().forEach(output::accept))
                            .withSearchBar()
                            .hideTitle()
                            .build());

    private FacilityModule() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
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
        return "normal".equals(door.familyId) || "office".equals(door.familyId);
    }

    /**
     * Visual blocking is deliberately independent from render occlusion and
     * path collision. Animation frames on the open side of the midpoint allow
     * observation; frames on the closed side use the model-derived geometry.
     */
    public static VoxelShape doorVisualOcclusionShape(BlockState state) {
        if (state == null || !(state.getBlock() instanceof AnimatedDoorBlock door)
                || door.usesOpenVisualState()) {
            return Shapes.empty();
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        return door.family().directUse()
                ? FacilityDoorShapes.visualOcclusionShape(door.familyId, facing)
                : heavyDoorShape(facing);
    }

    public record CreativeSection(ResourceLocation sprite,
            List<ItemStack> items) {
    }

    /**
     * Curated categories for the facility creative tab. Headers are rendered by
     * the client over empty grid rows, so no fake items enter inventories or search.
     */
    public static List<CreativeSection> creativeSections() {
        List<CreativeSection> sections = new ArrayList<>();

        List<ItemStack> functional = new ArrayList<>();
        addExternalCreativeItem(functional, ScpAdditionsModBlocks.TESLA_GATE.get().asItem());
        addExternalCreativeItem(functional, ScpAdditionsModBlocks.TESLA_TERMINAL_OFF.get().asItem());
        addExternalCreativeItem(functional, ScpAdditionsModBlocks.TESLA_TERMINAL_BLOCK.get().asItem());
        addFacilityCreativeItem(functional, "button_closed");
        addFacilityCreativeItem(functional, "button_locked");
        addExternalCreativeItem(functional, UnifiedReaderItems.KEYCARD_READER.get());
        addExternalCreativeItem(functional, ScpAdditionsModBlocks.DECON_OPEN.get().asItem());
        addExternalCreativeItem(functional, ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL.get().asItem());
        addExternalCreativeItem(functional, ScpAdditionsModBlocks.SCP_079CONTROLOFF.get().asItem());
        addFacilityCreativeItem(functional, "default_door");
        addFacilityCreativeItem(functional, "yellow_closed");
        addFacilityCreativeItem(functional, "black_closed");
        addFacilityCreativeItem(functional, "sign_support");
        addFacilityCreativeItem(functional, "door_sign");
        addFacilityCreativeItem(functional, "normal_door");
        addFacilityCreativeItem(functional, "left_log_door");
        addFacilityCreativeItem(functional, "right_log_door");
        addFacilityCreativeItem(functional, "office_door");
        addFacilityCreativeItem(functional, "bath_door");
        addFacilityCreativeItem(functional, "ws_dclosed");
        sections.add(section("functionaltab", functional));

        List<ItemStack> props = new ArrayList<>();
        addFacilityCreativeItem(props, "walllight");
        addFacilityCreativeItem(props, "heater");
        addFacilityCreativeItem(props, "emergency_button");
        addFacilityCreativeItem(props, "fire_extinguisher");
        addFacilityCreativeItem(props, "wet_floor");
        addFacilityCreativeItem(props, "water_faucet");
        addFacilityCreativeItem(props, "tv");
        addFacilityCreativeItem(props, "trashbin");
        addUBlockCreativeItem(props, "vent_open");
        sections.add(section("proptab", props));

        List<ItemStack> general = new ArrayList<>();
        addFacilityCreativeItem(general, "tesla_bottom");
        addFacilityCreativeItem(general, "tesla_mid_1");
        addFacilityCreativeItem(general, "tesla_mid_2");
        addFacilityCreativeItem(general, "tesla_bottom_alt");
        addFacilityCreativeItem(general, "tesla_top_alt");
        addFacilityCreativeItem(general, "archival_bottom");
        addFacilityCreativeItem(general, "archival_mid");
        addFacilityCreativeItem(general, "archival_top");
        addFacilityCreativeItem(general, "archival_bot_1");
        addFacilityCreativeItem(general, "archival_mid_2");
        addFacilityCreativeItem(general, "office_bottom");
        addFacilityCreativeItem(general, "office_mid");
        addFacilityCreativeItem(general, "office_top");
        addFacilityCreativeItem(general, "skyroom_bot_1");
        addFacilityCreativeItem(general, "skyroom_bot_2");
        addFacilityCreativeItem(general, "skyroom_mid");
        addFacilityCreativeItem(general, "skyroom_top_alt");
        addFacilityCreativeItem(general, "skyroom_block");
        addFacilityCreativeItem(general, "security_bot");
        addFacilityCreativeItem(general, "security_mid");
        addFacilityCreativeItem(general, "security_top");
        sections.add(section("generaltab", general));

        List<ItemStack> coreRoom = new ArrayList<>();
        addFacilityCreativeItem(coreRoom, "core_room_sign");
        sections.add(section("coreroomtab", coreRoom));
        sections.add(section("l0tab", List.of()));

        List<ItemStack> sublevel1 = new ArrayList<>();
        addUBlockCreativeItem(sublevel1, "sl_1_floor_2");
        addUBlockCreativeItem(sublevel1, "sl_1_floor_1");
        addUBlockCreativeItem(sublevel1, "sl1_wall_bot");
        addUBlockCreativeItem(sublevel1, "sl1_wall_mid");
        addUBlockCreativeItem(sublevel1, "sl_1_wall_top");
        addUBlockCreativeItem(sublevel1, "sl1_ceiling");
        addUBlockCreativeItem(sublevel1, "sl1_ceiling_alt");
        addUBlockCreativeItem(sublevel1, "sl1_lamp");
        addUBlockCreativeItem(sublevel1, "sl1_flickering_lamp");
        addUBlockCreativeItem(sublevel1, "sl_1_floor_detail_small");
        addUBlockCreativeItem(sublevel1, "sl_1_floor_detail_big");
        addUBlockCreativeItem(sublevel1, "sl_1_wall_detail_1_bot");
        addUBlockCreativeItem(sublevel1, "sl_1_wall_detail_2");
        sections.add(section("sl1tab", sublevel1));

        List<ItemStack> sublevel2 = new ArrayList<>();
        addUBlockCreativeItem(sublevel2, "sl_2_floor");
        addUBlockCreativeItem(sublevel2, "sl_2_wall_bot");
        addUBlockCreativeItem(sublevel2, "sl_2_wall_mid");
        addUBlockCreativeItem(sublevel2, "sl_2_wall_top");
        sections.add(section("sl2tab", sublevel2));

        sections.add(section("sl3tab", List.of()));
        sections.add(section("sl4tab", List.of()));
        sections.add(section("sl5tab", List.of()));

        return List.copyOf(sections);
    }

    public static List<ItemStack> creativeItemsInDisplayOrder() {
        List<ItemStack> ordered = new ArrayList<>();
        creativeSections().forEach(section -> section.items().forEach(stack ->
                addUnique(ordered, stack.copy())));
        return ordered;
    }

    /**
     * Adds one empty nine-slot row before every category. The client paints the
     * corresponding full-width header over those rows and follows vanilla scroll.
     */
    public static List<ItemStack> creativeTabDisplayStacks() {
        List<ItemStack> display = new ArrayList<>();
        for (CreativeSection section : creativeSections()) {
            for (int i = 0; i < 9; i++) display.add(ItemStack.EMPTY);
            section.items().forEach(stack -> display.add(stack.copy()));
            while (display.size() % 9 != 0) display.add(ItemStack.EMPTY);
        }
        return display;
    }

    public static List<ItemStack> creativeTabIconStacks() {
        return creativeItemsInDisplayOrder().stream().map(ItemStack::copy).toList();
    }

    private static CreativeSection section(String sprite,
            List<ItemStack> items) {
        return new CreativeSection(
                new ResourceLocation(MODID, "textures/gui/facility_sections/" + sprite + ".png"),
                List.copyOf(items));
    }

    private static void addFacilityCreativeItem(List<ItemStack> ordered,
            String path) {
        RegistryObject<Item> item = ITEMS_BY_PATH.get(path);
        if (item != null) addUnique(ordered, new ItemStack(item.get()));
    }

    private static void addUBlockCreativeItem(List<ItemStack> ordered,
            String path) {
        RegistryObject<Item> item = UBlocksModule.itemByPath(path);
        if (item != null) addUnique(ordered, new ItemStack(item.get()));
    }

    private static void addExternalCreativeItem(List<ItemStack> ordered,
            Item item) {
        addUnique(ordered, new ItemStack(item));
    }

    private static void addUnique(List<ItemStack> ordered, ItemStack stack) {
        boolean alreadyPresent = ordered.stream()
                .anyMatch(existing -> ItemStack.isSameItemSameTags(existing, stack));
        if (!alreadyPresent) ordered.add(stack);
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

    private static RegistryObject<Block> registerSign(String path,
            FacilitySignBlock.SignType type) {
        RegistryObject<Block> block = BLOCKS.register(path,
                () -> new FacilitySignBlock(type));
        RegistryObject<Item> item = ITEMS.register(path,
                () -> new FacilitySignBlockItem(block.get(),
                        new Item.Properties(), type));
        BLOCKS_BY_PATH.put(path, block);
        ITEMS_BY_PATH.put(path, item);
        CREATIVE_ITEMS.add(item);
        return block;
    }

    private static RegistryObject<Block> registerWetFloor() {
        String path = "wet_floor";
        RegistryObject<Block> block = BLOCKS.register(path, WetFloorBlock::new);
        RegistryObject<Item> item = ITEMS.register(path,
                () -> new WetFloorBlockItem(block.get(), new Item.Properties()));
        BLOCKS_BY_PATH.put(path, block);
        ITEMS_BY_PATH.put(path, item);
        CREATIVE_ITEMS.add(item);
        return block;
    }

    private static RegistryObject<Block> registerBlock(String path,
            Supplier<? extends Block> factory, boolean publicItem) {
        RegistryObject<Block> block = BLOCKS.register(path, factory);
        RegistryObject<Item> item = ITEMS.register(path,
                () -> isDecorativeProp(path)
                        ? new DecorativePropBlockItem(block.get(), new Item.Properties())
                        : new BlockItem(block.get(), new Item.Properties()));
        BLOCKS_BY_PATH.put(path, block);
        ITEMS_BY_PATH.put(path, item);
        if (publicItem) CREATIVE_ITEMS.add(item);
        return block;
    }

    private static boolean isDecorativeProp(String path) {
        return "heater".equals(path)
                || "emergency_button".equals(path)
                || "fire_extinguisher".equals(path)
                || "water_faucet".equals(path)
                || "trashbin".equals(path);
    }

    private static final class DecorativePropBlockItem extends BlockItem {
        private DecorativePropBlockItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.scp_additions.decorative_prop")
                    .withStyle(ChatFormatting.GRAY));
            super.appendHoverText(stack, level, tooltip, flag);
        }
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

    private abstract static class WallMountedWaterloggedPropBlock
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

    private static final class EmergencyButtonBlock extends WallMountedWaterloggedPropBlock {
        private EmergencyButtonBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F));
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return horizontalShape(state.getValue(FACING),
                    box(5.79, 1.5, 13, 10.21, 7.61, 16));
        }
    }

    private static final class FireExtinguisherBlock extends WallMountedWaterloggedPropBlock {
        private FireExtinguisherBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F));
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            VoxelShape north = Shapes.or(
                    box(4.4, 0, 10.7, 11.6, 16, 16),
                    box(6.4, 1.25, 10.7, 9.6, 11.35, 16.4));
            return horizontalShape(state.getValue(FACING), north);
        }
    }

    private static final class WaterFaucetBlock extends WallMountedWaterloggedPropBlock {
        private static final VoxelShape NORTH_SHAPE =
                box(4.0, 2.0, 12.0, 12.0, 8.8, 16.0);

        private WaterFaucetBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F));
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState state = super.getStateForPlacement(context);
            return state != null && state.canSurvive(
                    context.getLevel(), context.getClickedPos()) ? state : null;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level,
                BlockPos pos) {
            Direction facing = state.getValue(FACING);
            BlockPos supportPos = pos.relative(facing.getOpposite());
            return level.getBlockState(supportPos).isFaceSturdy(
                    level, supportPos, facing);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighbor, LevelAccessor level, BlockPos pos,
                BlockPos neighborPos) {
            if (direction == state.getValue(FACING).getOpposite()
                    && !state.canSurvive(level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
            return super.updateShape(state, direction, neighbor,
                    level, pos, neighborPos);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return horizontalShape(state.getValue(FACING), NORTH_SHAPE);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state,
                BlockGetter level, BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }
    }

    private static VoxelShape horizontalShape(Direction facing, VoxelShape north) {
        if (facing == Direction.NORTH) return north;
        return rotateShape(Direction.NORTH, facing, north);
    }

    private static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = { shape, Shapes.empty() };
        int rotations = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;
        for (int i = 0; i < rotations; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    buffer[1] = Shapes.or(buffer[1],
                            Shapes.box(1 - maxZ, minY, minX,
                                    1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }
        return buffer[0];
    }

    private static final class SignSupportBlock extends HorizontalWaterloggedPropBlock {
        private SignSupportBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.GLASS)
                    .strength(1.0F, 10.0F).randomTicks());
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            if (clickedFace.getAxis() == Direction.Axis.Y) return null;
            if (!FacilityLargePropStructure.canPlace(context.getLevel(),
                    context.getClickedPos(),
                    FacilityLargePropStructure.Kind.SIGN_SUPPORT,
                    clickedFace)) {
                return null;
            }
            boolean waterlogged = context.getLevel().getFluidState(
                    context.getClickedPos()).getType() == Fluids.WATER;
            return defaultBlockState().setValue(FACING, clickedFace)
                    .setValue(WATERLOGGED, waterlogged);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level,
                BlockPos pos, CollisionContext context) {
            return FacilityLargePropStructure.controllerShape(
                    FacilityLargePropStructure.Kind.SIGN_SUPPORT,
                    state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state,
                BlockGetter level, BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level.isClientSide || oldState.getBlock() == this) return;
            Direction facing = state.getValue(FACING);
            if (!FacilityLargePropStructure.placeParts(level, pos,
                    FacilityLargePropStructure.Kind.SIGN_SUPPORT, facing)) {
                level.destroyBlock(pos, true);
                return;
            }
            level.scheduleTick(pos, this, 1);
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) {
            FacilityLargePropStructure.ensureParts(level, pos,
                    FacilityLargePropStructure.Kind.SIGN_SUPPORT,
                    state.getValue(FACING));
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level,
                BlockPos pos, RandomSource random) {
            FacilityLargePropStructure.ensureParts(level, pos,
                    FacilityLargePropStructure.Kind.SIGN_SUPPORT,
                    state.getValue(FACING));
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (state.getBlock() != newState.getBlock()
                    && !level.isClientSide) {
                FacilityLargePropStructure.removeParts(level, pos,
                        FacilityLargePropStructure.Kind.SIGN_SUPPORT,
                        state.getValue(FACING));
            }
            super.onRemove(state, level, pos, newState, moving);
        }
    }

    private static final class TvBlock extends DirectionalBlock {
        private TvBlock() {
            super(BlockBehaviour.Properties.of().sound(SoundType.METAL)
                    .strength(1.0F, 10.0F).noOcclusion().randomTicks()
                    .isRedstoneConductor((state, level, pos) -> false));
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction clickedFace = context.getClickedFace();
            if (!FacilityLargePropStructure.canPlace(context.getLevel(),
                    context.getClickedPos(),
                    FacilityLargePropStructure.Kind.TV, clickedFace)) {
                return null;
            }
            return defaultBlockState().setValue(FACING, clickedFace);
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
            return FacilityLargePropStructure.controllerShape(
                    FacilityLargePropStructure.Kind.TV,
                    state.getValue(FACING));
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state,
                BlockGetter level, BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean moving) {
            super.onPlace(state, level, pos, oldState, moving);
            if (level.isClientSide || oldState.getBlock() == this) return;
            Direction facing = state.getValue(FACING);
            if (!FacilityLargePropStructure.placeParts(level, pos,
                    FacilityLargePropStructure.Kind.TV, facing)) {
                level.destroyBlock(pos, true);
                return;
            }
            level.scheduleTick(pos, this, 1);
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) {
            FacilityLargePropStructure.ensureParts(level, pos,
                    FacilityLargePropStructure.Kind.TV,
                    state.getValue(FACING));
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level,
                BlockPos pos, RandomSource random) {
            FacilityLargePropStructure.ensureParts(level, pos,
                    FacilityLargePropStructure.Kind.TV,
                    state.getValue(FACING));
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean moving) {
            if (state.getBlock() != newState.getBlock()
                    && !level.isClientSide) {
                FacilityLargePropStructure.removeParts(level, pos,
                        FacilityLargePropStructure.Kind.TV,
                        state.getValue(FACING));
            }
            super.onRemove(state, level, pos, newState, moving);
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
                    ? box(5.5, 0, 4.25, 10.5, 15.61, 11.75)
                    : box(4.25, 0, 5.5, 11.75, 15.61, 10.5);
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

        private boolean usesOpenVisualState() {
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
                    ? FacilityDoorShapes.shape(familyId, usesOpenVisualState(), facing)
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
