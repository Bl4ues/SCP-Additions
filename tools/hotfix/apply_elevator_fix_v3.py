import json
import re
from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


# SCP-330: enlarge another 25%, scaling upward from the lowest authored Y.
geo_path = Path("src/main/resources/assets/scp_additions/geo/block/scp330.geo.json")
geo = json.loads(geo_path.read_text(encoding="utf-8"))
scale = 1.25
for geometry in geo.get("minecraft:geometry", []):
    description = geometry.get("description", {})
    width = float(description.get("visible_bounds_width", 0.0))
    if not 2.75 <= width <= 2.85:
        raise SystemExit(f"Unexpected SCP-330 width before scaling: {width}")

    cubes = [
        cube
        for bone in geometry.get("bones", [])
        for cube in bone.get("cubes", [])
        if isinstance(cube.get("origin"), list)
    ]
    base_y = min(float(cube["origin"][1]) for cube in cubes)

    def scale_position(vector: object) -> None:
        if not isinstance(vector, list) or len(vector) < 3:
            return
        vector[0] = round(float(vector[0]) * scale, 6)
        vector[1] = round(base_y + (float(vector[1]) - base_y) * scale, 6)
        vector[2] = round(float(vector[2]) * scale, 6)

    def scale_size(vector: object) -> None:
        if not isinstance(vector, list) or len(vector) < 3:
            return
        for index in range(3):
            vector[index] = round(float(vector[index]) * scale, 6)

    description["visible_bounds_width"] = round(width * scale, 6)
    if "visible_bounds_height" in description:
        description["visible_bounds_height"] = round(
            float(description["visible_bounds_height"]) * scale, 6
        )
    if isinstance(description.get("visible_bounds_offset"), list):
        description["visible_bounds_offset"][0] = round(
            float(description["visible_bounds_offset"][0]) * scale, 6
        )
        description["visible_bounds_offset"][1] = round(
            float(description["visible_bounds_offset"][1]) * scale, 6
        )
        description["visible_bounds_offset"][2] = round(
            float(description["visible_bounds_offset"][2]) * scale, 6
        )

    for bone in geometry.get("bones", []):
        scale_position(bone.get("pivot"))
        for cube in bone.get("cubes", []):
            scale_position(cube.get("origin"))
            scale_size(cube.get("size"))
            scale_position(cube.get("pivot"))
        for locator in bone.get("locators", {}).values():
            scale_position(locator)

geo_path.write_text(json.dumps(geo, separators=(",", ":")) + "\n", encoding="utf-8")

scp330_path = Path("src/main/java/net/mcreator/scpadditions/block/Scp330Block.java")
scp330 = scp330_path.read_text(encoding="utf-8")
scp330, collision_count = re.subn(
    r"private static final VoxelShape SHAPE = box\(\s*"
    r"5\.1875D, 0\.0D, 5\.1875D,\s*"
    r"10\.8125D, 2\.1875D, 10\.8125D\);",
    """private static final VoxelShape SHAPE = box(
            4.484375D, 0.0D, 4.484375D,
            11.515625D, 2.734375D, 11.515625D);""",
    scp330,
    count=1,
)
if collision_count != 1:
    raise SystemExit(f"SCP-330 collision replacement: {collision_count}")

scp330, facing_count = re.subn(
    r"(public BlockState getStateForPlacement\(BlockPlaceContext context\) \{\s*"
    r"return defaultBlockState\(\)\.setValue\(FACING,\s*)"
    r"context\.getHorizontalDirection\(\)(\);\s*\})",
    r"\1context.getHorizontalDirection().getOpposite()\2",
    scp330,
    count=1,
    flags=re.S,
)
if facing_count != 1:
    raise SystemExit(f"SCP-330 placement facing replacement: {facing_count}")
scp330_path.write_text(scp330, encoding="utf-8")

# Floor station: mirror button anchors to the visible right-side panel and add
# the missing railings around the carriage opening. The animated gate remains a
# separate AABB, so open and closed collision states stay distinct.
geometry_path = Path(
    "src/main/java/net/mcreator/scpadditions/facility/elevator/"
    "CoreRoomElevatorGeometry.java"
)
geometry = geometry_path.read_text(encoding="utf-8")
button_old = "private static final double STATION_BUTTON_X = 14.64492D / 16.0D;"
button_new = "private static final double STATION_BUTTON_X = -14.64492D / 16.0D;"
if geometry.count(button_old) != 1:
    raise SystemExit("Station button X anchor not found")
