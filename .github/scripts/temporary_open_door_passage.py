from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective')

def once(text,old,new):
 n=text.count(old)
 if n!=1: raise AssertionError(f'Expected unique occurrence (got {n}): {old[:115]!r}')
 return text.replace(old,new,1)

p=root/'facility/transform/TransformConstructionManager.java';s=p.read_text()
s=once(s,'''        if (collision.isEmpty()) return;
        int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;''','''        if (collision.isEmpty()) return;
        List<AABB> doorways = TransformDoorwayCollision.nearbyPassages(
                group, cell);
        int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;''')
s=once(s,'''                        addWorldBox(index, owner,
                                transformedBounds(group, local),
                                false, true, state.getLightEmission());''','''                        for (AABB worldBox : TransformDoorwayCollision.clip(
                                transformedBounds(group, local), doorways)) {
                            addWorldBox(index, owner, worldBox,
                                    false, true, state.getLightEmission());
                        }''')
s=once(s,'''    public static synchronized void refreshGroupCellRuntime(
            MinecraftServer server, UUID groupId, GridPos cell) {
        refreshGroupCell(server, groupId, cell, false);
    }
''','''    public static synchronized void refreshGroupCellRuntime(
            MinecraftServer server, UUID groupId, GridPos cell) {
        refreshGroupCell(server, groupId, cell, false);
    }

    /** Door state changes can also change the clipped collision of adjacent
     * rotated wall cells; refresh only those owners, never the whole group. */
    public static void refreshOpenDoorNeighbours(MinecraftServer server,
            UUID groupId, GridPos doorCell) {
        if (server == null || groupId == null || doorCell == null) return;
        TransformGroup group = TransformConstructionSavedData.get(server)
                .group(groupId);
        if (group == null) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    GridPos neighbor = doorCell.offset(dx, dy, dz);
                    if (group.cells().containsKey(neighbor)) {
                        refreshGroupCellRuntime(server, groupId, neighbor);
                    }
                }
            }
        }
    }
''')
p.write_text(s);print('Server uses shared open-door passage clipping and local index refresh.')

p=root/'facility/transform/client/TransformConstructionClientState.java';s=p.read_text()
s=once(s,'import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;\n','import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;\nimport com.bl4ues.scpclassifieddirective.facility.transform.TransformDoorwayCollision;\n')
s=once(s,'''        addGroup(mutable, single);
        return freezeContribution(mutable);''','''        addGroup(mutable, single, group);
        return freezeContribution(mutable);''')
s=once(s,'''    private static void addGroup(Map<Long, MutableProxyCell> index,
            TransformGroup group) {''','''    private static void addGroup(Map<Long, MutableProxyCell> index,
            TransformGroup group, TransformGroup neighborhood) {''')
s=once(s,'''            if (collision.isEmpty()) continue;
            int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;''','''            if (collision.isEmpty()) continue;
            List<AABB> doorways = TransformDoorwayCollision.nearbyPassages(
                    neighborhood, cell);
            int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;''')
s=once(s,'''                            addWorldBox(index, transformedBounds(group, local),
                                    group.id(), null, false, true,
                                    state.getLightEmission());''','''                            for (AABB worldBox : TransformDoorwayCollision.clip(
                                    transformedBounds(group, local), doorways)) {
                                addWorldBox(index, worldBox, group.id(), null,
                                        false, true, state.getLightEmission());
                            }''')
s=once(s,'''            if (physicsChanged(previous, state)) {
                rebuildGroupCellProxyCells(groupId, cell);
            }
            TransformAlarmAudioClient.groupCellChanged(groupId, cell, state);''','''            if (physicsChanged(previous, state)) {
                rebuildGroupCellProxyCells(groupId, cell);
            }
            if (FacilityModule.isFacilityDoor(previous)
                    && FacilityModule.isFacilityDoor(state)
                    && FacilityModule.isDoorPassable(previous)
                            != FacilityModule.isDoorPassable(state)) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = 0; dy <= 2; dy++) {
                            if (dx == 0 && dz == 0 && dy == 0) continue;
                            TransformGroup.GridPos neighbor = cell.offset(dx, dy, dz);
                            if (next.get(index).cells().containsKey(neighbor)) {
                                rebuildGroupCellProxyCells(groupId, neighbor);
                            }
                        }
                    }
                }
            }
            TransformAlarmAudioClient.groupCellChanged(groupId, cell, state);''')
p.write_text(s);print('Client predicts identical clipped collision and updates adjacent owners on door transitions.')

p=root/'facility/transform/TransformDoorRuntime.java';s=p.read_text()
s=once(s,'''        if (refreshCollision || passabilityChanged) {
            TransformConstructionManager.refreshGroupCellRuntime(
                    level.getServer(), group.id(), cell);
        }
''','''        if (refreshCollision || passabilityChanged) {
            TransformConstructionManager.refreshGroupCellRuntime(
                    level.getServer(), group.id(), cell);
        }
        if (passabilityChanged) {
            TransformConstructionManager.refreshOpenDoorNeighbours(
                    level.getServer(), group.id(), cell);
        }
''')
p.write_text(s);print('Open/close triggers focused neighbouring collision rebuild.')
