from pathlib import Path
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s=p.read_text()
def once(old,new):
 global s
 count=s.count(old)
 if count!=1: raise AssertionError(f'Expected 1 anchor, found {count}: {old[:90]!r}')
 s=s.replace(old,new,1)
once('''        VertexFrame frame = TransformSurfaceGeometry.effectiveDeform(attachment)
                ? deformedFrame(surface, slot, point.x, point.y, point.z,
                        localNormal, normalSign, depthOffset)
''','''        // Miter shared physical end edges of distinct Surface planes. This is
        // only checked for a structural vertex on the OUTER edge of a Surface,
        // not for every quad in a large authored wall.
        boolean joinEdge = !overlay
                && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                && (slot.column() == 0 && Math.abs(point.x) < 1.0E-6D
                    || slot.column() == surface.columns() - 1
                        && Math.abs(point.x - 1.0D) < 1.0E-6D)
                && fullSurfaceCell(attachment.state());
        VertexFrame frame = TransformSurfaceGeometry.effectiveDeform(attachment)
                ? deformedFrame(surface, slot, point.x, point.y, point.z,
                        localNormal, normalSign, depthOffset, joinEdge)
''')
once('''            Vec3 localNormal, int normalSign, double depthOffset) {
        int side = normalSign < 0 ? -1 : 1;
        double baseX = surface.flipped() ? 1.0D - x : x;''','''            Vec3 localNormal, int normalSign, double depthOffset,
            boolean joinEdge) {
        int side = normalSign < 0 ? -1 : 1;
        double baseX = surface.flipped() ? 1.0D - x : x;''')
once('''        Vec3 position = surface.gridPoint(u, v)
                .add(normal.scale(z + depthOffset));
        Vec3 transformedNormal = TransformMath.safeNormalize(''','''        Vec3 position = surface.gridPoint(u, v)
                .add(normal.scale(z + depthOffset));
        if (joinEdge && z >= 0.0D && z <= 1.000001D) {
            Vec3 joined = joinedSurfaceEdge(surface, u, v, z);
            if (joined != null) position = joined;
        }
        Vec3 transformedNormal = TransformMath.safeNormalize(''')
anchor='''    private static VertexFrame rigidFrame(ConstructionSurface surface,
'''
assert s.count(anchor)==1
helper='''    /** Miter the two thickness vectors at an authored shared end edge.
     * Simply increasing tessellation cannot close the wedge that appears when
     * two independent curved wall normals meet at an angle. Do not merge
     * free-standing, parallel, disconnected or decorative surfaces. */
    private static Vec3 joinedSurfaceEdge(ConstructionSurface surface,
            double edgeU, double v, double depth) {
        boolean atStart = Math.abs(edgeU) < 1.0E-6D;
        boolean atEnd = Math.abs(edgeU - 1.0D) < 1.0E-6D;
        if (!atStart && !atEnd) return null;
        double sourceEdge = atStart ? 0.0D : 1.0D;
        Vec3 sourceStart = surface.gridPoint(sourceEdge, 0.0D);
        Vec3 sourceMid = surface.gridPoint(sourceEdge, 0.5D);
        Vec3 sourceEnd = surface.gridPoint(sourceEdge, 1.0D);
        Vec3 sourcePoint = surface.gridPoint(edgeU, v);
        Vec3 sourceNormal = surface.gridNormal(edgeU, v);
        Vec3 result = null;
        int matches = 0;
        for (ConstructionSurface other : TransformConstructionClientState
                .surfaces(surface.dimension())) {
            if (surface.id().equals(other.id())) continue;
            for (int end = 0; end < 2; end++) {
                double otherEdge = end;
                Vec3 otherStart = other.gridPoint(otherEdge, 0.0D);
                Vec3 otherMid = other.gridPoint(otherEdge, 0.5D);
                Vec3 otherEnd = other.gridPoint(otherEdge, 1.0D);
                boolean same = sourceStart.distanceToSqr(otherStart) < 0.0064D
                        && sourceEnd.distanceToSqr(otherEnd) < 0.0064D;
                boolean reversed = !same
                        && sourceStart.distanceToSqr(otherEnd) < 0.0064D
                        && sourceEnd.distanceToSqr(otherStart) < 0.0064D;
                if ((!same && !reversed)
                        || sourceMid.distanceToSqr(otherMid) >= 0.0064D) {
                    continue;
                }
                double otherV = reversed ? 1.0D - v : v;
                Vec3 otherPoint = other.gridPoint(otherEdge, otherV);
                if (sourcePoint.distanceToSqr(otherPoint) >= 0.0064D) continue;
                int row = Math.min(other.rows() - 1,
                        Math.max(0, (int) Math.floor(otherV * other.rows())));
                int column = end == 0 ? 0 : other.columns() - 1;
                ConstructionSurface.SurfaceAttachment otherAttachment =
                        other.attachments().get(new ConstructionSurface.SurfaceSlot(
                                column, row));
                if (otherAttachment == null
                        || !fullSurfaceCell(otherAttachment.state())) continue;
                Vec3 otherNormal = other.gridNormal(otherEdge, otherV);
                double dot = sourceNormal.dot(otherNormal);
                if (dot <= -0.2D || dot >= 0.985D) continue;
                Vec3 miter = sourceNormal.add(otherNormal)
                        .scale(1.0D / (1.0D + dot));
                if (miter.lengthSqr() > 3.24D) continue;
                result = sourcePoint.add(otherPoint).scale(0.5D)
                        .add(miter.scale(depth));
                matches++;
                // Ambiguous T-junctions are deliberately left unchanged;
                // mitering to the wrong wall is worse than an exposed cap.
                if (matches > 1) return null;
            }
        }
        return matches == 1 ? result : null;
    }

'''
s=s.replace(anchor,helper+anchor,1)
p.write_text(s)
print('Compatible independent Surface end edges now meet at a shared miter.')
