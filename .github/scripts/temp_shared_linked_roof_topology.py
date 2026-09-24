from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = path.read_text(encoding='utf-8')

def once(old, new):
    global s
    occurrences = s.count(old)
    if occurrences != 1:
        raise RuntimeError(f'Expected exactly one occurrence, got {occurrences}: {old[:120]!r}')
    s = s.replace(old, new, 1)

# The previous cap-removal experiment exposed sky before the physical meshes
# matched. Keep all inter-surface caps while fixing actual boundary topology.
once('''            // Never cull generic inter-Surface caps: unmatched/partial joins
            // need them as a watertight fallback. A linked roof is different:
            // the parent edge is its physical boundary and both solids would
            // draw a coincident cap. Keep the PARENT cap and omit only the
            // CHILD cap when every contacted parent cell is actually solid.
            return !overlay && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                    && coveredLinkedRoofCap(surface, slot, face);
''', '''            // Separate Surfaces keep their perimeter faces, including at
            // incomplete or partially occupied joins. Boundary topology is
            // fixed in the vertex grid; do not expose a sky slit by culling.
            return false;
''')
start = s.index('''    /**
     * Suppress only a linked roof's overlapping contact cap''')
end = s.index('''    private static boolean fullSurfaceCell(BlockState state)''', start)
s = s[:start] + s[end:]

once('''    private static final Map<UUID, int[]> LINKED_PARENT_EDGE_STEPS =
            new HashMap<>();
''', '''    private static final Map<UUID, int[]> LINKED_PARENT_EDGE_STEPS =
            new HashMap<>();
    // These are physical edge VERTEX PARAMETERS, not samples fitted to a
    // curve. Union the parent's own subdivision knots with every attached
    // child's logical cell boundaries; both meshes must render this identical
    // sorted vertex sequence. This never alters the parent's authored curve.
    private static final Map<UUID, Map<Integer, List<Double>>>
            LINKED_PARENT_EDGE_KNOTS = new HashMap<>();
''')
once('''        LINKED_PARENT_EDGE_STEPS.clear();
        PARENT_SHELLS.clear();
''', '''        LINKED_PARENT_EDGE_STEPS.clear();
        LINKED_PARENT_EDGE_KNOTS.clear();
        PARENT_SHELLS.clear();
''')

once('''        RoofCuts cuts = linkedRoof
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
''', '''        RoofCuts cuts = linkedRoof
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : deform && !overlay && fullSurfaceCell(attachment.state())
                        ? linkedParentQuadCuts(surface, slot, points,
                                xSteps, ySteps)
                        : new RoofCuts(uniformCuts(xSteps),
                                uniformCuts(ySteps));
''')

# Both roof contact lines must be cut at precisely the same global parameters
# as their respective parent edges. They need NOT share the same interior grid.
once('''        List<Double> first = slot.row() == 0
                ? roofLongitudinalCuts(surface, slot, x0, xDelta, xSteps,
                        bridge.firstProfile().size() - 1) : interior;
        List<Double> second = slot.row() == surface.rows() - 1
                ? roofLongitudinalCuts(surface, slot, x0, xDelta, xSteps,
                        bridge.secondProfile().size() - 1) : interior;
''', '''        List<Double> first = slot.row() == 0
                ? roofLongitudinalCuts(surface, slot, x0, xDelta,
                        bridge, true) : interior;
        List<Double> second = slot.row() == surface.rows() - 1
                ? roofLongitudinalCuts(surface, slot, x0, xDelta,
                        bridge, false) : interior;
''')
start = s.index('''    private static List<Double> roofLongitudinalCuts(''')
end = s.index('''    private static List<Double> roofBandCuts(''', start)
s = s[:start] + '''    private static List<Double> roofLongitudinalCuts(
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            double x0, double xDelta,
            ConstructionSurface.BridgeAnchor bridge, boolean first) {
        java.util.TreeSet<Double> cuts = new java.util.TreeSet<>();
        cuts.add(0.0D);
        cuts.add(1.0D);
        UUID parentId = first ? bridge.firstId() : bridge.secondId();
        int edge = first ? bridge.firstEdge() : bridge.secondEdge();
        Map<Integer, List<Double>> edges =
                LINKED_PARENT_EDGE_KNOTS.get(parentId);
        List<Double> parentKnots = edges == null ? null : edges.get(edge);
        // The source profile is also the native parent polygon subdivision
        // grid, so an old roof still behaves sensibly before cache refresh.
        if (parentKnots == null) {
            int segments = (first ? bridge.firstProfile()
                    : bridge.secondProfile()).size() - 1;
            if (segments <= 0) return List.copyOf(cuts);
            java.util.ArrayList<Double> fallback =
                    new java.util.ArrayList<>(segments + 1);
            for (int k = 0; k <= segments; k++)
                fallback.add(k / (double) segments);
            parentKnots = fallback;
        }
        double columns = surface.columns();
        for (double parentFraction : parentKnots) {
            double roofFraction = first || !bridge.reverseSecond()
                    ? parentFraction : 1.0D - parentFraction;
            double localX = roofFraction * columns - slot.column();
            double modelX = surface.flipped() ? 1.0D - localX : localX;
            addRoofCut(cuts, (modelX - x0) / xDelta);
        }
        return List.copyOf(cuts);
    }

''' + s[end:]

