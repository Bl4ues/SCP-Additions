from pathlib import Path
import json


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


root = Path(".")
java_path = root / "src/main/java/net/mcreator/scpadditions/facility/UBlocksModule.java"
text = java_path.read_text(encoding="utf-8")

text = replace_once(
    text,
    """    // Sector 1 structural set. Metal Floor is a static connection target;
    // Blue, Rest Area and Kitchen floors own fixed-orientation transition states.
""",
    """    // Sector 1 structural set. Metal and Kitchen floors are static connection
    // targets; Blue and Rest Area own fixed-orientation transition states.
""",
    "SL1 floor ownership comment",
)

text = replace_once(
    text,
    '"sl_1_floor_2", () -> new TransitionConnectedFloorBlock(ConnectionTarget.METAL), false);',
    '"sl_1_floor_2", BlueConnectedFloorBlock::new, false);',
    "Blue Floor registration",
)
text = replace_once(
    text,
    '"sl_1_kitchen_floor", () -> new TransitionConnectedFloorBlock(ConnectionTarget.BLUE), false);',
    '"sl_1_kitchen_floor", GrayConnectedFloorBlock::new, false);',
    "Kitchen Floor registration",
)

class_end = text.index("\n    private static void refreshTransitionFloors")
blue_class = '''

    private static final class BlueConnectedFloorBlock extends ConnectedFloorBlock {
        private static final EnumProperty<BlueFloorTransition> TRANSITION =
                EnumProperty.create("transition", BlueFloorTransition.class);

        private BlueConnectedFloorBlock() {
            super();
            registerDefaultState(stateDefinition.any()
                    .setValue(TRANSITION, BlueFloorTransition.NONE));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TRANSITION);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(TRANSITION,
                    resolveBlueTransition(context.getLevel(),
                            context.getClickedPos(), null, null));
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction,
                BlockState neighborState, LevelAccessor level,
                BlockPos currentPos, BlockPos neighborPos) {
            BlueFloorTransition transition = resolveBlueTransition(level,
                    currentPos, neighborPos, neighborState);
            return state.getValue(TRANSITION) == transition
                    ? state
                    : state.setValue(TRANSITION, transition);
        }
    }
'''
text = text[:class_end] + blue_class + text[class_end:]

refresh_start = text.index("    private static void refreshTransitionFloors")
refresh_end = text.index("\n    private static FloorTransition resolveTransition", refresh_start)
refresh_replacement = '''    private static void refreshTransitionFloors(Level level, BlockPos changedPos,
            BlockState replacementState) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos target = changedPos.offset(x, 0, z);
                BlockState targetState = target.equals(changedPos)
                        ? replacementState
                        : level.getBlockState(target);

                if (targetState.getBlock() instanceof TransitionConnectedFloorBlock floor) {
                    FloorTransition transition = resolveTransition(level, target,
                            changedPos, replacementState, floor.connectionTarget);
                    if (targetState.getValue(TransitionConnectedFloorBlock.TRANSITION)
                            != transition) {
                        level.setBlock(target,
                                targetState.setValue(
                                        TransitionConnectedFloorBlock.TRANSITION,
                                        transition),
                                Block.UPDATE_CLIENTS);
                    }
                    continue;
                }

                if (targetState.getBlock() instanceof BlueConnectedFloorBlock) {
                    BlueFloorTransition transition = resolveBlueTransition(level,
                            target, changedPos, replacementState);
                    if (targetState.getValue(BlueConnectedFloorBlock.TRANSITION)
                            != transition) {
                        level.setBlock(target,
                                targetState.setValue(
                                        BlueConnectedFloorBlock.TRANSITION,
                                        transition),
                                Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static BlueFloorTransition resolveBlueTransition(BlockGetter level,
            BlockPos pos, BlockPos overriddenPos, BlockState overriddenState) {
        FloorTransition kitchenTransition = resolveTransition(level, pos,
                overriddenPos, overriddenState, ConnectionTarget.KITCHEN);
        if (kitchenTransition != FloorTransition.NONE) {
            return BlueFloorTransition.from(kitchenTransition, true);
        }

        FloorTransition metalTransition = resolveTransition(level, pos,
                overriddenPos, overriddenState, ConnectionTarget.METAL);
        return BlueFloorTransition.from(metalTransition, false);
    }
'''
text = text[:refresh_start] + refresh_replacement + text[refresh_end:]

