from pathlib import Path
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s=p.read_text(encoding='utf-8')
def replace_once(old,new):
    global s
    n=s.count(old)
    if n!=1: raise RuntimeError(f'Anchor mismatch {n}: {old[:100]!r}')
    s=s.replace(old,new,1)
replace_once('''    private static final int MIN_SURFACE_VBO_VERTICES = 512;
''','''    private static final int MIN_SURFACE_VBO_VERTICES = 512;
    // Avoid a large first-visible-frame upload burst. If the visible facility
    // exceeds the GPU cache budget, keep already-visible batches resident and
    // use the regular CPU buffer path for the remainder instead of evicting
    // and re-uploading the same geometry every frame.
    private static final int MAX_SURFACE_VBO_UPLOAD_PER_FRAME = 128_000;
    private static long surfaceGpuFrame;
    private static int surfaceGpuUploadedThisFrame;
''')
replace_once('''        if (groups.isEmpty() && surfaces.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
''','''        if (groups.isEmpty() && surfaces.isEmpty()) return;
        surfaceGpuFrame++;
        surfaceGpuUploadedThisFrame = 0;

        Vec3 camera = event.getCamera().getPosition();
''')
replace_once('''        SurfaceGpuBatch cached = SURFACE_GPU_BATCHES.get(key);
        if (cached != null) return cached;
        // Bound video memory and release stale buffers instead of retaining
        // the entire facility's previously viewed geometry indefinitely.
        while (surfaceGpuVertices + vertices.size()
                > MAX_SURFACE_VBO_VERTICES && !SURFACE_GPU_BATCHES.isEmpty()) {
            var entries = SURFACE_GPU_BATCHES.entrySet().iterator();
            SurfaceGpuBatch oldest = entries.next().getValue();
            entries.remove();
            surfaceGpuVertices -= oldest.vertexCount();
            disposeGpuBuffer(oldest.buffer());
        }
''','''        SurfaceGpuBatch cached = SURFACE_GPU_BATCHES.get(key);
        if (cached != null) {
            cached.lastVisibleFrame = surfaceGpuFrame;
            return cached;
        }
        if (vertices.size() > MAX_SURFACE_VBO_UPLOAD_PER_FRAME
                || surfaceGpuUploadedThisFrame + vertices.size()
                        > MAX_SURFACE_VBO_UPLOAD_PER_FRAME) return null;
        // Only reclaim batches NOT rendered this frame. Once all resident
        // geometry is visible, fallback to the CPU path until the view moves;
        // evicting another visible batch would cause perpetual GPU uploads.
        var entries = SURFACE_GPU_BATCHES.entrySet().iterator();
        while (surfaceGpuVertices + vertices.size()
                > MAX_SURFACE_VBO_VERTICES && entries.hasNext()) {
            SurfaceGpuBatch oldest = entries.next().getValue();
            if (oldest.lastVisibleFrame == surfaceGpuFrame) continue;
            entries.remove();
            surfaceGpuVertices -= oldest.vertexCount();
            disposeGpuBuffer(oldest.buffer());
        }
        if (surfaceGpuVertices + vertices.size()
                > MAX_SURFACE_VBO_VERTICES) return null;
''')
replace_once('''        SURFACE_GPU_BATCHES.put(key, result);
        surfaceGpuVertices += vertices.size();
        return result;
''','''        result.lastVisibleFrame = surfaceGpuFrame;
        SURFACE_GPU_BATCHES.put(key, result);
        surfaceGpuVertices += vertices.size();
        surfaceGpuUploadedThisFrame += vertices.size();
        return result;
''')
replace_once('''    private record SurfaceGpuBatch(VertexBuffer buffer, Vec3 origin,
            int vertexCount) { }
''','''    private static final class SurfaceGpuBatch {
        private final VertexBuffer buffer;
        private final Vec3 origin;
        private final int vertexCount;
        private long lastVisibleFrame;

        private SurfaceGpuBatch(VertexBuffer buffer, Vec3 origin,
                int vertexCount) {
            this.buffer = buffer;
            this.origin = origin;
            this.vertexCount = vertexCount;
        }

        private VertexBuffer buffer() { return buffer; }
        private Vec3 origin() { return origin; }
        private int vertexCount() { return vertexCount; }
    }
''')
assert 'linkedParentShellPosition' in s and 'LINKED_PARENT_EDGE_KNOTS' in s
assert 'drawSurfaceGpu(pose, type, gpu)' in s
p.write_text(s,encoding='utf-8')
print('GPU cache warmup and eviction are frame-bounded; parent/child topology unchanged')
