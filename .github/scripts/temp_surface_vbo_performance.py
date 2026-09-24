from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = path.read_text(encoding='utf-8')

def replace_once(old, new):
    global s
    count = s.count(old)
    if count != 1:
        raise RuntimeError(f'Expected exactly one match, found {count}: {old[:130]!r}')
    s = s.replace(old, new, 1)

replace_once(
'''import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
''',
'''import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
''')

replace_once(
'''    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();
''',
'''    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();
    // PreparedSurface vertices are immutable, but pushing all of them through
    // BufferSource every frame is the dominant CPU path on linked corridors.
    // Retain solid batches as GPU buffers; translucent and custom RenderTypes
    // stay on the regular Forge/vanilla path, including their sorting.
    private static final boolean USE_SURFACE_VBO =
            !Boolean.getBoolean("scp_classified_directive.disable_surface_vbo");
    private static final int MAX_SURFACE_VBO_VERTICES = 1_800_000;
    private static final int MIN_SURFACE_VBO_VERTICES = 512;
    private static final Map<SurfaceGpuKey, SurfaceGpuBatch> SURFACE_GPU_BATCHES =
            new LinkedHashMap<>(64, 0.75F, true);
    private static int surfaceGpuVertices;
    private static final BufferBuilder SURFACE_GPU_BUILDER =
            new BufferBuilder(65536);
''')

replace_once(
'''    public static void clearSurfaceCache() {
        SURFACE_MESHES.clear();
''',
'''    public static void clearSurfaceCache() {
        clearSurfaceGpuCache();
        SURFACE_MESHES.clear();
''')

replace_once(
'''                SURFACE_MESHES.remove(edge.other().id());
''',
'''                invalidateSurfaceMesh(edge.other().id());
''')

replace_once(
'''        for (TransformGroup group : groups) {
            renderGroup(minecraft, pose, buffers, group, camera);
        }
        updateSharedSurfaceEdges(surfaces);
''',
'''        for (TransformGroup group : groups) {
            renderGroup(minecraft, pose, buffers, group, camera);
        }
        // GPU-backed solid surfaces draw immediately. Flush any buffered
        // off-grid groups first; non-opaque surface layers still use the normal
        // deferred buffer source and its shader/RenderType ordering.
        if (USE_SURFACE_VBO && !surfaces.isEmpty()) buffers.endBatch();
        updateSharedSurfaceEdges(surfaces);
''')

replace_once(
'''        SURFACE_MESHES.keySet().removeIf(id -> !current.contains(id));
''',
'''        SURFACE_MESHES.entrySet().removeIf(entry -> {
            if (current.contains(entry.getKey())) return false;
            releaseSurfaceGpu(entry.getValue());
            return true;
        });
''')

replace_once(
'''        } else if (cached.surface() != surface
                && !TransformConstructionClientControls.previewingSurface(
                        surface.id())) {
            if (!sameSurfaceGeometry(cached.surface(), surface)) {
                cached = buildSurfaceMesh(minecraft, surface);
            } else {
                cached = updateSurfaceMesh(minecraft, surface, cached);
            }
            SURFACE_MESHES.put(surface.id(), cached);
''',
'''        } else if (cached.surface() != surface
                && !TransformConstructionClientControls.previewingSurface(
                        surface.id())) {
            CachedSurface previous = cached;
            if (!sameSurfaceGeometry(cached.surface(), surface)) {
                cached = buildSurfaceMesh(minecraft, surface);
            } else {
                cached = updateSurfaceMesh(minecraft, surface, cached);
            }
            releaseSurfaceGpu(previous);
            SURFACE_MESHES.put(surface.id(), cached);
''')

replace_once(
'''            renderSurfaceLayers(pose, buffers, batch.layers());
''',
'''            renderSurfaceBatch(pose, buffers, batch);
''')

