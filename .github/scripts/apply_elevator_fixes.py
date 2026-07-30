from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java"
RES = ROOT / "src/main/resources"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise RuntimeError(f"Unable to locate {label}")
    return text.replace(old, new, 1)


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, data: dict) -> None:
    write(path, json.dumps(data, ensure_ascii=False, separators=(",", ":")) + "\n")


# ---------------------------------------------------------------------------
# Render orientation and alpha handling
# ---------------------------------------------------------------------------
client_path = JAVA / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorClient.java"
client = client_path.read_text(encoding="utf-8")
client = replace_once(
    client,
    "                root.setRotY(rotationFor(animatable.getBlockState()\n"
    "                        .getValue(CoreRoomElevatorModule.FACING)));\n",
    "                root.setRotY((float) (-Math.PI / 2.0D)\n"
    "                        + rotationFor(animatable.getBlockState()\n"
    "                        .getValue(CoreRoomElevatorModule.FACING)));\n",
    "station authored quarter-turn",
)
client = replace_once(
    client,
    "                root.setRotY((float) (-Math.PI / 2.0D)\n"
    "                        + rotationFor(animatable.getBlockState()\n"
    "                        .getValue(CoreRoomElevatorModule.FACING)));\n",
    "                root.setRotY(rotationFor(animatable.getBlockState()\n"
    "                        .getValue(CoreRoomElevatorModule.FACING)));\n",
    "pulley duplicate quarter-turn",
)
# The first identical pattern above belongs to the station after replacement.
# Replace the next authored double rotation specifically in the carriage block.
carriage_old = (
    "            if (root != null) {\n"
    "                root.setRotY((float) (-Math.PI / 2.0D)\n"
    "                        + rotationFor(animatable.facing()));\n"
    "            }\n"
)
carriage_new = (
    "            if (root != null) {\n"
    "                root.setRotY(rotationFor(animatable.facing()));\n"
    "            }\n"
)
client = replace_once(client, carriage_old, carriage_new,
                      "carriage duplicate quarter-turn")
client = client.replace("RenderType.entityTranslucent(texture)",
                        "RenderType.entityCutoutNoCull(texture)")
if client.count("RenderType.entityCutoutNoCull(texture)") < 3:
    raise RuntimeError("Unable to convert every elevator renderer to cutout")
write(client_path, client)

screens_path = JAVA / "net/mcreator/scpadditions/init/ScpAdditionsModScreens.java"
screens = screens_path.read_text(encoding="utf-8")
screens = replace_once(
    screens,
    "import net.mcreator.scpadditions.client.gui.Scp294GuiScreen;\n",
    "import net.mcreator.scpadditions.client.gui.Scp294GuiScreen;\n"
    "import net.mcreator.scpadditions.facility.elevator.CoreRoomElevatorModule;\n",
    "elevator render-layer import",
)
screens = replace_once(
    screens,
    "\t\t\tItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_ACTIVE_4.get(), RenderType.cutout());\n",
    "\t\t\tItemBlockRenderTypes.setRenderLayer(ScpAdditionsModBlocks.TESLA_ACTIVE_4.get(), RenderType.cutout());\n"
    "\t\t\tItemBlockRenderTypes.setRenderLayer(CoreRoomElevatorModule.FLOOR.get(), RenderType.cutout());\n",
    "Core Room floor cutout layer",
)
write(screens_path, screens)


