from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{path}: expected exactly one replacement, got {count}: {old[:90]!r}')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')

renderer = 'src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java'
replace_once(renderer, '''    private record MatchedSurfaceEdge(ConstructionSurface other,
            int otherEdge, boolean reversed, boolean partial,
            List<Vec3> samples) { }''', '''    private record MatchedSurfaceEdge(ConstructionSurface other,
            int otherEdge, boolean reversed, boolean partial,
            List<Vec3> samples, Map<Long, Double> projectionCache) { }''')
replace_once(renderer, '''                        if (dot <= -0.2D || dot >= 0.985D) continue;
                        Vec3 otherStart = edgePoint(other, otherEdge,''', '''                        // Nearly tangent neighbouring curves still share a
                        // physical seam. Reject opposite-facing overlays, not
                        // the smooth transition from ceiling into wall.
                        if (dot <= -0.2D) continue;
                        Vec3 otherStart = edgePoint(other, otherEdge,''')
replace_once(renderer, '''                                endFraction < startFraction, partial, samples);''', '''                                endFraction < startFraction, partial, samples,
                                new HashMap<>());''')
replace_once(renderer, '''            double otherFraction = closestEdgeFraction(match.samples(),
                    sourcePoint);''', '''            // Each boundary coordinate is shared by several model quads and
            // both depth faces. Project it once per committed seam geometry.
            double otherFraction = match.projectionCache().computeIfAbsent(
                    Math.round(fraction * 1_000_000.0D), ignored ->
                            closestEdgeFraction(match.samples(), sourcePoint));''')
replace_once(renderer, '''            if (dot <= -0.2D || dot >= 0.985D) continue;
            // Compute the closest intersection of the two displaced faces.''', '''            if (dot <= -0.2D) continue;
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
            // Compute the closest intersection of the two displaced faces.''')
replace_once(renderer, '''            result = match.partial()
                    ? otherPoint.add(otherNormal.scale(depth))
                    : intersection;
            if (++found > 1) return null;
        }
        return found == 1 ? result : null;
''', '''            Vec3 candidate = match.partial()
                    ? otherPoint.add(otherNormal.scale(depth))
                    : intersection;
            // A three-way wall/ceiling corner may have two legitimate shared
            // edges. The old ambiguity fallback returned null and left a hole.
            result = result == null ? candidate : result.add(candidate);
            found++;
        }
        return found > 0 ? result.scale(1.0D / found) : null;
''')

map_file = 'src/main/java/com/bl4ues/scpclassifieddirective/client/scp079/Scp079FacilityMapScreen.java'
p = Path(map_file)
text = p.read_text(encoding='utf-8')
start = text.index('    private static void renderDoorPath(GuiGraphics graphics,')
end = text.index('    private static Vec3 lerp(Vec3 a, Vec3 b, double t)', start)
old = text[start:end]
assert old.count('drawDoorSegment(graphics') == 2, 'Door path changed; inspect before editing'
new = '''    private static void renderDoorPath(GuiGraphics graphics,
            MapDoorMarker marker, MapTransform transform, int color,
            boolean open) {
        List<Vec3> path = doorPath(marker);
        if (path.size() < 2) return;

        double total = 0.0D;
        double[] lengths = new double[path.size() - 1];
        for (int index = 0; index + 1 < path.size(); index++) {
            lengths[index] = horizontalDistance(path.get(index),
                    path.get(index + 1));
            total += lengths[index];
        }
        if (total < 1.0E-7D) return;

        // Rasterize all segments of a logical door into one coverage map.
        // Independent translucent segments used to overdraw their shared ends,
        // leaving bright dots at the vertices of off-grid/curved doors.
        Map<Long, Double> coverage = new HashMap<>();
        double travelled = 0.0D;
        for (int index = 0; index + 1 < path.size(); index++) {
            Vec3 a = path.get(index);
            Vec3 b = path.get(index + 1);
            double length = lengths[index];
            if (length < 1.0E-8D) continue;
            double start = travelled / total;
            double end = (travelled + length) / total;
            travelled += length;

            if (!open) {
                accumulateDoorSegment(coverage, graphics, transform, a, b);
                continue;
            }
            accumulateDoorRange(coverage, graphics, transform, a, b,
                    start, end, 0.0D, 0.18D);
            accumulateDoorRange(coverage, graphics, transform, a, b,
                    start, end, 0.82D, 1.0D);
        }
        for (Map.Entry<Long, Double> pixel : coverage.entrySet()) {
            int x = (int) (pixel.getKey() >> 32);
            int y = (int) (long) pixel.getKey();
            plotMapPixel(graphics, x, y, color, pixel.getValue());
        }
    }

    private static void accumulateDoorRange(Map<Long, Double> coverage,
            GuiGraphics graphics, MapTransform transform, Vec3 a, Vec3 b,
            double segmentStart, double segmentEnd,
            double rangeStart, double rangeEnd) {
        double from = Math.max(segmentStart, rangeStart);
        double to = Math.min(segmentEnd, rangeEnd);
        if (to <= from + 1.0E-8D) return;
        double span = segmentEnd - segmentStart;
        double t0 = (from - segmentStart) / span;
        double t1 = (to - segmentStart) / span;
        accumulateDoorSegment(coverage, graphics, transform,
                lerp(a, b, t0), lerp(a, b, t1));
    }

    private static void accumulateDoorSegment(Map<Long, Double> coverage,
            GuiGraphics graphics, MapTransform transform, Vec3 a, Vec3 b) {
        accumulateMapLine(coverage, transform.fx(a.x), transform.fy(a.z),
                transform.fx(b.x), transform.fy(b.z),
                graphics.guiWidth(), graphics.guiHeight());
    }

'''
p.write_text(text[:start] + new + text[end:], encoding='utf-8')
print('Applied tangent/corner seam joins and unified door-path rasterization.')
