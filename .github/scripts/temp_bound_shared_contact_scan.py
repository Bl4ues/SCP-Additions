from pathlib import Path
p = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = p.read_text(encoding='utf-8')
start = s.index('    private static RoofCuts linkedParentQuadCuts(')
end = s.index('    private static RoofCuts roofCuts(', start)
replacement = '''    private static RoofCuts linkedParentQuadCuts(
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            Vec3[] points, int xSteps, int ySteps) {
        Map<Integer, List<Double>> edges =
                LINKED_PARENT_EDGE_KNOTS.get(surface.id());
        if (edges == null || edges.isEmpty())
            return new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
        java.util.TreeSet<Double> sCuts =
                new java.util.TreeSet<>(uniformCuts(xSteps));
        java.util.TreeSet<Double> tCuts =
                new java.util.TreeSet<>(uniformCuts(ySteps));
        double xS = bilerp(points, 1.0D, 0.5D).x
                - bilerp(points, 0.0D, 0.5D).x;
        double xT = bilerp(points, 0.5D, 1.0D).x
                - bilerp(points, 0.5D, 0.0D).x;
        double yS = bilerp(points, 1.0D, 0.5D).y
                - bilerp(points, 0.0D, 0.5D).y;
        double yT = bilerp(points, 0.5D, 1.0D).y
                - bilerp(points, 0.5D, 0.0D).y;
        double x0 = bilerp(points, 0.0D, 0.5D).x;
        double xt0 = bilerp(points, 0.5D, 0.0D).x;
        double y0 = bilerp(points, 0.0D, 0.5D).y;
        double yt0 = bilerp(points, 0.5D, 0.0D).y;
        // A long parent edge can contain >2,000 shared knots. Do not copy
        // and scan its entire edge for every baked face of every parent cell.
        // Binary-search only the 24 or so knots INSIDE this logical cell.
        addParentCellAxisCuts(edges.get(2), slot.column(),
                surface.columns(), surface.flipped(), xS, xT,
                x0, xt0, sCuts, tCuts);
        addParentCellAxisCuts(edges.get(3), slot.column(),
                surface.columns(), surface.flipped(), xS, xT,
                x0, xt0, sCuts, tCuts);
        addParentCellAxisCuts(edges.get(0), slot.row(),
                surface.rows(), false, yS, yT,
                y0, yt0, sCuts, tCuts);
        addParentCellAxisCuts(edges.get(1), slot.row(),
                surface.rows(), false, yS, yT,
                y0, yt0, sCuts, tCuts);
        return new RoofCuts(List.copyOf(sCuts), List.copyOf(tCuts));
    }

    private static void addParentCellAxisCuts(List<Double> knots,
            int cell, int cells, boolean flipped,
            double deltaS, double deltaT, double startS, double startT,
            java.util.TreeSet<Double> sCuts,
            java.util.TreeSet<Double> tCuts) {
        if (knots == null || knots.isEmpty()
                || Math.abs(deltaS) <= 0.20D
                        && Math.abs(deltaT) <= 0.20D)
            return;
        double from = cell / (double) cells;
        double to = (cell + 1.0D) / cells;
        int index = java.util.Collections.binarySearch(knots, from);
        if (index < 0) index = -index - 1;
        else index++; // the current cell's endpoint is already a vertex
        for (; index < knots.size(); index++) {
            double fraction = knots.get(index);
            if (fraction >= to - 1.0E-10D) break;
            double local = fraction * cells - cell;
            double model = flipped ? 1.0D - local : local;
            if (Math.abs(deltaS) > 0.20D
                    && Math.abs(deltaT) < 1.0E-6D)
                addRoofCut(sCuts, (model - startS) / deltaS);
            else if (Math.abs(deltaT) > 0.20D
                    && Math.abs(deltaS) < 1.0E-6D)
                addRoofCut(tCuts, (model - startT) / deltaT);
        }
    }

'''
s = s[:start] + replacement + s[end:]
assert 'LINKED_PARENT_EDGE_KNOTS.get(surface.id())' in s
assert 'java.util.Collections.binarySearch(knots, from)' in s
assert 'roofLongitudinalCuts(surface, slot,' in s
assert 'coveredLinkedRoofCap' not in s
p.write_text(s, encoding='utf-8')
print('Only the parent knots within each block are inspected; shared topology unchanged')
