from pathlib import Path

def rep(path, old, new, count=None):
    p=Path(path)
    s=p.read_text()
    n=s.count(old)
    if n == 0:
        raise SystemExit(f"missing patch anchor in {path}: {old[:100]!r}")
    if count is not None and n != count:
        raise SystemExit(f"unexpected anchor count in {path}: {n} != {count}")
    p.write_text(s.replace(old,new))

renderer="src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java"
door="src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/TransformDoorwayCollision.java"

rep(renderer,
"package com.bl4ues.scpclassifieddirective.facility.transform.client;\n\n",
"package com.bl4ues.scpclassifieddirective.facility.transform.client;\n\nimport com.mojang.blaze3d.systems.RenderSystem;\n",
1)

rep(renderer,
'''        renderSurfaceCache(pose, buffers, cached.slots().values(), frustum);
        renderSurfaceCache(pose, buffers, cached.overlays().values(), frustum);
''',
'''        // Static surface payloads are already culled as one authored object.
        // Re-submit one packed layer per RenderType instead of re-entering the
        // buffer map once for every logical slot on every frame.
        renderSurfaceLayers(pose, buffers, cached.layers());
''',
1)

anchor='''    private static void renderSurfaceCache(PoseStack pose,
            MultiBufferSource.BufferSource buffers,
            Iterable<CachedSurfaceSlot> cachedSlots,
            net.minecraft.client.renderer.culling.Frustum frustum) {
'''
insert='''    private static void renderSurfaceLayers(PoseStack pose,
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
rep(renderer, anchor, insert+anchor, 1)

rep(renderer,
'''        return new CachedSurface(surface, Map.copyOf(slots),
                Map.copyOf(overlays));
''',
'''        Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> frozenSlots =
                Map.copyOf(slots);
        Map<ConstructionSurface.SurfaceOverlaySlot, CachedSurfaceSlot>
                frozenOverlays = Map.copyOf(overlays);
        return new CachedSurface(surface, frozenSlots, frozenOverlays,
                mergeSurfaceLayers(frozenSlots, frozenOverlays));
''')

build_anchor='''    private static CachedSurfaceSlot buildSurfaceSlot(Minecraft minecraft,
'''
merge='''    private static Map<RenderType, List<PreparedVertex>> mergeSurfaceLayers(
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots,
            Map<ConstructionSurface.SurfaceOverlaySlot, CachedSurfaceSlot> overlays) {
        Map<RenderType, List<PreparedVertex>> merged = new LinkedHashMap<>();
        java.util.function.Consumer<CachedSurfaceSlot> append = cached -> {
            if (cached == null) return;
            cached.layers().forEach((type, vertices) ->
                    merged.computeIfAbsent(type,
                            ignored -> new ArrayList<>()).addAll(vertices));
        };
        slots.values().forEach(append);
        overlays.values().forEach(append);
        Map<RenderType, List<PreparedVertex>> frozen = new LinkedHashMap<>();
        merged.forEach((type, vertices) ->
                frozen.put(type, List.copyOf(vertices)));
        return Map.copyOf(frozen);
    }

'''
rep(renderer, build_anchor, merge+build_anchor, 1)

rep(renderer,
'''    private record CachedSurface(ConstructionSurface surface,
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots,
            Map<ConstructionSurface.SurfaceOverlaySlot,
                    CachedSurfaceSlot> overlays) {
    }
''',
'''    private record CachedSurface(ConstructionSurface surface,
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots,
            Map<ConstructionSurface.SurfaceOverlaySlot,
                    CachedSurfaceSlot> overlays,
            Map<RenderType, List<PreparedVertex>> layers) {
    }
''',
1)

rep(renderer,
'''        int columnStep = Math.max(1, (columns + (preview ? 17 : 39))
                / (preview ? 18 : 40));
        int rowStep = Math.max(1, (rows + (preview ? 11 : 19))
                / (preview ? 12 : 20));
''',
'''        int columnStep = Math.max(1, (columns + (preview ? 11 : 23))
                / (preview ? 12 : 24));
        int rowStep = Math.max(1, (rows + (preview ? 7 : 11))
                / (preview ? 8 : 12));
''',
1)
rep(renderer,
'''            int samples = preview ? Math.max(4, Math.min(20, rows))
                    : Math.max(4, Math.min(96, rows * 2));
''',
'''            int samples = preview ? Math.max(4, Math.min(12, rows))
                    : Math.max(4, Math.min(40, rows * 2));
''',
1)
rep(renderer,
'''            int samples = preview ? Math.max(8, Math.min(36, columns))
                    : Math.max(8, Math.min(144, columns * 3));
''',
'''            int samples = preview ? Math.max(8, Math.min(24, columns))
                    : Math.max(8, Math.min(64, columns * 2));
''',
1)

rep(renderer,
'''        int xSteps = deform
                && surface.curveOffset().lengthSqr() > 1.0E-8D
                && maxX - minX > 0.20D
                ? curvedPipe ? 16 : perimeter
                        && surface.curveOffset().lengthSqr() > 1.0E-8D
                        ? 12 : 4 : 1;
        int ySteps = deform
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                && maxY - minY > 0.20D ? perimeter ? 6 : 2 : 1;
''',
'''        // Interior structural tiles do not need the same tessellation as a
        // visible seam. Keep shared boundaries dense so independent planes
        // still weld cleanly, while cutting the normal curved-wall vertex
        // count roughly in half. Pipes retain extra samples for their profile.
        int xSteps = deform
                && surface.curveOffset().lengthSqr() > 1.0E-8D
                && maxX - minX > 0.20D
                ? curvedPipe ? 12 : perimeter ? 12 : 2 : 1;
        int ySteps = deform
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                && maxY - minY > 0.20D ? perimeter ? 12 : 1 : 1;
''',
1)

rep(renderer,
'''                    } else {
                        renderSurfaceOutline(pose, lines, surface, camera);
                    }
