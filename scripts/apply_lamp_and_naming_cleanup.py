from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require_replace(path: str, old: str, new: str, count: int = -1) -> None:
    target = ROOT / path
    text = target.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"Expected text missing in {path}: {old[:100]!r}")
    target.write_text(text.replace(old, new, count), encoding="utf-8")


# Use one aggregate positional hum, retargeted to the nearest powered lamp.
(ROOT / "src/main/java/net/mcreator/scpadditions/client/CeilingLampAudioClient.java").write_text(r'''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Maintains one positional electrical hum for the nearest powered ceiling lamp. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CeilingLampAudioClient {
    private static final int DISCOVERY_INTERVAL_TICKS = 10;
    private static final int HORIZONTAL_DISCOVERY_RADIUS = 16;
    private static final int VERTICAL_DISCOVERY_RADIUS = 8;

    private static CeilingLampLoopSound activeLoop;
    private static int discoveryTicks;

    private CeilingLampAudioClient() {
    }

    public static void ensureLoop(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)
                || !CeilingLampLoopSound.shouldPlayFor(
                clientLevel.getBlockState(pos))) {
            return;
        }
        startOrRetarget(clientLevel, pos);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopLoop();
            discoveryTicks = 0;
            return;
        }

        if (activeLoop != null && activeLoop.isFinished()) activeLoop = null;

        discoveryTicks++;
        if (discoveryTicks < DISCOVERY_INTERVAL_TICKS) return;
        discoveryTicks = 0;

        BlockPos nearest = findNearestPoweredLamp(minecraft.level,
                minecraft.player.blockPosition());
        if (nearest == null) stopLoop();
        else startOrRetarget(minecraft.level, nearest);
    }

    private static void startOrRetarget(ClientLevel level, BlockPos pos) {
        if (activeLoop != null && !activeLoop.isFinished()
                && activeLoop.level() == level) {
            activeLoop.retarget(pos);
            return;
        }
        stopLoop();
        activeLoop = new CeilingLampLoopSound(level, pos);
        Minecraft.getInstance().getSoundManager().play(activeLoop);
    }

    private static void stopLoop() {
        if (activeLoop != null) {
            activeLoop.finish();
            activeLoop = null;
        }
    }

    private static BlockPos findNearestPoweredLamp(ClientLevel level,
            BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int y = -VERTICAL_DISCOVERY_RADIUS;
                y <= VERTICAL_DISCOVERY_RADIUS; y++) {
            for (int x = -HORIZONTAL_DISCOVERY_RADIUS;
                    x <= HORIZONTAL_DISCOVERY_RADIUS; x++) {
                for (int z = -HORIZONTAL_DISCOVERY_RADIUS;
                        z <= HORIZONTAL_DISCOVERY_RADIUS; z++) {
                    cursor.set(center.getX() + x, center.getY() + y,
                            center.getZ() + z);
                    if (!level.hasChunkAt(cursor)
                            || !CeilingLampLoopSound.shouldPlayFor(
                            level.getBlockState(cursor))) continue;
                    double distance = cursor.distSqr(center);
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        return nearest;
    }
}
''', encoding="utf-8")

(ROOT / "src/main/java/net/mcreator/scpadditions/client/CeilingLampLoopSound.java").write_text(r'''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.mcreator.scpadditions.facility.UBlocksModule;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Positional electrical hum retargeted to the nearest powered ceiling lamp. */
public final class CeilingLampLoopSound extends AbstractTickableSoundInstance {
    private final ClientLevel level;
    private BlockPos pos;
    private boolean finished;

    public CeilingLampLoopSound(ClientLevel level, BlockPos pos) {
        super(ScpAdditionsModSounds.LAMP_LOOP.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.58F;
        this.pitch = 0.98F + RandomSource.create().nextFloat() * 0.04F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        retarget(pos);
    }

    ClientLevel level() {
        return level;
    }

    void retarget(BlockPos target) {
        this.pos = target.immutable();
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || minecraft.player == null) finish();
    }

    static boolean shouldPlayFor(BlockState state) {
        if (state.is(UBlocksModule.SL1_LAMP.get())) {
            return state.hasProperty(BlockStateProperties.LIT)
                    && state.getValue(BlockStateProperties.LIT);
        }
        if (state.is(UBlocksModule.SL1_FLICKERING_LAMP.get())) {
            return state.hasProperty(BlockStateProperties.POWERED)
                    && state.getValue(BlockStateProperties.POWERED);
        }
        return false;
    }

    public boolean isFinished() {
        return finished;
    }

    public void finish() {
        finished = true;
        stop();
    }
}
''', encoding="utf-8")

