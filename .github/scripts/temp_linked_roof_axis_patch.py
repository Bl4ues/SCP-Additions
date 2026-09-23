from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
source = path.read_text()

def change(before, after):
    global source
    occurrences = source.count(before)
    if occurrences != 1:
        raise RuntimeError(f'Expected exactly one linked roof patch anchor, got {occurrences}: {before[:100]!r}')
    source = source.replace(before, after, 1)

change('''        double x0 = bilerp(points, 0.0D, 0.5D).x;
        double xDelta = bilerp(points, 1.0D, 0.5D).x - x0;
        double xAcross = bilerp(points, 0.5D, 1.0D).x
                - bilerp(points, 0.5D, 0.0D).x;
        double y0 = bilerp(points, 0.5D, 0.0D).y;
        double yDelta = bilerp(points, 0.5D, 1.0D).y - y0;
        double yAlong = bilerp(points, 1.0D, 0.5D).y
                - bilerp(points, 0.0D, 0.5D).y;
        // Only broad, flat/model front-back quads have two real parametric
        // axes. Side caps continue through the ordinary per-quad renderer.
        if (Math.abs(xDelta) < 0.20D || Math.abs(yDelta) < 0.20D
                || Math.abs(xAcross) > 1.0E-4D
                || Math.abs(yAlong) > 1.0E-4D) return false;
''','''        // Minecraft's baked model faces do NOT guarantee that vertex 0->1
        // follows local X. Some front/back quads have the X and Y axes swapped.
        // The old zipper silently fell back to a single, combined grid for
        // those faces. Detect the two actual model axes and preserve the
        // original quad winding after mapping the independent parent grids.
        double xS = bilerp(points, 1.0D, 0.5D).x
                - bilerp(points, 0.0D, 0.5D).x;
        double xT = bilerp(points, 0.5D, 1.0D).x
                - bilerp(points, 0.5D, 0.0D).x;
        double yS = bilerp(points, 1.0D, 0.5D).y
                - bilerp(points, 0.0D, 0.5D).y;
        double yT = bilerp(points, 0.5D, 1.0D).y
                - bilerp(points, 0.5D, 0.0D).y;
        boolean swapped = Math.abs(xT) > 0.20D
                && Math.abs(yS) > 0.20D
                && Math.abs(xS) < 1.0E-4D
                && Math.abs(yT) < 1.0E-4D;
        boolean canonical = Math.abs(xS) > 0.20D
                && Math.abs(yT) > 0.20D
                && Math.abs(xT) < 1.0E-4D
                && Math.abs(yS) < 1.0E-4D;
        // The side caps do not span both local axes; leave them to the
        // regular face renderer, which preserves their actual model shape.
        if (!swapped && !canonical) return false;
        double x0 = swapped ? bilerp(points, 0.5D, 0.0D).x
                : bilerp(points, 0.0D, 0.5D).x;
        double xDelta = swapped ? xT : xS;
        double y0 = swapped ? bilerp(points, 0.0D, 0.5D).y
                : bilerp(points, 0.5D, 0.0D).y;
        double yDelta = swapped ? yS : yT;
''')

change('''        List<Double> first = roofLongitudinalCuts(surface, slot, x0,
                xDelta, xSteps, bridge.firstProfile().size() - 1);
        List<Double> second = roofLongitudinalCuts(surface, slot, x0,
                xDelta, xSteps, bridge.secondProfile().size() - 1);
        java.util.TreeSet<Double> central = new java.util.TreeSet<>(first);
        central.addAll(second);
        List<Double> crown = List.copyOf(central);
        java.util.TreeSet<Double> across = new java.util.TreeSet<>(
                uniformCuts(Math.max(2, ySteps)));
        double localCrown = surface.rows() * 0.5D - slot.row();
        addRoofCut(across, (localCrown - y0) / yDelta);
        List<Double> transverse = List.copyOf(across);
''','''        // Only the actual CONTACT strip inherits the full parent polyline.
        // Each parent owns its own vertex count on its respective boundary;
        // the center is a shared, inexpensive four-cut grid. A zipper between
        // each contact strip and the center accepts arbitrary vertex counts
        // without multiplying both parent grids across every roof cell.
        List<Double> interior = uniformCuts(Math.max(2,
                Math.min(4, xSteps)));
        List<Double> first = slot.row() == 0
                ? roofLongitudinalCuts(surface, slot, x0, xDelta, xSteps,
                        bridge.firstProfile().size() - 1) : interior;
        List<Double> second = slot.row() == surface.rows() - 1
                ? roofLongitudinalCuts(surface, slot, x0, xDelta, xSteps,
                        bridge.secondProfile().size() - 1) : interior;
        java.util.TreeSet<Double> across = new java.util.TreeSet<>(
                uniformCuts(Math.max(2, Math.min(8, ySteps))));
        double localCrown = surface.rows() * 0.5D - slot.row();
        addRoofCut(across, (localCrown - y0) / yDelta);
        List<Double> transverse = List.copyOf(across);
        // Reversing the baked s/t axes reverses triangle winding. Repeat the
        // final vertex so the second triangle of the Minecraft quad is truly
        // degenerate rather than drawing a second, opposite-wound face.
        int[] cornerOrder = swapped ? new int[] {0, 2, 1, 1}
                : new int[] {0, 1, 2, 3};
''')

change('''            List<Double> a = roofBandCuts(surface, slot, y0, yDelta,
                    from, first, second, crown);
            List<Double> b = roofBandCuts(surface, slot, y0, yDelta,
                    to, first, second, crown);
''','''            List<Double> a = roofBandCuts(surface, slot, y0, yDelta,
                    from, first, second, interior);
            List<Double> b = roofBandCuts(surface, slot, y0, yDelta,
                    to, first, second, interior);
''')

change('''                for (int vertex = 0; vertex < 4; vertex++) {
                    double s = ss[vertex], t = tt[vertex];
                    RoofVertexKey key = new RoofVertexKey(s, t);
''','''                for (int vertex = 0; vertex < 4; vertex++) {
                    int corner = cornerOrder[vertex];
                    double s = swapped ? tt[corner] : ss[corner];
                    double t = swapped ? ss[corner] : tt[corner];
                    RoofVertexKey key = new RoofVertexKey(s, t);
''')

change('''        double fraction = (slot.row() + y0 + yDelta * t)
                / surface.rows();
        if (Math.abs(fraction - 0.5D) < 1.0E-6D) return central;
        return fraction < 0.5D ? first : second;
''','''        double fraction = (slot.row() + y0 + yDelta * t)
                / surface.rows();
        // Parent samples belong ONLY to the contact row; interior bands can
        // interpolate between the parent's dense mesh and the low-cost crown.
        if (slot.row() == 0 && Math.abs(fraction) < 1.0E-7D)
            return first;
        if (slot.row() == surface.rows() - 1
                && Math.abs(fraction - 1.0D) < 1.0E-7D)
            return second;
        return central;
''')

path.write_text(source)
print('Applied orientation-independent linked roof zipper and border-only parent grids')
