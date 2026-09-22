from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = path.read_text()
def change(old, new, count=1):
    global s
    n = s.count(old)
    if n != count:
        raise RuntimeError(f'expected {count} anchors, got {n}: {old[:120]!r}')
    s = s.replace(old, new)

change('''        for (int ix = 0; ix < xSteps; ix++) {
            double s0 = ix / (double) xSteps;
            double s1 = (ix + 1.0D) / xSteps;
            for (int iy = 0; iy < ySteps; iy++) {
                double t0 = iy / (double) ySteps;
                double t1 = (iy + 1.0D) / ySteps;
''', '''        // A linked roof is split at the ridge and at the authored cell
        // boundaries of EACH parent, not only at its own rectangular slot
        // boundaries. Both halves can thus contain different longitudinal
        // subdivisions while still sharing a crack-free central grid.
        RoofCuts cuts = surface.bridge() != null && deform && !overlay
                && fullSurfaceCell(attachment.state())
                && surface.bridge().hasProfiles()
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
        for (int ix = 0; ix + 1 < cuts.s().size(); ix++) {
            double s0 = cuts.s().get(ix);
            double s1 = cuts.s().get(ix + 1);
            for (int iy = 0; iy + 1 < cuts.t().size(); iy++) {
                double t0 = cuts.t().get(iy);
                double t1 = cuts.t().get(iy + 1);
''', 1)
anchor='''    private static boolean pipeNeighbor(ConstructionSurface surface,
'''
helper='''    private record RoofCuts(List<Double> s, List<Double> t) { }

    private static List<Double> uniformCuts(int steps) {
        List<Double> cuts = new ArrayList<>(steps + 1);
        for (int index = 0; index <= steps; index++)
            cuts.add(index / (double) steps);
        return cuts;
    }

    private static void addRoofCut(java.util.TreeSet<Double> cuts,
            double fraction) {
        if (fraction > 1.0E-6D && fraction < 1.0D - 1.0E-6D) {
            // Round so a parent boundary nearly coincident with a regular
            // tessellation cut does not create a zero-width sliver.
            cuts.add(Math.rint(fraction * 1.0E7D) / 1.0E7D);
        }
    }

    private static RoofCuts roofCuts(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, Vec3[] points,
            int xSteps, int ySteps) {
        java.util.TreeSet<Double> sCuts = new java.util.TreeSet<>(
                uniformCuts(xSteps));
        java.util.TreeSet<Double> tCuts = new java.util.TreeSet<>(
                uniformCuts(Math.max(ySteps, 2)));
        ConstructionSurface.BridgeAnchor bridge = surface.bridge();
        int columns = surface.columns();

        // The front/back and roof faces can encode their block-model X axis
        // along either quad axis. Determine it from the actual baked positions,
        // not a fixed s/t assumption that fails on rotated/mirrored models.
        double xS0 = bilerp(points, 0.0D, 0.5D).x;
        double xDS = bilerp(points, 1.0D, 0.5D).x - xS0;
        double xT0 = bilerp(points, 0.5D, 0.0D).x;
        double xDT = bilerp(points, 0.5D, 1.0D).x - xT0;
        boolean alongS = Math.abs(xDS) >= Math.abs(xDT)
                && Math.abs(xDS) > 0.20D;
        boolean alongT = !alongS && Math.abs(xDT) > 0.20D;
        if (alongS || alongT) {
            int[] parentCounts = {bridge.firstCells(), bridge.secondCells()};
            double u0 = slot.column() / (double) columns;
            double u1 = (slot.column() + 1.0D) / columns;
            for (int count : parentCounts) {
                if (count <= 0) continue;
                int begin = Math.max(1, (int) Math.ceil(u0 * count - 1.0E-8D));
                int end = Math.min(count - 1,
                        (int) Math.floor(u1 * count + 1.0E-8D));
                for (int k = begin; k <= end; k++) {
                    double localX = k / (double) count * columns
                            - slot.column();
                    double modelX = surface.flipped()
                            ? 1.0D - localX : localX;
                    if (alongS) addRoofCut(sCuts,
                            (modelX - xS0) / xDS);
                    else addRoofCut(tCuts, (modelX - xT0) / xDT);
                }
            }
        }
        // Every panel of a linked ceiling has a true center division. The
        // exact crown has one common position on both halves of the loft.
        double modelY = surface.rows() * 0.5D - slot.row();
        if (modelY > 1.0E-6D && modelY < 1.0D - 1.0E-6D) {
            double yS0 = bilerp(points, 0.0D, 0.5D).y;
            double yDS = bilerp(points, 1.0D, 0.5D).y - yS0;
            double yT0 = bilerp(points, 0.5D, 0.0D).y;
            double yDT = bilerp(points, 0.5D, 1.0D).y - yT0;
            if (Math.abs(yDS) > Math.abs(yDT)
                    && Math.abs(yDS) > 0.20D)
                addRoofCut(sCuts, (modelY - yS0) / yDS);
            else if (Math.abs(yDT) > 0.20D)
                addRoofCut(tCuts, (modelY - yT0) / yDT);
        }
        return new RoofCuts(List.copyOf(sCuts), List.copyOf(tCuts));
    }

'''
change(anchor, helper + anchor, 1)
path.write_text(s)
print('Linked roof mesh now samples both mother grids and the shared crown')
