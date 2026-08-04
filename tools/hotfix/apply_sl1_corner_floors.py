from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[2]


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def write_json(path: Path, data, *, compact: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if compact:
        rendered = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    else:
        rendered = json.dumps(data, ensure_ascii=False, indent=2)
    path.write_text(rendered + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------
# Block registration and automatic fixed-orientation floor transitions.
# ---------------------------------------------------------------------------
ublocks_path = ROOT / "src/main/java/net/mcreator/scpadditions/facility/UBlocksModule.java"
ublocks = ublocks_path.read_text(encoding="utf-8")

ublocks = replace_once(
    ublocks,
    '''    // Sector 1 structural set. Floor 1 is the static gray surface; Floor 2 owns
    // the visual transition states rendered around neighboring gray tiles.
    public static final RegistryObject<Block> SL_1_FLOOR_1 = registerBlock(
            "sl_1_floor_1", GrayConnectedFloorBlock::new, false);
    public static final RegistryObject<Block> SL_1_FLOOR_2 = registerBlock(
            "sl_1_floor_2", BlueConnectedFloorBlock::new, false);
    public static final RegistryObject<Block> SL1_WALL_BOT = structure("sl1_wall_bot");
''',
    '''    // Sector 1 structural set. Metal Floor is a static connection target;
    // Blue, Rest Area and Kitchen floors own fixed-orientation transition states.
    public static final RegistryObject<Block> SL_1_FLOOR_1 = registerBlock(
            "sl_1_floor_1", GrayConnectedFloorBlock::new, false);
    public static final RegistryObject<Block> SL_1_FLOOR_2 = registerBlock(
            "sl_1_floor_2", () -> new TransitionConnectedFloorBlock(ConnectionTarget.METAL), false);
    public static final RegistryObject<Block> SL_1_RESTING_FLOOR = registerBlock(
            "sl_1_resting_floor", () -> new TransitionConnectedFloorBlock(ConnectionTarget.METAL), false);
    public static final RegistryObject<Block> SL_1_KITCHEN_FLOOR = registerBlock(
            "sl_1_kitchen_floor", () -> new TransitionConnectedFloorBlock(ConnectionTarget.BLUE), false);
    public static final RegistryObject<Block> SL1_WALL_BOT = structure("sl1_wall_bot");
    public static final RegistryObject<Block> SL1_BOTTOM_ALT = structure("sl1_bottom_alt");
''',
    "SL1 registrations",
)

ublocks = replace_once(
    ublocks,
    '''    private static boolean isConnectedFloorPath(String path) {
        return "sl_1_floor_1".equals(path) || "sl_1_floor_2".equals(path);
    }
''',
    '''    private static boolean isConnectedFloorPath(String path) {
        return "sl_1_floor_1".equals(path)
                || "sl_1_floor_2".equals(path)
                || "sl_1_resting_floor".equals(path)
                || "sl_1_kitchen_floor".equals(path);
    }
''',
    "connected floor item paths",
)

ublocks = replace_once(
    ublocks,
    '''            String connectionKey = "sl_1_floor_1".equals(path)
                    ? "tooltip.scp_additions.sl1_metal_floor_connection"
                    : "tooltip.scp_additions.sl1_blue_floor_connection";
''',
    '''            String connectionKey = switch (path) {
                case "sl_1_floor_1" -> "tooltip.scp_additions.sl1_metal_floor_connection";
                case "sl_1_floor_2" -> "tooltip.scp_additions.sl1_blue_floor_connection";
                case "sl_1_resting_floor" -> "tooltip.scp_additions.sl1_resting_floor_connection";
                case "sl_1_kitchen_floor" -> "tooltip.scp_additions.sl1_kitchen_floor_connection";
                default -> "tooltip.scp_additions.sl1_connected_floors";
            };
''',
    "connected floor tooltip selection",
)

connected_pattern = re.compile(
    r"    private abstract static class ConnectedFloorBlock extends UBlockStructureBlock \{.*?\n    private static int count\(boolean\.\.\. values\) \{",
    re.DOTALL,
)
connected_replacement = '''    private abstract static class ConnectedFloorBlock extends UBlockStructureBlock {
        private ConnectedFloorBlock() {
            super();
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos,
                BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            if (oldState.getBlock() != state.getBlock()) {
                refreshTransitionFloors(level, pos, state);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos,
                BlockState newState, boolean movedByPiston) {
            if (state.getBlock() != newState.getBlock()) {
                // onRemove runs before the replacement is fully visible through
                // the level, so pass it explicitly while recalculating neighbors.
                refreshTransitionFloors(level, pos, newState);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    private static final class GrayConnectedFloorBlock extends ConnectedFloorBlock {
        private GrayConnectedFloorBlock() {
            super();
        }
    }

    private static final class TransitionConnectedFloorBlock extends ConnectedFloorBlock {
        private static final EnumProperty<FloorTransition> TRANSITION =
                EnumProperty.create("transition", FloorTransition.class);
        private final ConnectionTarget connectionTarget;

        private TransitionConnectedFloorBlock(ConnectionTarget connectionTarget) {
            super();
            this.connectionTarget = connectionTarget;
            registerDefaultState(stateDefinition.any().setValue(TRANSITION, FloorTransition.NONE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TRANSITION);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(TRANSITION,
                    resolveTransition(context.getLevel(), context.getClickedPos(),
                            null, null, connectionTarget));
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level, BlockPos currentPos,
                BlockPos neighborPos) {
            FloorTransition transition = resolveTransition(level, currentPos,
                    neighborPos, neighborState, connectionTarget);
            return state.getValue(TRANSITION) == transition
                    ? state
                    : state.setValue(TRANSITION, transition);
        }
    }

    private static void refreshTransitionFloors(Level level, BlockPos changedPos,
            BlockState replacementState) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos target = changedPos.offset(x, 0, z);
                BlockState targetState = target.equals(changedPos)
                        ? replacementState
                        : level.getBlockState(target);
                if (!(targetState.getBlock() instanceof TransitionConnectedFloorBlock floor)) {
                    continue;
                }

                FloorTransition transition = resolveTransition(level, target,
                        changedPos, replacementState, floor.connectionTarget);
                if (targetState.getValue(TransitionConnectedFloorBlock.TRANSITION) != transition) {
                    level.setBlock(target,
                            targetState.setValue(TransitionConnectedFloorBlock.TRANSITION, transition),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static FloorTransition resolveTransition(BlockGetter level, BlockPos pos,
            BlockPos overriddenPos, BlockState overriddenState,
            ConnectionTarget connectionTarget) {
        boolean north = isConnectionTarget(level, pos.north(), overriddenPos,
                overriddenState, connectionTarget);
        boolean east = isConnectionTarget(level, pos.east(), overriddenPos,
                overriddenState, connectionTarget);
        boolean south = isConnectionTarget(level, pos.south(), overriddenPos,
                overriddenState, connectionTarget);
        boolean west = isConnectionTarget(level, pos.west(), overriddenPos,
                overriddenState, connectionTarget);

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

        boolean northWest = isConnectionTarget(level, pos.north().west(),
                overriddenPos, overriddenState, connectionTarget);
        boolean northEast = isConnectionTarget(level, pos.north().east(),
                overriddenPos, overriddenState, connectionTarget);
        boolean southEast = isConnectionTarget(level, pos.south().east(),
                overriddenPos, overriddenState, connectionTarget);
        boolean southWest = isConnectionTarget(level, pos.south().west(),
                overriddenPos, overriddenState, connectionTarget);

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
            // The supplied twelve transition textures do not include the two
            // opposite-corner checkerboard cases.
            return FloorTransition.NONE;
        }
        if (northWest) return FloorTransition.CORNER_NW;
        if (northEast) return FloorTransition.CORNER_NE;
        if (southEast) return FloorTransition.CORNER_SE;
        if (southWest) return FloorTransition.CORNER_SW;
        return FloorTransition.NONE;
    }

    private static boolean isConnectionTarget(BlockGetter level, BlockPos pos,
            BlockPos overriddenPos, BlockState overriddenState,
            ConnectionTarget connectionTarget) {
        BlockState state = overriddenPos != null && overriddenPos.equals(pos)
                ? overriddenState
                : level.getBlockState(pos);
        return state != null && connectionTarget.matches(state);
    }

    private enum ConnectionTarget {
        METAL {
            @Override
            boolean matches(BlockState state) {
                return state.is(SL_1_FLOOR_1.get());
            }
        },
        BLUE {
            @Override
            boolean matches(BlockState state) {
                return state.is(SL_1_FLOOR_2.get());
            }
        };

        abstract boolean matches(BlockState state);
    }

    private static int count(boolean... values) {'''
ublocks, matches = connected_pattern.subn(connected_replacement, ublocks, count=1)
if matches != 1:
    raise RuntimeError(f"connected floor engine: expected one match, found {matches}")
ublocks_path.write_text(ublocks, encoding="utf-8")


# ---------------------------------------------------------------------------
# Curated creative-tab order.
# ---------------------------------------------------------------------------
facility_path = ROOT / "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java"
facility = facility_path.read_text(encoding="utf-8")
facility = replace_once(
    facility,
    '''        addUBlockCreativeItem(sublevel1, "sl_1_floor_2");
        addUBlockCreativeItem(sublevel1, "sl_1_floor_1");
        addUBlockCreativeItem(sublevel1, "sl1_wall_bot");
        addUBlockCreativeItem(sublevel1, "sl1_wall_mid");
''',
    '''        addUBlockCreativeItem(sublevel1, "sl_1_floor_2");
        addUBlockCreativeItem(sublevel1, "sl_1_floor_1");
        addUBlockCreativeItem(sublevel1, "sl_1_resting_floor");
        addUBlockCreativeItem(sublevel1, "sl_1_kitchen_floor");
        addUBlockCreativeItem(sublevel1, "sl1_wall_bot");
        addUBlockCreativeItem(sublevel1, "sl1_bottom_alt");
        addUBlockCreativeItem(sublevel1, "sl1_wall_mid");
''',
    "SL1 creative order",
)
facility_path.write_text(facility, encoding="utf-8")


# ---------------------------------------------------------------------------
# English names and tooltip punctuation.
# ---------------------------------------------------------------------------
patch_path = ROOT / "src/main/resources/assets/scp_additions/lang/en_us_3_0.json"
patch = json.loads(patch_path.read_text(encoding="utf-8"))
patch.update({
    "block.scp_additions.core_room_sign": "Facility Directional Sign",
    "block.scp_additions.sign_support": "SCP Information Sign",
    "block.scp_additions.sl_1_floor_detail_big": "Large Floor Arrow",
    "block.scp_additions.sl_1_wall_top": "Metal Wall (Top)",
    "block.scp_additions.sl_1_wall_detail_2": "Wall Pillar Detail",
    "block.scp_additions.sl_1_wall_detail_1_bot": "Wall Corner Detail",
    "block.scp_additions.sl1_wall_bot": "Metal Wall (Bottom)",
    "block.scp_additions.sl1_bottom_alt": "Alternative Metal Wall (Bottom)",
    "block.scp_additions.sl1_wall_mid": "Metal Wall (Middle)",
    "block.scp_additions.sl1_ceiling": "Ceiling Tile",
    "block.scp_additions.sl1_ceiling_alt": "Dark Ceiling Tile",
    "block.scp_additions.sl_1_resting_floor": "Rest Area Corner Floor",
    "block.scp_additions.sl_1_kitchen_floor": "Kitchen Corner Floor",
    "block.scp_additions.sl_2_wall_bot": "Concrete Wall (Bottom)",
    "block.scp_additions.sl_2_wall_mid": "Concrete Wall (Middle)",
    "block.scp_additions.sl_2_wall_top": "Concrete Wall (Top)",
    "block.scp_additions.vent_open": "Vent",
    "block.scp_additions.tesla_bottom": "Tesla Room Wall (Bottom)",
    "block.scp_additions.tesla_mid_1": "Tesla Room Wall",
    "block.scp_additions.tesla_mid_2": "Tesla Room Wall (Middle)",
    "block.scp_additions.tesla_bottom_alt": "Alternative Tesla Room Wall (Bottom)",
    "block.scp_additions.tesla_top_alt": "Alternative Tesla Room Wall (Top)",
    "block.scp_additions.archival_bottom": "Archival Entrance Wall (Bottom)",
    "block.scp_additions.archival_mid": "Archival Entrance Wall (Middle)",
    "block.scp_additions.archival_top": "Archival Entrance Wall (Top)",
    "block.scp_additions.archival_bot_1": "Archival Wall (Bottom)",
    "block.scp_additions.archival_mid_2": "Archival Wall (Middle)",
    "block.scp_additions.office_bottom": "Office Wall (Bottom)",
    "block.scp_additions.office_mid": "Office Wall (Middle)",
    "block.scp_additions.office_top": "Office Wall (Top)",
    "block.scp_additions.skyroom_bot_1": "Skyroom Wall (Bottom)",
    "block.scp_additions.skyroom_bot_2": "Alternative Skyroom Wall (Bottom)",
    "block.scp_additions.skyroom_mid": "Skyroom Wall (Middle)",
    "block.scp_additions.skyroom_top_alt": "Skyroom Wall (Top)",
    "block.scp_additions.skyroom_block": "Skyroom Wall",
    "block.scp_additions.security_bot": "Security Wall (Bottom)",
    "block.scp_additions.security_mid": "Security Wall (Middle)",
    "block.scp_additions.security_top": "Security Wall (Top)",
    "tooltip.scp_additions.sl1_metal_floor_connection":
        "Connects automatically with Blue Floor and Rest Area Corner Floor",
    "tooltip.scp_additions.sl1_blue_floor_connection":
        "Connects automatically with Metal Floor and Kitchen Corner Floor",
    "tooltip.scp_additions.sl1_resting_floor_connection":
        "Connects automatically with Metal Floor",
    "tooltip.scp_additions.sl1_kitchen_floor_connection":
        "Connects automatically with Blue Floor",
})
write_json(patch_path, patch, compact=True)

base_lang_path = ROOT / "src/main/resources/assets/scp_additions/lang/en_us.json"
base_lang = json.loads(base_lang_path.read_text(encoding="utf-8"))
base_lang.update({
    "block.scp_additions.scp_914body": "SCP-914 Body",
    "block.scp_additions.scp_914dial_rough": "SCP-914 Dial",
    "block.scp_additions.scp_914dial_coarse": "SCP-914 Dial",
    "block.scp_additions.scp_914dial_1to_1": "SCP-914 Dial",
    "block.scp_additions.scp_914dial_fine": "SCP-914 Dial",
    "block.scp_additions.scp_914dial_very_fine": "SCP-914 Dial",
    "block.scp_additions.scp_914_intake_door_closed": "Closed SCP-914 Intake Door",
    "block.scp_additions.scp_914_output_door_closed": "Closed SCP-914 Output Door",
    "item.scp_additions.spray": "Methyl Isothiocyanate Spray Bottle",
    "item.scp_additions.pieces_of_paper": "Pieces of Paper",
    "item.scp_additions.cup_of_coffee": "Cup of Coffee",
})
write_json(base_lang_path, base_lang, compact=True)

legacy_ublocks_path = ROOT / "src/main/resources/assets/scp_ublocks/lang/en_us.json"
legacy_ublocks = json.loads(legacy_ublocks_path.read_text(encoding="utf-8"))
legacy_ublocks.update({
    "block.scp_ublocks.sl_1_floor_detail_big": "Large Floor Arrow",
    "block.scp_ublocks.sl_1_wall_top": "Metal Wall (Top)",
    "block.scp_ublocks.sl_1_wall_detail_2": "Wall Pillar Detail",
    "block.scp_ublocks.sl_1_wall_detail_1_bot": "Wall Corner Detail",
    "block.scp_ublocks.sl1_wall_bot": "Metal Wall (Bottom)",
    "block.scp_ublocks.sl1_wall_mid": "Metal Wall (Middle)",
    "block.scp_ublocks.sl1_ceiling": "Ceiling Tile",
    "block.scp_ublocks.sl1_ceiling_alt": "Dark Ceiling Tile",
    "block.scp_ublocks.sl_2_wall_bot": "Concrete Wall (Bottom)",
    "block.scp_ublocks.sl_2_wall_mid": "Concrete Wall (Middle)",
    "block.scp_ublocks.sl_2_wall_top": "Concrete Wall (Top)",
})
write_json(legacy_ublocks_path, legacy_ublocks)

# Remove a single terminal period from every translated tooltip. Ellipses are
# intentional prose and remain untouched. The existing runtime normalizer stays
# as a safeguard for dynamically assembled or third-party tooltip components.
for lang_path in (ROOT / "src/main/resources/assets").glob("*/lang/*.json"):
    try:
        entries = json.loads(lang_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, UnicodeDecodeError):
        continue
    changed = False
    for key, value in list(entries.items()):
        if (isinstance(value, str) and key.startswith("tooltip.")
                and value.endswith(".") and not value.endswith("...")):
            entries[key] = value[:-1]
            changed = True
    if changed:
        write_json(lang_path, entries, compact=(lang_path.name == "en_us_3_0.json"
                                                or "scp_additions/lang" in lang_path.as_posix()))


# ---------------------------------------------------------------------------
# Models, blockstates and item display.
# ---------------------------------------------------------------------------
assets = ROOT / "src/main/resources/assets/scp_additions"
blockstates = assets / "blockstates"
block_models = assets / "models/block"
item_models = assets / "models/item"


def cube_model(base_texture: str, top_texture: str | None = None):
    top = top_texture or base_texture
    return {
        "parent": "block/cube",
        "textures": {
            "down": base_texture,
            "up": top,
            "north": base_texture,
            "east": base_texture,
            "south": base_texture,
            "west": base_texture,
            "particle": base_texture,
        },
        "render_type": "solid",
    }


write_json(blockstates / "sl1_bottom_alt.json", {
    "variants": {"": {"model": "scp_additions:block/sl1_bottom_alt"}}
})
write_json(block_models / "sl1_bottom_alt.json", {
    "parent": "block/cube",
    "textures": {
        "down": "scp_ublocks:block/unity_sector_1_sky",
        "up": "scp_ublocks:block/unity_sector_1_middle",
        "north": "scp_additions:block/sl1_bottom_alt",
        "east": "scp_additions:block/sl1_bottom_alt",
        "south": "scp_additions:block/sl1_bottom_alt",
        "west": "scp_additions:block/sl1_bottom_alt",
        "particle": "scp_additions:block/sl1_bottom_alt",
    },
    "render_type": "solid",
})
write_json(item_models / "sl1_bottom_alt.json", {
    "parent": "scp_additions:block/sl1_bottom_alt"
})

transition_map = {
    "corner_sw": 1,
    "edge_w": 2,
    "inner_nw": 3,
    "corner_nw": 4,
    "edge_n": 5,
    "inner_ne": 6,
    "corner_ne": 7,
    "edge_e": 8,
    "inner_se": 9,
    "corner_se": 10,
    "edge_s": 11,
    "inner_sw": 12,
}


def write_transition_floor(block_id: str, base_texture_name: str,
                           transition_prefix: str, full_model: str) -> None:
    base_model = f"scp_additions:block/{block_id}"
    variants = {
        "transition=none": {"model": base_model},
        "transition=full": {"model": full_model},
    }
    base_texture = f"scp_additions:block/{base_texture_name}"
    write_json(block_models / f"{block_id}.json", cube_model(base_texture))
    write_json(item_models / f"{block_id}.json", {"parent": base_model})
    for transition, number in transition_map.items():
        model_name = f"{block_id}_{number}"
        variants[f"transition={transition}"] = {
            "model": f"scp_additions:block/{model_name}"
        }
        write_json(
            block_models / f"{model_name}.json",
            cube_model(base_texture,
                       f"scp_additions:block/{transition_prefix}{number}"),
        )
    write_json(blockstates / f"{block_id}.json", {"variants": variants})


write_transition_floor(
    "sl_1_resting_floor", "resting_floor", "resting",
    "scp_ublocks:block/sl_1_floor_1",
)
write_transition_floor(
    "sl_1_kitchen_floor", "kitchen_floor", "kitchen",
    "scp_ublocks:block/sl_1_floor_2",
)

vent_item_path = ROOT / "src/main/resources/assets/scp_ublocks/models/item/vent_open.json"
vent_item = json.loads(vent_item_path.read_text(encoding="utf-8"))
rotation = vent_item.get("display", {}).get("thirdperson", {}).get("rotation")
if rotation != [10, -45, 170]:
    raise RuntimeError(f"Unexpected Vent item rotation: {rotation!r}")
rotation[1] = 135
write_json(vent_item_path, vent_item)


# ---------------------------------------------------------------------------
# Changelog: additions only. Corrections inside an unreleased version are not
# advertised as post-release fixes.
# ---------------------------------------------------------------------------
changelog_path = ROOT / "CHANGELOG.md"
changelog = changelog_path.read_text(encoding="utf-8")
changelog = replace_once(
    changelog,
    '''- Standardized the creative-tab names as **SCP Additions - SCPs**, **SCP Additions - Items**, and **SCP Additions - Blocks**;
- Organized facility content under ten headers in this order: **Functional**, **Decoration**, **General**, **Core Room**, and **Zones**.
''',
    '''- Standardized the creative-tab names as **SCP Additions - SCPs**, **SCP Additions - Items**, and **SCP Additions - Blocks**;
- Organized facility content under ten headers in this order: **Functional**, **Decoration**, **General**, **Core Room**, and **Zones**;
- Added **Alternative Metal Wall (Bottom)**, **Rest Area Corner Floor**, and **Kitchen Corner Floor** to **LCZ - Sublevel 1**;
- Added fixed-orientation automatic transitions from Rest Area Corner Floor to Metal Floor and from Kitchen Corner Floor to Blue Floor, using the original Blue/Metal floor corner, edge, inner-corner, and full-connection mapping.
''',
    "3.1.0 changelog facility additions",
)
changelog_path.write_text(changelog, encoding="utf-8")

print("SL1 blocks, language cleanup and Vent display changes applied")
