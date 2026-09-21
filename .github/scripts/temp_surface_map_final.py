from pathlib import Path

ROOT = Path('src/main/java/com/bl4ues/scpclassifieddirective')

def replace(path, old, new, count=1):
    file = ROOT / path
    text = file.read_text(encoding='utf-8')
    seen = text.count(old)
    if seen != count:
        raise SystemExit(f'{file}: expected {count} occurrences, found {seen}: {old[:85]!r}')
    file.write_text(text.replace(old, new), encoding='utf-8')

renderer = 'facility/transform/client/TransformConstructionClientRenderer.java'
replace(renderer, '''        boolean joinEdge = !overlay
                && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                && fullSurfaceCell(attachment.state())
                && ((slot.column() == 0
                        && Math.abs(point.x) < 1.0E-6D)
                    || (slot.column() == surface.columns() - 1
                        && Math.abs(point.x - 1.0D) < 1.0E-6D)
                    || (slot.row() == 0
                        && Math.abs(point.y) < 1.0E-6D)
                    || (slot.row() == surface.rows() - 1
                        && Math.abs(point.y - 1.0D) < 1.0E-6D));
''', '''        // The authored X axis can be mirrored by Surface.flip. Never infer
        // the world-space border from an unmapped local x=0/1: a flipped first
        // column reaches the physical border at x=1, not x=0. The weld method
        // below checks the final world-side (u,v) before doing any projection.
        boolean joinEdge = !overlay
                && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                && fullSurfaceCell(attachment.state())
                && (((slot.column() == 0
                        || slot.column() == surface.columns() - 1)
                        && (Math.abs(point.x) < 1.0E-6D
                        || Math.abs(point.x - 1.0D) < 1.0E-6D))
                    || (slot.row() == 0
                        && Math.abs(point.y) < 1.0E-6D)
                    || (slot.row() == surface.rows() - 1
                        && Math.abs(point.y - 1.0D) < 1.0E-6D));
''')

# A cap is only a fallback: preserving it should not leave a visible line where
# two surfaces are supposed to join. Use more samples on the edge that actually
# follows a curve; this is still far cheaper than 12x12 on every perimeter tile.
replace(renderer, '''                ? curvedPipe ? 12 : horizontalEdge ? 8 : 2 : 1;
''', '''                ? curvedPipe ? 12 : horizontalEdge ? 16 : 2 : 1;
''')
replace(renderer, '''                ? verticalEdge ? 8 : 2 : 1;
''', '''                ? verticalEdge ? 16 : 2 : 1;
''')

map_file = 'client/scp079/Scp079FacilityMapScreen.java'
replace(map_file, '''        double bestDistance = 12.0D * 12.0D;
''', '''        // Hit testing includes the visible rectangular marker plus a modest
        // mouse margin. It remains reachable at low map zoom.
        double bestDistance = 12.0D * 12.0D;
''')
replace(map_file, '''    private static void accumulateDoorSegment(Map<Long, Double> coverage,
            GuiGraphics graphics, MapTransform transform, Vec3 a, Vec3 b) {
        accumulateMapLine(coverage, transform.fx(a.x), transform.fy(a.z),
                transform.fx(b.x), transform.fy(b.z),
                graphics.guiWidth(), graphics.guiHeight());
    }
''', '''    private static void accumulateDoorSegment(Map<Long, Double> coverage,
            GuiGraphics graphics, MapTransform transform, Vec3 a, Vec3 b) {
        // A door is a selectable, physical slab on the map, not a one-pixel
        // annotation. Its long dimension follows the exact authored doorway.
        double thickness = Mth.clamp(transform.scale() * 0.45D,
                3.5D, 6.0D);
        accumulateMapStroke(coverage, transform.fx(a.x), transform.fy(a.z),
                transform.fx(b.x), transform.fy(b.z), thickness, true,
                graphics.guiWidth(), graphics.guiHeight());
    }
''')

