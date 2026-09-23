from pathlib import Path

p = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = p.read_text()
def rep(old, new, times=1):
    global s
    count=s.count(old)
    if count != times:
        raise SystemExit(f'Unexpected renderer patch anchor count {count} vs {times}: {old[:120]!r}')
    s=s.replace(old,new)

rep('''    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();
''','''    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();
    // Linked roofs reuse the SAME rendered parent-shell samples for thousands
    // of roof vertices. Keep only nearby/recent edge/depth pairs to bound VRAM
    // and heap in facilities with many rooms.
    private record ParentShellKey(UUID id, int edge, int layer) { }
    private static final Map<ParentShellKey, Vec3[]> PARENT_SHELLS =
            new LinkedHashMap<>(32, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<ParentShellKey, Vec3[]> eldest) {
                    return size() > 32;
                }
            };
''')
rep('''        SURFACE_MESHES.clear();
        FULL_SURFACE_CELLS.clear();
''','''        SURFACE_MESHES.clear();
        PARENT_SHELLS.clear();
        FULL_SURFACE_CELLS.clear();
''')
rep('''        if (owner != null && (slot.column() == 0
                || slot.column() == owner.columns() - 1
                || slot.row() == 0 || slot.row() == owner.rows() - 1)) {
''','''        if (owner != null && (slot.column() == 0
                || slot.column() == owner.columns() - 1
                || slot.row() == 0 || slot.row() == owner.rows() - 1)) {
            // A border block can expose a previously hidden welded face.
            PARENT_SHELLS.clear();
''')
rep('''        if (!changed || surfaces.stream().anyMatch(surface ->
                TransformConstructionClientControls.previewingSurface(
                        surface.id()))) return;
        Set<UUID> affected = new java.util.HashSet<>();
''','''        if (!changed || surfaces.stream().anyMatch(surface ->
                TransformConstructionClientControls.previewingSurface(
                        surface.id()))) return;
        PARENT_SHELLS.clear();
        Set<UUID> affected = new java.util.HashSet<>();
''')
rep('''    private static void addRoofCut(java.util.TreeSet<Double> cuts,
            double fraction) {
        if (fraction > 1.0E-6D && fraction < 1.0D - 1.0E-6D) {
            // Round so a parent boundary nearly coincident with a regular
            // tessellation cut does not create a zero-width sliver.
            cuts.add(Math.rint(fraction * 1.0E7D) / 1.0E7D);
        }
    }
''','''    private static void addRoofCut(java.util.TreeSet<Double> cuts,
            double fraction) {
        if (fraction > 1.0E-6D && fraction < 1.0D - 1.0E-6D) {
            double value = Math.rint(fraction * 1.0E7D) / 1.0E7D;
            // Parent cuts and uniform cuts sometimes differ only in the last
            // digits; never emit zero-width quads along the stitched ridge.
            Double lower = cuts.floor(value);
            Double upper = cuts.ceiling(value);
            if ((lower != null && Math.abs(lower - value) < 1.0E-6D)
                    || (upper != null && Math.abs(upper - value) < 1.0E-6D))
                return;
            cuts.add(value);
        }
    }
''')
old='''        RoofCuts cuts = surface.bridge() != null && deform && !overlay
                && fullSurfaceCell(attachment.state())
                && surface.bridge().hasProfiles()
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
'''
new='''        boolean linkedRoof = surface.bridge() != null && deform && !overlay
                && fullSurfaceCell(attachment.state())
                && surface.bridge().hasProfiles();
        // The two parent walls may have DIFFERENT grids. Their boundaries
        // must not be forced onto a single set of longitudinal cuts. Stitch
        // each independently sampled side to the shared crown by triangles.
        if (linkedRoof && emitLinkedRoofQuad(output, surface, slot, attachment,
                normalSign, overlay, points, quadNormal, us, vs, red, green,
                blue, fallbackLight, xSteps, ySteps)) return;
        RoofCuts cuts = linkedRoof
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
'''
rep(old,new)
marker='''    private record RoofCuts(List<Double> s, List<Double> t) { }
'''
addition='''    /**
     * Variable-resolution loft: the first wall owns the longitudinal samples
     * on the first side and the second wall owns those on the opposite side.
     * Each transverse band is joined with a zipper of degenerate quads (one
     * genuine triangle each). At the crown we use the UNION of the cuts, so
     * the two unequal parent meshes agree on one common ridge without forcing
     * an expensive Cartesian product of both grids across the entire roof.
     */
    private static boolean emitLinkedRoofQuad(List<PreparedVertex> output,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment,
            int normalSign, boolean overlay, Vec3[] points, Vec3 quadNormal,
            float[] us, float[] vs, int red, int green, int blue,
            int fallbackLight, int xSteps, int ySteps) {
        double x0 = bilerp(points, 0.0D, 0.5D).x;
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
        ConstructionSurface.BridgeAnchor bridge = surface.bridge();
        List<Double> first = roofLongitudinalCuts(surface, slot, x0,
                xDelta, xSteps, bridge.firstCells());
        List<Double> second = roofLongitudinalCuts(surface, slot, x0,
                xDelta, xSteps, bridge.secondCells());
        java.util.TreeSet<Double> central = new java.util.TreeSet<>(first);
        central.addAll(second);
        List<Double> crown = List.copyOf(central);
        java.util.TreeSet<Double> across = new java.util.TreeSet<>(
                uniformCuts(Math.max(2, ySteps)));
        double localCrown = surface.rows() * 0.5D - slot.row();
        addRoofCut(across, (localCrown - y0) / yDelta);
        List<Double> transverse = List.copyOf(across);
        for (int band = 0; band + 1 < transverse.size(); band++) {
            double from = transverse.get(band);
            double to = transverse.get(band + 1);
            List<Double> a = roofBandCuts(surface, slot, y0, yDelta,
                    from, first, second, crown);
            List<Double> b = roofBandCuts(surface, slot, y0, yDelta,
                    to, first, second, crown);
            int ai = 0, bi = 0;
            while (ai + 1 < a.size() || bi + 1 < b.size()) {
                boolean advanceFirst = ai + 1 < a.size()
                        && (bi + 1 >= b.size()
                                || a.get(ai + 1) <= b.get(bi + 1));
                double[] ss;
                double[] tt;
                if (advanceFirst) {
                    ss = new double[] {a.get(ai), a.get(ai + 1),
                            b.get(bi), b.get(bi)};
                    tt = new double[] {from, from, to, to};
                    ai++;
                } else {
                    ss = new double[] {a.get(ai), b.get(bi + 1),
                            b.get(bi), a.get(ai)};
                    tt = new double[] {from, to, to, from};
                    bi++;
                }
                for (int vertex = 0; vertex < 4; vertex++) {
                    double s = ss[vertex], t = tt[vertex];
                    emitSurfaceVertex(output, surface, slot, attachment,
                            normalSign, overlay, bilerp(points, s, t),
                            quadNormal, bilerp(us, s, t), bilerp(vs, s, t),
                            red, green, blue, fallbackLight);
                }
            }
        }
        return true;
    }

    private static List<Double> roofLongitudinalCuts(
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            double x0, double xDelta, int steps, int parentCells) {
        java.util.TreeSet<Double> cuts = new java.util.TreeSet<>(
                uniformCuts(steps));
        if (parentCells <= 0) return List.copyOf(cuts);
        int columns = surface.columns();
        int subdivisions = parentCells * Math.max(1,
                Math.min(24, 2048 / parentCells));
        double u0 = slot.column() / (double) columns;
        double u1 = (slot.column() + 1.0D) / columns;
        int begin = Math.max(1,
                (int) Math.ceil(u0 * subdivisions - 1.0E-8D));
        int end = Math.min(subdivisions - 1,
                (int) Math.floor(u1 * subdivisions + 1.0E-8D));
        for (int k = begin; k <= end; k++) {
            double localX = k / (double) subdivisions * columns
                    - slot.column();
            double modelX = surface.flipped() ? 1.0D - localX : localX;
            addRoofCut(cuts, (modelX - x0) / xDelta);
        }
        return List.copyOf(cuts);
    }

    private static List<Double> roofBandCuts(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double y0, double yDelta,
            double t, List<Double> first, List<Double> second,
            List<Double> central) {
        double fraction = (slot.row() + y0 + yDelta * t)
                / surface.rows();
        if (Math.abs(fraction - 0.5D) < 1.0E-6D) return central;
        return fraction < 0.5D ? first : second;
    }

'''
rep(marker,addition+marker)
old='''        int cells = edge < 2 ? parent.rows() : parent.columns();
        int segments = Math.max(1, cells * 24);
        double scaled = Math.max(0.0D, Math.min(1.0D, fraction)) * segments;
        int lower = Math.min(segments - 1, (int) Math.floor(scaled));
        double amount = scaled - lower;
        Vec3 a = parentShellVertex(parent, edge,
                lower / (double) segments, depth);
        if (amount < 1.0E-9D) return a;
        Vec3 b = parentShellVertex(parent, edge,
                (lower + 1.0D) / segments, depth);
        return a.scale(1.0D - amount).add(b.scale(amount));
'''
new='''        int cells = edge < 2 ? parent.rows() : parent.columns();
        int segments = Math.max(1, cells * Math.max(1,
                Math.min(24, 2048 / Math.max(1, cells))));
        double scaled = Math.max(0.0D, Math.min(1.0D, fraction)) * segments;
        int lower = Math.min(segments - 1, (int) Math.floor(scaled));
        double amount = scaled - lower;
        Vec3 a, b;
        if (Math.abs(depth) < 1.0E-7D
                || Math.abs(depth - 1.0D) < 1.0E-7D) {
            int layer = depth < 0.5D ? 0 : 1;
            ParentShellKey key = new ParentShellKey(parent.id(), edge, layer);
            Vec3[] shell = PARENT_SHELLS.computeIfAbsent(key, ignored -> {
                Vec3[] points = new Vec3[segments + 1];
                for (int i = 0; i <= segments; i++)
                    points[i] = parentShellVertex(parent, edge,
                            i / (double) segments, layer);
                return points;
            });
            a = shell[lower];
            if (amount < 1.0E-9D) return a;
            b = shell[lower + 1];
        } else {
            a = parentShellVertex(parent, edge,
                    lower / (double) segments, depth);
            if (amount < 1.0E-9D) return a;
            b = parentShellVertex(parent, edge,
                    (lower + 1.0D) / segments, depth);
        }
        return a.scale(1.0D - amount).add(b.scale(amount));
'''
rep(old,new)
p.write_text(s)
print('Variable-resolution linked roof mesh and bounded parent-edge shell cache applied')
