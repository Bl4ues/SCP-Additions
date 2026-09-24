from pathlib import Path
p = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = p.read_text(encoding='utf-8')
old = '''        boolean alongS = Math.abs(xDS) >= Math.abs(xDT)
                && Math.abs(xDS) > 0.20D;
        boolean alongT = !alongS && Math.abs(xDT) > 0.20D;
        if (alongS || alongT) {
            int[] parentCounts = {bridge.firstCells(), bridge.secondCells()};
'''
new = '''        boolean alongS = Math.abs(xDS) >= Math.abs(xDT)
                && Math.abs(xDS) > 0.20D;
        boolean alongT = !alongS && Math.abs(xDT) > 0.20D;
        // The front/back roof faces use the shared contact knots via the
        // zipper renderer. The thin physical END CAP at y=0 or y=1 must use
        // that VERY SAME knot sequence. The old cap path added arbitrary
        // 24-step cuts from both parents even on one wall's contact edge,
        // producing a T-junction inside the linked roof itself and the
        // visible sky pixels between parent and child blocks.
        double minQuadY = Double.POSITIVE_INFINITY;
        double maxQuadY = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : points) {
            minQuadY = Math.min(minQuadY, corner.y);
            maxQuadY = Math.max(maxQuadY, corner.y);
        }
        if ((alongS || alongT) && maxQuadY - minQuadY < 1.0E-6D) {
            boolean firstContact = slot.row() == 0
                    && Math.abs(minQuadY) < 1.0E-6D;
            boolean secondContact = slot.row() == surface.rows() - 1
                    && Math.abs(maxQuadY - 1.0D) < 1.0E-6D;
            if (firstContact || secondContact) {
                List<Double> contact = roofLongitudinalCuts(surface, slot,
                        alongS ? xS0 : xT0, alongS ? xDS : xDT,
                        bridge, firstContact);
                // Thickness spans the other quad axis exactly once, like the
                // mother wall's exposed cap. No extra vertices on either
                // edge can be introduced by the child's cap tessellation.
                return alongS
                        ? new RoofCuts(contact, uniformCuts(1))
                        : new RoofCuts(uniformCuts(1), contact);
            }
        }
        if (alongS || alongT) {
            int[] parentCounts = {bridge.firstCells(), bridge.secondCells()};
'''
assert s.count(old) == 1, f'expected one roofCuts orientation branch, saw {s.count(old)}'
s = s.replace(old,new,1)
assert s.count('roofLongitudinalCuts(surface, slot,') == 3
assert 'LINKED_PARENT_EDGE_KNOTS' in s
assert 'coveredLinkedRoofCap' not in s
p.write_text(s, encoding='utf-8')
print('Roof surface and physical contact caps use the same exact parent/child boundary vertices')
