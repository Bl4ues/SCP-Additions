from pathlib import Path
ROOT = Path('src/main/java/com/bl4ues/scpclassifieddirective')

def replace(path, old, new):
    file = ROOT / path
    text = file.read_text(encoding='utf-8')
    occurrences = text.count(old)
    if occurrences != 1:
        raise SystemExit(f'{file}: anchor occurrences {occurrences}: {old[:70]!r}')
    file.write_text(text.replace(old, new), encoding='utf-8')

state = 'facility/transform/client/TransformConstructionClientState.java'
replace(state, '''    public static void sync(ResourceLocation nextDimension,
            List<TransformGroup> nextGroups,
            List<ConstructionSurface> nextSurfaces) {
        dimension = nextDimension;
        groups = nextGroups == null ? List.of() : List.copyOf(nextGroups);
        surfaces = nextSurfaces == null ? List.of() : List.copyOf(nextSurfaces);
        TransformAlarmClientRenderer.resetIndices();
        refreshDoorPassages();
        rebuildProxyCells();
        TransformAlarmAudioClient.sync(groups, surfaces);
        if (selection != null && !selectionStillExists()) selection = null;
    }
''', '''    public static void sync(ResourceLocation nextDimension,
            List<TransformGroup> nextGroups,
            List<ConstructionSurface> nextSurfaces) {
        List<TransformGroup> previousGroups = groups;
        List<ConstructionSurface> previousSurfaces = surfaces;
        boolean sameDimension = nextDimension != null
                && nextDimension.equals(dimension);
        dimension = nextDimension;
        groups = nextGroups == null ? List.of() : List.copyOf(nextGroups);
        surfaces = nextSurfaces == null ? List.of() : List.copyOf(nextSurfaces);
        TransformAlarmClientRenderer.resetIndices();

        if (!sameDimension) {
            // Joining/changing worlds still requires a complete authoritative
            // snapshot. Never carry any transformed collision into another
            // dimension, even if two owners happen to share an UUID.
            refreshDoorPassages();
            rebuildProxyCells();
        } else {
            // A gizmo commit already rebakes its local collision once. The
            // authoritative echo normally describes the same immutable record:
            // rebuilding every owner on that echo caused a second frame hitch.
            // Keep the per-owner reverse indices and refresh only changed
            // owners. New, removed, or server-corrected geometry is still
            // applied immediately on the client render thread.
            java.util.Map<UUID, TransformGroup> beforeGroups =
                    new java.util.HashMap<>();
            for (TransformGroup old : previousGroups) {
                beforeGroups.put(old.id(), old);
            }
            java.util.Map<UUID, ConstructionSurface> beforeSurfaces =
                    new java.util.HashMap<>();
            for (ConstructionSurface old : previousSurfaces) {
                beforeSurfaces.put(old.id(), old);
            }
            java.util.Set<UUID> nextGroupIds = new java.util.HashSet<>();
            for (TransformGroup group : groups) nextGroupIds.add(group.id());
            java.util.Set<UUID> nextSurfaceIds = new java.util.HashSet<>();
            for (ConstructionSurface surface : surfaces)
                nextSurfaceIds.add(surface.id());
            if (!previousGroups.equals(groups)) refreshDoorPassages();
            for (TransformGroup old : previousGroups) {
                if (!nextGroupIds.contains(old.id())) {
                    removeGroupProxyCells(old.id());
                }
            }
            for (ConstructionSurface old : previousSurfaces) {
                if (!nextSurfaceIds.contains(old.id())) {
                    removeSurfaceProxyCells(old.id());
                }
            }
            for (TransformGroup group : groups) {
                if (!group.equals(beforeGroups.get(group.id()))) {
                    rebuildGroupProxyCells(group.id());
                }
            }
            for (ConstructionSurface surface : surfaces) {
                if (!surface.equals(beforeSurfaces.get(surface.id()))) {
                    rebuildSurfaceProxyCells(surface.id());
                }
            }
        }
        TransformAlarmAudioClient.sync(groups, surfaces);
        if (selection != null && !selectionStillExists()) selection = null;
    }
''')

controls = 'facility/transform/client/TransformConstructionClientControls.java'
replace(controls, '''        TransformConstructionClientState.commitPreviewGeometry();
        drag = null;
    }
''', '''        // Clicking an axis or releasing without changing the pose does not
        // change physics and must not reconstruct the entire collision index.
        if (drag.remembered()
                && Math.abs(drag.lastDelta()) > 1.0E-5D) {
            TransformConstructionClientState.commitPreviewGeometry();
        }
        drag = null;
    }
''')

print('Updated authoritative sync to reuse unchanged collision owners and skip no-op drag rebuilds')
