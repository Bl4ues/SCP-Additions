from __future__ import annotations

from pathlib import Path
import re
import struct


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}: {old[:120]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


def regex_replace_once(path: str, pattern: str, replacement: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.DOTALL)
    if count != 1:
        raise SystemExit(f"Expected exactly one regex match in {path}, found {count}: {pattern[:120]!r}")
    file.write_text(updated, encoding="utf-8")


# Verify every user-supplied section texture before wiring it into the GUI.
texture_dir = Path("src/main/resources/assets/scp_additions/textures/gui/facility_sections")
section_textures = [
    "functionaltab.png",
    "proptab.png",
    "generaltab.png",
    "coreroomtab.png",
    "l0tab.png",
    "sl1tab.png",
    "sl2tab.png",
    "sl3tab.png",
    "sl4tab.png",
    "sl5tab.png",
]
for texture_name in section_textures:
    texture = texture_dir / texture_name
    if not texture.is_file():
        raise SystemExit(f"Missing facility section texture: {texture}")
    data = texture.read_bytes()
    if len(data) < 24 or data[:8] != b"\x89PNG\r\n\x1a\n":
        raise SystemExit(f"Invalid PNG texture: {texture}")
    width, height = struct.unpack(">II", data[16:24])
    if (width, height) != (162, 18):
        raise SystemExit(
            f"Unexpected dimensions for {texture}: {width}x{height}; expected 162x18"
        )


# Debounce the normal lamp's redstone response. A stable scheduled update prevents
# recursive neighbor notifications from producing repeated on/off clicks while the
# externally powered state has not actually changed.
ublocks_path = "src/main/java/net/mcreator/scpadditions/facility/UBlocksModule.java"
new_redstone_lamp = r'''    private static final class RedstoneCeilingLampBlock
            extends UBlockStructureBlock {
        private static final BooleanProperty LIT = BlockStateProperties.LIT;
        private static final int POWER_UPDATE_DELAY = 2;

        private RedstoneCeilingLampBlock() {
            registerDefaultState(stateDefinition.any().setValue(LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(LIT,
                    context.getLevel().hasNeighborSignal(
                            context.getClickedPos()));
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            if (level.isClientSide) {
                ensureLampLoop(state, level, pos);
                return;
            }
            if (oldState.getBlock() == this) return;
            if (state.getValue(LIT)) {
                playLampTransition(level, pos, true, false);
            }
            level.scheduleTick(pos, this, POWER_UPDATE_DELAY);
        }

        @Override
        public void neighborChanged(BlockState state, Level level, BlockPos pos,
                Block neighborBlock, BlockPos neighborPos,
                boolean movedByPiston) {
            if (!level.isClientSide) {
                level.scheduleTick(pos, this, POWER_UPDATE_DELAY);
            }
        }

        @Override
        public void tick(BlockState state, ServerLevel level, BlockPos pos,
                RandomSource random) {
            boolean powered = level.hasNeighborSignal(pos);
            boolean lit = state.getValue(LIT);
            if (lit == powered) return;

            level.setBlock(pos, state.setValue(LIT, powered),
                    Block.UPDATE_CLIENTS);
            playLampTransition(level, pos, powered, false);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos,
                RandomSource random) {
            ensureLampLoop(state, level, pos);
        }

        @Override
        public int getLightEmission(BlockState state, BlockGetter level,
                BlockPos pos) {
            return state.getValue(LIT) ? 15 : 0;
        }
    }

    private static final class FlickeringCeilingLampBlock'''
regex_replace_once(
    ublocks_path,
    r"    private static final class RedstoneCeilingLampBlock.*?\n    }\n\n    private static final class FlickeringCeilingLampBlock",
    new_redstone_lamp,
)
replace_once(
    ublocks_path,
    """            super.onPlace(state, level, pos, oldState, movedByPiston);\n            if (level.isClientSide || oldState.getBlock() == this) return;\n            if (state.getValue(LIT)) {""",
    """            super.onPlace(state, level, pos, oldState, movedByPiston);\n            if (level.isClientSide) {\n                ensureLampLoop(state, level, pos);\n                return;\n            }\n            if (oldState.getBlock() == this) return;\n            if (state.getValue(LIT)) {""",
)


# Make lamp loops deterministic near the player instead of relying only on random
# display ticks. Loops are bounded by distance and flickering lamps keep their hum
# while externally powered, even during their brief internal dark frames.
Path("src/main/java/net/mcreator/scpadditions/client/CeilingLampAudioClient.java").write_text(
'''package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Maintains one positional electrical loop for each nearby powered ceiling lamp. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CeilingLampAudioClient {
    private static final Map<LampKey, CeilingLampLoopSound> LOOPS =
            new HashMap<>();
    private static final int DISCOVERY_INTERVAL_TICKS = 20;
    private static final int HORIZONTAL_DISCOVERY_RADIUS = 16;
    private static final int VERTICAL_DISCOVERY_RADIUS = 8;

    private static int discoveryTicks;

    private CeilingLampAudioClient() {
    }

    public static void ensureLoop(Level level, BlockPos pos) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        LampKey key = new LampKey(clientLevel.dimension(), pos.asLong());
        CeilingLampLoopSound existing = LOOPS.get(key);
        if (existing != null && !existing.isFinished()) return;

        CeilingLampLoopSound sound = new CeilingLampLoopSound(clientLevel, pos);
        LOOPS.put(key, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();

        Iterator<Map.Entry<LampKey, CeilingLampLoopSound>> iterator =
                LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LampKey, CeilingLampLoopSound> entry = iterator.next();
            CeilingLampLoopSound sound = entry.getValue();
            if (minecraft.level == null
                    || !entry.getKey().dimension().equals(
                    minecraft.level.dimension())) {
                sound.finish();
            }
            if (sound.isFinished()) iterator.remove();
        }

        if (minecraft.level == null || minecraft.player == null) {
            discoveryTicks = 0;
            return;
        }

        discoveryTicks++;
        if (discoveryTicks < DISCOVERY_INTERVAL_TICKS) return;
        discoveryTicks = 0;
        discoverNearbyPoweredLamps(minecraft.level,
                minecraft.player.blockPosition());
    }

    private static void discoverNearbyPoweredLamps(ClientLevel level,
            BlockPos center) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -VERTICAL_DISCOVERY_RADIUS;
                y <= VERTICAL_DISCOVERY_RADIUS; y++) {
            for (int x = -HORIZONTAL_DISCOVERY_RADIUS;
                    x <= HORIZONTAL_DISCOVERY_RADIUS; x++) {
                for (int z = -HORIZONTAL_DISCOVERY_RADIUS;
                        z <= HORIZONTAL_DISCOVERY_RADIUS; z++) {
                    cursor.set(center.getX() + x, center.getY() + y,
                            center.getZ() + z);
                    if (!level.hasChunkAt(cursor)) continue;
                    if (CeilingLampLoopSound.shouldPlayFor(
                            level.getBlockState(cursor))) {
                        ensureLoop(level, cursor);
                    }
                }
            }
        }
    }

    private record LampKey(ResourceKey<Level> dimension, long pos) {
    }
}
''', encoding="utf-8")

Path("src/main/java/net/mcreator/scpadditions/client/CeilingLampLoopSound.java").write_text(
'''package net.mcreator.scpadditions.client;

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

/** Positional electrical hum owned by one nearby powered ceiling lamp. */
public final class CeilingLampLoopSound extends AbstractTickableSoundInstance {
    private static final double MAX_DISTANCE_SQR = 24.0D * 24.0D;

    private final ClientLevel level;
    private final BlockPos pos;
    private boolean finished;

    public CeilingLampLoopSound(ClientLevel level, BlockPos pos) {
        super(ScpAdditionsModSounds.LAMP_LOOP.get(), SoundSource.BLOCKS,
                RandomSource.create());
        this.level = level;
        this.pos = pos.immutable();
        this.looping = true;
        this.delay = 0;
        this.volume = 0.80F;
        this.pitch = 0.98F + RandomSource.create().nextFloat() * 0.04F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || minecraft.player == null
                || minecraft.player.distanceToSqr(x, y, z) > MAX_DISTANCE_SQR
                || !shouldPlayFor(level.getBlockState(pos))) {
            finish();
        }
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

# Short ambience should be buffered for reliable manual looping in 1.20.1.
sounds_path = "src/main/resources/assets/scp_additions/sounds.json"
replace_once(
    sounds_path,
    '"lamp_loop":{"subtitle":"subtitles.scp_additions.lamp_loop","sounds":[{"name":"scp_additions:lamp_loop","stream":true,"volume":1.0}]}',
    '"lamp_loop":{"subtitle":"subtitles.scp_additions.lamp_loop","sounds":[{"name":"scp_additions:lamp_loop","stream":false,"volume":1.0}]}',
)


# Replace the five provisional section definitions with the complete ten-header
# roadmap and point them at the supplied textures. Titles are baked into the PNGs.
facility_path = "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java"
replace_once(
    facility_path,
    """    public record CreativeSection(String translationKey, ResourceLocation sprite,\n            int textColor, List<ItemStack> items) {\n    }""",
    """    public record CreativeSection(ResourceLocation sprite,\n            List<ItemStack> items) {\n    }""",
)
new_sections_method = r'''    public static List<CreativeSection> creativeSections() {
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
        addFacilityCreativeItem(props, "sign_support");
        addFacilityCreativeItem(props, "core_room_sign");
        addFacilityCreativeItem(props, "door_sign");
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

        sections.add(section("coreroomtab", List.of()));
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

    public static List<ItemStack> creativeItemsInDisplayOrder()'''
regex_replace_once(
    facility_path,
    r"    public static List<CreativeSection> creativeSections\(\) \{.*?\n    }\n\n    public static List<ItemStack> creativeItemsInDisplayOrder\(\)",
    new_sections_method,
)
replace_once(
    facility_path,
    """    private static CreativeSection section(String translationKey, String sprite,\n            int textColor, List<ItemStack> items) {\n        return new CreativeSection(translationKey,\n                new ResourceLocation(MODID, \"textures/gui/facility_sections/\" + sprite + \".png\"),\n                textColor, List.copyOf(items));\n    }""",
    """    private static CreativeSection section(String sprite,\n            List<ItemStack> items) {\n        return new CreativeSection(\n                new ResourceLocation(MODID, \"textures/gui/facility_sections/\" + sprite + \".png\"),\n                List.copyOf(items));\n    }""",
)

presentation_path = "src/main/java/net/mcreator/scpadditions/client/CreativeTabPresentation.java"
replace_once(
    presentation_path,
    """        Font font = Minecraft.getInstance().font;\n\n        for (FacilityModule.CreativeSection section :""",
    """        for (FacilityModule.CreativeSection section :""",
)
replace_once(
    presentation_path,
    """                graphics.blit(section.sprite(), left, y, 0.0F, 0.0F,\n                        HEADER_WIDTH, HEADER_HEIGHT, HEADER_WIDTH, HEADER_HEIGHT);\n                graphics.drawString(font,\n                        Component.translatable(section.translationKey()),\n                        left + 5, y + 5, section.textColor(), false);""",
    """                graphics.blit(section.sprite(), left, y, 0.0F, 0.0F,\n                        HEADER_WIDTH, HEADER_HEIGHT, HEADER_WIDTH, HEADER_HEIGHT);""",
)
replace_once(
    presentation_path,
    """            sectionRow += 1 + Math.max(1,\n                    (section.items().size() + 8) / 9);""",
    """            sectionRow += 1 + (section.items().size() + 8) / 9;""",
)


# Keep the development changelog aligned with the actual interface and audio fixes.
changelog_path = "CHANGELOG.md"
replace_once(
    changelog_path,
    "- Organized facility content under full-width **Functional**, **Props**, **General**, **LCZ - Sublevel 1**, and **LCZ - Sublevel 2** section headers (until I add more sectors);",
    "- Organized facility content under ten textured, full-width section headers in this order: **Functional**, **Props**, **General**, **Core Room**, **Light Containment Zone**, and **LCZ - Sublevels 1-5**, including reserved empty sections for future content;",
)
replace_once(
    changelog_path,
    """- Added an SL1 Ceiling Lamp that emits light while powered by redstone, with subtle positional startup, shutdown, and electrical-loop audio;\n- Added an SL1 Flickering Ceiling Lamp with the same redstone control and irregular defective-light flickering.""",
    """- Added an SL1 Ceiling Lamp that emits light while powered by redstone, with subtle positional startup, shutdown, and electrical-loop audio;\n- Added an SL1 Flickering Ceiling Lamp with the same redstone control and irregular defective-light flickering;\n- Increased and stabilized the positional electrical loop, keeping it active through the defective lamp's internal flicker while external power remains on.""",
)
replace_once(
    changelog_path,
    """## Bug Fixes\n\n- Limited Tesla Gate damage""",
    """## Bug Fixes\n\n- Prevented stable SL1 Ceiling Lamps from repeatedly playing power-on and power-off clicks without an actual redstone-state change;\n- Limited Tesla Gate damage""",
)

print("Applied stable lamp audio and the complete textured facility-section layout.")
