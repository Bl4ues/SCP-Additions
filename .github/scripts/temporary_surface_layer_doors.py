from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')
path = root / 'TransformSurfaceDoorRuntime.java'
s = path.read_text(encoding='utf-8')

def once(old, new):
    global s
    if s.count(old) != 1:
        raise RuntimeError(f'Surface door anchor matched {s.count(old)} times: {old[:115]!r}')
    s = s.replace(old, new, 1)

def section(start, end, replacement):
    global s
    if s.count(start) != 1 or s.count(end) != 1:
        raise RuntimeError(f'Surface door method boundary missing or ambiguous: {start[:70]}')
    i, j = s.index(start), s.index(end)
    if j <= i:
        raise RuntimeError('Surface door method boundaries reversed')
    s = s[:i] + replacement + s[j:]

once('start(level, hit.surface(), hit.slot(), address.family(),',
     'start(level, hit.surface(), hit.slot(), hit.normalSign(), address.family(),')
section('    public static boolean useSurfaceSlot(ServerPlayer player, UUID surfaceId,',
        '    @SubscribeEvent\n    public static void onServerTick(', '''    public static boolean useSurfaceSlot(ServerPlayer player, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
        return useSurfaceDoor(player, surfaceId, slot, 0);
    }

    public static boolean useSurfaceOverlay(ServerPlayer player, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
        return useSurfaceDoor(player, surfaceId, slot,
                normalSign < 0 ? -1 : 1);
    }

    private static boolean useSurfaceDoor(ServerPlayer player, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
        if (player == null || surfaceId == null || slot == null
                || !(player.level() instanceof ServerLevel level)) return false;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                level.getServer());
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface == null || !surface.dimension().equals(
                level.dimension().location())) return false;
        ConstructionSurface.SurfaceAttachment attachment =
                attachment(surface, slot, normalSign);
        if (attachment == null) return false;
        DoorAddress address = address(attachment.state());
        if (address == null || !address.family().directUse()) return false;
        if (player.getEyePosition().distanceToSqr(
                center(surface, slot, normalSign)) > 36.0D) return false;
        if (address.stage() == DoorStage.CLOSED
                || address.stage() == DoorStage.OPEN) {
            start(level, surface, slot, normalSign, address.family(),
                    address.stage() == DoorStage.CLOSED);
        }
        return true;
    }

    private static ConstructionSurface.SurfaceAttachment attachment(
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            int normalSign) {
        return normalSign == 0 ? surface.attachments().get(slot)
                : surface.overlay(slot, normalSign);
    }

''')
once('''            ConstructionSurface.SurfaceAttachment attachment =
                    surface.attachments().get(ref.slot());''', '''            ConstructionSurface.SurfaceAttachment attachment =
                    attachment(surface, ref.slot(), ref.normalSign());''')
once('CellKey key = new CellKey(surface.id(), ref.slot());',
     'CellKey key = new CellKey(surface.id(), ref.slot(), ref.normalSign());')
once('''                start(level, surface, ref.slot(), address.family(), true);''',
     '''                start(level, surface, ref.slot(), ref.normalSign(),
                        address.family(), true);''')
once('''                start(level, surface, ref.slot(), address.family(), false);''',
     '''                start(level, surface, ref.slot(), ref.normalSign(),
                        address.family(), false);''')
