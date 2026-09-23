from pathlib import Path

bridge = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/TransformSurfaceBridge.java')
s = bridge.read_text()
old = '''    private static int boundarySamples(int parentCells) {
        // Each parent cell boundary is exactly one sample endpoint. The two
        // edge profiles may have different lengths; no LCM or 768-sample cap
        // silently discards one wall's authored vertices.
        return parentCells * Math.max(4, (48 + parentCells - 1) / parentCells);
    }
'''
new = '''    static int boundarySamples(int parentCells) {
        // Save the actual parent-side polygonal chord positions, not just a
        // coarse approximation of its quadratic curve. A roof can only share
        // the rendered edge if its persisted profile has the same cut points
        // as the parent's (up to 24 subdivisions per logical cell). Keep the
        // NBT profile within BridgeAnchor.loadProfile's 2049-point limit for
        // unusually large authored walls.
        int cells = Math.max(1, parentCells);
        return cells * Math.max(1, Math.min(24, 2048 / cells));
    }
'''
assert s.count(old) == 1
bridge.write_text(s.replace(old,new))

saved = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/TransformConstructionSavedData.java')
s = saved.read_text()
old = '''            if (link == null || link.firstCells() > 0 && link.secondCells() > 0)
                continue;
'''
new = '''            if (link == null) continue;
            // Previously linked roofs may already record the parent cell
            // counts yet still carry coarse boundary profiles. Upgrade those
            // on world load too, preserving the roof's ID, blocks and crown.
            if (link.firstCells() > 0 && link.secondCells() > 0
                    && link.firstProfile().size() == TransformSurfaceBridge
                            .boundarySamples(link.firstCells()) + 1
                    && link.secondProfile().size() == TransformSurfaceBridge
                            .boundarySamples(link.secondCells()) + 1)
                continue;
'''
assert s.count(old) == 1
saved.write_text(s.replace(old,new))
print('Parent mesh profile precision and saved-world upgrade applied')
