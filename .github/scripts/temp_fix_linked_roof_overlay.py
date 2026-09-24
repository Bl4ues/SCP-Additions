from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s = path.read_text(encoding='utf-8')

def replace_one(before, after):
    global s
    count = s.count(before)
    if count != 1:
        raise RuntimeError(f'expected one overlay anchor; got {count}: {before[:100]!r}')
    s = s.replace(before, after, 1)

replace_one(
'''        boolean linkedRoof = surface.bridge() != null && deform && !overlay
                && fullSurfaceCell(attachment.state())
                && surface.bridge().hasProfiles();
        if (linkedRoof) {
            // Use identical transverse cuts on every linked-roof column and
            // every baked face, including the side caps. More samples are
            // reserved for short roofs whose entire arch spans 1-2 cells.
            ySteps = surface.rows() <= 2 ? 8 : 4;
        }
''',
'''        boolean linkedProfile = surface.bridge() != null
                && surface.bridge().hasProfiles();
        // A second structural layer follows the SAME Hermite loft as the
        // underlying linked roof. Without the loft's transverse subdivisions,
        // a full cube on a strongly curved roof becomes one large non-planar
        // quad; Minecraft triangulates that quad into the protruding wedge.
        // Only full cubes use the variable-resolution roof zipper; thin pipes
        // and other deformable overlays still receive enough transverse cuts
        // to avoid spanning the entire arch with one quad.
        if (linkedProfile && deform && (overlay || curvedPipe)) {
            ySteps = Math.max(ySteps, surface.rows() <= 2 ? 8 : 4);
            if (overlay) xSteps = Math.max(xSteps, 4);
        }
        boolean linkedRoof = linkedProfile && deform
                && fullSurfaceCell(attachment.state());
        if (linkedRoof) {
            // Match the loft's transverse bands for both structural layers.
            // The outer layer retains its one-block normal offset, but must
            // not lose the interior cuts which give the arch its cube shape.
            ySteps = Math.max(ySteps, surface.rows() <= 2 ? 8 : 4);
        }
''')

assert 'boolean linkedRoof = linkedProfile && deform' in s
assert 'if (overlay) xSteps = Math.max(xSteps, 4);' in s
assert 'if (linkedRoof && emitLinkedRoofQuad' in s
assert 'boolean seamEligible = !overlay' in s
path.write_text(s, encoding='utf-8')
print('Linked roof structural overlays share roof loft tessellation, not a single warped face.')