# ---------------------------------------------------------------------------
# Multiblock orientation and stable station collision
# ---------------------------------------------------------------------------
module_path = JAVA / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java"
module = module_path.read_text(encoding="utf-8")
helper_anchor = (
    "    public static BlockPos rotateOffset(Direction facing, int x, int y, int z) {\n"
    "        return switch (facing) {\n"
    "            case SOUTH -> new BlockPos(-x, y, -z);\n"
    "            case EAST -> new BlockPos(-z, y, x);\n"
    "            case WEST -> new BlockPos(z, y, -x);\n"
    "            default -> new BlockPos(x, y, z);\n"
    "        };\n"
    "    }\n"
)
helper_replacement = helper_anchor + (
    "\n"
    "    static BlockPos rotateStructureOffset(Direction facing,\n"
    "            StructureKind kind, int x, int y, int z) {\n"
    "        if (kind == StructureKind.BEAMS) {\n"
    "            return rotateOffset(facing, x, y, z);\n"
    "        }\n"
    "        BlockPos authored = rotateOffset(Direction.EAST, x, y, z);\n"
    "        return rotateOffset(facing, authored.getX(), authored.getY(),\n"
    "                authored.getZ());\n"
    "    }\n"
)
module = replace_once(module, helper_anchor, helper_replacement,
                      "structure authored rotation helper")
module = module.replace(
    "BlockPos rotated = rotateOffset(facing, cell.x(), cell.y(), cell.z());",
    "BlockPos rotated = rotateStructureOffset(facing, kind, cell.x(),\n"
    "                    cell.y(), cell.z());",
)
if module.count("rotateStructureOffset(facing, kind") < 3:
    raise RuntimeError("Unable to rotate every station/pulley structure cell")
write(module_path, module)

geometry_path = JAVA / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorGeometry.java"
geometry = geometry_path.read_text(encoding="utf-8")
start = geometry.index("    private static final List<AABB> STATION_STATIC = List.of(")
end = geometry.index("\n\n    private static final AABB STATION_GATE", start)
stable_station = '''    private static final List<AABB> STATION_STATIC = List.of(
            // Four load-bearing posts. Thin window and trim planes are visual
            // only so players cannot snag on sub-pixel collision slivers.
            modelBox(13, 0, -15.75, 16, 48, -12.25),
            modelBox(-16, 0, -15.75, -13, 48, -12.25),
            modelBox(13, 0, 13, 14.5, 48, 15),
            modelBox(-14.5, 0, 13, -13, 48, 15),

            // One continuous-height ring matching the visible station deck.
            // Its top is only 1/64 block above the placement plane, avoiding
            // the repeated step-up/down jitter of the old fragmented slabs.
            modelBox(17, 0, -24, 24, 0.25, 24),
            modelBox(-24, 0, -24, -17, 0.25, 24),
            modelBox(-17, 0, 16.5, 17, 0.25, 24),
            modelBox(-12, 0, -29.75, 12, 0.25, -18.5),

            // Solid button-side pedestals.
            modelBox(10, 0, -18.5, 12, 17, -14.25),
            modelBox(-12, 0, -18.5, -10, 17, -14.25)
    );'''
