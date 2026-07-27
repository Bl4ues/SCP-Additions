from pathlib import Path
import binascii
import json
import struct
import zlib

ROOT = Path('.')


def replace_once(path: str, old: str, new: str) -> None:
    file = ROOT / path
    text = file.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'Expected exactly one match in {path}, found {count}')
    file.write_text(text.replace(old, new, 1), encoding='utf-8')


# 3.1.0 version and changelog heading.
replace_once('build.gradle', "version = '3.0.8'", "version = '3.1.0'")

changelog_path = ROOT / 'CHANGELOG.md'
changelog = changelog_path.read_text(encoding='utf-8')
old_heading = '# SCP Additions 3.0.8 — In Development'
new_heading = '# SCP Additions 3.1.0 — In Development'
if changelog.count(old_heading) != 1:
    raise SystemExit('Could not locate the 3.0.8 changelog heading')
changelog = changelog.replace(old_heading, new_heading, 1)
insert_marker = new_heading + '\n\n'
section = '''## Creative inventory organization

- Renamed the former **SCP Unity Blocks** creative tab to **SCP Facility Blocks**;
- Organized facility content under colored **Functional**, **Props**, **General**, **LCZ - Sublevel 1**, and **LCZ - Sublevel 2** dividers;
- Moved facility construction controls out of the general SCP Additions tab and into the Functional section;
- Made every SCP Additions creative-tab icon cycle through the visible items in its own tab.

'''
if changelog.count(insert_marker) != 1:
    raise SystemExit('Could not locate changelog insertion point')
changelog_path.write_text(changelog.replace(insert_marker, insert_marker + section, 1), encoding='utf-8')


# Hidden animated icon item and client renderer.
icon_item_path = ROOT / 'src/main/java/net/mcreator/scpadditions/init/CyclingCreativeTabIconItem.java'
icon_item_path.write_text('''package net.mcreator.scpadditions.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CyclingCreativeTabIconItem extends Item {
    private static final long DISPLAY_TIME_MILLIS = 900L;

    private final Supplier<List<ItemStack>> stackSupplier;
    private volatile List<ItemStack> cachedStacks;

    public CyclingCreativeTabIconItem(Supplier<List<ItemStack>> stackSupplier) {
        super(new Item.Properties().stacksTo(1));
        this.stackSupplier = Objects.requireNonNull(stackSupplier);
    }

    public ItemStack currentDisplayStack() {
        List<ItemStack> stacks = cachedStacks;
        if (stacks == null) {
            stacks = stackSupplier.get().stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .map(ItemStack::copy)
                    .toList();
            cachedStacks = stacks;
        }
        if (stacks.isEmpty()) return ItemStack.EMPTY;

        int index = (int) ((System.currentTimeMillis() / DISPLAY_TIME_MILLIS)
                % stacks.size());
        return stacks.get(index);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CyclingCreativeTabIconRenderer renderer;

            @Override
            public CyclingCreativeTabIconRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new CyclingCreativeTabIconRenderer(
                            CyclingCreativeTabIconItem.this);
                }
                return renderer;
            }
        });
    }
}
''', encoding='utf-8')

renderer_path = ROOT / 'src/main/java/net/mcreator/scpadditions/init/CyclingCreativeTabIconRenderer.java'
renderer_path.write_text('''package net.mcreator.scpadditions.init;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CyclingCreativeTabIconRenderer
        extends BlockEntityWithoutLevelRenderer {
    private final CyclingCreativeTabIconItem iconItem;

    public CyclingCreativeTabIconRenderer(CyclingCreativeTabIconItem iconItem) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
        this.iconItem = iconItem;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        ItemStack display = iconItem.currentDisplayStack();
        if (display.isEmpty() || display.is(iconItem)) return;

        Minecraft.getInstance().getItemRenderer().renderStatic(display,
                context, packedLight, packedOverlay, poseStack, buffer,
                null, 0);
    }
}
''', encoding='utf-8')


