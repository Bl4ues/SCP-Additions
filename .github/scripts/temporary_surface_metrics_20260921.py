from pathlib import Path


def replace_once(path, old, new):
    file = Path(path)
    text = file.read_text(encoding='utf-8')
    matches = text.count(old)
    if matches != 1:
        raise RuntimeError(f'{path}: expected exactly one match, got {matches}: {old[:80]}')
    file.write_text(text.replace(old, new, 1), encoding='utf-8')


surface = 'src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/ConstructionSurface.java'
replace_once(surface, '''    public ConstructionSurface {
''', '''    // A mesh samples thousands of vertices from the same immutable surface.
    // Avoid allocating a GeometryKey and locking the global LRU for every one.
    // Identity is intentional: two records may share geometry yet differ in
    // attachments; changing the geometry always creates a new record.
    private static final ThreadLocal<MetricAccess> HOT_METRICS =
            new ThreadLocal<>();

    private record MetricAccess(ConstructionSurface surface,
                                GeometryMetrics metrics) { }

    public ConstructionSurface {
''')
replace_once(surface, '''    private GeometryMetrics metrics() {
        GeometryKey key = new GeometryKey(bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset);
        synchronized (METRICS) {
            return METRICS.computeIfAbsent(key,
                    ignored -> GeometryMetrics.build(this));
        }
    }
''', '''    private GeometryMetrics metrics() {
        MetricAccess recent = HOT_METRICS.get();
        if (recent != null && recent.surface() == this)
            return recent.metrics();
        GeometryKey key = new GeometryKey(bottomStart, bottomEnd,
                topStart, topEnd, curveOffset, heightCurveOffset);
        GeometryMetrics resolved;
        synchronized (METRICS) {
            resolved = METRICS.computeIfAbsent(key,
                    ignored -> GeometryMetrics.build(this));
        }
        HOT_METRICS.set(new MetricAccess(this, resolved));
        return resolved;
    }
''')
replace_once(surface, '''            long key = Math.round(u * 1_000_000.0D);
            return verticalTables.computeIfAbsent(key,
                    ignored -> ArcTable.sample(VERTICAL_SAMPLES,
                            v -> surface.point(u, v)));
''', '''            // The old million-step key built nearly one 24-sample arc table
            // per distinct tessellated vertex. 1024 horizontal samples bound
            // that work while keeping the height reparameterization sub-pixel.
            long key = Math.round(Math.max(0.0D,
                    Math.min(1.0D, u)) * 1024.0D);
            return verticalTables.computeIfAbsent(key,
                    ignored -> ArcTable.sample(VERTICAL_SAMPLES,
                            v -> surface.point(key / 1024.0D, v)));
''')

renderer = 'src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java'
replace_once(renderer, '''                Vec3 first = edgePoint(surface, edge, 0.0D);
                Vec3 middle = edgePoint(surface, edge, 0.5D);
                Vec3 last = edgePoint(surface, edge, 1.0D);
''', '''                List<Vec3> sourceSamples = edgeSamples.get(surface.id())
                        .get(edge);
                Vec3 first = sourceSamples.get(0);
                Vec3 middle = sourceSamples.get(24);
                Vec3 last = sourceSamples.get(48);
''')
replace_once(renderer, '''                        Vec3 otherFirst = edgePoint(other, otherEdge, 0.0D);
                        Vec3 otherLast = edgePoint(other, otherEdge, 1.0D);
''', '''                        List<Vec3> samples = edgeSamples.get(other.id())
                                .get(otherEdge);
                        Vec3 otherFirst = samples.get(0);
                        Vec3 otherLast = samples.get(48);
''')
replace_once(renderer, '''                        List<Vec3> samples = edgeSamples.get(other.id())
                                .get(otherEdge);
                        double startFraction = closestEdgeFraction(samples, first);
''', '''                        double startFraction = closestEdgeFraction(samples, first);
''')
print('Applied bounded arc table and shared-edge sampling optimizations.')
