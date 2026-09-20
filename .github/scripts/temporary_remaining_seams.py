from pathlib import Path
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s=p.read_text()
def once(old,new):
 global s
 count=s.count(old)
 assert count==1,(old[:115],count)
 s=s.replace(old,new,1)

once('''    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();''','''    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();
    // Neighbour relationships depend on geometry, not individual block states.
    // Resolve each shared edge once per committed geometry change rather than
    // searching every other Surface for every vertex of a long curved corridor.
    private static final Map<UUID, ConstructionSurface> SURFACE_EDGE_GEOMETRIES =
            new HashMap<>();
    private static final Map<UUID, Map<Integer, MatchedSurfaceEdge>>
            SHARED_SURFACE_EDGES = new HashMap<>();''')
once('''        SURFACE_MESHES.clear();
        GROUP_MESHES.clear();''','''        SURFACE_MESHES.clear();
        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        GROUP_MESHES.clear();''')
once('''        for (ConstructionSurface surface : surfaces) {
            renderSurfacePayloads(minecraft, pose, buffers, surface, camera);
        }''','''        updateSharedSurfaceEdges(surfaces);
        for (ConstructionSurface surface : surfaces) {
            renderSurfacePayloads(minecraft, pose, buffers, surface, camera);
        }''')
# Never hide a boundary cap without confirming a corresponding structural
# piece exists across the boundary at that location.
once('''        if (neighbour.column() < 0 || neighbour.column() >= surface.columns()
                || neighbour.row() < 0 || neighbour.row() >= surface.rows()) {
            return false;
        }''','''        if (neighbour.column() < 0 || neighbour.column() >= surface.columns()
                || neighbour.row() < 0 || neighbour.row() >= surface.rows()) {
            double u = neighbour.column() < 0 ? 0.0D
                    : neighbour.column() >= surface.columns() ? 1.0D
                    : (slot.column() + 0.5D) / surface.columns();
            double v = neighbour.row() < 0 ? 0.0D
                    : neighbour.row() >= surface.rows() ? 1.0D
                    : (slot.row() + 0.5D) / surface.rows();
            return joinedSurfaceEdge(surface, u, v, 0.5D) != null;
        }''')
once('''        // Miter shared physical end edges of distinct Surface planes. This is
        // only checked for a structural vertex on the OUTER edge of a Surface,
        // not for every quad in a large authored wall.
        boolean joinEdge = !overlay
                && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                && (slot.column() == 0
                    || slot.column() == surface.columns() - 1)
                && (Math.abs(point.x) < 1.0E-6D
                    || Math.abs(point.x - 1.0D) < 1.0E-6D)
                && fullSurfaceCell(attachment.state());''','''        // A ceiling meets a wall at the latter's TOP/BOTTOM border, not just
        // its START/END border. All four physical edges are eligible; internal
        // tile borders, fixtures and overlays never get mitered.
        boolean joinEdge = !overlay
                && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                && fullSurfaceCell(attachment.state())
                && ((slot.column() == 0
                        && Math.abs(point.x) < 1.0E-6D)
                    || (slot.column() == surface.columns() - 1
                        && Math.abs(point.x - 1.0D) < 1.0E-6D)
                    || (slot.row() == 0
                        && Math.abs(point.y) < 1.0E-6D)
                    || (slot.row() == surface.rows() - 1
                        && Math.abs(point.y - 1.0D) < 1.0E-6D));''')
