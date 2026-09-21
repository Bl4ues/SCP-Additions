from pathlib import Path

p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s=p.read_text(encoding='utf-8')

def rep(old,new,n=1):
    global s
    actual=s.count(old)
    if actual!=n: raise SystemExit(f'Expected {n} anchors, found {actual}: {old[:100]!r}')
    s=s.replace(old,new)

rep('''    private static final Map<UUID, Map<Integer, MatchedSurfaceEdge>>
            SHARED_SURFACE_EDGES = new HashMap<>();
''','''    private static final Map<UUID, Map<Integer, MatchedSurfaceEdge>>
            SHARED_SURFACE_EDGES = new HashMap<>();
    // An unchanged curved border is expensive to resample. Keep its arc and
    // coarse bounds across unrelated gizmo edits, instead of traversing every
    // room's 4x49 geometry again on each committed handle movement.
    private static final Map<UUID, List<List<Vec3>>> SURFACE_EDGE_SAMPLES =
            new HashMap<>();
    private static final Map<UUID, List<AABB>> SURFACE_EDGE_BOUNDS =
            new HashMap<>();
''')
rep('''        SHARED_SURFACE_EDGES.clear();
        GROUP_MESHES.clear();
''','''        SHARED_SURFACE_EDGES.clear();
        SURFACE_EDGE_SAMPLES.clear();
        SURFACE_EDGE_BOUNDS.clear();
        GROUP_MESHES.clear();
''')
rep('''            double u = neighbour.column() < 0 ? 0.0D
                    : neighbour.column() >= surface.columns() ? 1.0D
                    : (slot.column() + 0.5D) / surface.columns();
            double v = neighbour.row() < 0 ? 0.0D
                    : neighbour.row() >= surface.rows() ? 1.0D
                    : (slot.row() + 0.5D) / surface.rows();
            return joinedSurfaceEdge(surface, u, v, 0.5D) != null;
''','''            // Another Surface is NOT the same as a neighbouring solid cell.
            // Their parametric border vertices may differ slightly, even if
            // a geometric join exists. Culling this cap exposed the sky along
            // the entire wall/ceiling seam. The cap is needed as a watertight
            // fallback for unmatched subdivisions and partial-length joins.
            return false;
''')
rep('''    private static Vec3 edgePoint(ConstructionSurface surface,
            int edge, double fraction) {
''','''    private static AABB sampledEdgeBounds(List<Vec3> samples) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 point : samples) {
            minX = Math.min(minX, point.x); minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z); maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y); maxZ = Math.max(maxZ, point.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Vec3 edgePoint(ConstructionSurface surface,
            int edge, double fraction) {
''')
rep('''        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        Map<UUID, List<List<Vec3>>> edgeSamples = new HashMap<>();
        for (ConstructionSurface surface : surfaces) {
            edgeSamples.put(surface.id(), List.of(sampleEdge(surface, 0),
                    sampleEdge(surface, 1), sampleEdge(surface, 2),
                    sampleEdge(surface, 3)));
        }
''','''        Set<UUID> currentIds = new java.util.HashSet<>();
        for (ConstructionSurface surface : surfaces) currentIds.add(surface.id());
        SURFACE_EDGE_SAMPLES.keySet().retainAll(currentIds);
        SURFACE_EDGE_BOUNDS.keySet().retainAll(currentIds);
        for (ConstructionSurface surface : surfaces) {
            if (SURFACE_EDGE_SAMPLES.containsKey(surface.id())
                    && !affected.contains(surface.id())) continue;
            List<List<Vec3>> samples = List.of(sampleEdge(surface, 0),
                    sampleEdge(surface, 1), sampleEdge(surface, 2),
                    sampleEdge(surface, 3));
            SURFACE_EDGE_SAMPLES.put(surface.id(), samples);
            SURFACE_EDGE_BOUNDS.put(surface.id(), List.of(
                    sampledEdgeBounds(samples.get(0)),
                    sampledEdgeBounds(samples.get(1)),
                    sampledEdgeBounds(samples.get(2)),
                    sampledEdgeBounds(samples.get(3))));
        }
        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        Map<UUID, List<List<Vec3>>> edgeSamples = SURFACE_EDGE_SAMPLES;
''')
rep('''                        List<Vec3> samples = edgeSamples.get(other.id())
                                .get(otherEdge);
                        Vec3 otherFirst = samples.get(0);
''','''                        List<Vec3> samples = edgeSamples.get(other.id())
                                .get(otherEdge);
                        AABB nearEdge = SURFACE_EDGE_BOUNDS.get(other.id())
                                .get(otherEdge).inflate(0.75D);
                        if (!nearEdge.contains(first)
                                || !nearEdge.contains(middle)
                                || !nearEdge.contains(last)) continue;
                        Vec3 otherFirst = samples.get(0);
''')
p.write_text(s,encoding='utf-8')