# Rewrite the small creative-tab registry around shared stack lists.
tabs_path = ROOT / 'src/main/java/net/mcreator/scpadditions/init/ScpAdditionsModTabs.java'
tabs_path.write_text('''package net.mcreator.scpadditions.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.FacilityModule;
import net.mcreator.scpadditions.scp012.Scp012Module;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScpAdditionsModTabs {
    public static final DeferredRegister<Item> ICON_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS,
                    ScpAdditionsMod.MODID);
    public static final DeferredRegister<CreativeModeTab> REGISTRY =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
                    ScpAdditionsMod.MODID);

    public static final RegistryObject<Item> SCP_ADDITIONS_TAB_ICON =
            ICON_ITEMS.register("scp_additions_tab_icon",
                    () -> new CyclingCreativeTabIconItem(
                            ScpAdditionsModTabs::scpAdditionsStacks));
    public static final RegistryObject<Item> SCPS_TAB_ICON =
            ICON_ITEMS.register("scps_tab_icon",
                    () -> new CyclingCreativeTabIconItem(
                            ScpAdditionsModTabs::scpStacks));
    public static final RegistryObject<Item> FACILITY_BLOCKS_TAB_ICON =
            ICON_ITEMS.register("facility_blocks_tab_icon",
                    () -> new CyclingCreativeTabIconItem(
                            FacilityModule::creativeTabIconStacks));

    public static final RegistryObject<CreativeModeTab> SCP_ADDITIONS =
            REGISTRY.register("scp_additions", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_additions.scp_additions"))
                            .icon(() -> new ItemStack(
                                    SCP_ADDITIONS_TAB_ICON.get()))
                            .displayItems((parameters, output) ->
                                    scpAdditionsStacks().forEach(output::accept))
                            .withSearchBar()
                            .build());

    public static final RegistryObject<CreativeModeTab> SC_PADDITIONS_SC_PS =
            REGISTRY.register("sc_padditions_sc_ps", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_additions.sc_padditions_sc_ps"))
                            .icon(() -> new ItemStack(SCPS_TAB_ICON.get()))
                            .displayItems((parameters, output) ->
                                    scpStacks().forEach(output::accept))
                            .withSearchBar()
                            .build());

    private static List<ItemStack> scpAdditionsStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ScpAdditionsModItems.SECURITY_CREDENTIALS.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_1_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_2_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_3_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_4_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_5_KEYCARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.LEVEL_6_KEYCARD.get()));
        stacks.add(new ItemStack(UnifiedReaderItems.SCREWDRIVER.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.HAZMAT_SUIT.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.PLAYING_CARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.CREDIT_CARD.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.PIECES_OF_PAPER.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.COIN.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.EMPTY_CUP.get()));
        return stacks;
    }

    private static List<ItemStack> scpStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(Scp012Module.SCP_012_ITEM.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_079ON.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_106_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_131_A_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_131_B_SPAWN_EGG.get()));
        stacks.add(new ItemStack(Scp131Items.SCP_173_SPAWN_EGG.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_294.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_330.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_426.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.SCP_572.get()));
        stacks.add(new ItemStack(Scp714Items.SCP_714.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_902_CLOSED.get()));
        stacks.add(new ItemStack(ScpAdditionsModItems.SCP_914_ASSEMBLY_KIT.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914BLOCK.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914CLOCKWORKS.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914BODY.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914DIAL_1TO_1.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_KEY_WIND.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_INTAKE.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_OUTPUT.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_INTAKE_DOOR.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_914_OUTPUT_DOOR.get()));
        stacks.add(new ItemStack(ScpAdditionsModBlocks.SCP_1176.get()));
        return stacks;
    }

    @SubscribeEvent
    public static void buildTabContentsVanilla(
            BuildCreativeModeTabContentsEvent tabData) {
        if (tabData.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            tabData.accept(ScpAdditionsModItems.SCP_330_RED_CANDY.get());
            tabData.accept(ScpAdditionsModItems.SCP_330_GREEN_CANDY.get());
            tabData.accept(ScpAdditionsModItems.SCP_330_YELLOW_CANDY.get());
            tabData.accept(ScpAdditionsModItems.SCP_330_BLUE_CANDY.get());
            tabData.accept(ScpAdditionsModItems.SCP_1176HONEY.get());
        }
    }
}
''', encoding='utf-8')