geometry = geometry.replace(button_old, button_new, 1)

rail_marker = """            modelBox(-12, 0, -18.5, -10, 17, -14.25)
    );"""
rail_replacement = """            modelBox(-12, 0, -18.5, -10, 17, -14.25),
            // Low glass/metal railings surrounding the carriage opening.
            modelBox(-17, 0, 16.25, 17, 13.5, 17),
            modelBox(-17, 0, -16.75, -16.25, 13.5, 17),
            modelBox(16.25, 0, -16.75, 17, 13.5, 17),
            modelBox(-17, 0, -16.75, -12, 13.5, -16.25),
            modelBox(12, 0, -16.75, 17, 13.5, -16.25)
    );"""
if geometry.count(rail_marker) != 1:
    raise SystemExit("Station railing insertion point not found")
geometry_path.write_text(
    geometry.replace(rail_marker, rail_replacement, 1), encoding="utf-8"
)

# Generated beam controllers and their multiblock parts are shaft structure,
# not user-removable decoration.
module_path = Path(
    "src/main/java/net/mcreator/scpadditions/facility/elevator/"
    "CoreRoomElevatorModule.java"
)
module = module_path.read_text(encoding="utf-8")
beam_start = module.index("    public static final class BeamBlock")
beam_end = module.index("    public static final class CoreRoomFloorBlock", beam_start)
beam_section = module[beam_start:beam_end]
if beam_section.count(".strength(5.0F, 15.0F)") != 1:
    raise SystemExit("Beam master strength not found exactly once")
beam_section = beam_section.replace(
    ".strength(5.0F, 15.0F)", ".strength(-1.0F, 3600000.0F)", 1
)
module = module[:beam_start] + beam_section + module[beam_end:]

part_start = module.index("    private static final class BeamStructurePartBlock")
part_end = module.index(
    "    public static final class StructurePartBlockEntity", part_start
)
part_section = module[part_start:part_end]
if part_section.count(".strength(5.0F, 15.0F)") != 1:
    raise SystemExit("Beam part strength not found exactly once")
part_section = part_section.replace(
    ".strength(5.0F, 15.0F)", ".strength(-1.0F, 3600000.0F)", 1
)
module_path.write_text(
    module[:part_start] + part_section + module[part_end:], encoding="utf-8"
)

# Both windowed elevator models require alpha blending. The station is already
# translucent; restore the carriage renderer to the same render type.
client_path = Path(
    "src/main/java/net/mcreator/scpadditions/facility/elevator/"
    "CoreRoomElevatorClient.java"
)
client = client_path.read_text(encoding="utf-8")
carriage_start = client.index("    public static final class CarriageRenderer")
carriage_renderer = client[carriage_start:]
if carriage_renderer.count("return RenderType.entityCutoutNoCull(texture);") != 1:
    raise SystemExit("Carriage cutout render call not found exactly once")
carriage_renderer = carriage_renderer.replace(
    "return RenderType.entityCutoutNoCull(texture);",
    "return RenderType.entityTranslucentCull(texture);",
    1,
)
client_path.write_text(client[:carriage_start] + carriage_renderer, encoding="utf-8")

# Run shell and floor correction on the client too. Local movement prediction
# otherwise crosses the non-block entity floor before the server correction is
# visible. Door collision remains conditional through isDoorCollisionSolid().
carriage_path = Path(
    "src/main/java/net/mcreator/scpadditions/facility/elevator/"
    "CoreRoomElevatorCarriageEntity.java"
)
carriage = carriage_path.read_text(encoding="utf-8")
client_return_old = "        if (level().isClientSide) return;"
client_return_new = """        if (level().isClientSide) {
            previousServerY = getY();
            resolveNearbyEntities(0.0D);
            return;
        }"""
if carriage.count(client_return_old) != 1:
    raise SystemExit("Carriage client tick return not found")
carriage = carriage.replace(client_return_old, client_return_new, 1)

