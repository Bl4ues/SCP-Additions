from pathlib import Path

p = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = p.read_text()

def replace_once(old, new):
    global s
    count = s.count(old)
    assert count == 1, f'Expected exactly one replacement, got {count}: {old[:130]!r}'
    s = s.replace(old, new, 1)

# A newly selected Surface or a held BlockItem should not force every nearby
# independent plane to compute thousands of guide line vertices every frame.
# The aimed Surface and the currently selected Surface are already drawn above.
replace_once('''            } else if (placingBlock) {
                for (ConstructionSurface surface : surfaces) {
                    Vec3 center = surface.gridPoint(0.5D, 0.5D);
                    if (center.distanceToSqr(camera) <= 40.0D * 40.0D
                            && !surface.attachments().isEmpty()) {
                        renderSurfaceGrid(pose, lines, surface, camera);
                    }
                }
            } else if (mappingTool) {''', '''            } else if (mappingTool) {''')

# A BlockState has immutable collision geometry for our full-cube test. Avoid
# Shape generation/toAabbs for every quad and tessellated vertex in one slot.
replace_once('''    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();''', '''    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();
    private static final Map<BlockState, Boolean> FULL_SURFACE_CELLS =
            new java.util.IdentityHashMap<>();''')
replace_once('''    public static void clearSurfaceCache() {
        SURFACE_MESHES.clear();''', '''    public static void clearSurfaceCache() {
        SURFACE_MESHES.clear();
        FULL_SURFACE_CELLS.clear();''')
replace_once('''        List<AABB> boxes = state.getCollisionShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                CollisionContext.empty()).toAabbs();''', '''        return FULL_SURFACE_CELLS.computeIfAbsent(state,
                TransformConstructionClientRenderer::computeFullSurfaceCell);
    }

    private static boolean computeFullSurfaceCell(BlockState state) {
        List<AABB> boxes = state.getCollisionShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                CollisionContext.empty()).toAabbs();''')

# A flat authoring plane never needs the four horizontal subdivisions intended
# to approximate quadratic curvature. Preserve the existing curved/perimeter
# tessellation, including the 16-step curved pipes.
replace_once('''        int xSteps = deform && maxX - minX > 0.20D
                ? curvedPipe ? 16 : perimeter''', '''        int xSteps = deform
                && surface.curveOffset().lengthSqr() > 1.0E-8D
                && maxX - minX > 0.20D
                ? curvedPipe ? 16 : perimeter''')

# Keep the existing nearby-distance test, then reject out-of-view whole planes
# before reading or baking their cached mesh. A generous box accounts for curve
# handles, width and surface depth, so grazing bends cannot disappear.
replace_once('''            renderSurfacePayloads(minecraft, pose, buffers, surface, camera);''', '''            renderSurfacePayloads(minecraft, pose, buffers, surface, camera,
                    event.getFrustum());''')
replace_once('''    private static void renderSurfacePayloads(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, ConstructionSurface surface,
            Vec3 camera) {
        Vec3 center = surface.gridPoint(0.5D, 0.5D);
        double radius = Math.max(surface.width(), surface.height()) * 0.75D + 2.0D;
        if (center.distanceToSqr(camera)
                > (192.0D + radius) * (192.0D + radius)) return;''', '''    private static void renderSurfacePayloads(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, ConstructionSurface surface,
            Vec3 camera, net.minecraft.client.renderer.culling.Frustum frustum) {
        Vec3 center = surface.gridPoint(0.5D, 0.5D);
        double radius = Math.max(surface.width(), surface.height())
                + surface.curveOffset().length()
                + surface.heightCurveOffset().length() + 3.0D;
        if (center.distanceToSqr(camera)
                > (192.0D + radius) * (192.0D + radius)) return;
        if (frustum != null && !frustum.isVisible(new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius))) return;''')
replace_once('''        renderSurfaceCache(pose, buffers, cached.slots().values());
        renderSurfaceCache(pose, buffers, cached.overlays().values());''', '''        renderSurfaceCache(pose, buffers, cached.slots().values(), frustum);
        renderSurfaceCache(pose, buffers, cached.overlays().values(), frustum);''')
replace_once('''    private static void renderSurfaceCache(PoseStack pose,
            MultiBufferSource.BufferSource buffers,
            Iterable<CachedSurfaceSlot> cachedSlots) {
        for (CachedSurfaceSlot slot : cachedSlots) {''', '''    private static void renderSurfaceCache(PoseStack pose,
            MultiBufferSource.BufferSource buffers,
            Iterable<CachedSurfaceSlot> cachedSlots,
            net.minecraft.client.renderer.culling.Frustum frustum) {
        for (CachedSurfaceSlot slot : cachedSlots) {
            if (frustum != null && slot.bounds() != null
                    && !frustum.isVisible(slot.bounds())) continue;''')

# Bound the precomputed slot using its actual deformed world vertices, rather
# than a vanilla AABB that can be wrong on curved or flipped walls.
replace_once('''        layers.forEach((type, vertices) ->
                immutable.put(type, List.copyOf(vertices)));
        return new CachedSurfaceSlot(Map.copyOf(immutable));''', '''        layers.forEach((type, vertices) ->
                immutable.put(type, List.copyOf(vertices)));
        AABB bounds = null;
        for (List<PreparedVertex> vertices : immutable.values()) {
            for (PreparedVertex vertex : vertices) {
                Vec3 point = vertex.position();
                AABB position = new AABB(point, point);
                bounds = bounds == null ? position : bounds.minmax(position);
            }
        }
        return new CachedSurfaceSlot(Map.copyOf(immutable),
                bounds == null ? null : bounds.inflate(0.02D));''')
replace_once('''    private record CachedSurfaceSlot(
            Map<RenderType, List<PreparedVertex>> layers) {''', '''    private record CachedSurfaceSlot(
            Map<RenderType, List<PreparedVertex>> layers, AABB bounds) {''')

p.write_text(s)
print('Patched construction mesh culling, full-block geometry memoization, flat-plane tessellation and redundant guides.')