text = replace_once(
    text,
    '''        BLUE {
            @Override
            boolean matches(BlockState state) {
                return state.is(SL_1_FLOOR_2.get());
            }
        };''',
    '''        KITCHEN {
            @Override
            boolean matches(BlockState state) {
                return state.is(SL_1_KITCHEN_FLOOR.get());
            }
        };''',
    "Kitchen connection target",
)

transition_end = text.index("\n    private static final class AdaptiveWallDetailBlock")
blue_enum = '''

    private enum BlueFloorTransition implements StringRepresentable {
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
        FULL("full"),
        KITCHEN_CORNER_SW("kitchen_corner_sw"),
        KITCHEN_CORNER_NW("kitchen_corner_nw"),
        KITCHEN_CORNER_NE("kitchen_corner_ne"),
        KITCHEN_CORNER_SE("kitchen_corner_se"),
        KITCHEN_EDGE_W("kitchen_edge_w"),
        KITCHEN_EDGE_N("kitchen_edge_n"),
        KITCHEN_EDGE_E("kitchen_edge_e"),
        KITCHEN_EDGE_S("kitchen_edge_s"),
        KITCHEN_INNER_NW("kitchen_inner_nw"),
        KITCHEN_INNER_NE("kitchen_inner_ne"),
        KITCHEN_INNER_SE("kitchen_inner_se"),
        KITCHEN_INNER_SW("kitchen_inner_sw"),
        KITCHEN_FULL("kitchen_full");

        private final String serializedName;

        BlueFloorTransition(String serializedName) {
            this.serializedName = serializedName;
        }

        private static BlueFloorTransition from(FloorTransition transition,
                boolean kitchen) {
            if (!kitchen) {
                return switch (transition) {
                    case CORNER_SW -> CORNER_SW;
                    case CORNER_NW -> CORNER_NW;
                    case CORNER_NE -> CORNER_NE;
                    case CORNER_SE -> CORNER_SE;
                    case EDGE_W -> EDGE_W;
                    case EDGE_N -> EDGE_N;
                    case EDGE_E -> EDGE_E;
                    case EDGE_S -> EDGE_S;
                    case INNER_NW -> INNER_NW;
                    case INNER_NE -> INNER_NE;
                    case INNER_SE -> INNER_SE;
                    case INNER_SW -> INNER_SW;
                    case FULL -> FULL;
                    default -> NONE;
                };
            }
            return switch (transition) {
                case CORNER_SW -> KITCHEN_CORNER_SW;
                case CORNER_NW -> KITCHEN_CORNER_NW;
                case CORNER_NE -> KITCHEN_CORNER_NE;
                case CORNER_SE -> KITCHEN_CORNER_SE;
                case EDGE_W -> KITCHEN_EDGE_W;
                case EDGE_N -> KITCHEN_EDGE_N;
                case EDGE_E -> KITCHEN_EDGE_E;
                case EDGE_S -> KITCHEN_EDGE_S;
                case INNER_NW -> KITCHEN_INNER_NW;
                case INNER_NE -> KITCHEN_INNER_NE;
                case INNER_SE -> KITCHEN_INNER_SE;
                case INNER_SW -> KITCHEN_INNER_SW;
                case FULL -> KITCHEN_FULL;
                default -> NONE;
            };
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
'''
text = text[:transition_end] + blue_enum + text[transition_end:]
java_path.write_text(text, encoding="utf-8")