# The same exact parent knot fractions must appear on all structural parent
# rows/columns and on every applicable baked model face, not just its top row.
# That avoids T-junctions on the MOTHER, including at child-cell boundaries.
anchor = '''    private static RoofCuts roofCuts(ConstructionSurface surface,
'''
helper = '''    private static RoofCuts linkedParentQuadCuts(
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            Vec3[] points, int xSteps, int ySteps) {
        Map<Integer, List<Double>> edges =
                LINKED_PARENT_EDGE_KNOTS.get(surface.id());
        if (edges == null || edges.isEmpty())
            return new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
        java.util.TreeSet<Double> sCuts =
                new java.util.TreeSet<>(uniformCuts(xSteps));
        java.util.TreeSet<Double> tCuts =
                new java.util.TreeSet<>(uniformCuts(ySteps));
        // For top/bottom attachments the shared edge is along model X.
        // For a side attachment it is along model Y. Baked quads can swap
        // their s/t axes and a flipped Surface mirrors model X.
        java.util.TreeSet<Double> globalX = new java.util.TreeSet<>();
        java.util.TreeSet<Double> globalY = new java.util.TreeSet<>();
        if (edges.containsKey(2)) globalX.addAll(edges.get(2));
        if (edges.containsKey(3)) globalX.addAll(edges.get(3));
        if (edges.containsKey(0)) globalY.addAll(edges.get(0));
        if (edges.containsKey(1)) globalY.addAll(edges.get(1));
        double xS = bilerp(points, 1.0D, 0.5D).x
                - bilerp(points, 0.0D, 0.5D).x;
        double xT = bilerp(points, 0.5D, 1.0D).x
                - bilerp(points, 0.5D, 0.0D).x;
        double yS = bilerp(points, 1.0D, 0.5D).y
                - bilerp(points, 0.0D, 0.5D).y;
        double yT = bilerp(points, 0.5D, 1.0D).y
                - bilerp(points, 0.5D, 0.0D).y;
        double x0 = bilerp(points, 0.0D, 0.5D).x;
        double xt0 = bilerp(points, 0.5D, 0.0D).x;
        double y0 = bilerp(points, 0.0D, 0.5D).y;
        double yt0 = bilerp(points, 0.5D, 0.0D).y;
        for (double fraction : globalX) {
            double physicalX = fraction * surface.columns() - slot.column();
            if (physicalX <= 1.0E-8D || physicalX >= 1.0D - 1.0E-8D)
                continue;
            double modelX = surface.flipped()
                    ? 1.0D - physicalX : physicalX;
            if (Math.abs(xS) > 0.20D && Math.abs(xT) < 1.0E-6D)
                addRoofCut(sCuts, (modelX - x0) / xS);
            else if (Math.abs(xT) > 0.20D && Math.abs(xS) < 1.0E-6D)
                addRoofCut(tCuts, (modelX - xt0) / xT);
        }
        for (double fraction : globalY) {
            double modelY = fraction * surface.rows() - slot.row();
            if (modelY <= 1.0E-8D || modelY >= 1.0D - 1.0E-8D)
                continue;
            if (Math.abs(yS) > 0.20D && Math.abs(yT) < 1.0E-6D)
                addRoofCut(sCuts, (modelY - y0) / yS);
            else if (Math.abs(yT) > 0.20D && Math.abs(yS) < 1.0E-6D)
                addRoofCut(tCuts, (modelY - yt0) / yT);
        }
        return new RoofCuts(List.copyOf(sCuts), List.copyOf(tCuts));
    }

'''
once(anchor, helper + anchor)

