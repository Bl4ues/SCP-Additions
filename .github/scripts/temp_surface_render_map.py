from pathlib import Path

def rep(path,old,new,count=1):
    p=Path(path); value=p.read_text(encoding='utf-8'); found=value.count(old)
    if found!=count: raise SystemExit(f'Expected {count} anchors, found {found} in {path}: {old[:100]!r}')
    p.write_text(value.replace(old,new),encoding='utf-8')

r='src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java'
rep(r,'''    private static final int GROUP_BATCH_SIZE = 8;
''','''    private static final int GROUP_BATCH_SIZE = 8;
    private static final int SURFACE_BATCH_SIZE = 8;
''')
rep(r,'''        // Static surface payloads are already culled as one authored object.
        // Re-submit one packed layer per RenderType instead of re-entering the
        // buffer map once for every logical slot on every frame.
        renderSurfaceLayers(pose, buffers, cached.layers());
''','''        // One huge Surface can span several rooms: a surface-level frustum
        // check previously submitted its entire mesh when only one corner was
        // visible. Keep immutable 8x8 logical-region batches and cull each
        // by its actual world-space geometry bounds before visiting vertices.
        for (CachedSurfaceBatch batch : cached.batches()) {
            if (batch.bounds() != null && frustum != null
                    && !frustum.isVisible(batch.bounds())) continue;
            if (batch.bounds() != null) {
                double reach = 192.0D + batch.bounds().getSize() * 0.5D;
                if (batch.bounds().getCenter().distanceToSqr(camera)
                        > reach * reach) continue;
            }
            renderSurfaceLayers(pose, buffers, batch.layers());
        }
''')
rep(r,'''        return new CachedSurface(surface, frozenSlots, frozenOverlays,
                mergeSurfaceLayers(frozenSlots, frozenOverlays));
''','''        return new CachedSurface(surface, frozenSlots, frozenOverlays,
                buildSurfaceBatches(frozenSlots, frozenOverlays));
''')
start='''    private static Map<RenderType, List<PreparedVertex>> mergeSurfaceLayers(
'''
end='''    private static CachedSurfaceSlot buildSurfaceSlot(Minecraft minecraft,
'''
p=Path(r); data=p.read_text(encoding='utf-8'); assert data.count(start)==1 and data.count(end)==1
left=data.index(start); right=data.index(end,left)
replacement='''    private static long surfaceBatchKey(ConstructionSurface.SurfaceSlot slot) {
        long column = Math.floorDiv(slot.column(), SURFACE_BATCH_SIZE);
        long row = Math.floorDiv(slot.row(), SURFACE_BATCH_SIZE);
        return (column << 32) ^ (row & 0xffffffffL);
    }

    private static List<CachedSurfaceBatch> buildSurfaceBatches(
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots,
            Map<ConstructionSurface.SurfaceOverlaySlot, CachedSurfaceSlot> overlays) {
        Map<Long, List<CachedSurfaceSlot>> regions = new LinkedHashMap<>();
        slots.forEach((slot, cached) -> regions.computeIfAbsent(
                surfaceBatchKey(slot), ignored -> new ArrayList<>()).add(cached));
        overlays.forEach((slot, cached) -> regions.computeIfAbsent(
                surfaceBatchKey(slot.slot()),
                ignored -> new ArrayList<>()).add(cached));
        List<CachedSurfaceBatch> result = new ArrayList<>(regions.size());
        for (List<CachedSurfaceSlot> region : regions.values()) {
            Map<RenderType, List<PreparedVertex>> layers = new LinkedHashMap<>();
            AABB bounds = null;
            for (CachedSurfaceSlot cached : region) {
                cached.layers().forEach((type, vertices) -> layers
                        .computeIfAbsent(type, ignored -> new ArrayList<>())
                        .addAll(vertices));
                if (cached.bounds() != null) bounds = bounds == null
                        ? cached.bounds() : bounds.minmax(cached.bounds());
            }
            Map<RenderType, List<PreparedVertex>> frozen = new LinkedHashMap<>();
            layers.forEach((type, vertices) ->
                    frozen.put(type, List.copyOf(vertices)));
            result.add(new CachedSurfaceBatch(Map.copyOf(frozen),
                    bounds == null ? null : bounds.inflate(0.06D)));
        }
        return List.copyOf(result);
    }

'''
p.write_text(data[:left]+replacement+data[right:],encoding='utf-8')
rep(r,'''    private record CachedSurface(ConstructionSurface surface,
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots,
            Map<ConstructionSurface.SurfaceOverlaySlot,
                    CachedSurfaceSlot> overlays,
            Map<RenderType, List<PreparedVertex>> layers) {
    }
''','''    private record CachedSurface(ConstructionSurface surface,
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots,
            Map<ConstructionSurface.SurfaceOverlaySlot,
                    CachedSurfaceSlot> overlays,
            List<CachedSurfaceBatch> batches) {
    }

    private record CachedSurfaceBatch(
            Map<RenderType, List<PreparedVertex>> layers, AABB bounds) {
    }
''')
rep(r,'''        int xSteps = deform
                && surface.curveOffset().lengthSqr() > 1.0E-8D
                && maxX - minX > 0.20D
                ? curvedPipe ? 12 : perimeter ? 12 : 2 : 1;
        int ySteps = deform
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                && maxY - minY > 0.20D ? perimeter ? 12 : 1 : 1;
''','''        // Tessellate ALONG an exposed border, not 12x12 across every block
        // merely touching any border. The old cross-product made even short
        // two-axis curved corridors generate thousands of redundant quads.
        boolean horizontalEdge = perimeter && (slot.row() == 0
                || slot.row() == surface.rows() - 1);
        boolean verticalEdge = perimeter && (slot.column() == 0
                || slot.column() == surface.columns() - 1);
        int xSteps = deform
                && surface.curveOffset().lengthSqr() > 1.0E-8D
                && maxX - minX > 0.20D
                ? curvedPipe ? 12 : horizontalEdge ? 8 : 2 : 1;
        int ySteps = deform
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                && maxY - minY > 0.20D
                ? verticalEdge ? 8 : 2 : 1;
''')

