from pathlib import Path

def rep(path, old, new, count=None):
    p=Path(path)
    s=p.read_text()
    n=s.count(old)
    if n == 0:
        raise SystemExit(f"missing anchor in {path}: {old[:120]!r}")
    if count is not None and n != count:
        raise SystemExit(f"unexpected anchor count in {path}: {n} != {count}")
    p.write_text(s.replace(old,new))

renderer="src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java"
mapfile="src/main/java/com/bl4ues/scpclassifieddirective/client/scp079/Scp079FacilityMapScreen.java"

rep(renderer,
'''        boolean joinEdge = !overlay
                && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                && fullSurfaceCell(attachment.state())
                && (((slot.column() == 0
''',
'''        boolean seamEligible = !overlay
                && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                && fullSurfaceCell(attachment.state());
        boolean joinEdge = seamEligible
                && (((slot.column() == 0
''',
1)

rep(renderer,
'''        VertexFrame frame = TransformSurfaceGeometry.effectiveDeform(attachment)
                ? deformedFrame(surface, slot, point.x, point.y, point.z,
                        localNormal, normalSign, depthOffset, joinEdge)
''',
'''        VertexFrame frame = TransformSurfaceGeometry.effectiveDeform(attachment)
                ? deformedFrame(surface, slot, point.x, point.y, point.z,
                        localNormal, normalSign, depthOffset, seamEligible)
''',
1)

rep(renderer,
'''    private static VertexFrame deformedFrame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double x, double y, double z,
            Vec3 localNormal, int normalSign, double depthOffset,
            boolean joinEdge) {
''',
'''    private static VertexFrame deformedFrame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double x, double y, double z,
            Vec3 localNormal, int normalSign, double depthOffset,
            boolean seamEligible) {
''',
1)

rep(renderer,
'''        if (joinEdge && z >= 0.0D && z <= 1.000001D) {
            Vec3 joined = joinedSurfaceEdge(surface, u, v, z);
            if (joined != null) position = joined;
        }
''',
'''        if (seamEligible && z >= 0.0D && z <= 1.000001D) {
            Vec3 welded = smoothJoinedSurfacePosition(surface, u, v, z,
                    position);
            if (welded != null) position = welded;
        }
''',
1)

insert_anchor='''    /** Endpoint agreement is checked for both directions and at the center,
'''
insert='''    /**
     * Pull the first physical cell next to a matched seam toward the same
     * canonical border used by its neighbour. Moving only the terminal row of
     * vertices closes the mathematical edge, but leaves a visible lip whenever
     * two old Surfaces approach the join with different tangents. This one-cell
     * smoothstep keeps the authored curves intact away from the seam while the
     * local grid reorganises itself into a continuous transition.
     */
    private static Vec3 smoothJoinedSurfacePosition(ConstructionSurface surface,
            double u, double v, double depth, Vec3 original) {
        Map<Integer, MatchedSurfaceEdge> matches =
                SHARED_SURFACE_EDGES.get(surface.id());
        if (matches == null || matches.isEmpty()) return null;

        Vec3 totalDelta = Vec3.ZERO;
        double totalWeight = 0.0D;
        for (int edge = 0; edge < 4; edge++) {
            if (!matches.containsKey(edge)) continue;
            double cellsFromEdge = switch (edge) {
                case 0 -> u * surface.columns();
                case 1 -> (1.0D - u) * surface.columns();
                case 2 -> v * surface.rows();
                default -> (1.0D - v) * surface.rows();
            };
            if (cellsFromEdge < -1.0E-6D || cellsFromEdge > 1.0D) continue;

            double boundaryU = edge == 0 ? 0.0D
                    : edge == 1 ? 1.0D : u;
            double boundaryV = edge == 2 ? 0.0D
                    : edge == 3 ? 1.0D : v;
            Vec3 joined = joinedSurfaceEdge(surface, boundaryU, boundaryV,
                    depth);
            if (joined == null) continue;
            Vec3 base = surface.gridPoint(boundaryU, boundaryV)
                    .add(surface.gridNormal(boundaryU, boundaryV)
                            .scale(depth));
            Vec3 delta = joined.subtract(base);

            double t = 1.0D - Math.max(0.0D,
                    Math.min(1.0D, cellsFromEdge));
            // C1 smoothstep. The derivative is zero both at the seam and at
            // the end of the one-cell blend band, avoiding a second visible
            // crease immediately beside the join.
            double weight = t * t * (3.0D - 2.0D * t);
            totalDelta = totalDelta.add(delta.scale(weight));
            totalWeight += weight;
        }
        return totalWeight <= 1.0E-9D ? null : original.add(totalDelta);
    }

'''
rep(renderer,insert_anchor,insert+insert_anchor,1)

rep(renderer,
'''            Vec3 sharedBase = match.partial()
                    ? sourcePoint.add(otherPoint).scale(0.5D)
                    : canonicalSharedEdgePoint(surface, edge, other,
                            match.otherEdge(), match.reversed(), fraction);
''',
'''            // Partial joins are common in old corridors where a long roof
            // edge overlaps only part of a shorter wall. Averaging independently
            // from both directions is not symmetric and can leave two nearly
            // identical polylines separated by a bright slit. Elect one edge
            // deterministically as the seam master so both meshes converge onto
            // the exact same authored curve.
            Vec3 sharedBase;
            if (match.partial()) {
                boolean sourceMaster = surface.id().toString().compareTo(
                        other.id().toString()) <= 0;
                sharedBase = sourceMaster ? sourcePoint : otherPoint;
            } else {
                sharedBase = canonicalSharedEdgePoint(surface, edge, other,
                        match.otherEdge(), match.reversed(), fraction);
            }
''',
1)

rep(mapfile,
'''        double thickness = Math.max(0.25D, transform.scale() * 0.27D);
''',
'''        // Keep the marker a slim physical slab. It scales with the map like
        // the rooms themselves; text remains the only screen-space exception.
        double thickness = Math.max(0.18D, transform.scale() * 0.18D);
''',
1)
