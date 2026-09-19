from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective')
p=root/'facility/transform/client/TransformConstructionClientRenderer.java'
s=p.read_text()
a='''        int xSteps = attachment.deform() && maxX - minX > 0.20D ? 4 : 1;
        int ySteps = attachment.deform()
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D'''
b='''        int xSteps = TransformSurfaceGeometry.effectiveDeform(attachment)
                && maxX - minX > 0.20D ? 4 : 1;
        int ySteps = TransformSurfaceGeometry.effectiveDeform(attachment)
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D'''
assert s.count(a)==1
s=s.replace(a,b)
a='''        VertexFrame frame = attachment.deform()
                ? deformedFrame(surface, slot, point.x, point.y, point.z,'''
b='''        VertexFrame frame = TransformSurfaceGeometry.effectiveDeform(attachment)
                ? deformedFrame(surface, slot, point.x, point.y, point.z,'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s)
p=root/'facility/transform/client/TransformSurfaceRaycast.java'
s=p.read_text()
a='facePoint(surface, visualSlot, attachment.deform(),'
assert s.count(a)==4,s.count(a)
s=s.replace(a,'facePoint(surface, visualSlot,\n                            TransformSurfaceGeometry.effectiveDeform(attachment),')
p.write_text(s)
p=root/'facility/transform/TransformPlacementStateRuntime.java'
s=p.read_text()
a='''import com.bl4ues.scpclassifieddirective.facility.WallMountedSupportEvents;'''
b='''import com.bl4ues.scpclassifieddirective.facility.WallMountedSupportEvents;
import com.bl4ues.scpclassifieddirective.facility.FacilityPipeModule;'''
assert s.count(a)==1;s=s.replace(a,b)
a='''        // Alarm placement uses the clicked sub-cell position as part of its'''
b='''        // Pipe models are authored on the local SOUTH edge for FACING NORTH.
        // The parent-world clicked face must not reverse them on rotated grids.
        if (item.getBlock() instanceof FacilityPipeModule.PipeBlock
                && outwardLocal.getAxis().isHorizontal()) {
            return item.getBlock().defaultBlockState().setValue(
                    BlockStateProperties.HORIZONTAL_FACING,
                    outwardLocal.getOpposite());
        }

        // Alarm placement uses the clicked sub-cell position as part of its'''
assert s.count(a)==1;s=s.replace(a,b)
a='''        // Surface payloads use a canonical local cell: wall plane at local Z=0,'''
b='''        // The local Surface wall is at Z=0 and pipe geometry is authored
        // against its positive Z side when FACING is NORTH, on either face.
        if (item.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            return item.getBlock().defaultBlockState().setValue(
                    BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }

        // Surface payloads use a canonical local cell: wall plane at local Z=0,'''
assert s.count(a)==1;s=s.replace(a,b)
p.write_text(s)
