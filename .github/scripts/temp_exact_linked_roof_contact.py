from pathlib import Path

path = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
source = path.read_text(encoding='utf-8')
old = '''        // At a child cell boundary the mother now renders an explicit vertex,
        // not an interpolated point along its older, coarser polygon chord.
        // Evaluate exactly that same transformed mother vertex on the roof.
        double roofCell = u * roof.columns();
        Vec3 parentShell = Math.abs(roofCell - Math.rint(roofCell))
                < 1.0E-9D
                ? parentShellVertex(parent, parentEdge, parentU, depth)
                : parentShellOnTessellatedEdge(parent, parentEdge,
                        parentU, depth);
'''
new = '''        // The parent owns the physical seam. Its polygonal edge has vertices
        // at the parent's render-grid cuts, NOT at each child-cell boundary.
        // A child column boundary may land halfway along a parent polygon:
        // sampling the analytic parent curve at that point places the child's
        // vertex outside the mother's straight rendered chord. This produced
        // the intermittent visible slits along the otherwise smooth arch.
        // Always sample the actual parent polygon, including at child corners.
        Vec3 parentShell = parentShellOnTessellatedEdge(parent,
                parentEdge, parentU, depth);
'''
if source.count(old) != 1:
    raise RuntimeError('Expected exactly one linked roof parent-shell boundary branch')
source = source.replace(old, new, 1)
assert 'Math.abs(roofCell - Math.rint(roofCell))' not in source
assert 'Vec3 parentShell = parentShellOnTessellatedEdge(parent,' in source
assert 'if (explicitLinkedPair(surface, other)) continue;' in source
path.write_text(source, encoding='utf-8')
print('Linked roof contact vertices now always follow the mother polygonal edge')