# Register hidden icon items before creative tabs.
replace_once(
    'src/main/java/net/mcreator/scpadditions/ScpAdditionsMod.java',
    '        ScpAdditionsModTabs.REGISTRY.register(bus);\n',
    '        ScpAdditionsModTabs.ICON_ITEMS.register(bus);\n'
    '        ScpAdditionsModTabs.REGISTRY.register(bus);\n')


# FacilityModule imports.
replace_once(
    'src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java',
    'import net.minecraft.network.chat.Component;\n',
    'import net.minecraft.network.chat.Component;\n'
    'import net.minecraft.network.chat.TextColor;\n')
replace_once(
    'src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java',
    'import net.mcreator.scpadditions.ScpAdditionsMod;\n',
    'import net.mcreator.scpadditions.ScpAdditionsMod;\n'
    'import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;\n'
    'import net.mcreator.scpadditions.init.ScpAdditionsModTabs;\n'
    'import net.mcreator.scpadditions.init.UnifiedReaderItems;\n')

facility_path = ROOT / 'src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java'
facility = facility_path.read_text(encoding='utf-8')

# Divider constants after the registry maps.
constants_marker = '    private static final Map<String, DoorFamily> DOOR_FAMILIES = new LinkedHashMap<>();\n'
constants = '''    private static final Map<String, DoorFamily> DOOR_FAMILIES = new LinkedHashMap<>();

    private static final String DIVIDER_COLOR_TAG = "FacilityDividerColor";
    private static final int FUNCTIONAL_DIVIDER_COLOR = 0xD6D6D6;
    private static final int PROPS_DIVIDER_COLOR = 0x8A8A8A;
    private static final int GENERAL_DIVIDER_COLOR = 0x444444;
    private static final int SL1_DIVIDER_COLOR = 0x0987BC;
    private static final int SL2_DIVIDER_COLOR = 0xFFD306;
'''
if facility.count(constants_marker) != 1:
    raise SystemExit('Could not locate FacilityModule registry maps')
facility = facility.replace(constants_marker, constants, 1)

# Replace the old tab registration with one tab using animated icon and dividers.
tab_start_marker = ('    public static final RegistryObject<CreativeModeTab> '
                    'SCP_UNITY_BLOCKS = TABS.register("scp_unity_blocks", () ->')
tab_start = facility.find(tab_start_marker)
tab_end = facility.find('    private FacilityModule() {', tab_start)
if tab_start < 0 or tab_end < 0:
    raise SystemExit('Could not locate old facility creative tab')
new_tab = '''    public static final RegistryObject<Item> FACILITY_TAB_DIVIDER =
            ITEMS.register("facility_tab_divider",
                    () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> SCP_FACILITY_BLOCKS =
            TABS.register("scp_unity_blocks", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable(
                                    "item_group.scp_additions.scp_facility_blocks"))
                            .icon(() -> new ItemStack(
                                    ScpAdditionsModTabs.FACILITY_BLOCKS_TAB_ICON.get()))
                            .displayItems((parameters, output) -> {
                                for (ItemStack stack : creativeItemsInDisplayOrder()) {
                                    if (stack.is(FACILITY_TAB_DIVIDER.get())) {
                                        output.accept(stack,
                                                CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                                    } else {
                                        output.accept(stack);
                                    }
                                }
                            })
                            .withSearchBar()
                            .build());

'''
facility = facility[:tab_start] + new_tab + facility[tab_end:]

# Replace old flat order helpers with categorized stacks.
order_start = facility.find('    /**\n     * Stable, curated order for the public facility tab.')
order_end = facility.find('    private static RegistryObject<SoundEvent> sound(', order_start)
if order_start < 0 or order_end < 0:
    raise SystemExit('Could not locate old facility creative-order helpers')
