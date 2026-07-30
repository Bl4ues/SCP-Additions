from pathlib import Path
import json

ROOT = Path('.')


def replace(path: str, old: str, new: str) -> None:
    file = ROOT / path
    text = file.read_text()
    if old not in text:
        raise RuntimeError(f'Expected source block not found in {path}')
    file.write_text(text.replace(old, new, 1))


# Context prompts: use real camera-space projection and never fabricate a
# bottom-center fallback when an anchor is behind the camera.
replace(
    'src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java',
    '''        ScreenPoint point = projectToScreen(minecraft, target.anchor(),
                screenWidth, screenHeight);
        if (point == null) {
            point = new ScreenPoint(screenWidth / 2, screenHeight - 28);
        }
''',
    '''        ScreenPoint point = projectToScreen(minecraft, target.anchor(),
                screenWidth, screenHeight);
        if (point == null) return;
''')
replace(
    'src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java',
    '''        double z = transformed.z();
        double depth = Math.abs(z);
        if (depth < 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                - transformed.x() * scale / depth);
        int y = z < 0.0D ? screenHeight - 28
                : (int) Math.round(screenHeight / 2.0D
                - transformed.y() * scale / depth);
        return new ScreenPoint(x, y);
''',
    '''        double depth = -transformed.z();
        if (depth <= 0.05D) return null;
        double fov = minecraft.options.fov().get();
        double scale = screenHeight
                / (2.0D * Math.tan(Math.toRadians(fov) / 2.0D));
        int x = (int) Math.round(screenWidth / 2.0D
                + transformed.x() * scale / depth);
        int y = (int) Math.round(screenHeight / 2.0D
                - transformed.y() * scale / depth);
        return new ScreenPoint(x, y);
''')

# Carriage collision: the authored doorway is on model-local -X. Convert that
# frame to the station's -Z frame before applying the station direction. Drop
# the oversized swept horizontal solver that repelled players before contact.
carriage_path = ROOT / 'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorCarriageEntity.java'
carriage = carriage_path.read_text()
carriage = carriage.replace(
    '''            resolveSweptFloorCollision(entity, previous);
            resolveSweptHorizontalCollision(entity, previous);
            resolveShellCollision(entity);
''',
    '''            resolveSweptFloorCollision(entity, previous);
            resolveShellCollision(entity);
''', 1)
method_start = carriage.find('    private void resolveSweptHorizontalCollision(Entity entity, Vec3 previous) {')
method_end = carriage.find('    private void playCabinFootstep', method_start)
if method_start < 0 or method_end < 0:
    raise RuntimeError('Swept horizontal collision method not found')
