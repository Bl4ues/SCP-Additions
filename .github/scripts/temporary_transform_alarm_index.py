from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform/client')
renderer = root / 'TransformAlarmClientRenderer.java'
s = renderer.read_text(encoding='utf-8')


def replace_once(old, new):
    global s
    matches = s.count(old)
    if matches != 1:
        raise RuntimeError(f'Renderer anchor mismatch {matches}: {old[:100]!r}')
    s = s.replace(old, new, 1)


replace_once('import java.util.HashMap;\n', 'import java.util.HashMap;\nimport java.util.HashSet;\n')
replace_once('''    private TransformAlarmClientRenderer() {
    }
''', '''    private static final Map<UUID, Set<TransformGroup.GridPos>> GROUP_ALARMS =
            new HashMap<>();
    private static final Map<UUID, Set<SurfaceKey>> SURFACE_ALARMS =
            new HashMap<>();

    private TransformAlarmClientRenderer() {
    }

    /** A full snapshot changes logical owners; discard their address caches. */
    static void resetIndices() {
        GROUP_ALARMS.clear();
        SURFACE_ALARMS.clear();
        GROUP_HOSTS.clear();
        SURFACE_HOSTS.clear();
    }

    static void invalidateGroup(UUID id) {
        GROUP_ALARMS.remove(id);
        GROUP_HOSTS.keySet().removeIf(key -> key.groupId().equals(id));
    }

    static void invalidateSurface(UUID id) {
        SURFACE_ALARMS.remove(id);
        SURFACE_HOSTS.keySet().removeIf(key -> key.surfaceId().equals(id));
    }

    /** A runtime cell delta must never rescan the entire authored room. */
    static void groupCellChanged(UUID id, TransformGroup.GridPos cell,
            BlockState state) {
        Set<TransformGroup.GridPos> refs = GROUP_ALARMS.get(id);
        boolean alarm = state != null && AlarmModule.isController(state);
        if (refs != null) {
            if (alarm) refs.add(cell);
            else refs.remove(cell);
        }
        if (!alarm) GROUP_HOSTS.remove(new CellKey(id, cell));
    }

    static void surfaceSlotChanged(UUID id,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface surface) {
        Set<SurfaceKey> refs = SURFACE_ALARMS.get(id);
        if (refs == null) return;
        refs.removeIf(key -> key.slot().equals(slot));
        ConstructionSurface.SurfaceAttachment main =
                surface.attachments().get(slot);
        if (main != null && AlarmModule.isController(main.state())) {
            refs.add(new SurfaceKey(id, slot,
                    TransformSurfaceGeometry.MAIN_SIDE, false));
        }
        for (int side : new int[]{-1, 1}) {
            ConstructionSurface.SurfaceAttachment overlay =
                    surface.overlay(slot, side);
            if (overlay != null && AlarmModule.isController(overlay.state())) {
                refs.add(new SurfaceKey(id, slot, side, true));
            }
        }
        SURFACE_HOSTS.keySet().removeIf(key -> key.surfaceId().equals(id)
                && key.slot().equals(slot) && !refs.contains(key));
    }
''')
replace_once('''        if (minecraft.level == null || minecraft.player == null) {
            GROUP_HOSTS.clear();
            SURFACE_HOSTS.clear();
            return;
        }''', '''        if (minecraft.level == null || minecraft.player == null) {
            resetIndices();
            return;
        }''')
begin = s.index('        Set<CellKey> groupKeys = groups.stream().flatMap(group ->')
end = s.index('    private static void renderGroup(', begin)
s = s[:begin] + '''        // Rendering examines only Alarm addresses; updating an animated door
        // elsewhere in the grid does not require a full membership scan.
        Set<UUID> groupIds = groups.stream().map(TransformGroup::id)
                .collect(Collectors.toSet());
        Set<UUID> surfaceIds = surfaces.stream().map(ConstructionSurface::id)
                .collect(Collectors.toSet());
        GROUP_ALARMS.keySet().retainAll(groupIds);
        SURFACE_ALARMS.keySet().retainAll(surfaceIds);
        GROUP_HOSTS.keySet().removeIf(key -> !groupIds.contains(key.groupId())
                || !GROUP_ALARMS.getOrDefault(key.groupId(), Set.of())
                        .contains(key.cell()));
        SURFACE_HOSTS.keySet().removeIf(key -> !surfaceIds.contains(key.surfaceId())
                || !SURFACE_ALARMS.getOrDefault(key.surfaceId(), Set.of())
                        .contains(key));
    }

    private static Set<TransformGroup.GridPos> groupAlarms(TransformGroup group) {
        return GROUP_ALARMS.computeIfAbsent(group.id(), ignored -> {
            Set<TransformGroup.GridPos> cells = new HashSet<>();
            group.cells().forEach((cell, state) -> {
                if (AlarmModule.isController(state)) cells.add(cell);
            });
            return cells;
        });
    }

    private static Set<SurfaceKey> surfaceAlarms(ConstructionSurface surface) {
        return SURFACE_ALARMS.computeIfAbsent(surface.id(), ignored -> {
            Set<SurfaceKey> keys = new HashSet<>();
            surface.attachments().forEach((slot, attachment) -> {
                if (AlarmModule.isController(attachment.state())) {
                    keys.add(new SurfaceKey(surface.id(), slot,
                            TransformSurfaceGeometry.MAIN_SIDE, false));
                }
            });
            surface.overlays().forEach((slot, attachment) -> {
                if (AlarmModule.isController(attachment.state())) {
                    keys.add(new SurfaceKey(surface.id(), slot.slot(),
                            slot.normalSign() < 0 ? -1 : 1, true));
                }
            });
            return keys;
        });
    }

''' + s[end:]
replace_once('''        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : group.cells().entrySet()) {
            BlockState state = entry.getValue();
            if (state == null || !AlarmModule.isController(state)) continue;
            TransformGroup.GridPos cell = entry.getKey();''', '''        for (TransformGroup.GridPos cell : groupAlarms(group)) {
            BlockState state = group.cells().get(cell);
            if (state == null || !AlarmModule.isController(state)) continue;''')