geometry = geometry[:start] + stable_station + geometry[end:]
geometry = replace_once(
    geometry,
    "        return cellShape(boxes, facing, localX, localY, localZ);\n",
    "        return cellShape(boxes, facing, localX, localY, localZ, true);\n",
    "station authored collision rotation",
)
geometry = replace_once(
    geometry,
    "        return cellShape(PULLEY_STATIC, facing, localX, localY, localZ);\n",
    "        return cellShape(PULLEY_STATIC, facing, localX, localY, localZ, true);\n",
    "pulley authored collision rotation",
)
geometry = replace_once(
    geometry,
    "        return cellShape(BEAMS, facing, localX, localY, localZ);\n",
    "        return cellShape(BEAMS, facing, localX, localY, localZ, false);\n",
    "beam collision rotation mode",
)
old_cell = '''    private static VoxelShape cellShape(List<AABB> source, Direction facing,
            int localX, int localY, int localZ) {
        BlockPos rotatedCell = CoreRoomElevatorModule.rotateOffset(facing,
                localX, localY, localZ);
        AABB cell = new AABB(rotatedCell.getX(), rotatedCell.getY(),
                rotatedCell.getZ(), rotatedCell.getX() + 1.0D,
                rotatedCell.getY() + 1.0D, rotatedCell.getZ() + 1.0D);
        VoxelShape result = Shapes.empty();
        for (AABB original : source) {
            AABB rotated = rotateAabb(original, facing, 0.5D, 0.5D);
            AABB clipped = intersect(rotated, cell);
'''
new_cell = '''    private static VoxelShape cellShape(List<AABB> source, Direction facing,
            int localX, int localY, int localZ,
            boolean authoredQuarterTurn) {
        CoreRoomElevatorModule.StructureKind kind = authoredQuarterTurn
                ? CoreRoomElevatorModule.StructureKind.STATION
                : CoreRoomElevatorModule.StructureKind.BEAMS;
        BlockPos rotatedCell = authoredQuarterTurn
                ? CoreRoomElevatorModule.rotateStructureOffset(facing, kind,
                localX, localY, localZ)
                : CoreRoomElevatorModule.rotateOffset(facing,
                localX, localY, localZ);
        AABB cell = new AABB(rotatedCell.getX(), rotatedCell.getY(),
                rotatedCell.getZ(), rotatedCell.getX() + 1.0D,
                rotatedCell.getY() + 1.0D, rotatedCell.getZ() + 1.0D);
        VoxelShape result = Shapes.empty();
        for (AABB original : source) {
            AABB authored = authoredQuarterTurn
                    ? rotateAabb(original, Direction.EAST, 0.5D, 0.5D)
                    : original;
            AABB rotated = rotateAabb(authored, facing, 0.5D, 0.5D);
            AABB clipped = intersect(rotated, cell);
'''
geometry = replace_once(geometry, old_cell, new_cell,
                        "cell-shape authored rotation")
geometry = replace_once(
    geometry,
    "        return unrotated;\n    }\n\n    public static Vec3 rotateLocalVector",
    "        // Station geometry is rendered with the same authored -90 degree\n"
    "        // quarter-turn used by the carriage and pulley. Undo it after the\n"
    "        // block-facing transform so physical button hits match the model.\n"
    "        return new Vec3(unrotated.z, unrotated.y, -unrotated.x);\n"
    "    }\n\n    public static Vec3 rotateLocalVector",
    "station inverse authored transform",
)
write(geometry_path, geometry)


# ---------------------------------------------------------------------------
# Icon-only contextual prompts at the actual button locations
# ---------------------------------------------------------------------------
registry_path = JAVA / "com/bl4ues/scpinventory/context/ContextInteractionRegistry.java"
registry = registry_path.read_text(encoding="utf-8")
registry = replace_once(
    registry,
    "            double x = 0.5D + 14.64492D / 16.0D;\n"
    "            double z = 0.5D - 16.69749D / 16.0D;\n",
    "            double modelX = 14.64492D / 16.0D;\n"
    "            double modelZ = -16.69749D / 16.0D;\n"
    "            double x = 0.5D - modelZ;\n"
    "            double z = 0.5D + modelX;\n",
    "station contextual anchor quarter-turn",
)
for action, key in (("Call Up", "elevator_station_up"),
                    ("Call Down", "elevator_station_down"),
                    ("Go Up", "elevator_carriage_up"),
                    ("Go Down", "elevator_carriage_down")):
    old = f'                    "{key}", 2.8D, '
    if old not in registry:
        raise RuntimeError(f"Unable to locate {key} contextual rule")
    registry = registry.replace(f'"{action}",\n                    "Elevator", true, true, false,',
                                '"",\n                    "", false, false, false,', 1)
write(registry_path, registry)