# Facility creative ordering.
path = ROOT / "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java"
text = path.read_text(encoding="utf-8")
old = '''        addFacilityCreativeItem(functional, "black_closed");
        addFacilityCreativeItem(functional, "normal_door");'''
new = '''        addFacilityCreativeItem(functional, "black_closed");
        addFacilityCreativeItem(functional, "sign_support");
        addFacilityCreativeItem(functional, "door_sign");
        addFacilityCreativeItem(functional, "normal_door");'''
if old not in text: raise RuntimeError("Functional door split changed")
text = text.replace(old, new)
old = '''        addFacilityCreativeItem(props, "sign_support");
        addFacilityCreativeItem(props, "core_room_sign");
        addFacilityCreativeItem(props, "door_sign");
'''
if old not in text: raise RuntimeError("Props signs changed")
text = text.replace(old, '')
old = '        sections.add(section("coreroomtab", List.of()));'
new = '''        List<ItemStack> coreRoom = new ArrayList<>();
        addFacilityCreativeItem(coreRoom, "core_room_sign");
        sections.add(section("coreroomtab", coreRoom));'''
if old not in text: raise RuntimeError("Core Room section changed")
path.write_text(text.replace(old, new), encoding="utf-8")

# Short internal labels without shrinking; full tab names remain hover labels.
path = ROOT / "src/main/java/net/mcreator/scpadditions/client/CreativeTabPresentation.java"
text = path.read_text(encoding="utf-8")
text = text.replace('    private static final int TITLE_MAX_WIDTH = 70;\n', '')
text = text.replace('renderCompactTitle(screen, event.getGuiGraphics(), selected.getDisplayName());',
                    'renderShortTitle(screen, event.getGuiGraphics(), shortTitle(selected));')
old = '''    private static void renderCompactTitle(CreativeModeInventoryScreen screen,
            GuiGraphics graphics, Component title) {
        Font font = Minecraft.getInstance().font;
        int width = Math.max(font.width(title), 1);
        float scale = Math.min(1.0F, TITLE_MAX_WIDTH / (float) width);

        graphics.pose().pushPose();
        graphics.pose().translate(screen.getGuiLeft() + 8,
                screen.getGuiTop() + 6, 300.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, title, 0, 0, 0x404040, false);
        graphics.pose().popPose();
    }
'''
new = '''    private static void renderShortTitle(CreativeModeInventoryScreen screen,
            GuiGraphics graphics, Component title) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, title, screen.getGuiLeft() + 8,
                screen.getGuiTop() + 6, 0x404040, false);
    }

    private static Component shortTitle(CreativeModeTab tab) {
        if (tab == ScpAdditionsModTabs.SC_PADDITIONS_SC_PS.get()) {
            return Component.translatable("item_group.scp_additions.short_scps");
        }
        if (tab == FacilityModule.SCP_FACILITY_BLOCKS.get()) {
            return Component.translatable("item_group.scp_additions.short_blocks");
        }
        return Component.translatable("item_group.scp_additions.short_items");
    }
'''
if old not in text: raise RuntimeError("Compact title method changed")
path.write_text(text.replace(old, new), encoding="utf-8")

# Zone tooltips on all SL1/SL2 construction blocks, before specific descriptions.
path = ROOT / "src/main/java/net/mcreator/scpadditions/facility/UBlocksModule.java"
text = path.read_text(encoding="utf-8")
old = '''        RegistryObject<Item> item = ITEMS.register(path, () -> isConnectedFloorPath(path)
                ? new ConnectedFloorBlockItem(block.get(), new Item.Properties())
                : isCeilingLampPath(path)
                ? new CeilingLampBlockItem(block.get(), new Item.Properties(), path)
                : isDecorativePropPath(path)
                ? new DecorativePropBlockItem(block.get(), new Item.Properties())
                : new BlockItem(block.get(), new Item.Properties()));'''