new_order = '''    /**
     * Curated category order for the public facility tab. Fake divider items are
     * limited to the parent tab so they do not pollute creative search results.
     */
    private static List<ItemStack> creativeItemsInDisplayOrder() {
        List<ItemStack> ordered = new ArrayList<>();

        addDivider(ordered,
                "creative_tab.scp_additions.facility.functional",
                FUNCTIONAL_DIVIDER_COLOR);
        addExternalCreativeItem(ordered,
                ScpAdditionsModBlocks.TESLA_GATE.get().asItem());
        addExternalCreativeItem(ordered,
                ScpAdditionsModBlocks.TESLA_TERMINAL_OFF.get().asItem());
        addExternalCreativeItem(ordered,
                ScpAdditionsModBlocks.TESLA_TERMINAL_BLOCK.get().asItem());
        addFacilityCreativeItem(ordered, "button_closed");
        addFacilityCreativeItem(ordered, "button_locked");
        addExternalCreativeItem(ordered, UnifiedReaderItems.KEYCARD_READER.get());
        addExternalCreativeItem(ordered,
                ScpAdditionsModBlocks.DECON_OPEN.get().asItem());
        addExternalCreativeItem(ordered,
                ScpAdditionsModBlocks.SCP_079_SYSTEM_CONTROL.get().asItem());
        addExternalCreativeItem(ordered,
                ScpAdditionsModBlocks.SCP_079CONTROLOFF.get().asItem());
        addFacilityCreativeItem(ordered, "default_door");
        addFacilityCreativeItem(ordered, "yellow_closed");
        addFacilityCreativeItem(ordered, "black_closed");
        addFacilityCreativeItem(ordered, "normal_door");
        addFacilityCreativeItem(ordered, "left_log_door");
        addFacilityCreativeItem(ordered, "right_log_door");
        addFacilityCreativeItem(ordered, "office_door");
        addFacilityCreativeItem(ordered, "bath_door");
        addFacilityCreativeItem(ordered, "ws_dclosed");
        addUBlockCreativeItem(ordered, "sl1_lamp");
        addUBlockCreativeItem(ordered, "sl1_flickering_lamp");

        addDivider(ordered,
                "creative_tab.scp_additions.facility.props",
                PROPS_DIVIDER_COLOR);
        addFacilityCreativeItem(ordered, "walllight");
        addFacilityCreativeItem(ordered, "heater");
        addFacilityCreativeItem(ordered, "emergency_button");
        addFacilityCreativeItem(ordered, "fire_extinguisher");
        addFacilityCreativeItem(ordered, "sign_support");
        addFacilityCreativeItem(ordered, "core_room_sign");
        addFacilityCreativeItem(ordered, "door_sign");
        addFacilityCreativeItem(ordered, "tv");
        addFacilityCreativeItem(ordered, "trashbin");
        addUBlockCreativeItem(ordered, "vent_open");

        addDivider(ordered,
                "creative_tab.scp_additions.facility.general",
                GENERAL_DIVIDER_COLOR);
        addFacilityCreativeItem(ordered, "tesla_bottom");
        addFacilityCreativeItem(ordered, "tesla_mid_1");
        addFacilityCreativeItem(ordered, "tesla_mid_2");
        addFacilityCreativeItem(ordered, "tesla_bottom_alt");
        addFacilityCreativeItem(ordered, "tesla_top_alt");
        addFacilityCreativeItem(ordered, "archival_bottom");
        addFacilityCreativeItem(ordered, "archival_mid");
        addFacilityCreativeItem(ordered, "archival_top");
        addFacilityCreativeItem(ordered, "archival_bot_1");
        addFacilityCreativeItem(ordered, "archival_mid_2");
        addFacilityCreativeItem(ordered, "office_bottom");
        addFacilityCreativeItem(ordered, "office_mid");
        addFacilityCreativeItem(ordered, "office_top");
        addFacilityCreativeItem(ordered, "skyroom_bot_1");
        addFacilityCreativeItem(ordered, "skyroom_bot_2");
        addFacilityCreativeItem(ordered, "skyroom_mid");
        addFacilityCreativeItem(ordered, "skyroom_top_alt");
        addFacilityCreativeItem(ordered, "skyroom_block");
        addFacilityCreativeItem(ordered, "security_bot");
        addFacilityCreativeItem(ordered, "security_mid");
        addFacilityCreativeItem(ordered, "security_top");

        addDivider(ordered,
                "creative_tab.scp_additions.facility.lcz_sublevel_1",
                SL1_DIVIDER_COLOR);
        addUBlockCreativeItem(ordered, "sl_1_floor_2");
        addUBlockCreativeItem(ordered, "sl_1_floor_1");
        addUBlockCreativeItem(ordered, "sl1_wall_bot");
        addUBlockCreativeItem(ordered, "sl1_wall_mid");
        addUBlockCreativeItem(ordered, "sl_1_wall_top");
        addUBlockCreativeItem(ordered, "sl1_ceiling");
        addUBlockCreativeItem(ordered, "sl1_ceiling_alt");
        addUBlockCreativeItem(ordered, "sl_1_floor_detail_small");
        addUBlockCreativeItem(ordered, "sl_1_floor_detail_big");
        addUBlockCreativeItem(ordered, "sl_1_wall_detail_1_bot");
        addUBlockCreativeItem(ordered, "sl_1_wall_detail_2");

        addDivider(ordered,
                "creative_tab.scp_additions.facility.lcz_sublevel_2",
                SL2_DIVIDER_COLOR);
        addUBlockCreativeItem(ordered, "sl_2_floor");
        addUBlockCreativeItem(ordered, "sl_2_wall_bot");
        addUBlockCreativeItem(ordered, "sl_2_wall_mid");
        addUBlockCreativeItem(ordered, "sl_2_wall_top");

        return ordered;
    }

    public static List<ItemStack> creativeTabIconStacks() {
        return creativeItemsInDisplayOrder().stream()
                .filter(stack -> !stack.is(FACILITY_TAB_DIVIDER.get()))
                .map(ItemStack::copy)
                .toList();
    }

    public static int dividerColor(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(DIVIDER_COLOR_TAG)) {
            return stack.getTag().getInt(DIVIDER_COLOR_TAG);
        }
        return 0xFFFFFF;
    }

    private static void addDivider(List<ItemStack> ordered,
            String translationKey, int color) {
        ItemStack divider = new ItemStack(FACILITY_TAB_DIVIDER.get());
        divider.getOrCreateTag().putInt(DIVIDER_COLOR_TAG, color);
        divider.setHoverName(Component.translatable(translationKey)
                .withStyle(style -> style
                        .withColor(TextColor.fromRgb(color))
                        .withBold(true)
                        .withItalic(false)));
        ordered.add(divider);
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

'''
facility = facility[:order_start] + new_order + facility[order_end:]
facility_path.write_text(facility, encoding='utf-8')