# ---------------------------------------------------------------------------
# Carriage collision: align with the authored model and stop vertical jitter
# ---------------------------------------------------------------------------
carriage_path = JAVA / "net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java"
carriage = carriage_path.read_text(encoding="utf-8")
carriage = replace_once(
    carriage,
    "    private static final double BUTTON_HIT_RADIUS_SQR = 0.32D * 0.32D;\n",
    "    private static final double BUTTON_HIT_RADIUS_SQR = 0.32D * 0.32D;\n"
    "    private static final double FLOOR_TOP = 0.25D / 16.0D;\n"
    "    private static final double COLLISION_EPSILON = 1.0E-4D;\n",
    "stable carriage collision constants",
)
old_standing = '''    private boolean isStandingOnFloor(Entity entity, double oldFloorY) {
        AABB box = entity.getBoundingBox();
        return box.maxX > getX() - 0.74D && box.minX < getX() + 0.74D
                && box.maxZ > getZ() - 0.74D && box.minZ < getZ() + 0.74D
                && box.minY >= oldFloorY - 0.12D
                && box.minY <= oldFloorY + 0.34D;
    }
'''
new_standing = '''    private boolean isStandingOnFloor(Entity entity, double oldFloorY) {
        AABB box = entity.getBoundingBox();
        double floorTop = oldFloorY + FLOOR_TOP;
        return box.maxX > getX() - 0.74D && box.minX < getX() + 0.74D
                && box.maxZ > getZ() - 0.74D && box.minZ < getZ() + 0.74D
                && box.minY >= floorTop - 0.08D
                && box.minY <= floorTop + 0.12D;
    }
'''
carriage = replace_once(carriage, old_standing, new_standing,
                        "standing-on-carriage detection")
method_start = carriage.index("    private void resolveShellCollision(Entity entity) {")
method_end = carriage.index("\n    private List<AABB> shellBoxes()", method_start)
stable_resolution = '''    private void resolveShellCollision(Entity entity) {
        List<AABB> shells = shellBoxes();
        if (shells.size() < 5) return;

        resolveFloorCollision(entity, shells.get(0));
        resolveCeilingCollision(entity, shells.get(1));
        for (int index = 2; index < shells.size(); index++) {
            resolveHorizontalCollision(entity, shells.get(index));
        }
    }

    private void resolveFloorCollision(Entity entity, AABB floor) {
        AABB box = entity.getBoundingBox();
        if (!box.intersects(floor)) return;
        if (box.getCenter().y >= floor.getCenter().y) {
            moveResolved(entity, new Vec3(0.0D,
                    floor.maxY - box.minY + COLLISION_EPSILON, 0.0D));
            entity.setOnGround(true);
            entity.fallDistance = 0.0F;
        } else {
            moveResolved(entity, new Vec3(0.0D,
                    floor.minY - box.maxY - COLLISION_EPSILON, 0.0D));
        }
    }

    private void resolveCeilingCollision(Entity entity, AABB ceiling) {
        AABB box = entity.getBoundingBox();
        if (!box.intersects(ceiling)) return;
        if (box.getCenter().y <= ceiling.getCenter().y) {
            moveResolved(entity, new Vec3(0.0D,
                    ceiling.minY - box.maxY - COLLISION_EPSILON, 0.0D));
        } else {
            moveResolved(entity, new Vec3(0.0D,
                    ceiling.maxY - box.minY + COLLISION_EPSILON, 0.0D));
        }
    }

    private void resolveHorizontalCollision(Entity entity, AABB shell) {
        AABB box = entity.getBoundingBox();
        if (!box.intersects(shell)) return;

        double west = box.maxX - shell.minX;
        double east = shell.maxX - box.minX;
        double north = box.maxZ - shell.minZ;
        double south = shell.maxZ - box.minZ;
        double smallest = west;
        Vec3 push = new Vec3(-west - COLLISION_EPSILON, 0.0D, 0.0D);
        if (east < smallest) {
            smallest = east;
            push = new Vec3(east + COLLISION_EPSILON, 0.0D, 0.0D);
        }
        if (north < smallest) {
            smallest = north;
            push = new Vec3(0.0D, 0.0D,
                    -north - COLLISION_EPSILON);
        }
        if (south < smallest) {
            push = new Vec3(0.0D, 0.0D,
                    south + COLLISION_EPSILON);
        }
        moveResolved(entity, push);
    }

    private void moveResolved(Entity entity, Vec3 displacement) {
        if (displacement.lengthSqr() <= COLLISION_EPSILON
                * COLLISION_EPSILON) return;
        entity.move(MoverType.SHULKER, displacement);
    }
'''
carriage = carriage[:method_start] + stable_resolution + carriage[method_end:]
carriage = carriage.replace(
    "        local.add(new AABB(-0.82D, -0.20D, -0.82D,\n"
    "                0.82D, 0.0D, 0.82D));",
    "        local.add(new AABB(-0.82D, -0.20D, -0.82D,\n"
    "                0.82D, FLOOR_TOP, 0.82D));",
)
carriage = replace_once(
    carriage,
    "        for (AABB box : local) {\n"
    "            AABB rotated = CoreRoomElevatorGeometry.rotateAabb(box, facing(),\n"
    "                    0.0D, 0.0D);\n"
    "            world.add(rotated.move(getX(), getY(), getZ()));\n"
    "        }\n",
    "        for (AABB box : local) {\n"
    "            AABB authored = CoreRoomElevatorGeometry.rotateAabb(box,\n"
    "                    Direction.EAST, 0.0D, 0.0D);\n"
    "            AABB rotated = CoreRoomElevatorGeometry.rotateAabb(authored,\n"
    "                    facing(), 0.0D, 0.0D);\n"
    "            world.add(rotated.move(getX(), getY(), getZ()));\n"
    "        }\n",
    "authored carriage shell rotation",
)
carriage = carriage.replace(
    "        return new AABB(getX() - 0.72D, getY() - 0.05D,\n",
    "        return new AABB(getX() - 0.72D, getY() + FLOOR_TOP - 0.04D,\n",
)
write(carriage_path, carriage)


