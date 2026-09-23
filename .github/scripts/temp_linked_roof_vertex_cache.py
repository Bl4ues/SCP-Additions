from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = path.read_text()

def replace(old, new, expected=1):
    global s
    count = s.count(old)
    if count != expected:
        raise SystemExit(f'Unexpected patch anchor count {count} != {expected}: {old[:100]}')
    s = s.replace(old, new)

replace('''            // A border block can expose a previously hidden welded face.
            PARENT_SHELLS.clear();
''', '''            // Block occupancy changes the visible caps, not the physical
            // parent edge. Retain its already sampled shell and only rebake
            // the adjacent meshes that depend on the exposed face.
''')

replace('''    private static boolean emitLinkedRoofQuad(List<PreparedVertex> output,
''', '''    private record RoofVertexKey(double s, double t) { }

    private static boolean emitLinkedRoofQuad(List<PreparedVertex> output,
''')

replace('''        List<Double> first = roofLongitudinalCuts(surface, slot, x0,
                xDelta, xSteps, bridge.firstCells());
        List<Double> second = roofLongitudinalCuts(surface, slot, x0,
                xDelta, xSteps, bridge.secondCells());
''', '''        // Persisted edge profiles are the source of truth. Their two lengths
        // can differ even if the authored walls have identical cell counts.
        List<Double> first = roofLongitudinalCuts(surface, slot, x0,
                xDelta, xSteps, bridge.firstProfile().size() - 1);
        List<Double> second = roofLongitudinalCuts(surface, slot, x0,
                xDelta, xSteps, bridge.secondProfile().size() - 1);
''')

replace('''        List<Double> transverse = List.copyOf(across);
        for (int band = 0; band + 1 < transverse.size(); band++) {
''', '''        List<Double> transverse = List.copyOf(across);
        // The zipper references a border vertex from several neighbouring
        // triangles. Calculate its expensive curved frame and parent-shell
        // projection once per baked face instead of once per triangle corner.
        Map<RoofVertexKey, PreparedVertex> vertexCache = new HashMap<>();
        for (int band = 0; band + 1 < transverse.size(); band++) {
''')

replace('''                    emitSurfaceVertex(output, surface, slot, attachment,
                            normalSign, overlay, bilerp(points, s, t),
                            quadNormal, bilerp(us, s, t), bilerp(vs, s, t),
                            red, green, blue, fallbackLight);
''', '''                    RoofVertexKey key = new RoofVertexKey(s, t);
                    PreparedVertex cached = vertexCache.get(key);
                    if (cached == null) {
                        cached = prepareSurfaceVertex(surface, slot, attachment,
                                normalSign, overlay, bilerp(points, s, t),
                                quadNormal, bilerp(us, s, t), bilerp(vs, s, t),
                                red, green, blue, fallbackLight);
                        vertexCache.put(key, cached);
                    }
                    output.add(cached);
''')

replace('''            double x0, double xDelta, int steps, int parentCells) {
        java.util.TreeSet<Double> cuts = new java.util.TreeSet<>(
                uniformCuts(steps));
        if (parentCells <= 0) return List.copyOf(cuts);
        int columns = surface.columns();
        int subdivisions = parentCells * Math.max(1,
                Math.min(24, 2048 / parentCells));
''', '''            double x0, double xDelta, int steps, int profileSegments) {
        // The parent profile supplies all actual contact vertices. A small
        // interior base grid preserves curvature even if this child slot is
        // narrower than a single parent polygon; the unrelated 24-cut grid
        // formerly multiplied triangles and split the contact edge again.
        java.util.TreeSet<Double> cuts = new java.util.TreeSet<>(
                uniformCuts(Math.max(2, Math.min(4, steps))));
        if (profileSegments <= 0) return List.copyOf(cuts);
        int columns = surface.columns();
        int subdivisions = profileSegments;
''')

replace('''    private static void emitSurfaceVertex(List<PreparedVertex> output,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
''', '''    private static PreparedVertex prepareSurfaceVertex(
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
''')
replace('''        output.add(new PreparedVertex(attachment.state(), frame.position(),
                frame.normal(), u, v, red, green, blue, light));
    }

    private static Vec3 bilerp(Vec3[] p, double s, double t) {
''', '''        return new PreparedVertex(attachment.state(), frame.position(),
                frame.normal(), u, v, red, green, blue, light);
    }

    private static void emitSurfaceVertex(List<PreparedVertex> output,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment, int normalSign,
            boolean overlay, Vec3 point, Vec3 localNormal, float u, float v,
            int red, int green, int blue, int light) {
        output.add(prepareSurfaceVertex(surface, slot, attachment,
                normalSign, overlay, point, localNormal, u, v,
                red, green, blue, light));
    }

    private static Vec3 bilerp(Vec3[] p, double s, double t) {
''')

path.write_text(s)
print('Applied linked-roof profile and vertex-cache corrections')