search_old = "cabinOuterBox().inflate(0.45D, 0.45D, 0.45D)"
search_new = "cabinOuterBox().inflate(0.45D, 1.50D, 0.45D)"
if carriage.count(search_old) != 1:
    raise SystemExit("Carriage collision search box not found")
carriage_path.write_text(carriage.replace(search_old, search_new, 1), encoding="utf-8")

# Context prompts must discover the station through any nearby multiblock part,
# since the controller block can be several blocks from the visible button.
prompt_path = Path(
    "src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java"
)
prompt = prompt_path.read_text(encoding="utf-8")
precise_old = (
    "private static final double PRECISE_AIM_RADIUS_SQR = 0.52D * 0.52D;"
)
precise_new = (
    "private static final double PRECISE_AIM_RADIUS_SQR = 0.60D * 0.60D;"
)
if prompt.count(precise_old) != 1:
    raise SystemExit("Precise prompt radius constant not found")
prompt = prompt.replace(precise_old, precise_new, 1)

loop_start = prompt.index("            BlockPos pos = mutable.immutable();")
loop_end_marker = "            }\n        }\n        return best;"
loop_end = prompt.index(loop_end_marker, loop_start) + len("            }\n")
loop_replacement = """            BlockPos scannedPos = mutable.immutable();
            BlockState scannedState = player.level().getBlockState(scannedPos);
            BlockPos rulePos = scannedPos;
            BlockState ruleState = scannedState;
            if (player.level().getBlockEntity(scannedPos)
                    instanceof CoreRoomElevatorModule.StructurePartBlockEntity part) {
                BlockPos master = part.masterPos();
                BlockState masterState = player.level().getBlockState(master);
                if (masterState.getBlock()
                        instanceof CoreRoomElevatorModule.StationBlock) {
                    rulePos = master;
                    ruleState = masterState;
                }
            }
            List<ContextInteractionRegistry.Rule> rules =
                    ContextInteractionRegistry.getBlockRules(ruleState.getBlock());
            if (rules.isEmpty()) continue;
            boolean directHit = blockHit != null
                    && hitBelongsTo(blockHit.getBlockPos(), rulePos, player);
            for (ContextInteractionRegistry.Rule rule : rules) {
                if (!rule.isAvailable(player.level(), rulePos, ruleState)) continue;
                Vec3 anchor = rule.resolveBlockAnchor(rulePos, ruleState);
                double score = scorePoint(anchor, eye, look, rule.range(),
                        directHit, rule.priority(), rule.requiresPreciseAim());
                if (isCurrentBlockTarget(rulePos, rule.interactionKey())) {
                    score -= TARGET_STICKINESS_BONUS;
                }
                if (score < bestScore && hasBlockLineOfSight(player, eye,
                        anchor, rulePos, directHit)) {
                    bestScore = score;
                    String name = rule.showName()
                            ? rule.blockName(ruleState) : "";
                    boolean showName = rule.showName() && !name.isEmpty();
                    boolean showAction = rule.showAction() && showName;
                    ResourceLocation icon = ContextPromptIcons.resolve(
                            rule.icon(), rule.id());
                    best = new ContextTarget(rulePos, 0, false, anchor,
                            rule.interactionKey(), rule.action(), name,
                            showAction, showName, rule.allowE(),
                            rule.allowRightClick(), icon,
                            (float) rule.promptScale(), score);
                }
            }
"""
prompt_path.write_text(
    prompt[:loop_start] + loop_replacement + prompt[loop_end:], encoding="utf-8"
)

# Requested prop order in the facility creative tab.
facility_path = Path(
    "src/main/java/net/mcreator/scpadditions/facility/FacilityModule.java"
)
replace_once(
    facility_path,
    """        addFacilityCreativeItem(props, "wet_floor");
        addFacilityCreativeItem(props, "water_faucet");
        addFacilityCreativeItem(props, "scp_914_usage_notice");
        addFacilityCreativeItem(props, "tv");
        addFacilityCreativeItem(props, "trashbin");""",
    """        addFacilityCreativeItem(props, "water_faucet");
        addFacilityCreativeItem(props, "wet_floor");
        addFacilityCreativeItem(props, "trashbin");
        addFacilityCreativeItem(props, "scp_914_usage_notice");
        addFacilityCreativeItem(props, "tv");""",
    "Creative prop order",
)
