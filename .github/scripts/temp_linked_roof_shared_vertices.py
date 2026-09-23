from pathlib import Path
p = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = p.read_text()
def change(old, new):
    global s
    n = s.count(old)
    if n != 1:
        raise RuntimeError(f'Expected 1 patch anchor, got {n}: {old[:110]!r}')
    s = s.replace(old, new, 1)

change('''    private static final Map<UUID, int[]> LINKED_PARENT_EDGE_STEPS =
            new HashMap<>();
''','''    private static final Map<UUID, int[]> LINKED_PARENT_EDGE_STEPS =
            new HashMap<>();
    // The child's logical column boundaries must be explicit vertices in the
    // mother's contacted edge as well. A child corner floating in the middle
    // of a mother's polygon chord is not an identical vertex set.
    private static final Map<UUID, Map<Integer, List<Double>>>
            LINKED_PARENT_CONTACT_CUTS = new HashMap<>();
''')
change('''        LINKED_PARENT_EDGE_STEPS.clear();
        PARENT_SHELLS.clear();
''','''        LINKED_PARENT_EDGE_STEPS.clear();
        LINKED_PARENT_CONTACT_CUTS.clear();
        PARENT_SHELLS.clear();
''')
change('''        RoofCuts cuts = linkedRoof
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
''','''        RoofCuts cuts = linkedRoof
                ? roofCuts(surface, slot, points, xSteps, ySteps)
                : deform && !overlay && fullSurfaceCell(attachment.state())
                        ? linkedParentQuadCuts(surface, slot, points,
                                xSteps, ySteps, minX, maxX, minY, maxY)
                        : new RoofCuts(uniformCuts(xSteps),
                                uniformCuts(ySteps));
''')
change('''        java.util.TreeSet<Double> cuts = new java.util.TreeSet<>(
                uniformCuts(Math.max(2, Math.min(4, steps))));
        if (profileSegments <= 0) return List.copyOf(cuts);
''','''        // The actual contact edge contains ONLY mother's physical mesh knots
        // and this child's cell endpoints. Interior tessellation is handled
        // by the zipper, not by injecting unmatched contact vertices.
        java.util.TreeSet<Double> cuts = new java.util.TreeSet<>();
        cuts.add(0.0D);
        cuts.add(1.0D);
        if (profileSegments <= 0) return List.copyOf(cuts);
''')
anchor = '''    private static RoofCuts roofCuts(ConstructionSurface surface,
'''
helper = '''    /** Insert each linked child's cell corners into the mother's actual
     * structural mesh. Keep the mother's original 24-per-cell knots too: the
     * roof's contact strip uses their union rather than a parallel curve. */
    private static RoofCuts linkedParentQuadCuts(
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            Vec3[] points, int xSteps, int ySteps,
            double minX, double maxX, double minY, double maxY) {
        Map<Integer, List<Double>> edges =
                LINKED_PARENT_CONTACT_CUTS.get(surface.id());
        if (edges == null || edges.isEmpty())
            return new RoofCuts(uniformCuts(xSteps), uniformCuts(ySteps));
        java.util.TreeSet<Double> sCuts = new java.util.TreeSet<>(
                uniformCuts(xSteps));
        java.util.TreeSet<Double> tCuts = new java.util.TreeSet<>(
                uniformCuts(ySteps));
        List<Double> alongX = slot.row() == 0 && minY < 1.0E-5D
                ? edges.get(2) : null;
        if (slot.row() == surface.rows() - 1 && maxY > 0.99999D
                && edges.containsKey(3)) alongX = edges.get(3);
        if (alongX != null && maxX - minX > 0.20D) {
            double xS0 = bilerp(points, 0.0D, 0.5D).x;
            double xDS = bilerp(points, 1.0D, 0.5D).x - xS0;
            double xT0 = bilerp(points, 0.5D, 0.0D).x;
            double xDT = bilerp(points, 0.5D, 1.0D).x - xT0;
            for (double fraction : alongX) {
                double localX = fraction * surface.columns() - slot.column();
                if (localX <= 1.0E-9D || localX >= 1.0D - 1.0E-9D)
                    continue;
                double modelX = surface.flipped() ? 1.0D - localX
                        : localX;
                if (Math.abs(xDS) >= Math.abs(xDT)
                        && Math.abs(xDS) > 0.20D)
                    addRoofCut(sCuts, (modelX - xS0) / xDS);
                else if (Math.abs(xDT) > 0.20D)
                    addRoofCut(tCuts, (modelX - xT0) / xDT);
            }
        }
        List<Double> alongY = slot.column() == 0 && minX < 1.0E-5D
                ? edges.get(0) : null;
        if (slot.column() == surface.columns() - 1
                && maxX > 0.99999D && edges.containsKey(1))
            alongY = edges.get(1);
        if (alongY != null && maxY - minY > 0.20D) {
            double yS0 = bilerp(points, 0.0D, 0.5D).y;
            double yDS = bilerp(points, 1.0D, 0.5D).y - yS0;
            double yT0 = bilerp(points, 0.5D, 0.0D).y;
            double yDT = bilerp(points, 0.5D, 1.0D).y - yT0;
            for (double fraction : alongY) {
                double localY = fraction * surface.rows() - slot.row();
                if (localY <= 1.0E-9D || localY >= 1.0D - 1.0E-9D)
                    continue;
                if (Math.abs(yDS) >= Math.abs(yDT)
                        && Math.abs(yDS) > 0.20D)
                    addRoofCut(sCuts, (localY - yS0) / yDS);
                else if (Math.abs(yDT) > 0.20D)
                    addRoofCut(tCuts, (localY - yT0) / yDT);
            }
        }
        return new RoofCuts(List.copyOf(sCuts), List.copyOf(tCuts));
    }

'''
change(anchor, helper + anchor)
change('''        Vec3 parentShell = parentShellOnTessellatedEdge(parent, parentEdge,
                parentU, depth);
''','''        // At a child cell boundary the mother now renders an explicit vertex,
        // not an interpolated point along its older, coarser polygon chord.
        // Evaluate exactly that same transformed mother vertex on the roof.
        double roofCell = u * roof.columns();
        Vec3 parentShell = Math.abs(roofCell - Math.rint(roofCell))
                < 1.0E-9D
                ? parentShellVertex(parent, parentEdge, parentU, depth)
                : parentShellOnTessellatedEdge(parent, parentEdge,
                        parentU, depth);
''')
change('''        Map<UUID, int[]> nextLinkedSteps = new HashMap<>();
        for (ConstructionSurface child : surfaces) {
''','''        Map<UUID, int[]> nextLinkedSteps = new HashMap<>();
        Map<UUID, Map<Integer, java.util.TreeSet<Double>>>
                pendingContactCuts = new HashMap<>();
        for (ConstructionSurface child : surfaces) {
''')
change('''            secondSteps[link.secondEdge()] = Math.max(
                    secondSteps[link.secondEdge()],
                    Math.max(1, (link.secondProfile().size() - 1) / secondCells));
        }
        Set<UUID> parentIds = new java.util.HashSet<>(
''','''            secondSteps[link.secondEdge()] = Math.max(
                    secondSteps[link.secondEdge()],
                    Math.max(1, (link.secondProfile().size() - 1) / secondCells));
            java.util.TreeSet<Double> firstCuts = pendingContactCuts
                    .computeIfAbsent(link.firstId(), ignored -> new HashMap<>())
                    .computeIfAbsent(link.firstEdge(),
                            ignored -> new java.util.TreeSet<>());
            java.util.TreeSet<Double> secondCuts = pendingContactCuts
                    .computeIfAbsent(link.secondId(), ignored -> new HashMap<>())
                    .computeIfAbsent(link.secondEdge(),
                            ignored -> new java.util.TreeSet<>());
            for (int column = 0; column <= child.columns(); column++) {
                double fraction = column / (double) child.columns();
                firstCuts.add(fraction);
                secondCuts.add(link.reverseSecond()
                        ? 1.0D - fraction : fraction);
            }
        }
        Map<UUID, Map<Integer, List<Double>>> nextContactCuts =
                new HashMap<>();
        pendingContactCuts.forEach((id, edges) -> {
            Map<Integer, List<Double>> ordered = new HashMap<>();
            edges.forEach((edge, fractions) ->
                    ordered.put(edge, List.copyOf(fractions)));
            nextContactCuts.put(id, Map.copyOf(ordered));
        });
        Set<UUID> cutParents = new java.util.HashSet<>(
                LINKED_PARENT_CONTACT_CUTS.keySet());
        cutParents.addAll(nextContactCuts.keySet());
        for (UUID parentId : cutParents) {
            if (!java.util.Objects.equals(
                    LINKED_PARENT_CONTACT_CUTS.get(parentId),
                    nextContactCuts.get(parentId))) affected.add(parentId);
        }
        LINKED_PARENT_CONTACT_CUTS.clear();
        LINKED_PARENT_CONTACT_CUTS.putAll(nextContactCuts);
        Set<UUID> parentIds = new java.util.HashSet<>(
''')
p.write_text(s)
print('Linked child contact corners are now inserted in mother structural meshes and reused on the roof')