m='src/main/java/com/bl4ues/scpclassifieddirective/client/scp079/Scp079FacilityMapScreen.java'
rep(m,'''        int first = (int) Math.floor(x0);
        int last = (int) Math.ceil(x1);
        for (int major = first; major <= last; major++) {
            double sample = Mth.clamp(major + 0.5D, x0, x1);
''','''        // Only rasterize pixel centres INSIDE this finite segment. The old
        // floor/ceil loop clamped samples at both endpoints but still painted
        // their exterior pixels, leaving small dashes past 90/45-degree joins.
        int first = Math.max(0, (int) Math.ceil(x0 - 0.5D));
        int last = Math.min(steep ? height - 1 : width - 1,
                (int) Math.floor(x1 - 0.5D));
        if (first > last) {
            if (x1 - x0 > 0.001D) {
                double centre = (x0 + x1) * 0.5D;
                double minor = y0 + (centre - x0) * gradient;
                int major = (int) Math.floor(centre);
                int side = (int) Math.floor(minor);
                if (steep) accumulateCoverage(coverage, side, major,
                        Math.min(1.0D, x1 - x0), width, height);
                else accumulateCoverage(coverage, major, side,
                        Math.min(1.0D, x1 - x0), width, height);
            }
            return;
        }
        for (int major = first; major <= last; major++) {
            double sample = major + 0.5D;
''')

geom='src/main/java/com/bl4ues/scpclassifieddirective/facility/mapping/client/FacilityRoomOutlineGeometry.java'
p=Path(geom); source=p.read_text(encoding='utf-8'); start=source.index('    /**\n     * Refine the polygon once when room geometry is cached, not every frame.')
end=source.index('    private static void trimClosingDuplicate(',start)
source=source[:start]+'''    /** The union's Area is also the filled and selectable geometry. Do not
     * shift only its contour vertices: moving them independently produces a
     * bright/dark one-pixel halo and small spikes where 45-degree edges meet.
     * The map rasterizer performs screen-space antialiasing instead. */
    private static List<FacilityFloorPatch.Vertex> refineCurve(
            List<FacilityFloorPatch.Vertex> contour) {
        return List.copyOf(contour);
    }

'''+source[end:]
p.write_text(source,encoding='utf-8')