section('    private static List<DoorRef> doorRefs(MinecraftServer server,',
        '    private static boolean advance(ServerLevel level, CellKey key,', '''    private static List<DoorRef> doorRefs(MinecraftServer server,
            TransformConstructionSavedData data) {
        long revision = data.revision();
        DoorIndex cached = DOOR_INDEX.get(server);
        if (cached != null && cached.revision() == revision) {
            return cached.refs();
        }
        java.util.ArrayList<DoorRef> refs = new java.util.ArrayList<>();
        for (ConstructionSurface surface : data.surfaces()) {
            collectDoorRefs(refs, surface);
        }
        List<DoorRef> immutable = List.copyOf(refs);
        DOOR_INDEX.put(server, new DoorIndex(revision, immutable));
        return immutable;
    }

    private static void collectDoorRefs(List<DoorRef> refs,
            ConstructionSurface surface) {
        for (Map.Entry<ConstructionSurface.SurfaceSlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.attachments().entrySet()) {
            if (address(entry.getValue().state()) != null) {
                refs.add(new DoorRef(surface.id(), entry.getKey(), 0));
            }
        }
        for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.overlays().entrySet()) {
            if (address(entry.getValue().state()) != null) {
                refs.add(new DoorRef(surface.id(), entry.getKey().slot(),
                        entry.getKey().normalSign() < 0 ? -1 : 1));
            }
        }
    }

    private static void collectSlotDoorRefs(List<DoorRef> refs,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot) {
        for (int sign : new int[]{0, -1, 1}) {
            ConstructionSurface.SurfaceAttachment current =
                    attachment(surface, slot, sign);
            if (current != null && address(current.state()) != null) {
                refs.add(new DoorRef(surface.id(), slot, sign));
            }
        }
    }

''')
once('''        ConstructionSurface.SurfaceAttachment attachment =
                surface.attachments().get(key.slot());''', '''        ConstructionSurface.SurfaceAttachment attachment =
                attachment(surface, key.slot(), key.normalSign());''')
s = s.replace('''setState(level, surface, key.slot(), attachment,
                    copyFacing(current, next),''', '''setState(level, surface, key.slot(), key.normalSign(),
                    attachment, copyFacing(current, next),''')
if s.count('key.normalSign(),\n                    attachment, copyFacing(current, next),') != 2:
    raise RuntimeError('Expected both opening and closing transition updates')
once('''    private static void start(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, DoorFamily family,
            boolean opening) {''', '''    private static void start(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            DoorFamily family, boolean opening) {''')
once('''        ConstructionSurface.SurfaceAttachment attachment =
                surface.attachments().get(slot);
        if (attachment == null) return;
        BlockState current = attachment.state();''', '''        ConstructionSurface.SurfaceAttachment attachment =
                attachment(surface, slot, normalSign);
        if (attachment == null) return;
        BlockState current = attachment.state();''')
once('''        Vec3 center = center(surface, slot);
        Scp079ActivityPingManager.emitDoorAt(level, center);''', '''        Vec3 center = center(surface, slot, normalSign);
        Scp079ActivityPingManager.emitDoorAt(level, center);''')
once('''        setState(level, surface, slot, attachment, first, true);
        CellKey key = new CellKey(surface.id(), slot);''', '''        setState(level, surface, slot, normalSign, attachment, first, true);
        CellKey key = new CellKey(surface.id(), slot, normalSign);''')
