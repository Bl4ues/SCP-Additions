from pathlib import Path

renderer = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
surface = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/ConstructionSurface.java')

def patch(path, old, new):
    text = path.read_text(encoding='utf-8')
    matches = text.count(old)
    if matches != 1:
        raise RuntimeError(f'{path}: expected exactly one patch anchor; found {matches}: {old[:100]!r}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')

# Rebuild mesh when roof profile or independent top control changes without
# necessarily moving its four corner positions.
patch(renderer, '''                && java.util.Objects.equals(a.heightCurveOffset(),
                        b.heightCurveOffset())
                && a.flipped() == b.flipped();''', '''                && java.util.Objects.equals(a.heightCurveOffset(),
                        b.heightCurveOffset())
                && java.util.Objects.equals(a.topCurveOffset(),
                        b.topCurveOffset())
                && java.util.Objects.equals(a.bridge(), b.bridge())
                && a.flipped() == b.flipped();''')

# The linked roof cannot use 24 transverse cuts on its end columns and 2 on
# its interior columns; that produces unmatched T-junctions on a bent arch.
patch(renderer, '''        boolean linkedRoof = surface.bridge() != null && deform && !overlay
                && fullSurfaceCell(attachment.state())
                && surface.bridge().hasProfiles();
        // The two parent walls may have DIFFERENT grids.''', '''        boolean linkedRoof = surface.bridge() != null && deform && !overlay
                && fullSurfaceCell(attachment.state())
                && surface.bridge().hasProfiles();
        if (linkedRoof) {
            // Use identical transverse cuts on every linked-roof column and
            // every baked face, including the side caps. More samples are
            // reserved for short roofs whose entire arch spans 1-2 cells.
            ySteps = surface.rows() <= 2 ? 8 : 4;
        }
        // The two parent walls may have DIFFERENT grids.''')

patch(renderer, '''        List<Double> interior = uniformCuts(Math.max(2,
                Math.min(4, xSteps)));''', '''        // Every roof row shares identical interior longitudinal knots.
        // Only the contact strips inherit the two independent parent grids;
        // the zipper joins each one to this stable interior/crown grid.
        List<Double> interior = uniformCuts(4);''')

# Preserve exact parent knot parameters. Quantizing to seven decimal places
# moves the nominal contact vertices off their parent's stored samples.
patch(renderer, '''            double value = Math.rint(fraction * 1.0E7D) / 1.0E7D;
            // Parent cuts and uniform cuts sometimes differ only in the last
            // digits; never emit zero-width quads along the stitched ridge.
            Double lower = cuts.floor(value);
            Double upper = cuts.ceiling(value);
            if ((lower != null && Math.abs(lower - value) < 1.0E-6D)
                    || (upper != null && Math.abs(upper - value) < 1.0E-6D))
                return;
            cuts.add(value);''', '''            // Keep full precision and merge only machine-near duplicates.
            // The original 1e-7 rounding broke exact parent vertex identity.
            Double lower = cuts.floor(fraction);
            Double upper = cuts.ceiling(fraction);
            if ((lower != null && Math.abs(lower - fraction) < 1.0E-10D)
                    || (upper != null && Math.abs(upper - fraction) < 1.0E-10D))
                return;
            cuts.add(fraction);''')

# Return the same world-space shell point at contact, not a numerically
# reconstructed equivalent produced by subtracting/adding the roof normal.
patch(renderer, '''        double weight = t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
        return current.add(parentShell.subtract(roofEdge).scale(weight));''', '''        double weight = t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
        if (cells <= 1.0E-10D) return parentShell;
        return current.add(parentShell.subtract(roofEdge).scale(weight));''')

# At a stored parent sample, reuse its Vec3 verbatim rather than lerping it.
patch(surface, '''            double part = coordinate - index;
            return points.get(index).scale(1.0D - part)
                    .add(points.get(index + 1).scale(part));''', '''            int exact = (int) Math.rint(coordinate);
            if (exact >= 0 && exact < points.size()
                    && Math.abs(coordinate - exact) <= 1.0E-9D)
                return points.get(exact);
            double part = coordinate - index;
            return points.get(index).scale(1.0D - part)
                    .add(points.get(index + 1).scale(part));''')

print('Patched linked-roof mesh invalidation, uniform internal cuts, precise contact knots, and parent shell pinning')
