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

mapfile="src/main/java/com/bl4ues/scpclassifieddirective/client/scp079/Scp079FacilityMapScreen.java"
controls="src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientControls.java"

rep(mapfile,
'''    private long doorTopologyRefreshAt;
''',
'''    private long doorTopologyRefreshAt;
    // One antialias coverage field for the complete floor. Drawing every room
    // independently blends shared pixels repeatedly and creates the bright
    // speckles visible at off-grid/curved joins.
    private final Map<Long, Double> frameContourCoverage = new HashMap<>();
    private final Map<Long, Integer> frameContourColors = new HashMap<>();
''',
1)

rep(mapfile,
'''        MapTransform transform = transformFor(floor, mapZoom, panX, panY);
        if (transform == null) return;
''',
'''        MapTransform transform = transformFor(floor, mapZoom, panX, panY);
        if (transform == null) return;
        frameContourCoverage.clear();
        frameContourColors.clear();
''',
1)

rep(mapfile,
'''        renderDoorMarkers(graphics, floor, transform, geometryByRoom,
                mouseX, mouseY);
''',
'''        flushFrameContours(graphics);
        renderDoorMarkers(graphics, floor, transform, geometryByRoom,
                mouseX, mouseY);
''',
1)

rep(mapfile,
'''    private static void renderFacilityRoomOutlineGeometry(GuiGraphics graphics,
            FacilityRoomOutlineGeometry geometry, MapTransform transform,
            int fill, int lineColor) {
''',
'''    private void renderFacilityRoomOutlineGeometry(GuiGraphics graphics,
            FacilityRoomOutlineGeometry geometry, MapTransform transform,
            int fill, int lineColor) {
''',
1)

rep(mapfile,
'''        drawMapContours(graphics, geometry.contours(),
                transform, lineColor);
''',
'''        accumulateFrameContours(graphics, geometry.contours(),
                transform, lineColor);
''',
1)

old='''    private static void drawMapContours(GuiGraphics graphics,
            List<List<FacilityFloorPatch.Vertex>> contours,
            MapTransform transform, int color) {
        if (contours == null || contours.isEmpty()) return;
        // Accumulate the whole room in one coverage buffer. Independent
        // contours previously painted their shared/join pixels separately,
        // producing bright/dark seams along off-grid and curved unions.
        Map<Long, Double> coverage = new HashMap<>();
        for (List<FacilityFloorPatch.Vertex> contour : contours) {
            if (contour == null || contour.size() < 2) continue;
            for (int index = 0; index < contour.size(); index++) {
                FacilityFloorPatch.Vertex a = contour.get(index);
                FacilityFloorPatch.Vertex b = contour.get(
                        (index + 1) % contour.size());
                accumulateMapLine(coverage,
                        transform.fx(a.x()), transform.fy(a.z()),
                        transform.fx(b.x()), transform.fy(b.z()),
                        graphics.guiWidth(), graphics.guiHeight());
            }
        }
        for (Map.Entry<Long, Double> pixel : coverage.entrySet()) {
            int x = (int) (pixel.getKey() >> 32);
            int y = (int) (long) pixel.getKey();
            plotMapPixel(graphics, x, y, color, pixel.getValue());
        }
    }
'''
new='''    private void accumulateFrameContours(GuiGraphics graphics,
            List<List<FacilityFloorPatch.Vertex>> contours,
            MapTransform transform, int color) {
        if (contours == null || contours.isEmpty()) return;
        // Keep MAX coverage for the entire floor, not merely for one room.
        // The latest visible room owns an overlapping pixel, matching the room
        // draw order while avoiding translucent alpha-over bright spots.
        Map<Long, Double> local = new HashMap<>();
        for (List<FacilityFloorPatch.Vertex> contour : contours) {
            if (contour == null || contour.size() < 2) continue;
            for (int index = 0; index < contour.size(); index++) {
                FacilityFloorPatch.Vertex a = contour.get(index);
                FacilityFloorPatch.Vertex b = contour.get(
                        (index + 1) % contour.size());
                accumulateMapLine(local,
                        transform.fx(a.x()), transform.fy(a.z()),
                        transform.fx(b.x()), transform.fy(b.z()),
                        graphics.guiWidth(), graphics.guiHeight());
            }
        }
        local.forEach((key, value) -> {
            double previous = frameContourCoverage.getOrDefault(key, -1.0D);
            if (value + 1.0E-6D >= previous) {
                frameContourCoverage.put(key, value);
                frameContourColors.put(key, color);
            }
        });
    }

    private void flushFrameContours(GuiGraphics graphics) {
        for (Map.Entry<Long, Double> pixel : frameContourCoverage.entrySet()) {
            int x = (int) (pixel.getKey() >> 32);
            int y = (int) (long) pixel.getKey();
            int color = frameContourColors.getOrDefault(pixel.getKey(),
                    0xFFB8D8E1);
            plotMapPixel(graphics, x, y, color, pixel.getValue());
        }
    }
'''
rep(mapfile,old,new,1)

rep(controls,
'''        for (ConstructionSurface surface
                : TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location())) {
            for (SurfaceHandle handle : SurfaceHandle.values()) {
''',
'''        for (ConstructionSurface surface
                : TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location())) {
            Vec3 center = surface.gridPoint(0.5D, 0.5D);
            double radius = Math.max(surface.width(), surface.height())
                    * 0.75D + 2.0D;
            double broadReach = HANDLE_MAX_DISTANCE + radius;
            if (center.distanceToSqr(eye) > broadReach * broadReach) continue;
            for (SurfaceHandle handle : SurfaceHandle.values()) {
''',
1)