# Tint fake divider textures with their NBT category color.
ublocks_client_path = ROOT / 'src/main/java/net/mcreator/scpadditions/facility/UBlocksClientEvents.java'
ublocks_client = ublocks_client_path.read_text(encoding='utf-8')
old_item_colors = '''    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) ->
                        tintIndex == 0 ? FLOOR_DECAL_TINT : 0xFFFFFF,
                UBlocksModule.SL_1_FLOOR_DETAIL_SMALL.get().asItem(),
                UBlocksModule.SL_1_FLOOR_DETAIL_BIG.get().asItem());
    }
'''
new_item_colors = '''    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) ->
                        tintIndex == 0 ? FLOOR_DECAL_TINT : 0xFFFFFF,
                UBlocksModule.SL_1_FLOOR_DETAIL_SMALL.get().asItem(),
                UBlocksModule.SL_1_FLOOR_DETAIL_BIG.get().asItem());
        event.register((stack, tintIndex) ->
                        tintIndex == 0
                                ? FacilityModule.dividerColor(stack)
                                : 0xFFFFFF,
                FacilityModule.FACILITY_TAB_DIVIDER.get());
    }
'''
if ublocks_client.count(old_item_colors) != 1:
    raise SystemExit('Could not locate UBlocks item-color registration')
