from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
text = path.read_text()

def replace_one(old, new, name):
    global text
    count = text.count(old)
    if count != 1:
        raise AssertionError(f'{name}: expected one occurrence; got {count}')
    text = text.replace(old, new, 1)

# The previous 0.45-block bound rejected existing wall/ceiling authoring
# edges separated by their independent thickness offsets. Exact matching is
# still restricted to adjacent boundaries whose normals form a corner.
replace_one('''        double best = 0.45D * 0.45D;
        double fraction = Double.NaN;''',
'''        double best = 0.72D * 0.72D;
        double fraction = Double.NaN;''', 'edge proximity')

replace_one('''            if (sourcePoint.distanceToSqr(otherPoint) >= 0.45D * 0.45D)
                continue;''',
'''            if (sourcePoint.distanceToSqr(otherPoint) >= 0.72D * 0.72D)
                continue;''', 'seam projection distance')

# Near-coincident full-length curves are not partial just because their
# separately authored endpoints differ by >0.15 blocks. In that case the old
# code snapped the ceiling to the wall AND the wall to the ceiling, producing
# two incompatible seams. Only a genuine interval overlap is partial.
replace_one('''                MatchedSurfaceEdge match = null;
                boolean ambiguous = false;''',
'''                MatchedSurfaceEdge match = null;
                double bestScore = Double.POSITIVE_INFINITY;''', 'prefer nearest compatible adjoining plane')

replace_one('''                        if (match != null) {
                            ambiguous = true;
                            break;
                        }
                        match = new MatchedSurfaceEdge(other, otherEdge,
                                reverse, !same && !reverse, samples);
                    }
                    if (ambiguous) break;
                }
                if (!ambiguous && match != null) matches.put(edge, match);''',
'''                        Vec3 otherStart = edgePoint(other, otherEdge,
                                startFraction);
                        Vec3 otherMiddle = edgePoint(other, otherEdge,
                                middleFraction);
                        Vec3 otherEnd = edgePoint(other, otherEdge,
                                endFraction);
                        double score = first.distanceToSqr(otherStart)
                                + middle.distanceToSqr(otherMiddle)
                                + last.distanceToSqr(otherEnd);
                        if (score >= bestScore) continue;
                        bestScore = score;
                        boolean partial = Math.min(startFraction,
                                endFraction) > 0.06D
                                || Math.max(startFraction, endFraction) < 0.94D;
                        match = new MatchedSurfaceEdge(other, otherEdge,
                                endFraction < startFraction, partial, samples);
                    }
                }
                if (match != null) matches.put(edge, match);''', 'choose actual nearest edge instead of dropping ambiguous corners')

# Solve both intersecting outer surface planes instead of guessing a miter from
# their midpoint. The midpoint formula assumes identical authoring base curves;
# existing ceilings and walls can be half a block apart. The symmetric 2x2
# normal-plane intersection preserves both quadratic tangents and yields one
# common seam point for both sides.
replace_one('''            Vec3 miter = sourceNormal.add(otherNormal)
                    .scale(1.0D / (1.0D + dot));
            if (miter.lengthSqr() > 3.24D) continue;
            // A partial edge has no corresponding neighbouring seam vertices.
            // Weld its boundary directly onto the longer plane's *existing*
            // face instead of mitering toward vertices that do not exist.
            result = match.partial()
                    ? otherPoint.add(otherNormal.scale(depth))
                    : sourcePoint.add(otherPoint).scale(0.5D)
                            .add(miter.scale(depth));''',
'''            // Compute the closest intersection of the two displaced faces.
            // Unlike the old average-normal miter, this handles inherited
            // rooms whose independent curves are close, but not coincident.
            double determinant = 1.0D - dot * dot;
            if (determinant < 0.025D) continue;
            Vec3 middle = sourcePoint.add(otherPoint).scale(0.5D);
            double sourceHeight = depth
                    - middle.subtract(sourcePoint).dot(sourceNormal);
            double otherHeight = depth
                    - middle.subtract(otherPoint).dot(otherNormal);
            double sourceShift = (sourceHeight - dot * otherHeight)
                    / determinant;
            double otherShift = (otherHeight - dot * sourceHeight)
                    / determinant;
            Vec3 intersection = middle.add(sourceNormal.scale(sourceShift))
                    .add(otherNormal.scale(otherShift));
            if (intersection.distanceToSqr(middle) > 2.25D) continue;
            // An edge ending inside a longer neighbour must meet its existing
            // outer face, which has no counterpart boundary to move.
            result = match.partial()
                    ? otherPoint.add(otherNormal.scale(depth))
                    : intersection;''', 'intersect independent wall/ceiling face planes')

# Avoid computing all 48 samples afresh for every candidate in the same edit.
replace_one('''        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        for (ConstructionSurface surface : surfaces) {''',
'''        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        Map<UUID, List<List<Vec3>>> edgeSamples = new HashMap<>();
        for (ConstructionSurface surface : surfaces) {
            edgeSamples.put(surface.id(), List.of(sampleEdge(surface, 0),
                    sampleEdge(surface, 1), sampleEdge(surface, 2),
                    sampleEdge(surface, 3)));
        }
        for (ConstructionSurface surface : surfaces) {''', 'cache physical boundary samples per edit')
replace_one('''                        List<Vec3> samples = sampleEdge(other, otherEdge);
                        double startFraction''',
'''                        List<Vec3> samples = edgeSamples.get(other.id())
                                .get(otherEdge);
                        double startFraction''', 'reuse cached candidate edge')

path.write_text(text)
print('Nearby old and new curved edges use symmetric face intersection and cached nearest-edge matching.')
