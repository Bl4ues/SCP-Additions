from pathlib import Path

def replace(path, before, after, n=1):
    path=Path(path); text=path.read_text(encoding='utf-8')
    actual=text.count(before)
    if actual!=n: raise SystemExit(f'{path}: expected {n} anchors, found {actual}: {before[:90]}')
    path.write_text(text.replace(before,after),encoding='utf-8')

base='src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/'
p=base+'TransformDoorwayCollision.java'
replace(p, '''    // A world-aligned square narrows to a point as a player traverses its
    // diagonal. Instead preserve a constant width along the DOOR's own plane.
    private static final double CLEAR_HALF_WIDTH = 0.72D;
    private static final double CLEAR_BELOW = 0.56D;
    private static final double CLEAR_ABOVE = 1.78D;
    private static final double EPSILON = 1.0E-6D;
    private static final int PASSAGE_STEPS = 13;
    private static final double PASSAGE_STEP_LENGTH = 0.22D;
    private static final double PASSAGE_HALF_DEPTH = 0.70D;
''', '''    // One rotated doorway must not carve the entire world-axis bounding
    // rectangle surrounding its width and depth: the corners of that rectangle
    // are the adjacent WALL. Use a small tiled mask in the door's local axes.
    private static final double CLEAR_HALF_WIDTH = 0.64D;
    private static final double CLEAR_BELOW = 0.48D;
    private static final double CLEAR_ABOVE = 1.55D;
    private static final double EPSILON = 1.0E-6D;
    private static final int WIDTH_TILES = 7;
    private static final int DEPTH_TILES = 5;
    private static final double CLEAR_HALF_DEPTH = 0.53D;
''')
start='''        double halfX = Math.abs(width.x) * CLEAR_HALF_WIDTH
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
'''
end='''        // VoxelShapes cannot encode a single arbitrarily rotated prism. Tile
        // its horizontal footprint instead, in BOTH door-local directions.
        // The old wide AABB union extended into the neighbouring wall by a
        // full block; this tiled mask stays within the actual door opening.
        double widthStep = CLEAR_HALF_WIDTH * 2.0D / WIDTH_TILES;
        double depthStep = CLEAR_HALF_DEPTH * 2.0D / DEPTH_TILES;
        double tileHalfX = Math.abs(width.x) * (widthStep * 0.5D + 0.004D)
                + Math.abs(through.x) * (depthStep * 0.5D + 0.004D);
        double tileHalfZ = Math.abs(width.z) * (widthStep * 0.5D + 0.004D)
                + Math.abs(through.z) * (depthStep * 0.5D + 0.004D);
        List<AABB> result = new ArrayList<>(WIDTH_TILES * DEPTH_TILES);
        for (int w = 0; w < WIDTH_TILES; w++) {
            double sideways = (w + 0.5D) * widthStep - CLEAR_HALF_WIDTH;
            for (int d = 0; d < DEPTH_TILES; d++) {
                double along = (d + 0.5D) * depthStep - CLEAR_HALF_DEPTH;
                Vec3 at = center.add(width.scale(sideways))
                        .add(through.scale(along));
                result.add(new AABB(at.x - tileHalfX,
                        center.y - CLEAR_BELOW, at.z - tileHalfZ,
                        at.x + tileHalfX, center.y + CLEAR_ABOVE,
                        at.z + tileHalfZ));
            }
        }
        return result;
'''
replace(p,start,end)
# A group door may clip AABB overhang of its own transformed walls, but must
# never delete separately-authored Surface collision merely because the two
# broad-phase proxies occupy the same world-aligned cell.
manager=base+'TransformConstructionManager.java'
replace(manager,'''            ProxyCell result = doorPassages.containsKey(packed)
                    ? new ProxyCell(raw.selection(),
                            TransformDoorwayCollision.clipShape(
                                    raw.collision(), pos, doorPassages),
                            TransformDoorwayCollision.clipShape(
                                    raw.groupCollision(), pos, doorPassages),
                            TransformDoorwayCollision.clipShape(
                                    raw.surfaceCollision(), pos, doorPassages),
                            raw.light(), raw.groupIds(), raw.surfaceIds())
                    : raw;
''','''            ProxyCell result;
            if (doorPassages.containsKey(packed)) {
                VoxelShape group = TransformDoorwayCollision.clipShape(
                        raw.groupCollision(), pos, doorPassages);
                result = new ProxyCell(raw.selection(),
                        Shapes.or(group, raw.surfaceCollision()).optimize(),
                        group, raw.surfaceCollision(), raw.light(),
                        raw.groupIds(), raw.surfaceIds());
            } else {
                result = raw;
            }
''')
client=base+'client/TransformConstructionClientState.java'
replace(client,'''        TransformConstructionManager.ProxyCell masked =
                new TransformConstructionManager.ProxyCell(raw.selection(),
                        TransformDoorwayCollision.clipShape(raw.collision(),
                                pos, doorPassages),
                        TransformDoorwayCollision.clipShape(raw.groupCollision(),
                                pos, doorPassages),
                        TransformDoorwayCollision.clipShape(raw.surfaceCollision(),
                                pos, doorPassages),
                        raw.light(), raw.groupIds(), raw.surfaceIds());
''','''        VoxelShape group = TransformDoorwayCollision.clipShape(
                raw.groupCollision(), pos, doorPassages);
        TransformConstructionManager.ProxyCell masked =
                new TransformConstructionManager.ProxyCell(raw.selection(),
                        Shapes.or(group, raw.surfaceCollision()).optimize(),
                        group, raw.surfaceCollision(), raw.light(),
                        raw.groupIds(), raw.surfaceIds());
''')