new = '''        RegistryObject<Item> item = ITEMS.register(path, () -> isConnectedFloorPath(path)
                ? new ConnectedFloorBlockItem(block.get(), new Item.Properties(), path)
                : isCeilingLampPath(path)
                ? new CeilingLampBlockItem(block.get(), new Item.Properties(), path)
                : isDecorativePropPath(path)
                ? new DecorativePropBlockItem(block.get(), new Item.Properties())
                : zoneTooltipKey(path) != null
                ? new FacilityZoneBlockItem(block.get(), new Item.Properties(), path)
                : new BlockItem(block.get(), new Item.Properties()));'''
if old not in text: raise RuntimeError("UBlocks item factory changed")
text = text.replace(old, new)
text = text.replace('''    private static final class CeilingLampBlockItem extends BlockItem {
        private final String tooltipKey;
''', '''    private static final class CeilingLampBlockItem extends BlockItem {
        private final String tooltipKey;
        private final String path;
''')
text = text.replace('''            super(block, properties);
            this.tooltipKey = "sl1_flickering_lamp".equals(path)''', '''            super(block, properties);
            this.path = path;
            this.tooltipKey = "sl1_flickering_lamp".equals(path)''')
old = '''        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(tooltipKey)
                    .withStyle(ChatFormatting.GRAY));'''
new = '''        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            appendZoneTooltip(path, tooltip);
            tooltip.add(Component.translatable(tooltipKey)
                    .withStyle(ChatFormatting.GRAY));'''
if old not in text: raise RuntimeError("Lamp tooltip changed")
text = text.replace(old, new, 1)
text = text.replace('''    private static boolean isDecorativePropPath(String path) {
        return "vent_open".equals(path)
                || "sl_1_floor_detail_small".equals(path)
                || "sl_1_floor_detail_big".equals(path);
    }''', '''    private static boolean isDecorativePropPath(String path) {
        return "vent_open".equals(path);
    }''')
old = '''    private static final class ConnectedFloorBlockItem extends BlockItem {
        private ConnectedFloorBlockItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("tooltip.scp_additions.sl1_connected_floors")
                    .withStyle(ChatFormatting.GRAY));'''
new = '''    private static final class ConnectedFloorBlockItem extends BlockItem {
        private final String path;

        private ConnectedFloorBlockItem(Block block, Properties properties,
                String path) {
            super(block, properties);
            this.path = path;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            appendZoneTooltip(path, tooltip);
            tooltip.add(Component.translatable("tooltip.scp_additions.sl1_connected_floors")
                    .withStyle(ChatFormatting.GRAY));'''
if old not in text: raise RuntimeError("Connected floor tooltip changed")
text = text.replace(old, new)
marker = '    private static boolean isDecorativePropPath(String path) {'
helpers = '''    private static final class FacilityZoneBlockItem extends BlockItem {
        private final String path;

        private FacilityZoneBlockItem(Block block, Properties properties,
                String path) {
            super(block, properties);
            this.path = path;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level,
                List<Component> tooltip, TooltipFlag flag) {
            appendZoneTooltip(path, tooltip);
            super.appendHoverText(stack, level, tooltip, flag);
        }
    }

    private static String zoneTooltipKey(String path) {
        if (path.startsWith("sl_1_") || path.startsWith("sl1_")) {
            return "tooltip.scp_additions.sublevel_1";
        }
        if (path.startsWith("sl_2_") || path.startsWith("sl2_")) {
            return "tooltip.scp_additions.sublevel_2";
        }
        return null;
    }

    private static void appendZoneTooltip(String path,
            List<Component> tooltip) {
        String key = zoneTooltipKey(path);
        if (key != null) {
            tooltip.add(Component.translatable(key)
                    .withStyle(ChatFormatting.BLUE));
        }
    }

'''
if marker not in text: raise RuntimeError("Decorative marker missing")
path.write_text(text.replace(marker, helpers + marker), encoding="utf-8")