''',
'''                    } else {
                        Vec3 outlineCenter = surface.gridPoint(0.5D, 0.5D);
                        if (outlineCenter.distanceToSqr(camera) <= 64.0D * 64.0D) {
                            renderSurfaceOutline(pose, lines, surface, camera);
                        }
                    }
''',
1)

old_gizmo='''        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        renderEditorGizmos(minecraft, pose, buffers,
                event.getCamera().getPosition());
        buffers.endBatch(TransformEditorRenderTypes.GIZMO_LINES);
'''
new_gizmo='''        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        // Some shader pipelines restore their own depth state around the final
        // world composite. Explicitly disable depth for this tiny editor pass
        // so an axis remains visible even when its origin is inside a wall.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        renderEditorGizmos(minecraft, pose, buffers,
                event.getCamera().getPosition());
        buffers.endBatch(TransformEditorRenderTypes.GIZMO_LINES);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
'''
rep(renderer,old_gizmo,new_gizmo,1)

old_join='''            Vec3 otherNormal = other.gridNormal(otherU, otherV);
            double dot = sourceNormal.dot(otherNormal);
            if (dot <= -0.2D) continue;
            // Nearly parallel edge faces have no stable intersection line.
            // Their common point is the midpoint of the neighbouring sampled
            // boundary, keeping both existing curves and eliminating the slit.
            if (dot >= 0.985D) {
                Vec3 sharedNormal = TransformMath.safeNormalize(
                        sourceNormal.add(otherNormal), sourceNormal);
                Vec3 candidate = sourcePoint.add(otherPoint).scale(0.5D)
                        .add(sharedNormal.scale(depth));
                result = result == null ? candidate : result.add(candidate);
                found++;
                continue;
            }
            // Compute the closest intersection of the two displaced faces.
            // Unlike the old average-normal miter, this handles inherited
            // rooms whose independent curves are close, but not coincident.
            double determinant = 1.0D - dot * dot;
            if (determinant < 0.025D) continue;
            Vec3 middle = sourcePoint.add(otherPoint).scale(0.5D);
            double sourceHeight = depth
                    - middle.subtract(sourcePoint).dot(sourceNormal);
            double otherHeight = depth
                    - middle.subtract(otherPoint).dot(otherNormal);
            double sourceShift = (sourceHeight - dot * otherHeight)
                    / determinant;
            double otherShift = (otherHeight - dot * sourceHeight)
                    / determinant;
            Vec3 intersection = middle.add(sourceNormal.scale(sourceShift))
                    .add(otherNormal.scale(otherShift));
            if (intersection.distanceToSqr(middle) > 2.25D) continue;
            // An edge ending inside a longer neighbour must meet its existing
            // outer face, which has no counterpart boundary to move.
            Vec3 candidate = match.partial()
                    ? otherPoint.add(otherNormal.scale(depth))
                    : intersection;
            // A three-way wall/ceiling corner may have two legitimate shared
            // edges. The old ambiguity fallback returned null and left a hole.
            result = result == null ? candidate : result.add(candidate);
            found++;
