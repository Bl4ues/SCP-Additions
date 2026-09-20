from pathlib import Path
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformConstructionClientRenderer.java')
s=p.read_text()
old='''        int xSteps = TransformSurfaceGeometry.effectiveDeform(attachment)
                && maxX - minX > 0.20D ? 4 : 1;'''
new='''        // Adjacent tube faces already use the same parametric end vertices and
        // omit internal caps. Finer subdivisions only on curved pipe payloads
        // reduce long polygon chords and pixel-sized cracks at tight bends.
        // The tessellation is cached per slot; it never rebuilds every pipe
        // or the entire Surface when one neighbouring cell changes.
        boolean deform = TransformSurfaceGeometry.effectiveDeform(attachment);
        boolean curvedPipe = deform
                && attachment.state().getBlock() instanceof FacilityPipeModule.PipeBlock
                && surface.curveOffset().lengthSqr() > 1.0E-8D;
        int xSteps = deform && maxX - minX > 0.20D
                ? curvedPipe ? 16 : 4 : 1;'''
assert s.count(old)==1,(s.count(old),old)
s=s.replace(old,new,1)
p.write_text(s)
print('Refined curved pipe tube tessellation to 16 cached subdivisions, retaining shared seams.')