section('    private static void setState(ServerLevel level, ConstructionSurface surface,',
        '    public static synchronized void structuralSlotChanged(', '''    private static void setState(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            ConstructionSurface.SurfaceAttachment previous, BlockState state,
            boolean refreshCollision) {
        ConstructionSurface next;
        if (normalSign == 0) {
            next = surface.withAttachment(slot, state, previous.deform());
            TransformConstructionNetwork.broadcastSurfaceSlot(level, surface.id(),
                    slot, state, previous.deform());
        } else {
            next = surface.withOverlay(slot, normalSign, state,
                    previous.deform());
            TransformConstructionNetwork.broadcastSurfaceOverlay(level,
                    surface.id(), slot, normalSign, state, previous.deform());
        }
        TransformConstructionSavedData.get(level.getServer()).putSurfaceState(next);
        boolean passabilityChanged = FacilityModule.isFacilityDoor(state)
                && FacilityModule.isFacilityDoor(previous.state())
                && FacilityModule.isDoorPassable(state)
                != FacilityModule.isDoorPassable(previous.state());
        if (refreshCollision || passabilityChanged) {
            TransformConstructionManager.refreshSurfaceSlotRuntime(
                    level.getServer(), surface.id(), slot);
        }
    }

''')
section('    public static synchronized void structuralSlotChanged(',
        '    public static synchronized void structuralSurfaceChanged(', '''    public static synchronized void structuralSlotChanged(
            MinecraftServer server, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
        DoorIndex index = DOOR_INDEX.get(server);
        if (server == null || index == null || surfaceId == null || slot == null) {
            return;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<DoorRef> refs = new java.util.ArrayList<>(index.refs());
        refs.removeIf(ref -> ref.surfaceId().equals(surfaceId)
                && ref.slot().equals(slot));
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface != null) collectSlotDoorRefs(refs, surface, slot);
        DOOR_INDEX.put(server, new DoorIndex(data.revision(), List.copyOf(refs)));
    }

''')
section('    public static synchronized void structuralSurfaceChanged(',
        '    public static synchronized void acknowledgeStructuralRevision(', '''    public static synchronized void structuralSurfaceChanged(
            MinecraftServer server, UUID surfaceId) {
        DoorIndex index = DOOR_INDEX.get(server);
        if (server == null || index == null || surfaceId == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<DoorRef> refs = new java.util.ArrayList<>(index.refs());
        refs.removeIf(ref -> ref.surfaceId().equals(surfaceId));
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface != null) collectDoorRefs(refs, surface);
        DOOR_INDEX.put(server, new DoorIndex(data.revision(), List.copyOf(refs)));
    }

''')
section('    private static DoorHit nearestDoor(ServerLevel level, Vec3 world,',
        '    private static DoorAddress address(BlockState state) {', '''    private static DoorHit nearestDoor(ServerLevel level, Vec3 world,
            double maxDistanceSqr) {
        DoorHit best = null;
        double bestDistance = maxDistanceSqr;
        for (ConstructionSurface surface
                : TransformConstructionManager.surfaces(level)) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                DoorAddress address = address(entry.getValue().state());
                if (address == null) continue;
                Vec3 center = center(surface, entry.getKey(), 0);
                double distance = center.distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new DoorHit(surface, entry.getKey(), 0, address);
                }
            }
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                DoorAddress address = address(entry.getValue().state());
                if (address == null) continue;
                int sign = entry.getKey().normalSign() < 0 ? -1 : 1;
                Vec3 center = center(surface, entry.getKey().slot(), sign);
                double distance = center.distanceToSqr(world);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new DoorHit(surface, entry.getKey().slot(),
                            sign, address);
                }
            }
        }
        return best;
    }

    private static Vec3 center(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
        return TransformSurfaceGeometry.cellCenter(surface, slot,
                normalSign == 0 ? TransformSurfaceGeometry.MAIN_SIDE
                        : normalSign, normalSign != 0);
    }

''')
once('''    private record DoorRef(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {''', '''    private record DoorRef(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {''')
once('''    private record CellKey(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {''', '''    private record CellKey(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {''')
once('''    private record DoorHit(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, DoorAddress address) {''', '''    private record DoorHit(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            DoorAddress address) {''')
assert 'start(level, surface, slot, address.family()' not in s
assert 'new CellKey(surface.id(), slot)' not in s
assert 'private static Vec3 center(ConstructionSurface surface,' in s
path.write_text(s, encoding='utf-8')

network = root / 'network/TransformConstructionNetwork.java'
n = network.read_text(encoding='utf-8')
old = '''                    TransformKeycardReaderRuntime.useSurfaceOverlay(sender,
                            message.surfaceId, message.slot,
                            message.normalSign);'''
new = '''                    if (!TransformKeycardReaderRuntime.useSurfaceOverlay(sender,
                            message.surfaceId, message.slot,
                            message.normalSign)) {
                        TransformSurfaceDoorRuntime.useSurfaceOverlay(sender,
                                message.surfaceId, message.slot,
                                message.normalSign);
                    }'''
if n.count(old) != 1:
    raise RuntimeError('Surface overlay packet handler anchor mismatch')
network.write_text(n.replace(old, new, 1), encoding='utf-8')
print('Surface doors: main and both overlay layers have independent control, indexing and state sync')
print('Surface overlay network packet now falls back to the door runtime')