'''
new_join='''            Vec3 otherNormal = other.gridNormal(otherU, otherV);
            double dot = sourceNormal.dot(otherNormal);
            if (dot <= -0.2D) continue;
            // Weld both authored borders to one canonical miter curve. The old
            // implementation solved the intersection independently from each
            // Surface, so slightly different arc parameterization produced two
            // almost-equal edges and a bright slit. Midpoint + bisector is
            // symmetric: both meshes reach the exact same world-space vertex
            // while their original curves remain unchanged away from the edge.
            Vec3 sharedBase = sourcePoint.add(otherPoint).scale(0.5D);
            Vec3 bisector = TransformMath.safeNormalize(
                    sourceNormal.add(otherNormal), sourceNormal);
            double projection = Math.abs(bisector.dot(sourceNormal));
            if (projection < 0.12D) continue;
            Vec3 candidate = sharedBase.add(
                    bisector.scale(depth / projection));
            if (candidate.distanceToSqr(sharedBase) > 3.0D) continue;
            result = result == null ? candidate : result.add(candidate);
            found++;
'''
rep(renderer,old_join,new_join,1)

rep(renderer,
'''                        boolean same = first.distanceToSqr(otherFirst)
                                < 0.0225D && last.distanceToSqr(otherLast)
                                < 0.0225D;
                        boolean reverse = !same
                                && first.distanceToSqr(otherLast) < 0.0225D
                                && last.distanceToSqr(otherFirst) < 0.0225D;
''',
'''                        boolean same = first.distanceToSqr(otherFirst)
                                < 0.1225D && last.distanceToSqr(otherLast)
                                < 0.1225D;
                        boolean reverse = !same
                                && first.distanceToSqr(otherLast) < 0.1225D
                                && last.distanceToSqr(otherFirst) < 0.1225D;
''',
1)
rep(renderer,
'''                        double score = first.distanceToSqr(otherStart)
                                + middle.distanceToSqr(otherMiddle)
                                + last.distanceToSqr(otherEnd);
                        if (score >= bestScore) continue;
''',
'''                        double score = first.distanceToSqr(otherStart)
                                + middle.distanceToSqr(otherMiddle)
                                + last.distanceToSqr(otherEnd);
                        if (score > 0.3675D || score >= bestScore) continue;
''',
1)

rep(door,
'''    private static final double CLEAR_HALF_WIDTH = 0.59D;
    private static final double CLEAR_BELOW = 0.48D;
    private static final double CLEAR_ABOVE = 1.55D;
    private static final double EPSILON = 1.0E-6D;
    private static final int PASSAGE_STEPS = 9;
    private static final double PASSAGE_STEP_LENGTH = 0.30D;
    private static final double PASSAGE_HALF_DEPTH = 0.32D;
''',
'''    private static final double CLEAR_HALF_WIDTH = 0.72D;
    private static final double CLEAR_BELOW = 0.56D;
    private static final double CLEAR_ABOVE = 1.78D;
    private static final double EPSILON = 1.0E-6D;
    private static final int PASSAGE_STEPS = 13;
    private static final double PASSAGE_STEP_LENGTH = 0.22D;
    private static final double PASSAGE_HALF_DEPTH = 0.70D;
''',
1)
