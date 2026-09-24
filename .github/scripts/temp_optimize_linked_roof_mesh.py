from pathlib import Path

bridge = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/TransformSurfaceBridge.java')
renderer = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
b = bridge.read_text(encoding='utf-8')
r = renderer.read_text(encoding='utf-8')

def replace_once(text, old, new, name):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f'{name}: expected one anchor, found {count}: {old[:90]!r}')
    return text.replace(old, new, 1)

b = replace_once(b,
'''        // Save the actual parent-side polygonal chord positions, not just a
        // coarse approximation of its quadratic curve. A roof can only share
        // the rendered edge if its persisted profile has the same cut points
        // as the parent's (up to 24 subdivisions per logical cell). Keep the
        // NBT profile within BridgeAnchor.loadProfile's 2049-point limit for
        // unusually large authored walls.
        int cells = Math.max(1, parentCells);
        return cells * Math.max(1, Math.min(24, 2048 / cells));
''',
'''        // Eight segments per block are shared by the parent and child, not
        // independently reduced on either side. The previous 24-per-block
        // boundary multiplied *every row* of the parent wall's front/back
        // faces and every linked-roof contact strip; caching did not reduce
        // that per-frame vertex submission cost. This remains sub-block
        // curvature resolution and preserves the exact common polygon.
        // Keep both profiles inside BridgeAnchor's 2049-point NBT limit.
        int cells = Math.max(1, parentCells);
        return cells * Math.max(1, Math.min(8, 2048 / cells));
''', 'bridge edge sample budget')

r = replace_once(r,
'''        // Binary-search only the 24 or so knots INSIDE this logical cell.
''',
'''        // Binary-search only the shared knots INSIDE this logical cell.
''', 'parent knot comment')

r = replace_once(r,
'''        if (alongS || alongT) {
            int[] parentCounts = {bridge.firstCells(), bridge.secondCells()};
            double u0 = slot.column() / (double) columns;
            double u1 = (slot.column() + 1.0D) / columns;
            for (int count : parentCounts) {
                if (count <= 0) continue;
                // A border row also splits at the mother's 24-per-cell mesh
                // vertices. Otherwise the child spans a different polygonal
                // chord even if both curves share their mathematical points.
                int subdivisions = (slot.row() == 0
                        || slot.row() == surface.rows() - 1)
                        ? count * 24 : count;
''',
'''        if (alongS || alongT) {
            int[] parentCounts = {bridge.firstCells(), bridge.secondCells()};
            int[] parentSegments = {bridge.firstProfile().size() - 1,
                    bridge.secondProfile().size() - 1};
            double u0 = slot.column() / (double) columns;
            double u1 = (slot.column() + 1.0D) / columns;
            for (int parentIndex = 0; parentIndex < parentCounts.length;
                    parentIndex++) {
                int count = parentCounts[parentIndex];
                if (count <= 0) continue;
                // Contact-side caps must use the same polygon knots as the
                // parent AND the roof's main faces. Hard-coding 24 here after
                // changing the common profile density creates new T-junctions.
                int subdivisions = (slot.row() == 0
                        || slot.row() == surface.rows() - 1)
                        ? Math.max(1, parentSegments[parentIndex]) : count;
''', 'roof cap budget')

r = replace_once(r,
'''            int segments = Math.max(1, cells * Math.max(1,
                    Math.min(24, 2048 / Math.max(1, cells))));
''',
'''            int segments = Math.max(1, cells * Math.max(1,
                    Math.min(8, 2048 / Math.max(1, cells))));
''', 'parent polygon fallback density')

assert 'Math.min(8, 2048 / cells)' in b
assert 'parentSegments[parentIndex]' in r
assert 'Math.min(24, 2048 / Math.max(1, cells))' not in r
assert 'LINKED_PARENT_EDGE_STEPS' in r
assert 'roofLongitudinalCuts' in r
assert 'linkedParentShellPosition' in r

bridge.write_text(b, encoding='utf-8')
renderer.write_text(r, encoding='utf-8')
print('Both parent/child contact meshes now use one eight-segment-per-cell polygon budget.')