# ---------------------------------------------------------------------------
# Give exported zero-thickness planes a tiny real depth to eliminate
# self-z-fighting while preserving the authored silhouette.
# ---------------------------------------------------------------------------
def thicken_geo(path: Path) -> None:
    data = load_json(path)
    changed = 0
    for geometry_def in data.get("minecraft:geometry", []):
        for bone in geometry_def.get("bones", []):
            for cube in bone.get("cubes", []):
                origin = cube.get("origin")
                size = cube.get("size")
                if not isinstance(origin, list) or not isinstance(size, list):
                    continue
                for axis in range(min(3, len(size), len(origin))):
                    if abs(float(size[axis])) <= 1.0e-9:
                        origin[axis] = round(float(origin[axis]) - 0.03125, 5)
                        size[axis] = 0.0625
                        changed += 1
    if changed == 0:
        raise RuntimeError(f"No zero-thickness cubes found in {path}")
    write_json(path, data)


for relative in (
    "assets/scp_additions/geo/block/core_room_elevator_floor_station.geo.json",
    "assets/scp_additions/geo/block/core_room_elevator_pulley.geo.json",
    "assets/scp_additions/geo/entity/core_room_elevator_carriage.geo.json",
):
    thicken_geo(RES / relative)

floor_model_path = RES / "assets/scp_additions/models/block/core_room_floor.json"
floor_model = load_json(floor_model_path)
for element in floor_model.get("elements", []):
    lower = element.get("from")
    upper = element.get("to")
    if isinstance(lower, list) and isinstance(upper, list) \
            and len(lower) >= 3 and len(upper) >= 3 \
            and abs(float(upper[1]) - float(lower[1])) <= 1.0e-9:
        lower[1] = round(float(upper[1]) - 0.0625, 5)
write_json(floor_model_path, floor_model)

print("Applied Core Room elevator orientation, rendering, prompt, geometry and collision fixes.")