carriage = carriage[:method_start] + carriage[method_end:]
old_shell = '''    private List<AABB> shellBoxes() {
        List<AABB> local = new ArrayList<>();
        local.add(new AABB(-0.82D, -0.20D, -0.82D,
                0.82D, FLOOR_TOP, 0.82D));
        local.add(new AABB(-0.82D, 3.06D, -0.82D,
                0.82D, 3.32D, 0.82D));
        local.add(new AABB(-0.84D, 0.0D, -0.82D,
                -0.72D, 3.08D, 0.82D));
        local.add(new AABB(0.72D, 0.0D, -0.82D,
                0.84D, 3.08D, 0.82D));
        local.add(new AABB(-0.82D, 0.0D, 0.72D,
                0.82D, 3.08D, 0.84D));
        if (isDoorCollisionSolid()) {
            local.add(new AABB(-0.72D, 0.0D, -0.84D,
                    0.72D, 2.35D, -0.72D));
        }
        List<AABB> world = new ArrayList<>();
        for (AABB box : local) {
            AABB rotated = CoreRoomElevatorGeometry.rotateAabb(box,
                    facing(), 0.0D, 0.0D);
            world.add(rotated.move(getX(), getY(), getZ()));
        }
        return world;
    }
'''
new_shell = '''    private List<AABB> shellBoxes() {
        List<AABB> local = new ArrayList<>();
        local.add(new AABB(-0.8125D, -0.203125D, -0.8125D,
                0.8125D, FLOOR_TOP, 0.8125D));
        local.add(new AABB(-0.8125D, 2.5625D, -0.8125D,
                0.8125D, 3.3125D, 0.8125D));
        local.add(new AABB(0.71875D, 0.0D, -0.8125D,
                0.84375D, 3.08D, 0.8125D));
        local.add(new AABB(-0.8125D, 0.0D, -0.84375D,
                0.8125D, 3.08D, -0.71875D));
        local.add(new AABB(-0.8125D, 0.0D, 0.71875D,
                0.8125D, 3.08D, 0.84375D));
        if (isDoorCollisionSolid()) {
            local.add(new AABB(-0.84375D, 0.0D, -0.71875D,
                    -0.71875D, 2.35D, 0.71875D));
        }
        List<AABB> world = new ArrayList<>();
        for (AABB box : local) {
            AABB modelAligned = CoreRoomElevatorGeometry.rotateAabb(box,
                    Direction.EAST, 0.0D, 0.0D);
            AABB facingAligned = CoreRoomElevatorGeometry.rotateAabb(
                    modelAligned, facing(), 0.0D, 0.0D);
            world.add(facingAligned.move(getX(), getY(), getZ()));
        }
        return world;
    }
'''
if old_shell not in carriage:
    raise RuntimeError('Carriage shell block not found')
carriage = carriage.replace(old_shell, new_shell, 1)
old_anchor = '''        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing(), modelX, modelY, modelZ);
        return position().add(facingRotated);
'''
new_anchor = '''        Vec3 modelAligned = CoreRoomElevatorGeometry.rotateLocalVector(
                Direction.EAST, modelX, modelY, modelZ);
        Vec3 facingRotated = CoreRoomElevatorGeometry.rotateLocalVector(
                facing(), modelAligned.x, modelAligned.y, modelAligned.z);
        return position().add(facingRotated);
'''
if old_anchor not in carriage:
    raise RuntimeError('Carriage prompt anchor block not found')
carriage_path.write_text(carriage.replace(old_anchor, new_anchor, 1))

# Preserve alpha while culling duplicate backfaces from thin window planes.
client_path = ROOT / 'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorClient.java'
client = client_path.read_text()
if client.count('RenderType.entityTranslucent(texture)') != 2:
    raise RuntimeError('Unexpected elevator translucent render count')
client_path.write_text(client.replace('RenderType.entityTranslucent(texture)',
                                      'RenderType.entityTranslucentCull(texture)'))

# Pulley model and collision use the exact beam coordinates. The remaining
# discrepancy was one quarter of a Blockbench unit after the previous shift.
pulley_path = ROOT / 'src/main/resources/assets/scp_additions/geo/block/core_room_elevator_pulley.geo.json'
pulley = json.loads(pulley_path.read_text())
for bone in pulley['minecraft:geometry'][0]['bones']:
    if bone.get('name') != 'pulley':
        continue
    for cube in bone.get('cubes', []):
        origin = cube.get('origin')
        size = cube.get('size')
        if not origin or not size or size[1] != 16:
            continue
        if abs(origin[0] + 15.5) < 1e-6:
            origin[0] = -15.75
        elif abs(origin[0] - 13.25) < 1e-6:
            origin[0] = 13.0
pulley_path.write_text(json.dumps(pulley, separators=(',', ':')))
replace(
    'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorGeometry.java',
    '''            modelBox(-16, 0, -15.5, -13, 16, -12),
            modelBox(-14.5, 0, 13.25, -13, 16, 15.25),
            modelBox(13, 0, 13.25, 14.5, 16, 15.25),
            modelBox(13, 0, -15.5, 16, 16, -12)
''',
    '''            modelBox(-16, 0, -15.75, -13, 16, -12.25),
            modelBox(-14.5, 0, 13, -13, 16, 15),
            modelBox(13, 0, 13, 14.5, 16, 15),
            modelBox(13, 0, -15.75, 16, 16, -12.25)
''')

