from pathlib import Path
p=Path("src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java")
s=p.read_text(encoding="utf-8")

old='''    private static void addLinkedEdgeKnots(
            Map<UUID, Map<Integer, java.util.TreeSet<Double>>> pending,
            UUID id, int edge, int parentSegments, int childColumns,
            boolean reverse) {
        java.util.TreeSet<Double> knots = pending
                .computeIfAbsent(id, ignored -> new HashMap<>())
                .computeIfAbsent(edge, ignored -> new java.util.TreeSet<>());
        knots.add(0.0D);
        knots.add(1.0D);
        // The mother keeps its complete original tessellation. A linked
        // child may only add positions at which its own cells need a vertex.
        for (int k = 1; k < parentSegments; k++)
            knots.add(k / (double) parentSegments);
        for (int k = 1; k < childColumns; k++) {
            double u = k / (double) childColumns;
            knots.add(reverse ? 1.0D - u : u);
        }
    }
'''
new='''    private static void addLinkedEdgeKnots(
            Map<UUID, Map<Integer, java.util.TreeSet<Double>>> pending,
            UUID id, int edge, int parentSegments) {
        java.util.TreeSet<Double> knots = pending
                .computeIfAbsent(id, ignored -> new HashMap<>())
                .computeIfAbsent(edge, ignored -> new java.util.TreeSet<>());
        knots.add(0.0D);
        knots.add(1.0D);
        // The MOTHER owns the contact topology. Do not union the child's
        // logical column boundaries into this list: doing so gives the parent
        // a different polygon from the one persisted in BridgeAnchor and makes
        // the child's shell projection disagree with the actually rendered
        // mother. The roof may have extra block boundaries, but those vertices
        // are constrained onto these exact mother segments and cannot change
        // their angle or open a crack.
        for (int k = 1; k < parentSegments; k++)
            knots.add(k / (double) parentSegments);
    }
'''
if s.count(old)!=1: raise RuntimeError("addLinkedEdgeKnots anchor mismatch")
s=s.replace(old,new,1)

old_calls='''            addLinkedEdgeKnots(pendingKnots, link.firstId(),
                    link.firstEdge(), link.firstProfile().size() - 1,
                    child.columns(), false);
            addLinkedEdgeKnots(pendingKnots, link.secondId(),
                    link.secondEdge(), link.secondProfile().size() - 1,
                    child.columns(), link.reverseSecond());
'''
new_calls='''            addLinkedEdgeKnots(pendingKnots, link.firstId(),
                    link.firstEdge(), link.firstProfile().size() - 1);
            addLinkedEdgeKnots(pendingKnots, link.secondId(),
                    link.secondEdge(), link.secondProfile().size() - 1);
'''
if s.count(old_calls)!=1: raise RuntimeError("linked knot call anchor mismatch")
s=s.replace(old_calls,new_calls,1)

start=s.index('''    private static Vec3 parentShellOnTessellatedEdge(
            ConstructionSurface parent, int edge, double fraction,
            double depth) {''')
end=s.index('''    private static Vec3 parentShellVertex(''',start)
old_method=s[start:end]
new_method='''    private static Vec3 parentShellOnTessellatedEdge(
            ConstructionSurface parent, int edge, double fraction,
            double depth) {
        double clamped = Math.max(0.0D, Math.min(1.0D, fraction));
        Map<Integer, List<Double>> byEdge =
                LINKED_PARENT_EDGE_KNOTS.get(parent.id());
        List<Double> knots = byEdge == null ? null : byEdge.get(edge);
        if (knots == null || knots.size() < 2) {
            int cells = edge < 2 ? parent.rows() : parent.columns();
            int segments = Math.max(1, cells * Math.max(1,
                    Math.min(24, 2048 / Math.max(1, cells))));
            java.util.ArrayList<Double> fallback =
                    new java.util.ArrayList<>(segments + 1);
            for (int i = 0; i <= segments; i++)
                fallback.add(i / (double) segments);
            knots = fallback;
        }

        int exact = java.util.Collections.binarySearch(knots, clamped);
        if (exact >= 0)
            return cachedParentShellKnot(parent, edge, knots, exact, depth);

        int insertion = -exact - 1;
        int upper = Math.max(1, Math.min(knots.size() - 1, insertion));
        int lower = upper - 1;
        double aFraction = knots.get(lower);
        double bFraction = knots.get(upper);
        Vec3 a = cachedParentShellKnot(parent, edge, knots, lower, depth);
        if (clamped <= aFraction + 1.0E-12D) return a;
        Vec3 b = cachedParentShellKnot(parent, edge, knots, upper, depth);
        double span = bFraction - aFraction;
        double amount = span < 1.0E-12D ? 0.0D
                : (clamped - aFraction) / span;
        return a.scale(1.0D - amount).add(b.scale(amount));
    }

    private static Vec3 cachedParentShellKnot(ConstructionSurface parent,
            int edge, List<Double> knots, int index, double depth) {
        if (Math.abs(depth) < 1.0E-7D
                || Math.abs(depth - 1.0D) < 1.0E-7D) {
            int layer = depth < 0.5D ? 0 : 1;
            ParentShellKey key = new ParentShellKey(parent.id(), edge, layer);
            Vec3[] shell = PARENT_SHELLS.computeIfAbsent(key,
                    ignored -> new Vec3[knots.size()]);
            // Geometry refresh clears PARENT_SHELLS before knot tables change.
            // Still tolerate a stale-sized cache defensively.
            if (shell.length == knots.size()) {
                Vec3 cached = shell[index];
                if (cached == null) {
                    cached = parentShellVertex(parent, edge,
                            knots.get(index), layer);
                    shell[index] = cached;
                }
                return cached;
            }
        }
        return parentShellVertex(parent, edge, knots.get(index), depth);
    }

'''
s=s[:start]+new_method+s[end:]

# Guard the architectural invariant.
if "child.columns(), false" in s or "child.columns(), link.reverseSecond()" in s:
    raise RuntimeError("child topology still injected into mother knot list")
if "Collections.binarySearch(knots, clamped)" not in s:
    raise RuntimeError("mother polygon lookup missing")
if "roofLongitudinalCuts" not in s or "linkedParentQuadCuts" not in s:
    raise RuntimeError("expected linked-roof topology paths missing")

p.write_text(s,encoding="utf-8")
print("Applied mother-owned linked roof contact topology")