path = ROOT / map_file
text = path.read_text(encoding='utf-8')
start = '''    private static void accumulateMapLine(Map<Long, Double> coverage,
'''
end = '''    private static void accumulateCoverage(Map<Long, Double> coverage,
'''
if text.count(start) != 1 or text.count(end) != 1:
    raise SystemExit('Map stroke method anchors changed')
left = text.index(start)
right = text.index(end, left)
new = '''    private static void accumulateMapLine(Map<Long, Double> coverage,
            double x0, double y0, double x1, double y1,
            int width, int height) {
        accumulateMapStroke(coverage, x0, y0, x1, y1,
                1.05D, false, width, height);
    }

    /** Screen-space 4x4 coverage, with finite segment caps. The previous
     * major-axis loop discarded endpoint pixels whenever their centers fell
     * outside a segment: the next room's vertical border could then stop one
     * pixel short of the curved border it physically met. Round contour caps
     * close only subpixel raster gaps, without extending authored geometry or
     * redrawing overlapping segments brighter. Doors use square end caps. */
    private static void accumulateMapStroke(Map<Long, Double> coverage,
            double x0, double y0, double x1, double y1,
            double thickness, boolean squareEnds, int width, int height) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double lengthSquared = dx * dx + dy * dy;
        double radius = thickness * 0.5D;
        if (lengthSquared < 1.0E-12D) {
            accumulateCoverage(coverage, (int) Math.floor(x0),
                    (int) Math.floor(y0), 1.0D, width, height);
            return;
        }
        boolean steep = Math.abs(dy) > Math.abs(dx);
        double aMajor = steep ? y0 : x0;
        double bMajor = steep ? y1 : x1;
        double aMinor = steep ? x0 : y0;
        double bMinor = steep ? x1 : y1;
        int majorLimit = steep ? height : width;
        int minorLimit = steep ? width : height;
        int first = Math.max(0, (int) Math.floor(
                Math.min(aMajor, bMajor) - radius - 1.0D));
        int last = Math.min(majorLimit - 1, (int) Math.ceil(
                Math.max(aMajor, bMajor) + radius + 1.0D));
        double radiusSquared = radius * radius;
        for (int major = first; major <= last; major++) {
            double t = Mth.clamp((major + 0.5D - aMajor)
                    / (bMajor - aMajor), 0.0D, 1.0D);
            double minorCenter = Mth.lerp(t, aMinor, bMinor);
            int minMinor = Math.max(0, (int) Math.floor(
                    minorCenter - radius - 2.0D));
            int maxMinor = Math.min(minorLimit - 1, (int) Math.ceil(
                    minorCenter + radius + 2.0D));
            for (int minor = minMinor; minor <= maxMinor; minor++) {
                int covered = 0;
                for (int sy = 0; sy < 4; sy++) {
                    for (int sx = 0; sx < 4; sx++) {
                        double px = (steep ? minor : major)
                                + (sx + 0.5D) * 0.25D;
                        double py = (steep ? major : minor)
                                + (sy + 0.5D) * 0.25D;
                        double projection = ((px - x0) * dx
                                + (py - y0) * dy) / lengthSquared;
                        if (squareEnds && (projection < 0.0D
                                || projection > 1.0D)) continue;
                        double along = Mth.clamp(projection, 0.0D, 1.0D);
                        double ox = px - (x0 + along * dx);
                        double oy = py - (y0 + along * dy);
                        if (ox * ox + oy * oy <= radiusSquared) covered++;
                    }
                }
                if (covered > 0) {
                    accumulateCoverage(coverage,
                            steep ? minor : major, steep ? major : minor,
                            covered / 16.0D, width, height);
                }
            }
        }
    }

'''
path.write_text(text[:left] + new + text[right:], encoding='utf-8')
print('Patched Surface border welding and SCP-079 finite supersampled strokes')
