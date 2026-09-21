from pathlib import Path
path=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/mapping/FacilitySurfaceBoundaryConformer.java')
source=path.read_text(encoding='utf-8')
def patch(old,new):
    global source
    if source.count(old)!=1:
        raise SystemExit(f'Expected exactly one source anchor, found {source.count(old)}: {old[:95]!r}')
    source=source.replace(old,new)
patch('''    private static final int MIN_SAMPLES = 12;
    private static final int MAX_SAMPLES = 96;
    private static final int MAX_OUTPUT_VERTICES = 224;
''','''    private static final int MIN_SAMPLES = 20;
    private static final int MAX_SAMPLES = 192;
    private static final int MAX_OUTPUT_VERTICES = 768;
''')
patch('''                (int) Math.ceil(surface.width() * 6.0D)));''','''                (int) Math.ceil(surface.width() * 10.0D)));''')
patch('''    private static List<FacilityFloorPatch.Vertex> limit(
            List<FacilityFloorPatch.Vertex> source, int maximum) {
        if (source.size() <= maximum) return source;
        List<FacilityFloorPatch.Vertex> reduced = new ArrayList<>(maximum);
        for (int i = 0; i < maximum; i++) {
            int index = (int) Math.floor(i * source.size() / (double) maximum);
            reduced.add(source.get(Math.min(source.size() - 1, index)));
        }
        return List.copyOf(reduced);
    }
''','''    private static List<FacilityFloorPatch.Vertex> limit(
            List<FacilityFloorPatch.Vertex> source, int maximum) {
        if (source.size() <= maximum) return source;
        // Uniform index sampling discarded authored diagonal/vertical corners
        // irrespective of their geometric significance. Remove only vertices
        // whose chord error is subpixel at typical map scale; keep actual
        // corners even if that leaves more than the soft budget.
        List<FacilityFloorPatch.Vertex> reduced = new ArrayList<>(source);
        final double maxErrorSqr = 0.008D * 0.008D;
        boolean removed;
        do {
            removed = false;
            for (int i = 0; i < reduced.size()
                    && reduced.size() > maximum; i++) {
                int size = reduced.size();
                FacilityFloorPatch.Vertex before = reduced.get(
                        (i + size - 1) % size);
                FacilityFloorPatch.Vertex point = reduced.get(i);
                FacilityFloorPatch.Vertex after = reduced.get((i + 1) % size);
                double prevX = point.x() - before.x();
                double prevZ = point.z() - before.z();
                double nextX = after.x() - point.x();
                double nextZ = after.z() - point.z();
                double lengths = Math.hypot(prevX, prevZ)
                        * Math.hypot(nextX, nextZ);
                if (lengths < 1.0E-12D
                        || prevX * nextX + prevZ * nextZ
                                < 0.97D * lengths) continue;
                if (pointSegmentDistanceSqr(point.x(), point.z(),
                        before.x(), before.z(), after.x(), after.z())
                        > maxErrorSqr) continue;
                reduced.remove(i);
                removed = true;
                i = Math.max(-1, i - 2);
            }
        } while (removed && reduced.size() > maximum);
        return List.copyOf(reduced);
    }
''')
path.write_text(source,encoding='utf-8')