begin = s.index('    private static void renderSurface(Minecraft minecraft,')
end = s.index('    private static void renderSurfaceAlarm(', begin)
s = s[:begin] + '''    private static void renderSurface(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            ConstructionSurface surface) {
        for (SurfaceKey key : surfaceAlarms(surface)) {
            ConstructionSurface.SurfaceAttachment attachment = key.overlay()
                    ? surface.overlay(key.slot(), key.normalSign())
                    : surface.attachments().get(key.slot());
            if (attachment == null) continue;
            renderSurfaceAlarm(minecraft, event, pose, buffers, camera,
                    surface, key.slot(), key.normalSign(), key.overlay(),
                    attachment.state());
        }
    }

''' + s[end:]
renderer.write_text(s, encoding='utf-8')

state_file = root / 'TransformConstructionClientState.java'
s = state_file.read_text(encoding='utf-8')


def replace_state(old, new):
    global s
    matches = s.count(old)
    if matches != 1:
        raise RuntimeError(f'Client state anchor mismatch {matches}: {old[:120]!r}')
    s = s.replace(old, new, 1)


replace_state('''        surfaces = nextSurfaces == null ? List.of() : List.copyOf(nextSurfaces);
        rebuildProxyCells();''', '''        surfaces = nextSurfaces == null ? List.of() : List.copyOf(nextSurfaces);
        TransformAlarmClientRenderer.resetIndices();
        rebuildProxyCells();''')
replace_state('''        TransformAlarmAudioClient.clear();
    }''', '''        TransformAlarmAudioClient.clear();
        TransformAlarmClientRenderer.resetIndices();
    }''')
replace_state('''            next.set(index, current.withCell(cell, state));
            groups = List.copyOf(next);
            TransformConstructionClientRenderer.markGroupCellDirty(''', '''            next.set(index, current.withCell(cell, state));
            groups = List.copyOf(next);
            TransformAlarmClientRenderer.groupCellChanged(groupId, cell, state);
            TransformConstructionClientRenderer.markGroupCellDirty(''')
replace_state('''            next.set(index, current.withAttachment(slot, state, deform));
            surfaces = List.copyOf(next);
            TransformConstructionClientRenderer.markSurfaceSlotDirty(''', '''            next.set(index, current.withAttachment(slot, state, deform));
            surfaces = List.copyOf(next);
            TransformAlarmClientRenderer.surfaceSlotChanged(surfaceId, slot,
                    next.get(index));
            TransformConstructionClientRenderer.markSurfaceSlotDirty(''')
replace_state('''            next.set(index, current.withOverlay(slot, normalSign,
                    state, deform));
            surfaces = List.copyOf(next);
            TransformConstructionClientRenderer.markSurfaceSlotDirty(''', '''            next.set(index, current.withOverlay(slot, normalSign,
                    state, deform));
            surfaces = List.copyOf(next);
            TransformAlarmClientRenderer.surfaceSlotChanged(surfaceId, slot,
                    next.get(index));
            TransformConstructionClientRenderer.markSurfaceSlotDirty(''')
replace_state('''            next.set(index, current.withoutOverlay(slot, normalSign));
            surfaces = List.copyOf(next);
            TransformConstructionClientRenderer.markSurfaceSlotDirty(''', '''            next.set(index, current.withoutOverlay(slot, normalSign));
            surfaces = List.copyOf(next);
            TransformAlarmClientRenderer.surfaceSlotChanged(surfaceId, slot,
                    next.get(index));
            TransformConstructionClientRenderer.markSurfaceSlotDirty(''')
replace_state('''            next.set(index, current.withoutCell(cell));
            groups = List.copyOf(next);
            TransformConstructionClientRenderer.markGroupCellDirty(''', '''            next.set(index, current.withoutCell(cell));
            groups = List.copyOf(next);
            TransformAlarmClientRenderer.groupCellChanged(groupId, cell, null);
            TransformConstructionClientRenderer.markGroupCellDirty(''')
replace_state('''            next.set(index, current.withoutAttachment(slot));
            surfaces = List.copyOf(next);
            TransformConstructionClientRenderer.markSurfaceSlotDirty(''', '''            next.set(index, current.withoutAttachment(slot));
            surfaces = List.copyOf(next);
            TransformAlarmClientRenderer.surfaceSlotChanged(surfaceId, slot,
                    next.get(index));
            TransformConstructionClientRenderer.markSurfaceSlotDirty(''')
replace_state('''    public static void upsertGroup(TransformGroup replacement) {
        if (replacement == null || dimension == null
                || !dimension.equals(replacement.dimension())) return;
''', '''    public static void upsertGroup(TransformGroup replacement) {
        if (replacement == null || dimension == null
                || !dimension.equals(replacement.dimension())) return;
        TransformAlarmClientRenderer.invalidateGroup(replacement.id());
''')
replace_state('''    public static void upsertSurface(ConstructionSurface replacement) {
        if (replacement == null || dimension == null
                || !dimension.equals(replacement.dimension())) return;
''', '''    public static void upsertSurface(ConstructionSurface replacement) {
        if (replacement == null || dimension == null
                || !dimension.equals(replacement.dimension())) return;
        TransformAlarmClientRenderer.invalidateSurface(replacement.id());
''')
state_file.write_text(s, encoding='utf-8')
print('PASS: Alarm renderer and client delta indices patched')