variants = {
    "transition=none": {"model": "scp_ublocks:block/sl_1_floor_2"},
    "transition=corner_sw": {"model": "scp_ublocks:block/sl_1_floor_2_a1"},
    "transition=edge_w": {"model": "scp_ublocks:block/sl_1_floor_2_a2"},
    "transition=inner_nw": {"model": "scp_ublocks:block/sl_1_floor_2_a3"},
    "transition=corner_nw": {"model": "scp_ublocks:block/sl_1_floor_2_a4"},
    "transition=edge_n": {"model": "scp_ublocks:block/sl_1_floor_2_a5"},
    "transition=inner_ne": {"model": "scp_ublocks:block/sl_1_floor_2_a6"},
    "transition=corner_ne": {"model": "scp_ublocks:block/sl_1_floor_2_a7"},
    "transition=edge_e": {"model": "scp_ublocks:block/sl_1_floor_2_a8"},
    "transition=inner_se": {"model": "scp_ublocks:block/sl_1_floor_2_a9"},
    "transition=corner_se": {"model": "scp_ublocks:block/sl_1_floor_2_a10"},
    "transition=edge_s": {"model": "scp_ublocks:block/sl_1_floor_2_a11"},
    "transition=inner_sw": {"model": "scp_ublocks:block/sl_1_floor_2_a12"},
    "transition=full": {"model": "scp_ublocks:block/sl_1_floor_1"},
    "transition=kitchen_corner_sw": {"model": "scp_additions:block/sl_1_kitchen_floor_1"},
    "transition=kitchen_edge_w": {"model": "scp_additions:block/sl_1_kitchen_floor_2"},
    "transition=kitchen_inner_nw": {"model": "scp_additions:block/sl_1_kitchen_floor_3"},
    "transition=kitchen_corner_nw": {"model": "scp_additions:block/sl_1_kitchen_floor_4"},
    "transition=kitchen_edge_n": {"model": "scp_additions:block/sl_1_kitchen_floor_5"},
    "transition=kitchen_inner_ne": {"model": "scp_additions:block/sl_1_kitchen_floor_6"},
    "transition=kitchen_corner_ne": {"model": "scp_additions:block/sl_1_kitchen_floor_7"},
    "transition=kitchen_edge_e": {"model": "scp_additions:block/sl_1_kitchen_floor_8"},
    "transition=kitchen_inner_se": {"model": "scp_additions:block/sl_1_kitchen_floor_9"},
    "transition=kitchen_corner_se": {"model": "scp_additions:block/sl_1_kitchen_floor_10"},
    "transition=kitchen_edge_s": {"model": "scp_additions:block/sl_1_kitchen_floor_11"},
    "transition=kitchen_inner_sw": {"model": "scp_additions:block/sl_1_kitchen_floor_12"},
    "transition=kitchen_full": {"model": "scp_additions:block/sl_1_kitchen_floor"},
}
blue_state = json.dumps({"variants": variants}, indent=2) + "\n"
for blue_path in (
    root / "src/main/resources/assets/scp_additions/blockstates/sl_1_floor_2.json",
    root / "src/main/resources/assets/scp_ublocks/blockstates/sl_1_floor_2.json",
):
    blue_path.parent.mkdir(parents=True, exist_ok=True)
    blue_path.write_text(blue_state, encoding="utf-8")

kitchen_state = json.dumps(
    {"variants": {"": {"model": "scp_additions:block/sl_1_kitchen_floor"}}},
    indent=2,
) + "\n"
(root / "src/main/resources/assets/scp_additions/blockstates/sl_1_kitchen_floor.json").write_text(
    kitchen_state,
    encoding="utf-8",
)

changelog_path = root / "CHANGELOG.md"
changelog = changelog_path.read_text(encoding="utf-8")
changelog = replace_once(
    changelog,
    "- Added fixed-orientation automatic transitions from Rest Area Corner Floor to Metal Floor and from Kitchen Corner Floor to Blue Floor, using the original Blue/Metal floor corner, edge, inner-corner, and full-connection mapping.",
    "- Added fixed-orientation automatic transitions from Rest Area Corner Floor to Metal Floor and from Blue Floor to Kitchen Corner Floor, using the original Blue/Metal floor corner, edge, inner-corner, and full-connection mapping.",
    "changelog transition direction",
)
changelog_path.write_text(changelog, encoding="utf-8")
