from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective')
def patch(name, old, new):
    path=root/name
    value=path.read_text()
    assert value.count(old)==1,(name,old,value.count(old))
    path.write_text(value.replace(old,new))

patch('facility/transform/TransformPlacementStateRuntime.java', '''        // The local Surface wall is at Z=0 and pipe geometry is authored
        // against its positive Z side when FACING is NORTH, on either face.
        if (item.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            return item.getBlock().defaultBlockState().setValue(
                    BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }''', '''        // The pipe mesh is authored at the +Z edge with NORTH facing.
        // A Surface wall starts at local Z=0, so reverse the pipe model into
        // the outward-facing half-cell instead of projecting it a full block
        // behind the guide. The Surface normal handles front/back globally.
        if (item.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            return item.getBlock().defaultBlockState().setValue(
                    BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        }''')

name='facility/transform/client/TransformSurfacePlacementClient.java'
patch(name,'import com.bl4ues.scpclassifieddirective.facility.FacilityModule;', '''import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.item.ScrewdriverItem;''')
patch(name,'''        if (player.isShiftKeyDown()) return;
        // Empty authoring guides''', '''        if (player.isShiftKeyDown()) return;
        boolean configuringAlarm = player.getMainHandItem().getItem()
                instanceof ScrewdriverItem
                || player.getOffhandItem().getItem() instanceof ScrewdriverItem;
        // Empty authoring guides''')
patch(name, '''            if (!interactive(group.state())) return;
            TransformConstructionNetwork.useGroupCell(''', '''            if (!interactive(group.state()) && !(configuringAlarm
                    && AlarmModule.isController(group.state()))) return;
            TransformConstructionNetwork.useGroupCell(''')
patch(name, '''        if (surface == null || !interactiveSurfaceTarget(surface)) return;''', '''        if (surface == null
                || !interactiveSurfaceTarget(surface, configuringAlarm)) return;''')
patch(name, '''    private static boolean interactiveSurfaceTarget(
            TransformSurfaceRaycast.Target target) {
        ConstructionSurface.SurfaceAttachment attachment =
                surfaceAttachment(target);
        return attachment != null && interactive(attachment.state());
    }''', '''    private static boolean interactiveSurfaceTarget(
            TransformSurfaceRaycast.Target target, boolean configuringAlarm) {
        ConstructionSurface.SurfaceAttachment attachment =
                surfaceAttachment(target);
        return attachment != null && (interactive(attachment.state())
                || configuringAlarm
                && AlarmModule.isController(attachment.state()));
    }''')
