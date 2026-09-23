from pathlib import Path

ROOT = Path('src/main/java/com/bl4ues/scpclassifieddirective')

def replace(relative, before, after, count=1):
    file = ROOT / relative
    text = file.read_text(encoding='utf-8')
    actual = text.count(before)
    if actual != count:
        raise RuntimeError(f'{file}: expected {count} anchors, got {actual}: {before[:90]!r}')
    file.write_text(text.replace(before, after), encoding='utf-8')

replace('facility/transform/client/TransformConstructionClientControls.java',
'''                    if (surface != null) {
                        TransformConstructionClientState.remember(selection);
                        ConstructionSurface next =
                                surface.withFlipped(!surface.flipped());''',
'''                    if (surface != null && surface.bridge() != null) {
                        // A linked roof has two immutable parent-side borders.
                        // Flipping only its frame produces an impossible inverted
                        // shell and detaches the visible contact from its parents.
                        status("Linked surface: only crown height is editable");
                        return;
                    }
                    if (surface != null) {
                        TransformConstructionClientState.remember(selection);
                        ConstructionSurface next =
                                surface.withFlipped(!surface.flipped());''')

replace('facility/transform/TransformConstructionManager.java',
'''        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface next = surface.withFlipped(flipped);''',
'''        if (surface == null || !surface.dimension().equals(
                level.dimension().location()) || surface.bridge() != null)
            return false;
        ConstructionSurface next = surface.withFlipped(flipped);''')

renderer = 'facility/transform/client/TransformConstructionClientRenderer.java'
replace(renderer,
'''        if (seamEligible && z >= 0.0D && z <= 1.000001D) {
            Vec3 welded = smoothJoinedSurfacePosition(surface, u, v, z,
                    position);
            if (welded != null) position = welded;
        }
        Vec3 transformedNormal = TransformMath.safeNormalize(''',
'''        if (seamEligible && z >= 0.0D && z <= 1.000001D) {
            Vec3 welded = smoothJoinedSurfacePosition(surface, u, v, z,
                    position);
            if (welded != null) position = welded;
            // Parent surfaces and linked roofs must share the same *shell*,
            // not merely the same zero-thickness authoring curve. At the roof
            // edge, the mother's extrusion normal owns both depth vertices.
            // Blend this difference across the first roof row to avoid a lip.
            Vec3 anchored = linkedParentShellPosition(surface, u, v, z,
                    position);
            if (anchored != null) position = anchored;
        }
        Vec3 transformedNormal = TransformMath.safeNormalize(''')

insert_before = '''    /**
     * Pull the first physical cell next to a matched seam toward the same
'''
new_helper = '''    /**
     * A linked roof inherits the actual tessellated shell of its two parent
     * walls. Matching only gridPoint() joins their centerlines but leaves the
     * roof's depth displaced along its own normal, producing a solid step and
     * a narrow skylight at the wall/ceiling junction. Interpolate the parent's
     * rendered edge vertices, including its own seam welding. The linked roof
     * uses these exact endpoints and transitions to its own normal inside the
     * first row; the authored arch and all parent control points stay intact.
     */
    private static Vec3 linkedParentShellPosition(
            ConstructionSurface roof, double u, double v, double depth,
            Vec3 current) {
        ConstructionSurface.BridgeAnchor link = roof.bridge();
        if (link == null || !link.hasProfiles()) return null;
        double rows = roof.rows();
        double nearFirst = v * rows;
        double nearSecond = (1.0D - v) * rows;
        boolean first = nearFirst <= nearSecond;
        double cells = first ? nearFirst : nearSecond;
        double blendCells = Math.min(1.25D, rows * 0.45D);
        if (cells > blendCells || blendCells < 1.0E-6D) return null;

        ConstructionSurface parent = TransformConstructionClientState.surface(
                first ? link.firstId() : link.secondId());
        if (parent == null || !parent.dimension().equals(roof.dimension()))
            return null;
        int parentEdge = first ? link.firstEdge() : link.secondEdge();
        double parentU = first || !link.reverseSecond() ? u : 1.0D - u;
        Vec3 parentShell = parentShellOnTessellatedEdge(parent, parentEdge,
                parentU, depth);
        double edgeV = first ? 0.0D : 1.0D;
        Vec3 roofEdge = roof.gridPoint(u, edgeV)
                .add(roof.gridNormal(u, edgeV).scale(depth));
        Vec3 roofWeld = smoothJoinedSurfacePosition(roof, u, edgeV,
                depth, roofEdge);
        if (roofWeld != null) roofEdge = roofWeld;
        double t = 1.0D - Math.max(0.0D, cells / blendCells);
        double weight = t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
        return current.add(parentShell.subtract(roofEdge).scale(weight));
    }

    private static Vec3 parentShellOnTessellatedEdge(
            ConstructionSurface parent, int edge, double fraction,
            double depth) {
        int cells = edge < 2 ? parent.rows() : parent.columns();
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
    }

    private static Vec3 parentShellVertex(ConstructionSurface parent,
            int edge, double fraction, double depth) {
        double u = edge == 0 ? 0.0D : edge == 1 ? 1.0D : fraction;
        double v = edge == 2 ? 0.0D : edge == 3 ? 1.0D : fraction;
        Vec3 base = parent.gridPoint(u, v)
                .add(parent.gridNormal(u, v).scale(depth));
        Vec3 welded = smoothJoinedSurfacePosition(parent, u, v,
                depth, base);
        return welded == null ? base : welded;
    }

'''
replace(renderer, insert_before, new_helper + insert_before)

replace(renderer,
'''            for (int count : parentCounts) {
                if (count <= 0) continue;
                int begin = Math.max(1, (int) Math.ceil(u0 * count - 1.0E-8D));
                int end = Math.min(count - 1,
                        (int) Math.floor(u1 * count + 1.0E-8D));
                for (int k = begin; k <= end; k++) {
                    double localX = k / (double) count * columns
                            - slot.column();''',
'''            for (int count : parentCounts) {
                if (count <= 0) continue;
                // A border row also splits at the mother's 24-per-cell mesh
                // vertices. Otherwise the child spans a different polygonal
                // chord even if both curves share their mathematical points.
                int subdivisions = (slot.row() == 0
                        || slot.row() == surface.rows() - 1)
                        ? count * 24 : count;
                int begin = Math.max(1, (int) Math.ceil(
                        u0 * subdivisions - 1.0E-8D));
                int end = Math.min(subdivisions - 1,
                        (int) Math.floor(u1 * subdivisions + 1.0E-8D));
                for (int k = begin; k <= end; k++) {
                    double localX = k / (double) subdivisions * columns
                            - slot.column();''')
print('Patched linked roof contact mesh and locked flip on client/server.')
