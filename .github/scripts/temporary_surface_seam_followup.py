from pathlib import Path
p = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = p.read_text()
def change(before,after,desc):
    global s
    matches=s.count(before)
    if matches!=1: raise AssertionError(f'{desc}: expected one match, got {matches}')
    s=s.replace(before,after,1)

change('''        int xSteps = deform && maxX - minX > 0.20D
                ? curvedPipe ? 16 : 4 : 1;
        int ySteps = TransformSurfaceGeometry.effectiveDeform(attachment)
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                && maxY - minY > 0.20D ? 2 : 1;''', '''        // Give only structural perimeter tiles additional curve samples.
        // Two independently gridded quadratic planes need enough vertices on
        // their shared edge to avoid a visible sawtooth between their chords.
        // Interior tiles retain their cheap four-step tessellation.
        boolean perimeter = !overlay && fullSurfaceCell(attachment.state())
                && (slot.column() == 0 && minX < 1.0E-5D
                || slot.column() == surface.columns() - 1
                        && maxX > 0.99999D
                || slot.row() == 0 && minY < 1.0E-5D
                || slot.row() == surface.rows() - 1
                        && maxY > 0.99999D);
        int xSteps = deform && maxX - minX > 0.20D
                ? curvedPipe ? 16 : perimeter
                        && surface.curveOffset().lengthSqr() > 1.0E-8D
                        ? 12 : 4 : 1;
        int ySteps = deform
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                && maxY - minY > 0.20D ? perimeter ? 6 : 2 : 1;''', 'perimeter sampling')

change('''    private record MatchedSurfaceEdge(ConstructionSurface other,
            int otherEdge, boolean reversed) { }

    private static Vec3 edgePoint''', '''    private record MatchedSurfaceEdge(ConstructionSurface other,
            int otherEdge, boolean reversed, boolean partial,
            List<Vec3> samples) { }

    /** Cached arc points let a short wall edge meet an interior subsection of
     * a longer ceiling edge even when their slot counts and arc parameters
     * differ. Only neighbouring physical edges may be considered. */
    private static List<Vec3> sampleEdge(ConstructionSurface surface,
            int edge) {
        List<Vec3> points = new ArrayList<>(49);
        for (int i = 0; i <= 48; i++) {
            points.add(edgePoint(surface, edge, i / 48.0D));
        }
        return List.copyOf(points);
    }

    private static double closestEdgeFraction(List<Vec3> samples, Vec3 point) {
        double best = 0.45D * 0.45D;
        double fraction = Double.NaN;
        for (int i = 1; i < samples.size(); i++) {
            Vec3 a = samples.get(i - 1);
            Vec3 span = samples.get(i).subtract(a);
            double length2 = span.lengthSqr();
            double t = length2 < 1.0E-12D ? 0.0D
                    : net.minecraft.util.Mth.clamp(
                            point.subtract(a).dot(span) / length2,
                            0.0D, 1.0D);
            double distance = a.add(span.scale(t)).distanceToSqr(point);
            if (distance < best) {
                best = distance;
                fraction = (i - 1 + t) / (samples.size() - 1.0D);
            }
        }
        return fraction;
    }

    private static Vec3 edgePoint''', 'edge projection cache')

before='''                        boolean same = first.distanceToSqr(otherFirst)
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
                                reverse);'''
after='''                        boolean same = first.distanceToSqr(otherFirst)
                                < 0.0225D && last.distanceToSqr(otherLast)
                                < 0.0225D;
                        boolean reverse = !same
                                && first.distanceToSqr(otherLast) < 0.0225D
                                && last.distanceToSqr(otherFirst) < 0.0225D;
                        // A short edge is allowed to meet a portion of a
                        // longer edge. Do not match unrelated nearby planes:
                        // the entire edge must project continuously and their
                        // normals must form an actual corner, not a parallel
                        // coplanar overlay.
                        if (!same && !reverse && middle.distanceToSqr(
                                otherFirst.add(otherLast).scale(0.5D))
                                > Math.pow(otherFirst.distanceTo(otherLast)
                                        + 0.5D, 2.0D)) continue;
                        List<Vec3> samples = sampleEdge(other, otherEdge);
                        double startFraction = closestEdgeFraction(samples, first);
                        double middleFraction = closestEdgeFraction(samples, middle);
                        double endFraction = closestEdgeFraction(samples, last);
                        if (!Double.isFinite(startFraction)
                                || !Double.isFinite(middleFraction)
                                || !Double.isFinite(endFraction)
                                || Math.abs(endFraction - startFraction) < 0.12D
                                || (middleFraction - startFraction)
                                        * (middleFraction - endFraction) > 0.01D) {
                            continue;
                        }
                        double otherU = otherEdge == 0 ? 0.0D
                                : otherEdge == 1 ? 1.0D : middleFraction;
                        double otherV = otherEdge == 2 ? 0.0D
                                : otherEdge == 3 ? 1.0D : middleFraction;
                        double dot = surface.gridNormal(
                                edge < 2 ? edge : 0.5D,
                                edge == 2 ? 0.0D : edge == 3 ? 1.0D : 0.5D)
                                .dot(other.gridNormal(otherU, otherV));
                        if (dot <= -0.2D || dot >= 0.985D) continue;
                        if (match != null) {
                            ambiguous = true;
                            break;
                        }
                        match = new MatchedSurfaceEdge(other, otherEdge,
                                reverse, !same && !reverse, samples);'''
change(before,after,'partial edge matching')

change('''            double otherFraction = match.reversed()
                    ? 1.0D - fraction : fraction;
            double otherU = match.otherEdge() == 0 ? 0.0D''', '''            // Grid arc-length distributions can differ at an otherwise exact
            // physical edge. Project this vertex to the neighbour's edge
            // rather than assuming the same grid fraction on both meshes.
            double otherFraction = closestEdgeFraction(match.samples(),
                    sourcePoint);
            if (!Double.isFinite(otherFraction)) continue;
            double otherU = match.otherEdge() == 0 ? 0.0D''', 'vertex projection')
change('''            if (sourcePoint.distanceToSqr(otherPoint) >= 0.0225D) continue;''', '''            if (sourcePoint.distanceToSqr(otherPoint) >= 0.45D * 0.45D)
                continue;''', 'join vertex tolerance')
change('''            result = sourcePoint.add(otherPoint).scale(0.5D)
                    .add(miter.scale(depth));
            if (++found > 1) return null;''', '''            // A partial edge has no corresponding neighbouring seam vertices.
            // Weld its boundary directly onto the longer plane's *existing*
            // face instead of mitering toward vertices that do not exist.
            result = match.partial()
                    ? otherPoint.add(otherNormal.scale(depth))
                    : sourcePoint.add(otherPoint).scale(0.5D)
                            .add(miter.scale(depth));
            if (++found > 1) return null;''', 'weld partial structural border')
p.write_text(s)
print('Stitched compatible full and partial Surface edges with cached projections and perimeter-only tessellation.')
