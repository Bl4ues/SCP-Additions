import json
from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')
p=root/'TransformConstructionManager.java'
s=p.read_text(encoding='utf-8')
a='import com.bl4ues.scpclassifieddirective.facility.FacilityModule;'
assert s.count(a)==1
s=s.replace(a,a+'\nimport com.bl4ues.scpclassifieddirective.facility.FacilityPipeModule;')

def change_method(method,next_method,old,new):
    global s
    start=s.index('    public static boolean '+method+'(')
    end=s.index('    public static ',start+25) if next_method is None else s.index('    public static boolean '+next_method+'(',start+25)
    sub=s[start:end]
    assert sub.count(old)==1,(method,sub.count(old),old)
    s=s[:start]+sub.replace(old,new)+s[end:]

change_method('placeGroupBlock','placeSurfaceBlock',
'''        TransformConstructionNetwork.broadcastGroupCell(level, groupId,
                target, payload);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''',
'''        TransformConstructionNetwork.broadcastGroupCell(level, groupId,
                target, payload);
        if (payload.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            FacilityPipeModule.refreshGroup(level, groupId);
        }
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''')
change_method('placeSurfaceBlock','placeSurfaceOverlay',
'''        TransformConstructionNetwork.broadcastSurfaceSlot(level, surfaceId,
                targetSlot, payload, deform);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''',
'''        TransformConstructionNetwork.broadcastSurfaceSlot(level, surfaceId,
                targetSlot, payload, deform);
        if (payload.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            FacilityPipeModule.refreshSurface(level, surfaceId);
        }
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''')
change_method('placeSurfaceOverlay','removeSurfaceOverlay',
'''        TransformConstructionNetwork.broadcastSurfaceOverlay(level, surfaceId,
                targetSlot, side, payload, deform);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''',
'''        TransformConstructionNetwork.broadcastSurfaceOverlay(level, surfaceId,
                targetSlot, side, payload, deform);
        if (payload.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            FacilityPipeModule.refreshSurface(level, surfaceId);
        }
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''')
change_method('removeSurfaceOverlay','removeGroupCell',
'''        TransformConstructionNetwork.broadcastSurfaceOverlayRemoved(level, id,
                slot, side);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''',
'''        TransformConstructionNetwork.broadcastSurfaceOverlayRemoved(level, id,
                slot, side);
        if (surface.overlay(slot, side).state().getBlock()
                instanceof FacilityPipeModule.PipeBlock) {
            FacilityPipeModule.refreshSurface(level, id);
        }
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''')
change_method('removeGroupCell','removeSurfaceSlot',
'''        TransformConstructionNetwork.broadcastGroupCellRemoved(level, id, cell);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''',
'''        TransformConstructionNetwork.broadcastGroupCellRemoved(level, id, cell);
        if (state.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            FacilityPipeModule.refreshGroup(level, id);
        }
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''')
change_method('removeSurfaceSlot','removeGroup',
'''        TransformConstructionNetwork.broadcastSurfaceSlotRemoved(
                level, id, slot);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''',
'''        TransformConstructionNetwork.broadcastSurfaceSlotRemoved(
                level, id, slot);
        if (attachment.state().getBlock()
                instanceof FacilityPipeModule.PipeBlock) {
            FacilityPipeModule.refreshSurface(level, id);
        }
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());''')
p.write_text(s,encoding='utf-8')

# All twelve registry identifiers are kept for saved blocks; every model now
# supports four real-time bracket states on every horizontal FACING.
asset=Path('src/main/resources/assets/scp_classified_directive/blockstates')
variants={0:'none',1:'right',2:'left',3:'both'}
for finish in ('clean','caution','blue'):
    for registered in ('both','right','left','none'):
        path=asset/f'wall_pipe_{finish}_{registered}.json'
        assert path.exists(),path
        states={}
        for bracket,model in variants.items():
            for facing,angle in (('north',0),('east',90),('south',180),('west',270)):
                states[f'brackets={bracket},facing={facing}']={
                    'model':f'scp_classified_directive:block/wall_pipe_{finish}_{model}',
                    **({'y':angle} if angle else {})}
        path.write_text(json.dumps({'variants':states},separators=(',',':'))+'\n',encoding='utf-8')

# Middle-click on a legacy 12-variant pipe must always return its public item.
p=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client/TransformPickBlockClient.java')
s=p.read_text(encoding='utf-8')
a='import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;'
assert s.count(a)==1
s=s.replace(a,a+'\nimport com.bl4ues.scpclassifieddirective.facility.FacilityPipeModule;')
a='''        ItemStack picked = state.getBlock().asItem().getDefaultInstance();'''
b='''        ItemStack picked = FacilityPipeModule.pick(state);
        if (picked.isEmpty()) {
            picked = state.getBlock().asItem().getDefaultInstance();
        }'''
assert s.count(a)==1
s=s.replace(a,b)
p.write_text(s,encoding='utf-8')