old='''    private static void renderSurfaceLayers(PoseStack pose,
            MultiBufferSource.BufferSource buffers,
            Map<RenderType, List<PreparedVertex>> layers) {
        for (Map.Entry<RenderType, List<PreparedVertex>> layer
                : layers.entrySet()) {
            VertexConsumer consumer = buffers.getBuffer(layer.getKey());
            for (PreparedVertex vertex : layer.getValue()) {
                consumer.vertex(pose.last().pose(),
                                (float) vertex.position().x,
                                (float) vertex.position().y,
                                (float) vertex.position().z)
                        .color(vertex.red(), vertex.green(),
                                vertex.blue(), 255)
                        .uv(vertex.u(), vertex.v())
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(vertex.fallbackLight())
                        .normal(pose.last().normal(),
                                (float) vertex.normal().x,
                                (float) vertex.normal().y,
                                (float) vertex.normal().z)
                        .endVertex();
            }
        }
    }
'''
new='''    private static void renderSurfaceBatch(PoseStack pose,
            MultiBufferSource.BufferSource buffers, CachedSurfaceBatch batch) {
        for (Map.Entry<RenderType, List<PreparedVertex>> layer
                : batch.layers().entrySet()) {
            RenderType type = layer.getKey();
            List<PreparedVertex> vertices = layer.getValue();
            // Transparent and custom render types must preserve their normal
            // sorting and shader state. Never bake their draw order into a VBO.
            if (USE_SURFACE_VBO && type == RenderType.solid()
                    && vertices.size() >= MIN_SURFACE_VBO_VERTICES
                    && vertices.size() % 4 == 0
                    && vertices.size() <= MAX_SURFACE_VBO_VERTICES) {
                SurfaceGpuBatch gpu = surfaceGpuBatch(batch, vertices);
                if (gpu != null) {
                    // A previous small solid CPU batch may still be queued.
                    buffers.endBatch(RenderType.solid());
                    drawSurfaceGpu(pose, type, gpu);
                    continue;
                }
            }
            renderSurfaceLayer(pose, buffers, type, vertices);
        }
    }

    private static void renderSurfaceLayer(PoseStack pose,
            MultiBufferSource.BufferSource buffers, RenderType type,
            List<PreparedVertex> vertices) {
        VertexConsumer consumer = buffers.getBuffer(type);
        // PoseStack matrices never change within the immutable batch.
        org.joml.Matrix4f model = pose.last().pose();
        org.joml.Matrix3f normals = pose.last().normal();
        for (PreparedVertex vertex : vertices) {
            consumer.vertex(model,
                            (float) vertex.position().x,
                            (float) vertex.position().y,
                            (float) vertex.position().z)
                    .color(vertex.red(), vertex.green(),
                            vertex.blue(), 255)
                    .uv(vertex.u(), vertex.v())
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(vertex.fallbackLight())
                    .normal(normals,
                            (float) vertex.normal().x,
                            (float) vertex.normal().y,
                            (float) vertex.normal().z)
                    .endVertex();
        }
    }

    private static SurfaceGpuBatch surfaceGpuBatch(
            CachedSurfaceBatch batch, List<PreparedVertex> vertices) {
        SurfaceGpuKey key = new SurfaceGpuKey(batch);
        SurfaceGpuBatch cached = SURFACE_GPU_BATCHES.get(key);
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
        Vec3 origin = batch.bounds() == null ? Vec3.ZERO
                : batch.bounds().getCenter();
        SURFACE_GPU_BUILDER.begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.BLOCK);
        for (PreparedVertex vertex : vertices) {
            SURFACE_GPU_BUILDER.vertex(
                            (float) (vertex.position().x - origin.x),
                            (float) (vertex.position().y - origin.y),
                            (float) (vertex.position().z - origin.z))
                    .color(vertex.red(), vertex.green(), vertex.blue(), 255)
                    .uv(vertex.u(), vertex.v())
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(vertex.fallbackLight())
                    .normal((float) vertex.normal().x,
                            (float) vertex.normal().y,
                            (float) vertex.normal().z)
                    .endVertex();
        }
        VertexBuffer gpuBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        gpuBuffer.bind();
        try {
            gpuBuffer.upload(SURFACE_GPU_BUILDER.end());
        } finally {
            VertexBuffer.unbind();
        }
        SurfaceGpuBatch result = new SurfaceGpuBatch(gpuBuffer, origin,
                vertices.size());
        SURFACE_GPU_BATCHES.put(key, result);
        surfaceGpuVertices += vertices.size();
        return result;
    }

    private static void drawSurfaceGpu(PoseStack pose, RenderType type,
            SurfaceGpuBatch gpu) {
        type.setupRenderState();
        try {
            gpu.buffer().bind();
            // The regular BufferSource path applies pose.last() to each world
            // vertex and then RenderSystem's current model view on upload.
            // This is the same transform, with a batch-relative origin to
            // avoid losing precision on distant world coordinates.
            org.joml.Matrix4f model =
                    new org.joml.Matrix4f(RenderSystem.getModelViewMatrix())
                            .mul(pose.last().pose())
                            .translate((float) gpu.origin().x,
                                    (float) gpu.origin().y,
                                    (float) gpu.origin().z);
            gpu.buffer().drawWithShader(model,
                    RenderSystem.getProjectionMatrix(),
                    RenderSystem.getShader());
        } finally {
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }

    private static void releaseSurfaceGpu(CachedSurface surface) {
        if (surface == null) return;
        for (CachedSurfaceBatch batch : surface.batches()) {
            SurfaceGpuBatch removed = SURFACE_GPU_BATCHES.remove(
                    new SurfaceGpuKey(batch));
            if (removed == null) continue;
            surfaceGpuVertices -= removed.vertexCount();
            disposeGpuBuffer(removed.buffer());
        }
    }

    private static void invalidateSurfaceMesh(UUID id) {
        releaseSurfaceGpu(SURFACE_MESHES.remove(id));
    }

    private static void clearSurfaceGpuCache() {
        for (SurfaceGpuBatch batch : SURFACE_GPU_BATCHES.values())
            disposeGpuBuffer(batch.buffer());
        SURFACE_GPU_BATCHES.clear();
        surfaceGpuVertices = 0;
    }

    private static void disposeGpuBuffer(VertexBuffer buffer) {
        if (RenderSystem.isOnRenderThread()) buffer.close();
        else RenderSystem.recordRenderCall(buffer::close);
    }

    // Identity keys: record equality on the underlying List<PreparedVertex>
    // would otherwise compare entire large meshes on each per-frame lookup.
    private record SurfaceGpuKey(CachedSurfaceBatch batch) {
        @Override public int hashCode() {
            return System.identityHashCode(batch);
        }
        @Override public boolean equals(Object other) {
            return other instanceof SurfaceGpuKey key && key.batch == batch;
        }
    }

    private record SurfaceGpuBatch(VertexBuffer buffer, Vec3 origin,
            int vertexCount) { }
'''
replace_once(old,new)

replace_once(
'''        SURFACE_MESHES.keySet().removeAll(affected);
''',
'''        for (UUID id : affected) invalidateSurfaceMesh(id);
''')

assert s.count('renderSurfaceBatch(pose, buffers, batch);') == 1
assert 'LINKED_PARENT_EDGE_KNOTS' in s and 'linkedParentShellPosition' in s
assert 'SURFACE_MESHES.keySet().removeAll' not in s
path.write_text(s,encoding='utf-8')
print('Opaque curved-surface batches now have a bounded, invalidation-aware GPU cache; all existing geometry and contact topology were preserved.')