# Translations.
path = ROOT / "src/main/resources/assets/scp_additions/lang/en_us_3_0.json"
lang = json.loads(path.read_text(encoding="utf-8"))
lang.update({
    "item_group.scp_additions.short_scps": "SCPs",
    "item_group.scp_additions.short_items": "Items",
    "item_group.scp_additions.short_blocks": "Blocks",
    "tooltip.scp_additions.sublevel_1": "Sublevel 1",
    "tooltip.scp_additions.sublevel_2": "Sublevel 2",
    "block.scp_additions.sign_support": "SCP Sign Support",
    "block.scp_additions.sl_1_floor_detail_small": "Small Floor Arrow",
    "block.scp_additions.sl_1_floor_detail_big": "Big Floor Arrow",
    "block.scp_additions.sl_1_wall_top": "Wall Top",
    "block.scp_additions.sl_1_wall_detail_1_mid": "Middle Wall Detail (Legacy)",
    "block.scp_additions.sl_1_wall_detail_1_top": "Top Wall Detail (Legacy)",
    "block.scp_additions.sl_1_floor_2": "Floor Block 2",
    "block.scp_additions.sl_1_floor_1": "Floor Block 1",
    "block.scp_additions.sl_1_wall_detail_2": "Pillar Wall Detail",
    "block.scp_additions.sl_1_wall_detail_1_bot": "Corner Wall Detail",
    "block.scp_additions.sl1_wall_bot": "Wall Bottom",
    "block.scp_additions.sl1_wall_mid": "Wall Middle",
    "block.scp_additions.sl1_ceiling": "Ceiling",
    "block.scp_additions.sl1_ceiling_alt": "Ceiling Alt",
    "block.scp_additions.sl1_lamp": "Ceiling Lamp",
    "block.scp_additions.sl1_flickering_lamp": "Flickering Ceiling Lamp",
    "block.scp_additions.sl_2_floor": "Floor",
    "block.scp_additions.sl_2_wall_bot": "Wall Bottom",
    "block.scp_additions.sl_2_wall_mid": "Wall Middle",
    "block.scp_additions.sl_2_wall_top": "Wall Top"
})
path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

path = ROOT / "src/main/resources/assets/scp_ublocks/lang/en_us.json"
lang = json.loads(path.read_text(encoding="utf-8"))
for key, value in list(lang.items()):
    if key.startswith(("block.scp_ublocks.sl_1_", "block.scp_ublocks.sl1_")):
        lang[key] = value.replace("SL1 ", "")
    elif key.startswith(("block.scp_ublocks.sl_2_", "block.scp_ublocks.sl2_")):
        lang[key] = value.replace("SL2 ", "")
path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

path = ROOT / "src/main/resources/assets/scp_unity_extra_blocks/lang/en_us.json"
lang = json.loads(path.read_text(encoding="utf-8"))
lang["block.scp_unity_extra_blocks.sign_support"] = "SCP Sign Support"
path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

# Changelog.
path = ROOT / "CHANGELOG.md"
text = path.read_text(encoding="utf-8")
text = text.replace("- Scaled the custom tab titles to remain clear of their search fields;",
                    "- Kept full creative-tab names on hover while using the concise internal titles **SCPs**, **Items**, and **Blocks**;")
text = text.replace("including reserved empty sections for future content;",
                    "including reserved empty sections for future content; Core Room Sign now appears under Core Room, while Door Sign and SCP Sign Support appear under Functional;")
text = text.replace("- Increased and stabilized the positional electrical loop, keeping it active through the defective lamp's internal flicker while external power remains on.",
                    "- Rebuilt the ceiling-lamp hum as one clean positional loop for the nearest powered lamp, removing embedded startup/shutdown clicks and overlapping copies while keeping it active through defective-lamp flicker.")
text = text.replace("- Prevented stable SL1 Ceiling Lamps from repeatedly playing power-on and power-off clicks without an actual redstone-state change;",
                    "- Removed the actual source of repeated ceiling-lamp clicks: the old loop file contained embedded power-on and power-off samples, which compounded across nearby lamps;")
path.write_text(text, encoding="utf-8")

print("Applied lamp audio, creative ordering, short titles, and sublevel naming cleanup.")