ublocks_client_path.write_text(
    ublocks_client.replace(old_item_colors, new_item_colors, 1),
    encoding='utf-8')


# English names and section labels.
lang_path = ROOT / 'src/main/resources/assets/scp_additions/lang/en_us_3_0.json'
lang = json.loads(lang_path.read_text(encoding='utf-8'))
lang.pop('item_group.scp_additions.scp_unity_blocks', None)
lang.update({
    'item_group.scp_additions.scp_facility_blocks': 'SCP Facility Blocks',
    'creative_tab.scp_additions.facility.functional': 'Functional',
    'creative_tab.scp_additions.facility.props': 'Props',
    'creative_tab.scp_additions.facility.general': 'General',
    'creative_tab.scp_additions.facility.lcz_sublevel_1': 'LCZ - Sublevel 1',
    'creative_tab.scp_additions.facility.lcz_sublevel_2': 'LCZ - Sublevel 2',
    'item.scp_additions.facility_tab_divider': 'Facility Category Divider',
    'item.scp_additions.scp_additions_tab_icon': 'SCP Additions Tab Icon',
    'item.scp_additions.scps_tab_icon': 'SCPs Tab Icon',
    'item.scp_additions.facility_blocks_tab_icon': 'Facility Blocks Tab Icon'
})
lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


# Custom-rendered icon item models and tinted divider resource.
model_dir = ROOT / 'src/main/resources/assets/scp_additions/models/item'
model_dir.mkdir(parents=True, exist_ok=True)
for name in ('scp_additions_tab_icon', 'scps_tab_icon', 'facility_blocks_tab_icon'):
    (model_dir / f'{name}.json').write_text(
        '{\n  "parent": "builtin/entity"\n}\n', encoding='utf-8')
(model_dir / 'facility_tab_divider.json').write_text(
    '{\n  "parent": "minecraft:item/generated",\n'
    '  "textures": {\n'
    '    "layer0": "scp_additions:item/facility_tab_divider"\n'
    '  }\n}\n', encoding='utf-8')


def png_chunk(kind: bytes, data: bytes) -> bytes:
    payload = kind + data
    return (struct.pack('>I', len(data)) + payload
            + struct.pack('>I', binascii.crc32(payload) & 0xffffffff))


width = height = 16
rows = []
for y in range(height):
    row = bytearray()
    for x in range(width):
        visible = (3 <= y <= 12 and (x in (2, 13) or 6 <= y <= 9))
        row.extend((255, 255, 255, 255 if visible else 0))
    rows.append(b'\x00' + bytes(row))
png = (b'\x89PNG\r\n\x1a\n'
       + png_chunk(b'IHDR', struct.pack('>IIBBBBB',
               width, height, 8, 6, 0, 0, 0))
       + png_chunk(b'IDAT', zlib.compress(b''.join(rows), 9))
       + png_chunk(b'IEND', b''))
texture_path = ROOT / 'src/main/resources/assets/scp_additions/textures/item/facility_tab_divider.png'
texture_path.parent.mkdir(parents=True, exist_ok=True)
texture_path.write_bytes(png)


# Static validation before Gradle compilation.
for file in [lang_path, *(model_dir / f'{name}.json' for name in (
        'scp_additions_tab_icon', 'scps_tab_icon',
        'facility_blocks_tab_icon', 'facility_tab_divider'))]:
    json.loads(file.read_text(encoding='utf-8'))

assert "version = '3.1.0'" in (ROOT / 'build.gradle').read_text(encoding='utf-8')
final_facility = facility_path.read_text(encoding='utf-8')
assert 'SCP_UNITY_BLOCKS' not in final_facility
assert 'SCP_FACILITY_BLOCKS' in final_facility
assert 'FUNCTIONAL_DIVIDER_COLOR = 0xD6D6D6' in final_facility
assert 'SL1_DIVIDER_COLOR = 0x0987BC' in final_facility
assert 'SL2_DIVIDER_COLOR = 0xFFD306' in final_facility