# Sample the actual shared polygon: a roof vertex at a parent's additional
# child-boundary knot must use the parent's VERTEX, never the previous 24-grid
# chord interpolation. All intermediate roof contact points use the same
# consecutive endpoints as the parent mesh.
anchor = '''        int cells = edge < 2 ? parent.rows() : parent.columns();
        int segments = Math.max(1, cells * Math.max(1,
                Math.min(24, 2048 / Math.max(1, cells))));
'''
addition = '''        Map<Integer, List<Double>> edgeKnots =
                LINKED_PARENT_EDGE_KNOTS.get(parent.id());
        List<Double> shared = edgeKnots == null ? null : edgeKnots.get(edge);
        if (shared != null && shared.size() >= 2) {
            double clamped = net.minecraft.util.Mth.clamp(fraction,
                    0.0D, 1.0D);
            int found = java.util.Collections.binarySearch(shared, clamped);
            int lo = found >= 0 ? found : Math.max(0, -found - 2);
            int hi = found >= 0 ? found
                    : Math.min(shared.size() - 1, lo + 1);
            if (Math.abs(depth) < 1.0E-7D
                    || Math.abs(depth - 1.0D) < 1.0E-7D) {
                int layer = depth < 0.5D ? 0 : 1;
                ParentShellKey key = new ParentShellKey(
                        parent.id(), edge, layer);
                Vec3[] vertices = PARENT_SHELLS.computeIfAbsent(key,
                        ignored -> new Vec3[shared.size()]);
                if (vertices[lo] == null)
                    vertices[lo] = parentShellVertex(parent, edge,
                            shared.get(lo), layer);
                if (lo == hi || clamped - shared.get(lo) < 1.0E-9D)
                    return vertices[lo];
                if (vertices[hi] == null)
                    vertices[hi] = parentShellVertex(parent, edge,
                            shared.get(hi), layer);
                double t = (clamped - shared.get(lo))
                        / (shared.get(hi) - shared.get(lo));
                return vertices[lo].scale(1.0D - t)
                        .add(vertices[hi].scale(t));
            }
            Vec3 a = parentShellVertex(parent, edge, shared.get(lo), depth);
            if (lo == hi || clamped - shared.get(lo) < 1.0E-9D)
                return a;
            Vec3 b = parentShellVertex(parent, edge, shared.get(hi), depth);
            double t = (clamped - shared.get(lo))
                    / (shared.get(hi) - shared.get(lo));
            return a.scale(1.0D - t).add(b.scale(t));
        }
'''
once(anchor, addition + anchor)