# Tooltips carry the construction section; display names stay short.
replace(
    'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java',
    '''            tooltip.add(Component.translatable(tooltipKey)
                    .withStyle(ChatFormatting.GRAY));
''',
    '''            tooltip.add(Component.translatable(
                    "tooltip.scp_additions.core_room")
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(tooltipKey)
                    .withStyle(ChatFormatting.GRAY));
''')
replace(
    'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java',
    '''        private static final VoxelShape FLOOR_SHAPE = Block.box(
                0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
''',
    '''        private static final VoxelShape FLOOR_SHAPE = Block.box(
                0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D);
''')
replace(
    'src/main/java/net/mcreator/scpadditions/facility/FacilitySignBlockItem.java',
    '''        String prefix = type == FacilitySignBlock.SignType.CORE_ROOM
                ? "core_room_sign" : "door_sign";
''',
    '''        String prefix = type == FacilitySignBlock.SignType.CORE_ROOM
                ? "core_room_sign" : "door_sign";
        if (type == FacilitySignBlock.SignType.CORE_ROOM) {
            tooltip.add(Component.translatable(
                    "tooltip.scp_additions.core_room")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
''')

lang_path = ROOT / 'src/main/resources/assets/scp_additions/lang/en_us_3_0.json'
lang = json.loads(lang_path.read_text())
lang.update({
    'block.scp_additions.core_room_elevator_station': 'Elevator Station',
    'block.scp_additions.core_room_elevator_pulley': 'Elevator Pulley',
    'block.scp_additions.core_room_elevator_beams': 'Elevator Beams',
    'block.scp_additions.core_room_floor': 'Floor',
    'block.scp_additions.core_room_elevator_structure_part': 'Elevator Structure',
    'entity.scp_additions.core_room_elevator_carriage': 'Elevator Carriage',
    'block.scp_additions.core_room_sign': 'Facility Direction Sign',
    'tooltip.scp_additions.core_room': 'Core Room',
    'tooltip.scp_additions.core_room_sign_primary': 'Editable directional facility sign for nearby sectors and rooms.',
    'tooltip.scp_additions.core_room_sign_secondary': 'Use a Screwdriver'
})
lang_path.write_text(json.dumps(lang, separators=(',', ':'), ensure_ascii=False))

changelog_path = ROOT / 'CHANGELOG.md'
changelog = changelog_path.read_text()
entry = '''\n### Core Room elevator fixes\n- Corrected contextual prompt projection and button anchor alignment.\n- Rebuilt carriage collision around the authored doorway and removed the aggressive horizontal sweep that repelled players.\n- Corrected pulley-to-beam alignment, translucent backface handling, positional elevator audio assets, and Core Room floor collision thickness.\n- Simplified Core Room block display names and moved the section label into tooltips; renamed the editable sign to Facility Direction Sign.\n'''
if '### Core Room elevator fixes' not in changelog:
    marker = '## 3.1.0'
    index = changelog.find(marker)
    if index < 0:
        changelog = entry + changelog
    else:
        line_end = changelog.find('\n', index)
        changelog = changelog[:line_end + 1] + entry + changelog[line_end + 1:]
    changelog_path.write_text(changelog)

# Static contracts before Gradle catches Java/API mistakes.
assert 'double depth = -transformed.z();' in (ROOT / 'src/main/java/com/bl4ues/scpinventory/client/ContextPromptClient.java').read_text()
assert 'resolveSweptHorizontalCollision' not in carriage_path.read_text()
assert 'Direction.EAST, modelX, modelY, modelZ' in carriage_path.read_text()
assert '0.0D, 14.0D, 0.0D' in (ROOT / 'src/main/java/net/mcreator/scpadditions/facility/elevator/CoreRoomElevatorModule.java').read_text()
