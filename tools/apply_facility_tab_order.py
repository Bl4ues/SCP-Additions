from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label}, found {count}")
    return text.replace(old, new, 1)


ublocks_path = Path("src/main/java/net/mcreator/scpadditions/facility/UBlocksModule.java")
ublocks = ublocks_path.read_text(encoding="utf-8")

ublocks = replace_once(
    ublocks,
    '''    // Props.\n    public static final RegistryObject<Block> BENCH = directional(\n            "bench", DirectionalShape.BENCH, SoundType.METAL);\n    public static final RegistryObject<Block> VENT_OPEN = directional(\n            "vent_open", DirectionalShape.VENT, SoundType.METAL);''',
    '''    // Props.\n    public static final RegistryObject<Block> VENT_OPEN = directional(\n            "vent_open", DirectionalShape.VENT, SoundType.METAL);''',
    "bench registry declaration",
)

ublocks = replace_once(
    ublocks,
    '''        @Override\n        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,\n                BlockPos pos, CollisionContext context) {\n            return shape == DirectionalShape.BENCH\n                    ? shape.outline(state.getValue(FACING)) : Shapes.empty();\n        }''',
    '''        @Override\n        public VoxelShape getCollisionShape(BlockState state, BlockGetter level,\n                BlockPos pos, CollisionContext context) {\n            return Shapes.empty();\n        }''',
    "bench collision branch",
)

ublocks = replace_once(
    ublocks,
    '''    private enum DirectionalShape {\n        BENCH,\n        FLOOR_DECAL,''',
    '''    private enum DirectionalShape {\n        FLOOR_DECAL,''',
    "bench enum value",
)

ublocks = replace_once(
    ublocks,
    '''            return switch (this) {\n                case BENCH -> facing.getAxis() == Direction.Axis.X\n                        ? Block.box(2.0D, 0.0D, 0.0D, 14.0D, 9.0D, 16.0D)\n                        : Block.box(0.0D, 0.0D, 2.0D, 16.0D, 9.0D, 14.0D);\n                case FLOOR_DECAL ->''',
    '''            return switch (this) {\n                case FLOOR_DECAL ->''',
    "bench voxel shape",
)

if '"bench"' in ublocks:
    raise RuntimeError("Bench still appears in UBlocksModule after patch")

ublocks_path.write_text(ublocks, encoding="utf-8")

facility_path = Path("src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java")
facility = facility_path.read_text(encoding="utf-8")

facility = replace_once(
    facility,
    '''                    .displayItems((parameters, output) -> {\n                        UBlocksModule.creativeItems().forEach(item -> output.accept(item.get()));\n                        CREATIVE_ITEMS.forEach(item -> output.accept(item.get()));\n                    })''',
    '''                    .displayItems((parameters, output) ->\n                            creativeItemsInDisplayOrder().forEach(item -> output.accept(item.get())))''',
    "creative tab display callback",
)

anchor = '''    public static RegistryObject<Item> itemByPath(String path) {\n        return ITEMS_BY_PATH.get(path);\n    }\n'''
addition = '''    public static RegistryObject<Item> itemByPath(String path) {\n        return ITEMS_BY_PATH.get(path);\n    }\n\n    /**\n     * Stable, curated order for the public facility tab. Registration order is\n     * intentionally not used here because animation states and compatibility\n     * entries are interleaved with the public endpoints.\n     */\n    private static List<RegistryObject<Item>> creativeItemsInDisplayOrder() {\n        List<RegistryObject<Item>> ordered = new ArrayList<>();\n\n        // Frequently used facility props.\n        addFacilityCreativeItem(ordered, "walllight");\n        addFacilityCreativeItem(ordered, "heater");\n        addFacilityCreativeItem(ordered, "sign_support");\n        addFacilityCreativeItem(ordered, "tv");\n        addFacilityCreativeItem(ordered, "trashbin");\n\n        // Public closed endpoints for every door family, preserving the\n        // original family order from this module.\n        addFacilityCreativeItem(ordered, "default_door");\n        addFacilityCreativeItem(ordered, "yellow_closed");\n        addFacilityCreativeItem(ordered, "black_closed");\n        addFacilityCreativeItem(ordered, "normal_door");\n        addFacilityCreativeItem(ordered, "left_log_door");\n        addFacilityCreativeItem(ordered, "right_log_door");\n        addFacilityCreativeItem(ordered, "office_door");\n        addFacilityCreativeItem(ordered, "bath_door");\n        addFacilityCreativeItem(ordered, "ws_dclosed");\n\n        // Main SL1 navigation and wall-detail pieces.\n        addUBlockCreativeItem(ordered, "sl_1_floor_detail_small");\n        addUBlockCreativeItem(ordered, "sl_1_floor_detail_big");\n        addUBlockCreativeItem(ordered, "sl_1_wall_detail_1_bot");\n        addUBlockCreativeItem(ordered, "sl_1_wall_detail_1_mid");\n        addUBlockCreativeItem(ordered, "sl_1_wall_detail_1_top");\n        addUBlockCreativeItem(ordered, "sl_1_wall_detail_2");\n\n        // Preserve the relative order of everything else and suppress entries\n        // already placed above.\n        UBlocksModule.creativeItems().forEach(item -> addUnique(ordered, item));\n        CREATIVE_ITEMS.forEach(item -> addUnique(ordered, item));\n        return ordered;\n    }\n\n    private static void addFacilityCreativeItem(List<RegistryObject<Item>> ordered, String path) {\n        RegistryObject<Item> item = ITEMS_BY_PATH.get(path);\n        if (item != null) addUnique(ordered, item);\n    }\n\n    private static void addUBlockCreativeItem(List<RegistryObject<Item>> ordered, String path) {\n        RegistryObject<Item> item = UBlocksModule.itemByPath(path);\n        if (item != null) addUnique(ordered, item);\n    }\n\n    private static void addUnique(List<RegistryObject<Item>> ordered, RegistryObject<Item> item) {\n        if (!ordered.contains(item)) ordered.add(item);\n    }\n'''
facility = replace_once(facility, anchor, addition, "itemByPath insertion point")

facility_path.write_text(facility, encoding="utf-8")
