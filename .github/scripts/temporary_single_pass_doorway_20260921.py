from pathlib import Path


def replace_once(path, old, new):
    file = Path(path)
    text = file.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{path}: expected one occurrence, found {count}: {old[:100]!r}')
    file.write_text(text.replace(old, new, 1), encoding='utf-8')


root = 'src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/'
doorway = root + 'TransformDoorwayCollision.java'
p = Path(doorway)
text = p.read_text(encoding='utf-8')
start = text.index('    public static List<AABB> nearbyPassages(')
end = text.index('    /** Immutable, world-cell keyed openings', start)
old = text[start:end]
assert old.count('for (int dx = -2; dx <= 2; dx++)') == 1
new = '''    /** Only the authored neighbouring doors are looked up. The individual
     * opening geometry is shared with indexPassages so a doorway is generated
     * exactly once for its owner, not once for every nearby door. */
    public static List<AABB> nearbyPassages(TransformGroup group,
            GridPos source) {
        if (group == null || source == null) return List.of();
        List<AABB> result = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    GridPos candidate = source.offset(dx, dy, dz);
                    result.addAll(passagesForDoor(group, candidate,
                            group.cells().get(candidate)));
                }
            }
        }
        return result;
    }

    private static List<AABB> passagesForDoor(TransformGroup group,
            GridPos cell, BlockState state) {
        if (group == null || cell == null
                || !FacilityModule.isFacilityDoor(state)
                || !FacilityModule.isDoorPassable(state)
                || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return List.of();
        }
        Vec3 center = group.cellCenter(cell);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Vec3 localWidth = facing.getAxis() == Direction.Axis.Z
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 width = TransformMath.rotate(localWidth,
                group.rotationX(), group.rotationY(), group.rotationZ());
        Vec3 through = TransformMath.rotate(
                Vec3.atLowerCornerOf(facing.getNormal()),
                group.rotationX(), group.rotationY(), group.rotationZ());
        width = new Vec3(width.x, 0.0D, width.z).normalize();
        through = new Vec3(through.x, 0.0D, through.z).normalize();
        if (width.lengthSqr() < EPSILON || through.lengthSqr() < EPSILON)
            return List.of();
        double halfX = Math.abs(width.x) * CLEAR_HALF_WIDTH
                + Math.abs(through.x) * PASSAGE_HALF_DEPTH;
        double halfZ = Math.abs(width.z) * CLEAR_HALF_WIDTH
                + Math.abs(through.z) * PASSAGE_HALF_DEPTH;
        List<AABB> result = new ArrayList<>(PASSAGE_STEPS);
        for (int step = 0; step < PASSAGE_STEPS; step++) {
            double along = (step - (PASSAGE_STEPS - 1) * 0.5D)
                    * PASSAGE_STEP_LENGTH;
            Vec3 at = center.add(through.scale(along));
            result.add(new AABB(at.x - halfX, center.y - CLEAR_BELOW,
                    at.z - halfZ, at.x + halfX,
                    center.y + CLEAR_ABOVE, at.z + halfZ));
        }
        return result;
    }

'''
text = text[:start] + new + text[end:]
old = '''                for (AABB passage : nearbyPassages(group, entry.getKey())) {'''
assert text.count(old) == 1
text = text.replace(old, '''                for (AABB passage : passagesForDoor(group,
                        entry.getKey(), state)) {''', 1)
p.write_text(text, encoding='utf-8')

manager = root + 'TransformConstructionManager.java'
replace_once(manager, '''        List<AABB> doorways = TransformDoorwayCollision.nearbyPassages(
                group, cell);
        int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;''', '''        // The final, aggregated world-cell collision is clipped once by the
        // shared doorway index. Early per-cell clipping duplicates hundreds of
        // overlapping AABBs and is invalidated whenever a door animates.
        int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;''')
replace_once(manager, '''                        for (AABB worldBox : TransformDoorwayCollision.clip(
                                transformedBounds(group, local), doorways)) {
                            addWorldBox(index, owner, worldBox,
                                    false, true, state.getLightEmission());
                        }''', '''                        addWorldBox(index, owner,
                                transformedBounds(group, local),
                                false, true, state.getLightEmission());''')

client = root + 'client/TransformConstructionClientState.java'
replace_once(client, '''            List<AABB> doorways = TransformDoorwayCollision.nearbyPassages(
                    neighborhood, cell);
            int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;''', '''            // Match the server: clip the combined spatial index exactly once,
            // after contributions from every transformed owner are merged.
            int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;''')
replace_once(client, '''                            for (AABB worldBox : TransformDoorwayCollision.clip(
                                    transformedBounds(group, local), doorways)) {
                                addWorldBox(index, worldBox, group.id(), null,
                                        false, true, state.getLightEmission());
                            }''', '''                            addWorldBox(index,
                                    transformedBounds(group, local),
                                    group.id(), null, false, true,
                                    state.getLightEmission());''')
print('Applied single-pass server/client doorway clipping and per-door passage indexing.')
