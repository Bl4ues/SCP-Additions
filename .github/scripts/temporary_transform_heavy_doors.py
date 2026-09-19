from pathlib import Path
root = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')

def patch(filename, changes):
    path = root / filename
    source = path.read_text(encoding='utf-8')
    for old, new in changes:
        count = source.count(old)
        if count != 1:
            raise RuntimeError(f'{filename}: expected one anchor, found {count}: {old[:100]}')
        source = source.replace(old, new, 1)
    path.write_text(source, encoding='utf-8')

patch('TransformDoorRuntime.java', [
    ('import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;\n',
     'import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;\nimport com.bl4ues.scpclassifieddirective.facility.HeavyDoorPowerRelay;\n'),
    ('''            boolean powered = TransformPowerQuery.powered(level,
                    group, ref.cell());
            if (address.stage() == DoorStage.CLOSED && powered) {''',
     '''            boolean powered = TransformPowerQuery.powered(level,
                    group, ref.cell());
            // Heavy door controllers are stored in their lower cell. Their
            // upper frame and button positions are real adjacent local cells,
            // not vanilla-world relays when this door is transformed.
            if (!powered && HeavyDoorPowerRelay.isHeavyDoorState(
                    state.getBlock())) {
                for (int up = 1; up <= 2 && !powered; up++) {
                    powered = TransformPowerQuery.powered(level, group,
                            ref.cell().offset(0, up, 0));
                }
            }
            if (address.stage() == DoorStage.CLOSED && powered) {''')
])

patch('TransformSurfaceDoorRuntime.java', [
    ('import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;\n',
     'import com.bl4ues.scpclassifieddirective.facility.FacilityModule.DoorStage;\nimport com.bl4ues.scpclassifieddirective.facility.HeavyDoorPowerRelay;\n'),
    ('''            boolean powered = TransformPowerQuery.powered(level,
                    surface, ref.slot());
            if (address.stage() == DoorStage.CLOSED && powered) {''',
     '''            boolean powered = TransformPowerQuery.powered(level,
                    surface, ref.slot());
            // A heavy door occupies the cells above its logical controller.
            // Apply the same local adjacency semantics to every Surface layer.
            if (!powered && HeavyDoorPowerRelay.isHeavyDoorState(
                    attachment.state().getBlock())) {
                for (int up = 1; up <= 2 && !powered; up++) {
                    int row = ref.slot().row() + up;
                    if (row >= surface.rows()) break;
                    powered = TransformPowerQuery.powered(level, surface,
                            new ConstructionSurface.SurfaceSlot(
                                    ref.slot().column(), row));
                }
            }
            if (address.stage() == DoorStage.CLOSED && powered) {''')
])

patch('TransformAlarmRuntime.java', [
    ('import com.bl4ues.scpclassifieddirective.facility.FacilityModule;\n',
     'import com.bl4ues.scpclassifieddirective.facility.FacilityModule;\nimport com.bl4ues.scpclassifieddirective.facility.HeavyDoorPowerRelay;\n'),
    ('''            if (electricOpenDoor(group.cells().get(base))) return true;
            for (Direction direction : new Direction[]{
                    Direction.NORTH, Direction.SOUTH,
                    Direction.EAST, Direction.WEST}) {
                TransformGroup.GridPos neighbor = base.offset(
                        direction.getStepX(), 0, direction.getStepZ());
                if (electricOpenDoor(group.cells().get(neighbor))) return true;
            }''',
     '''            if (heavyDoorOpen(group.cells().get(base))) return true;
            for (Direction direction : new Direction[]{
                    Direction.NORTH, Direction.SOUTH,
                    Direction.EAST, Direction.WEST}) {
                TransformGroup.GridPos neighbor = base.offset(
                        direction.getStepX(), 0, direction.getStepZ());
                if (heavyDoorOpen(group.cells().get(neighbor))) return true;
            }'''),
    ('''    private static boolean electricOpenDoor(BlockState state) {
        return state != null
                && FacilityModule.isElectricDoorOpenOrOpening(state);
    }''',
     '''    private static boolean electricOpenDoor(BlockState state) {
        return state != null
                && FacilityModule.isElectricDoorOpenOrOpening(state);
    }

    private static boolean heavyDoorOpen(BlockState state) {
        return electricOpenDoor(state)
                && HeavyDoorPowerRelay.isHeavyDoorState(state.getBlock());
    }''')
])
print('Heavy door upper-footprint power and Alarm detection patched in Group and Surface runtimes')
