from pathlib import Path

path = Path("src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java")
s = path.read_text(encoding="utf-8")

def replace_once(before, after):
    global s
    n = s.count(before)
    if n != 1:
        raise RuntimeError(f"expected one anchor, found {n}: {before[:120]!r}")
    s = s.replace(before, after, 1)

replace_once(
'''    // An attached roof can have a different longitudinal grid on each side.
    // The parent wall must draw its contacted boundary on exactly the sample
    // grid recorded by that side of the roof, even if the parent is straight
    // or uses a different number of physical cells than the other parent.
    private static final Map<UUID, int[]> LINKED_PARENT_EDGE_STEPS =
            new HashMap<>();
    // The child's logical column boundaries must be explicit vertices in the
    // mother's contacted edge as well. A child corner floating in the middle
    // of a mother's polygon chord is not an identical vertex set.
    private static final Map<UUID, Map<Integer, List<Double>>>
            LINKED_PARENT_CONTACT_CUTS = new HashMap<>();
''',
'''    // A linked roof samples each parent edge on the parent's own rendered
    // subdivision grid. Parent walls may need that edge density propagated
    // through their rows/columns to avoid internal T-junctions, but the CHILD
    // must never inject extra cut positions into the mother mesh. The parent
    // remains the geometric source of truth.
    private static final Map<UUID, int[]> LINKED_PARENT_EDGE_STEPS =
            new HashMap<>();
''')

replace_once(
'''        LINKED_PARENT_EDGE_STEPS.clear();
        LINKED_PARENT_CONTACT_CUTS.clear();
        PARENT_SHELLS.clear();
''',
'''        LINKED_PARENT_EDGE_STEPS.clear();
        PARENT_SHELLS.clear();
''')

replace_once(
'''        RoofCuts cuts = linkedRoof
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : deform && !overlay && fullSurfaceCell(attachment.state())
                        ? linkedParentQuadCuts(surface, slot, points,
                                xSteps, ySteps, minX, maxX, minY, maxY)
                        : new RoofCuts(uniformCuts(xSteps),
                                uniformCuts(ySteps));
''',
'''        RoofCuts cuts = linkedRoof
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
''')

start = s.find('''    /** Insert each linked child's cell corners into the mother's actual
     * structural mesh.''')
end = s.find('''    private static RoofCuts roofCuts(''', start)
if start < 0 or end < 0:
    raise RuntimeError("linkedParentQuadCuts block not found")
s = s[:start] + s[end:]

replace_once(
'''        Map<UUID, int[]> nextLinkedSteps = new HashMap<>();
        Map<UUID, Map<Integer, java.util.TreeSet<Double>>>
                pendingContactCuts = new HashMap<>();
''',
'''        Map<UUID, int[]> nextLinkedSteps = new HashMap<>();
''')

start = s.find('''            java.util.TreeSet<Double> firstCuts = pendingContactCuts''')
end = s.find('''        Set<UUID> parentIds = new java.util.HashSet<>(''', start)
if start < 0 or end < 0:
    raise RuntimeError("pending linked contact cut block not found")
s = s[:start] + s[end:]

replace_once(
'''                for (ConstructionSurface other : surfaces) {
                    if (other.id().equals(surface.id())) continue;
                    for (int otherEdge = 0; otherEdge < 4; otherEdge++) {
''',
'''                for (ConstructionSurface other : surfaces) {
                    if (other.id().equals(surface.id())) continue;
                    // Explicit linked ceilings already inherit exact parent
                    // profiles, shell positions and G1 tangents. Running the
                    // generic seam solver on that same parent/child pair pulls
                    // the MOTHER toward the child and creates the visible cut
                    // through the wall. Parent geometry is immutable here.
                    if (explicitLinkedPair(surface, other)) continue;
                    for (int otherEdge = 0; otherEdge < 4; otherEdge++) {
''')

insert_anchor = '''    /** Refresh neighbours only after a committed edit. A temporary drag
     * changes its authoring guides but must not rebake the entire facility. */
'''
helper = '''    private static boolean explicitLinkedPair(ConstructionSurface a,
            ConstructionSurface b) {
        if (a == null || b == null) return false;
        ConstructionSurface.BridgeAnchor ab = a.bridge();
        if (ab != null && (ab.firstId().equals(b.id())
                || ab.secondId().equals(b.id()))) return true;
        ConstructionSurface.BridgeAnchor ba = b.bridge();
        return ba != null && (ba.firstId().equals(a.id())
                || ba.secondId().equals(a.id()));
    }

'''
replace_once(insert_anchor, helper + insert_anchor)

# Assertions describe the intended architecture, not just compilation.
if "LINKED_PARENT_CONTACT_CUTS" in s or "linkedParentQuadCuts" in s:
    raise RuntimeError("child-driven mother retessellation still present")
if "explicitLinkedPair(surface, other)" not in s:
    raise RuntimeError("explicit bridge exclusion missing")
if "LINKED_PARENT_EDGE_STEPS" not in s:
    raise RuntimeError("parent edge density propagation was accidentally removed")

path.write_text(s, encoding="utf-8")
print("Applied parent-immutable linked ceiling seam correction")
