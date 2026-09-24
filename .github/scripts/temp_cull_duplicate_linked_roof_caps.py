from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = path.read_text(encoding='utf-8')
old = '''        if (neighbour.column() < 0 || neighbour.column() >= surface.columns()
                || neighbour.row() < 0 || neighbour.row() >= surface.rows()) {
            // Another Surface is NOT the same as a neighbouring solid cell.
            // Their parametric border vertices may differ slightly, even if
            // a geometric join exists. Culling this cap exposed the sky along
            // the entire wall/ceiling seam. The cap is needed as a watertight
            // fallback for unmatched subdivisions and partial-length joins.
            return false;
        }
'''
new = '''        if (neighbour.column() < 0 || neighbour.column() >= surface.columns()
                || neighbour.row() < 0 || neighbour.row() >= surface.rows()) {
            // Never cull generic inter-Surface caps: unmatched/partial joins
            // need them as a watertight fallback. A linked roof is different:
            // the parent edge is its physical boundary and both solids would
            // draw a coincident cap. Keep the PARENT cap and omit only the
            // CHILD cap when every contacted parent cell is actually solid.
            return !overlay && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                    && coveredLinkedRoofCap(surface, slot, face);
        }
'''
if s.count(old) != 1:
    raise RuntimeError('Expected one boundary cap branch')
s = s.replace(old,new,1)
anchor = '''    private static boolean fullSurfaceCell(BlockState state) {
'''
helper = '''    /**
     * Suppress only a linked roof's overlapping contact cap, and only where
     * the complete corresponding parent edge has structural full cells. The
     * parent cap is left intact, so partial roofs and wall openings retain
     * their watertight fallback instead of exposing sky.
     */
    private static boolean coveredLinkedRoofCap(ConstructionSurface roof,
            ConstructionSurface.SurfaceSlot slot, Direction face) {
        ConstructionSurface.BridgeAnchor link = roof.bridge();
        if (link == null || !link.hasProfiles()) return false;
        boolean first;
        if (face == Direction.DOWN && slot.row() == 0) first = true;
        else if (face == Direction.UP
                && slot.row() == roof.rows() - 1) first = false;
        else return false;
        ConstructionSurface parent = TransformConstructionClientState.surface(
                first ? link.firstId() : link.secondId());
        if (parent == null || !parent.dimension().equals(roof.dimension()))
            return false;
        int edge = first ? link.firstEdge() : link.secondEdge();
        double start = slot.column() / (double) roof.columns();
        double end = (slot.column() + 1.0D) / roof.columns();
        if (!first && link.reverseSecond()) {
            start = 1.0D - start;
            end = 1.0D - end;
        }
        double lo = Math.min(start, end);
        double hi = Math.max(start, end);
        int cells = edge < 2 ? parent.rows() : parent.columns();
        int begin = Math.max(0, (int) Math.floor(lo * cells + 1.0E-9D));
        int finish = Math.min(cells - 1,
                (int) Math.ceil(hi * cells - 1.0E-9D) - 1);
        if (begin > finish) return false;
        for (int index = begin; index <= finish; index++) {
            ConstructionSurface.SurfaceSlot parentSlot = edge < 2
                    ? new ConstructionSurface.SurfaceSlot(
                            edge == 0 ? 0 : parent.columns() - 1, index)
                    : new ConstructionSurface.SurfaceSlot(index,
                            edge == 2 ? 0 : parent.rows() - 1);
            ConstructionSurface.SurfaceAttachment attachment =
                    parent.attachments().get(parentSlot);
            if (attachment == null || !fullSurfaceCell(attachment.state())
                    || !TransformSurfaceGeometry.effectiveDeform(attachment))
                return false;
        }
        return true;
    }

'''
if s.count(anchor) != 1:
    raise RuntimeError('Expected one helper anchor')
s = s.replace(anchor,helper+anchor,1)
assert 'parentShellOnTessellatedEdge(parent,' in s
assert 'return !overlay && normalSign == TransformSurfaceGeometry.MAIN_SIDE' in s
assert 'if (explicitLinkedPair(surface, other)) continue;' in s
path.write_text(s, encoding='utf-8')
print('Only duplicate linked-roof caps over fully occupied mother edges are suppressed')