start=s.index('    /** Miter the two thickness vectors at an authored shared end edge.')
end=s.index('    private static VertexFrame rigidFrame(',start)
s=s[:start]+'''    /** Endpoint agreement is checked for both directions and at the center,
     * so independent curved planes only meet when their physical edges do. */
    private record MatchedSurfaceEdge(ConstructionSurface other,
            int otherEdge, boolean reversed) { }

    private static Vec3 edgePoint(ConstructionSurface surface,
            int edge, double fraction) {
        return switch (edge) {
            case 0 -> surface.gridPoint(0.0D, fraction);
            case 1 -> surface.gridPoint(1.0D, fraction);
            case 2 -> surface.gridPoint(fraction, 0.0D);
            default -> surface.gridPoint(fraction, 1.0D);
        };
    }

    /** Refresh neighbours only after a committed edit. A temporary drag
     * changes its authoring guides but must not rebake the entire facility. */
    private static void updateSharedSurfaceEdges(
            List<ConstructionSurface> surfaces) {
        boolean changed = SURFACE_EDGE_GEOMETRIES.size() != surfaces.size();
        for (ConstructionSurface surface : surfaces) {
            if (!sameSurfaceGeometry(
                    SURFACE_EDGE_GEOMETRIES.get(surface.id()), surface)) {
                changed = true;
            }
        }
        if (!changed || surfaces.stream().anyMatch(surface ->
                TransformConstructionClientControls.previewingSurface(
                        surface.id()))) return;
        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        // Both sides must be rebaked: the wall may predate the newly authored
        // ceiling, but its outer vertices and caps still need the same join.
        SURFACE_MESHES.clear();
        for (ConstructionSurface surface : surfaces) {
            SURFACE_EDGE_GEOMETRIES.put(surface.id(), surface);
            Map<Integer, MatchedSurfaceEdge> matches = new HashMap<>();
            for (int edge = 0; edge < 4; edge++) {
                Vec3 first = edgePoint(surface, edge, 0.0D);
                Vec3 middle = edgePoint(surface, edge, 0.5D);
                Vec3 last = edgePoint(surface, edge, 1.0D);
                MatchedSurfaceEdge match = null;
                boolean ambiguous = false;
                for (ConstructionSurface other : surfaces) {
                    if (other.id().equals(surface.id())) continue;
                    for (int otherEdge = 0; otherEdge < 4; otherEdge++) {
                        Vec3 otherFirst = edgePoint(other, otherEdge, 0.0D);
                        Vec3 otherLast = edgePoint(other, otherEdge, 1.0D);
                        boolean same = first.distanceToSqr(otherFirst)
                                < 0.0225D && last.distanceToSqr(otherLast)
                                < 0.0225D;
                        boolean reverse = !same
                                && first.distanceToSqr(otherLast) < 0.0225D
                                && last.distanceToSqr(otherFirst) < 0.0225D;
                        if ((!same && !reverse)
                                || middle.distanceToSqr(edgePoint(other,
                                        otherEdge, 0.5D)) >= 0.0225D) continue;
                        if (match != null) {
                            ambiguous = true;
                            break;
                        }
                        match = new MatchedSurfaceEdge(other, otherEdge,
                                reverse);
                    }
                    if (ambiguous) break;
                }
                if (!ambiguous && match != null) matches.put(edge, match);
            }
            SHARED_SURFACE_EDGES.put(surface.id(), Map.copyOf(matches));
        }
    }

    /** Use the same physical miter point for both meshes. The shared-edge
     * cache supports wall/ceiling joins even when one edge is horizontal in
     * authoring space and the other is vertical. */
    private static Vec3 joinedSurfaceEdge(ConstructionSurface surface,
            double u, double v, double depth) {
        Map<Integer, MatchedSurfaceEdge> matches =
                SHARED_SURFACE_EDGES.get(surface.id());
        if (matches == null || matches.isEmpty()) return null;
        Vec3 sourcePoint = surface.gridPoint(u, v);
        Vec3 sourceNormal = surface.gridNormal(u, v);
        Vec3 result = null;
        int found = 0;
        for (int edge = 0; edge < 4; edge++) {
            if (edge == 0 && Math.abs(u) >= 1.0E-6D
                    || edge == 1 && Math.abs(u - 1.0D) >= 1.0E-6D
                    || edge == 2 && Math.abs(v) >= 1.0E-6D
                    || edge == 3 && Math.abs(v - 1.0D) >= 1.0E-6D) continue;
            MatchedSurfaceEdge match = matches.get(edge);
            if (match == null) continue;
            ConstructionSurface other = match.other();
            double fraction = edge < 2 ? v : u;
            double otherFraction = match.reversed()
                    ? 1.0D - fraction : fraction;
            double otherU = match.otherEdge() == 0 ? 0.0D
                    : match.otherEdge() == 1 ? 1.0D : otherFraction;
            double otherV = match.otherEdge() == 2 ? 0.0D
                    : match.otherEdge() == 3 ? 1.0D : otherFraction;
            Vec3 otherPoint = other.gridPoint(otherU, otherV);
            if (sourcePoint.distanceToSqr(otherPoint) >= 0.0225D) continue;
            int column = Math.min(other.columns() - 1, Math.max(0,
                    (int) Math.floor(otherU * other.columns())));
            int row = Math.min(other.rows() - 1, Math.max(0,
                    (int) Math.floor(otherV * other.rows())));
            ConstructionSurface.SurfaceAttachment attached =
                    other.attachments().get(
                            new ConstructionSurface.SurfaceSlot(column, row));
            if (attached == null || !fullSurfaceCell(attached.state())) continue;
            Vec3 otherNormal = other.gridNormal(otherU, otherV);
            double dot = sourceNormal.dot(otherNormal);
            if (dot <= -0.2D || dot >= 0.985D) continue;
            Vec3 miter = sourceNormal.add(otherNormal)
                    .scale(1.0D / (1.0D + dot));
            if (miter.lengthSqr() > 3.24D) continue;
            result = sourcePoint.add(otherPoint).scale(0.5D)
                    .add(miter.scale(depth));
            if (++found > 1) return null;
        }
        return found == 1 ? result : null;
    }

'''+s[end:]
p.write_text(s)
print('All four Surface edges now share cached geometric joins and rebake both adjoining sides after commit.')