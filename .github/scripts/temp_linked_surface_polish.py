from pathlib import Path

def rep(path, old, new, count=1):
    p=Path(path)
    s=p.read_text()
    n=s.count(old)
    if n != count:
        raise SystemExit(f"{path}: expected {count} anchor(s), found {n}: {old[:120]!r}")
    p.write_text(s.replace(old,new))

renderer="src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java"
mapfile="src/main/java/com/bl4ues/scpclassifieddirective/client/scp079/Scp079FacilityMapScreen.java"

rep(renderer,
'''            double cellsFromEdge = switch (edge) {
                case 0 -> u * surface.columns();
                case 1 -> (1.0D - u) * surface.columns();
                case 2 -> v * surface.rows();
                default -> (1.0D - v) * surface.rows();
            };
            if (cellsFromEdge < -1.0E-6D || cellsFromEdge > 1.0D) continue;
''',
'''            double cellsFromEdge = switch (edge) {
                case 0 -> u * surface.columns();
                case 1 -> (1.0D - u) * surface.columns();
                case 2 -> v * surface.rows();
                default -> (1.0D - v) * surface.rows();
            };
            // A one-cell correction closes the mathematical edge but can leave
            // a visible kink immediately beside it. Blend the canonical seam
            // through a short grid band so the neighbouring quads reorganise
            // into one continuous curve instead of forming a raised step.
            final double blendCells = 2.25D;
            if (cellsFromEdge < -1.0E-6D || cellsFromEdge > blendCells) continue;
''')

rep(renderer,
'''            double t = 1.0D - Math.max(0.0D,
                    Math.min(1.0D, cellsFromEdge));
            // C1 smoothstep. The derivative is zero both at the seam and at
            // the end of the one-cell blend band, avoiding a second visible
            // crease immediately beside the join.
            double weight = t * t * (3.0D - 2.0D * t);
''',
'''            double t = 1.0D - Math.max(0.0D,
                    Math.min(1.0D, cellsFromEdge / blendCells));
            // C2 smootherstep keeps both slope and acceleration quiet at the
            // seam and where the blend rejoins the authored Surface. The
            // original curve is untouched beyond this narrow band.
            double weight = t * t * t
                    * (t * (t * 6.0D - 15.0D) + 10.0D);
''')

rep(renderer,
'''        List<Vec3> points = new ArrayList<>(49);
        for (int i = 0; i <= 48; i++) {
            points.add(edgePoint(surface, edge, i / 48.0D));
        }
''',
'''        // Seam projection is prepared only after committed geometry edits,
        // so spend a little more precision here instead of carrying a visible
        // 1/48-step mismatch into every rendered block along a long curve.
        List<Vec3> points = new ArrayList<>(97);
        for (int i = 0; i <= 96; i++) {
            points.add(edgePoint(surface, edge, i / 96.0D));
        }
''')

rep(renderer,
'''                        for (int sample = 0; sample <= 48; sample += 4) {
                            Vec3 point = sourceSamples.get(sample);
''',
'''                        for (int sample = 0; sample <= 96; sample += 8) {
                            Vec3 point = sourceSamples.get(sample);
''')

rep(renderer,
'''            double otherFraction = match.partial()
                    ? match.projectionCache().computeIfAbsent(
                            Math.round(fraction * 1_000_000.0D), ignored ->
                                    closestEdgeFraction(match.samples(),
                                            sourcePoint))
                    : (match.reversed() ? 1.0D - fraction : fraction);
            if (!Double.isFinite(otherFraction)) continue;
''',
'''            // Always project onto the neighbour's sampled physical edge.
            // Equal logical fractions are not equal world positions when two
            // old Surfaces have different arc-length distributions.
            double otherFraction = match.projectionCache().computeIfAbsent(
                    Math.round(fraction * 1_000_000.0D), ignored ->
                            closestEdgeFraction(match.samples(), sourcePoint));
            if (!Double.isFinite(otherFraction)) {
                otherFraction = match.reversed()
                        ? 1.0D - fraction : fraction;
            }
''')

rep(renderer,
'''            Vec3 sharedBase;
            if (match.partial()) {
                boolean sourceMaster = surface.id().toString().compareTo(
                        other.id().toString()) <= 0;
                sharedBase = sourceMaster ? sourcePoint : otherPoint;
            } else {
                sharedBase = canonicalSharedEdgePoint(surface, edge, other,
                        match.otherEdge(), match.reversed(), fraction);
            }
''',
'''            // Elect one authored edge as the seam master for *all* joins,
            // not only partial overlaps. Both meshes therefore target the very
            // same world-space curve instead of independently averaging two
            // parameterizations and ending up a few millimetres apart.
            boolean sourceMaster = surface.id().toString().compareTo(
                    other.id().toString()) <= 0;
            Vec3 sharedBase = sourceMaster ? sourcePoint : otherPoint;
''')

rep(mapfile,
'''        double thickness = Math.max(0.18D, transform.scale() * 0.18D);
''',
'''        // Keep the useful rectangular hit/visual language from the close
        // view, but slimmer. Thickness remains a world-map dimension so it
        // shrinks together with rooms when zooming out.
        double thickness = Math.max(0.10D, transform.scale() * 0.10D);
''')

