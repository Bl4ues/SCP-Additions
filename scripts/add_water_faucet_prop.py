from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FACILITY = ROOT / "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java"
MODEL = ROOT / "src/main/resources/assets/scp_additions/models/block/water_faucet.json"
LANG = ROOT / "src/main/resources/assets/scp_additions/lang/en_us.json"
CHANGELOG = ROOT / "CHANGELOG.md"
WORKFLOW = ROOT / ".github/workflows/add-water-faucet-prop.yml"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected exactly one {label} anchor, found {count}")
    return text.replace(old, new, 1)


facility = FACILITY.read_text(encoding="utf-8")

facility = replace_once(
    facility,
    "import net.minecraft.world.level.LevelAccessor;\n",
    "import net.minecraft.world.level.LevelAccessor;\nimport net.minecraft.world.level.LevelReader;\n",
    "LevelAccessor import",
)
facility = replace_once(
    facility,
    "import net.minecraft.world.level.block.Block;\n",
    "import net.minecraft.world.level.block.Block;\nimport net.minecraft.world.level.block.Blocks;\n",
    "Block import",
)
facility = replace_once(
    facility,
    "    public static final RegistryObject<Block> WET_FLOOR = registerWetFloor();\n",
    "    public static final RegistryObject<Block> WET_FLOOR = registerWetFloor();\n"
    "    public static final RegistryObject<Block> WATER_FAUCET = registerBlock(\n"
    "            \"water_faucet\", WaterFaucetBlock::new, true);\n",
    "Wet Floor registration",
)
facility = replace_once(
    facility,
    "        addFacilityCreativeItem(props, \"wet_floor\");\n"
    "        addFacilityCreativeItem(props, \"tv\");\n",
    "        addFacilityCreativeItem(props, \"wet_floor\");\n"
    "        addFacilityCreativeItem(props, \"water_faucet\");\n"
    "        addFacilityCreativeItem(props, \"tv\");\n",
    "props ordering",
)
facility = replace_once(
    facility,
    "                || \"fire_extinguisher\".equals(path)\n"
    "                || \"trashbin\".equals(path);\n",
    "                || \"fire_extinguisher\".equals(path)\n"
    "                || \"water_faucet\".equals(path)\n"
    "                || \"trashbin\".equals(path);\n",
    "decorative prop list",
)

water_faucet_class = '''    private static final class WaterFaucetBlock extends WallMountedWaterloggedPropBlock {
        private static final VoxelShape NORTH_SHAPE = Shapes.or(
                box(4.75, 4.0, 12.8, 11.25, 7.0, 15.8),
                box(3.9, 2.0, 11.7, 7.1, 8.8, 16.5),
                box(8.9, 2.0, 11.7, 12.1, 8.8, 16.5));

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

'''
facility = replace_once(
    facility,
    "    private static VoxelShape horizontalShape(Direction facing, VoxelShape north) {\n",
    water_faucet_class
    + "    private static VoxelShape horizontalShape(Direction facing, VoxelShape north) {\n",
    "horizontalShape method",
)
FACILITY.write_text(facility, encoding="utf-8")

model = MODEL.read_text(encoding="utf-8")
model = replace_once(
    model,
    '\t\t"0": "water_faucet",\n\t\t"particle": "water_faucet"',
    '\t\t"0": "scp_additions:block/water_faucet",\n'
    '\t\t"particle": "scp_additions:block/water_faucet"',
    "Water Faucet texture references",
)
if '"render_type"' not in model:
    model = replace_once(
        model,
        '\t"elements": [\n',
        '\t"render_type": "cutout",\n\t"elements": [\n',
        "model elements",
    )
MODEL.write_text(model, encoding="utf-8")

blockstate = '''{
  "variants": {
    "facing=north,waterlogged=false": {"model": "scp_additions:block/water_faucet"},
    "facing=north,waterlogged=true": {"model": "scp_additions:block/water_faucet"},
    "facing=east,waterlogged=false": {"model": "scp_additions:block/water_faucet", "y": 90},
    "facing=east,waterlogged=true": {"model": "scp_additions:block/water_faucet", "y": 90},
    "facing=south,waterlogged=false": {"model": "scp_additions:block/water_faucet", "y": 180},
    "facing=south,waterlogged=true": {"model": "scp_additions:block/water_faucet", "y": 180},
    "facing=west,waterlogged=false": {"model": "scp_additions:block/water_faucet", "y": 270},
    "facing=west,waterlogged=true": {"model": "scp_additions:block/water_faucet", "y": 270}
  }
}
'''
item_model = '''{
  "parent": "scp_additions:block/water_faucet"
}
'''
loot_table = '''{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "scp_additions:water_faucet"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ]
    }
  ]
}
'''

resources = {
    ROOT / "src/main/resources/assets/scp_additions/blockstates/water_faucet.json": blockstate,
    ROOT / "src/main/resources/assets/scp_additions/models/item/water_faucet.json": item_model,
    ROOT / "src/main/resources/data/scp_additions/loot_tables/blocks/water_faucet.json": loot_table,
}
for path, content in resources.items():
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")

lang = LANG.read_text(encoding="utf-8")
if '"block.scp_additions.water_faucet"' not in lang:
    closing = lang.rfind("}")
    if closing < 0:
        raise RuntimeError("en_us.json has no closing brace")
    prefix = lang[:closing].rstrip()
    if not prefix.endswith("{"):
        prefix += ","
    lang = prefix + '\n  "block.scp_additions.water_faucet": "Non-potable Water Faucet"\n}\n'
LANG.write_text(lang, encoding="utf-8")

changelog = CHANGELOG.read_text(encoding="utf-8")
changelog = replace_once(
    changelog,
    "- Added decorative Emergency Button, Fire Extinguisher, and Wet Floor Sign facility props;",
    "- Added decorative Emergency Button, Fire Extinguisher, Wet Floor Sign, and Non-potable Water Faucet facility props;",
    "facility props changelog line",
)
CHANGELOG.write_text(changelog, encoding="utf-8")

# Do not leave migration machinery in the repository.
Path(__file__).unlink()
if WORKFLOW.exists():
    WORKFLOW.unlink()