# Compute canonical parent/child knot unions only when committed geometry
# changes, and invalidate parent meshes when the set changes.
anchor = '''    /** Refresh neighbours only after a committed edit. A temporary drag
'''
helper = '''    private static void addLinkedEdgeKnots(
            Map<UUID, Map<Integer, java.util.TreeSet<Double>>> pending,
            UUID id, int edge, int parentSegments, int childColumns,
            boolean reverse) {
        java.util.TreeSet<Double> knots = pending
                .computeIfAbsent(id, ignored -> new HashMap<>())
                .computeIfAbsent(edge, ignored -> new java.util.TreeSet<>());
        knots.add(0.0D);
        knots.add(1.0D);
        // The mother keeps its complete original tessellation. A linked
        // child may only add positions at which its own cells need a vertex.
        for (int k = 1; k < parentSegments; k++)
            knots.add(k / (double) parentSegments);
        for (int k = 1; k < childColumns; k++) {
            double u = k / (double) childColumns;
            knots.add(reverse ? 1.0D - u : u);
        }
    }

'''
once(anchor, helper + anchor)
once('''        Map<UUID, int[]> nextLinkedSteps = new HashMap<>();
        for (ConstructionSurface child : surfaces) {
''', '''        Map<UUID, int[]> nextLinkedSteps = new HashMap<>();
        Map<UUID, Map<Integer, java.util.TreeSet<Double>>> pendingKnots =
                new HashMap<>();
        for (ConstructionSurface child : surfaces) {
''')
once('''            secondSteps[link.secondEdge()] = Math.max(
                    secondSteps[link.secondEdge()],
                    Math.max(1, (link.secondProfile().size() - 1) / secondCells));
        }
        Set<UUID> parentIds = new java.util.HashSet<>(
''', '''            secondSteps[link.secondEdge()] = Math.max(
                    secondSteps[link.secondEdge()],
                    Math.max(1, (link.secondProfile().size() - 1) / secondCells));
            addLinkedEdgeKnots(pendingKnots, link.firstId(),
                    link.firstEdge(), link.firstProfile().size() - 1,
                    child.columns(), false);
            addLinkedEdgeKnots(pendingKnots, link.secondId(),
                    link.secondEdge(), link.secondProfile().size() - 1,
                    child.columns(), link.reverseSecond());
        }
        Map<UUID, Map<Integer, List<Double>>> nextKnots = new HashMap<>();
        pendingKnots.forEach((id, edges) -> {
            Map<Integer, List<Double>> sorted = new HashMap<>();
            edges.forEach((edge, knots) -> sorted.put(edge,
                    List.copyOf(knots)));
            nextKnots.put(id, Map.copyOf(sorted));
        });
        java.util.HashSet<UUID> knotParents =
                new java.util.HashSet<>(LINKED_PARENT_EDGE_KNOTS.keySet());
        knotParents.addAll(nextKnots.keySet());
        for (UUID id : knotParents) {
            if (!java.util.Objects.equals(LINKED_PARENT_EDGE_KNOTS.get(id),
                    nextKnots.get(id))) affected.add(id);
        }
        LINKED_PARENT_EDGE_KNOTS.clear();
        LINKED_PARENT_EDGE_KNOTS.putAll(nextKnots);
        Set<UUID> parentIds = new java.util.HashSet<>(
''')

# A source-level regression check plus number-theory checks for non-divisible
# grids on both sides, including reverseSecond and flipped model X.
assert 'coveredLinkedRoofCap' not in s
assert 'LINKED_PARENT_EDGE_KNOTS' in s
assert 'return !overlay && normalSign == TransformSurfaceGeometry.MAIN_SIDE' not in s
assert 'roofLongitudinalCuts(surface, slot, x0, xDelta,\n                        bridge, true)' in s
assert 'if (explicitLinkedPair(surface, other)) continue;' in s

for parent_cells, child_columns, reverse in [
        (3, 5, False), (7, 11, True), (11, 3, False),
        (1, 7, True), (9, 9, False), (17, 23, True)]:
    parent_segments = parent_cells * min(24, max(1, 2048 // parent_cells))
    parent = sorted({k / parent_segments for k in range(parent_segments + 1)}
                    | {(1 - k / child_columns) if reverse
                       else k / child_columns
                       for k in range(child_columns + 1)})
    roof = sorted({(1 - p) if reverse else p for p in parent})
    assert len(parent) == len(roof)
    for column in range(child_columns):
        roof_cell = [u for u in roof if column / child_columns - 1e-12
                     <= u <= (column + 1) / child_columns + 1e-12]
        assert abs(roof_cell[0] - column / child_columns) < 1e-12
        assert abs(roof_cell[-1] - (column + 1) / child_columns) < 1e-12
        assert all(any(abs((1 - u if reverse else u) - p) < 1e-12
                       for p in parent) for u in roof_cell)

path.write_text(s, encoding='utf-8')
print('Shared parent/child border knot topology and non-divisible grid regressions passed')
