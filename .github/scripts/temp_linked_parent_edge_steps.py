from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = path.read_text()

def replace(old, new, expected=1):
    global s
    n = s.count(old)
    if n != expected:
        raise SystemExit(f'Wrong anchor count {n}, expected {expected}: {old[:110]}')
    s = s.replace(old, new)

replace('''    private static final Map<ParentShellKey, Vec3[]> PARENT_SHELLS =
''', '''    // An attached roof can have a different longitudinal grid on each side.
    // The parent wall must draw its contacted boundary on exactly the sample
    // grid recorded by that side of the roof, even if the parent is straight
    // or uses a different number of physical cells than the other parent.
    private static final Map<UUID, int[]> LINKED_PARENT_EDGE_STEPS =
            new HashMap<>();
    private static final Map<ParentShellKey, Vec3[]> PARENT_SHELLS =
''')
replace('''        SURFACE_MESHES.clear();
        PARENT_SHELLS.clear();
''', '''        SURFACE_MESHES.clear();
        LINKED_PARENT_EDGE_STEPS.clear();
        PARENT_SHELLS.clear();
''')
replace('''        boolean horizontalEdge = perimeter && (slot.row() == 0
                || slot.row() == surface.rows() - 1);
        boolean verticalEdge = perimeter && (slot.column() == 0
                || slot.column() == surface.columns() - 1);
        int xSteps = deform
                && surface.curveOffset().lengthSqr() > 1.0E-8D
                && maxX - minX > 0.20D
                ? curvedPipe ? 12 : horizontalEdge ? 24 : 2 : 1;
        int ySteps = deform
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                && maxY - minY > 0.20D
                ? verticalEdge ? 24 : 2 : 1;
''', '''        boolean horizontalEdge = perimeter && (slot.row() == 0
                || slot.row() == surface.rows() - 1);
        boolean verticalEdge = perimeter && (slot.column() == 0
                || slot.column() == surface.columns() - 1);
        int[] linkedSteps = LINKED_PARENT_EDGE_STEPS.get(surface.id());
        int linkedHorizontal = linkedSteps == null ? 0 :
                Math.max(slot.row() == 0 && minY < 1.0E-5D
                        ? linkedSteps[2] : 0,
                        slot.row() == surface.rows() - 1
                        && maxY > 0.99999D ? linkedSteps[3] : 0);
        int linkedVertical = linkedSteps == null ? 0 :
                Math.max(slot.column() == 0 && minX < 1.0E-5D
                        ? linkedSteps[0] : 0,
                        slot.column() == surface.columns() - 1
                        && maxX > 0.99999D ? linkedSteps[1] : 0);
        boolean curvedAcross = surface.curveOffset().lengthSqr() > 1.0E-8D
                || surface.topCurveOffset().lengthSqr() > 1.0E-8D;
        int xSteps = deform && maxX - minX > 0.20D
                ? curvedPipe ? 12 : linkedHorizontal > 0
                        ? linkedHorizontal : curvedAcross
                                ? horizontalEdge ? 24 : 2 : 1 : 1;
        int ySteps = deform && maxY - minY > 0.20D
                ? linkedVertical > 0 ? linkedVertical
                        : surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                                ? verticalEdge ? 24 : 2 : 1 : 1;
''')

replace('''        PARENT_SHELLS.clear();
        Set<UUID> affected = new java.util.HashSet<>();
        for (ConstructionSurface surface : surfaces) {
''', '''        PARENT_SHELLS.clear();
        Set<UUID> affected = new java.util.HashSet<>();
        Map<UUID, int[]> nextLinkedSteps = new HashMap<>();
        for (ConstructionSurface child : surfaces) {
            ConstructionSurface.BridgeAnchor link = child.bridge();
            if (link == null || !link.hasProfiles()) continue;
            int firstCells = Math.max(1, link.firstCells());
            int secondCells = Math.max(1, link.secondCells());
            int[] firstSteps = nextLinkedSteps.computeIfAbsent(
                    link.firstId(), ignored -> new int[4]);
            firstSteps[link.firstEdge()] = Math.max(
                    firstSteps[link.firstEdge()],
                    Math.max(1, (link.firstProfile().size() - 1) / firstCells));
            int[] secondSteps = nextLinkedSteps.computeIfAbsent(
                    link.secondId(), ignored -> new int[4]);
            secondSteps[link.secondEdge()] = Math.max(
                    secondSteps[link.secondEdge()],
                    Math.max(1, (link.secondProfile().size() - 1) / secondCells));
        }
        Set<UUID> parentIds = new java.util.HashSet<>(
                LINKED_PARENT_EDGE_STEPS.keySet());
        parentIds.addAll(nextLinkedSteps.keySet());
        for (UUID id : parentIds) {
            if (!java.util.Arrays.equals(LINKED_PARENT_EDGE_STEPS.get(id),
                    nextLinkedSteps.get(id))) affected.add(id);
        }
        LINKED_PARENT_EDGE_STEPS.clear();
        LINKED_PARENT_EDGE_STEPS.putAll(nextLinkedSteps);
        for (ConstructionSurface surface : surfaces) {
''')
path.write_text(s)
print('Applied linked parent boundary sample matching')
