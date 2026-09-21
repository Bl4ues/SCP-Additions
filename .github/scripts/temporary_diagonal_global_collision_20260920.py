from pathlib import Path
root=Path('src/main/java/com/bl4ues/scpclassifieddirective/facility/transform')

def rewrite(path,old,new,tag):
    text=path.read_text(); count=text.count(old)
    if count!=1: raise AssertionError(f'{tag}: {count} matches')
    path.write_text(text.replace(old,new,1))

# Only precompute passages on authored structure/door changes, never in the
# collision hot path. Apply these passages AFTER combining all owners, so a
# neighboring Surface or independent group cannot refill an open door.
door=root/'TransformDoorwayCollision.java'
rewrite(door,'''import java.util.ArrayList;
import java.util.List;''','''import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;''','door passage index imports')
rewrite(door,'''    public static List<AABB> clip(AABB box, List<AABB> passages) {''','''    /** Immutable, world-cell keyed openings for every active transformed door.
     * Both physical indices use this cache to clip the FINAL combined collision
     * rather than only the group which owns the door. */
    public static Map<Long, List<AABB>> indexPassages(
            Collection<TransformGroup> groups) {
        if (groups == null || groups.isEmpty()) return Map.of();
        Map<Long, List<AABB>> indexed = new HashMap<>();
        for (TransformGroup group : groups) {
            for (Map.Entry<GridPos, BlockState> entry : group.cells().entrySet()) {
                BlockState state = entry.getValue();
                if (!FacilityModule.isFacilityDoor(state)
                        || !FacilityModule.isDoorPassable(state)
                        || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    continue;
                }
                for (AABB passage : nearbyPassages(group, entry.getKey())) {
                    int x0 = (int) Math.floor(passage.minX);
                    int x1 = (int) Math.floor(passage.maxX - EPSILON);
                    int y0 = (int) Math.floor(passage.minY);
                    int y1 = (int) Math.floor(passage.maxY - EPSILON);
                    int z0 = (int) Math.floor(passage.minZ);
                    int z1 = (int) Math.floor(passage.maxZ - EPSILON);
                    for (int x = x0; x <= x1; x++) {
                        for (int y = y0; y <= y1; y++) {
                            for (int z = z0; z <= z1; z++) {
                                indexed.computeIfAbsent(
                                        BlockPos.asLong(x, y, z),
                                        ignored -> new ArrayList<>()).add(passage);
                            }
                        }
                    }
                }
            }
        }
        Map<Long, List<AABB>> result = new HashMap<>();
        indexed.forEach((key,value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    /** World-space clip of a logical world-cell VoxelShape. Call only when the
     * passage cache contains this cell and memoize its result at index level. */
    public static VoxelShape clipShape(VoxelShape original, BlockPos cell,
            Map<Long, List<AABB>> indexed) {
        if (original == null || original.isEmpty() || cell == null
                || indexed == null || indexed.isEmpty()) return original;
        List<AABB> passages = indexed.get(cell.asLong());
        if (passages == null || passages.isEmpty()) return original;
        VoxelShape result = Shapes.empty();
        for (AABB local : original.toAabbs()) {
            for (AABB remaining : clip(local.move(cell), passages)) {
                result = Shapes.or(result, Shapes.create(
                        remaining.move(-cell.getX(), -cell.getY(),
                                -cell.getZ())));
            }
        }
        return result.optimize();
    }

    public static List<AABB> clip(AABB box, List<AABB> passages) {''','global doorway passages and clip')

server=root/'TransformConstructionManager.java'
rewrite(server,'''        return cell == null ? Shapes.empty() : cell.collision();
    }

    /**
     * Group-only collision,''','''        return cell == null ? Shapes.empty() : cell.collision();
    }

    /**
     * Group-only collision,''','leave proxy lookup for index-level clip')
# Fold final clipping into SpatialIndex.cell(), where merged collision is memoized.
rewrite(server,'''            ProxyCell result = aggregate.freeze();
            cache.put(packed, result);
            return result;''','''            ProxyCell raw = aggregate.freeze();
            ProxyCell result = doorPassages.containsKey(packed)
                    ? new ProxyCell(raw.selection(),
                            TransformDoorwayCollision.clipShape(
                                    raw.collision(), pos, doorPassages),
                            TransformDoorwayCollision.clipShape(
                                    raw.groupCollision(), pos, doorPassages),
                            TransformDoorwayCollision.clipShape(
                                    raw.surfaceCollision(), pos, doorPassages),
                            raw.light(), raw.groupIds(), raw.surfaceIds())
                    : raw;
            cache.put(packed, result);
            return result;''','mask merged server collision')
rewrite(server,'''        private final Map<ResourceLocation, Map<Long, ProxyCell>> frozen =
                new LinkedHashMap<>();

        private void add(OwnerKey owner''','''        private final Map<ResourceLocation, Map<Long, ProxyCell>> frozen =
                new LinkedHashMap<>();
        private Map<Long, List<AABB>> doorPassages = Map.of();

        private void refreshDoorPassages(
                java.util.Collection<TransformGroup> groups) {
            doorPassages = TransformDoorwayCollision.indexPassages(groups);
            // Other owners' cached collision must be reclipped too.
            frozen.clear();
        }

        private void add(OwnerKey owner''','server doorway index cache')
rewrite(server,'''        if (replacementSurface != null) addSurface(index, replacementSurface);
        return index;''','''        if (replacementSurface != null) addSurface(index, replacementSurface);
        index.refreshDoorPassages(data.groups());
        return index;''','initial global passage index')
rewrite(server,'''        materializeAffected(server, index, affected);
        if (surfaceOwner) {''','''        if (!surfaceOwner) index.refreshDoorPassages(data.groups());
        materializeAffected(server, index, affected);
        if (surfaceOwner) {''','group structure passage changes')
rewrite(server,'''        if (group == null) return;
        for (int dx = -2; dx <= 2; dx++) {''','''        if (group == null) return;
        SpatialIndex index = INDEXES.get(server);
        if (index != null) index.refreshDoorPassages(
                TransformConstructionSavedData.get(server).groups());
        for (int dx = -2; dx <= 2; dx++) {''','door passability invalidates all merged passage cells')

client=root/'client/TransformConstructionClientState.java'
rewrite(client,'''    private static Map<Long, TransformConstructionManager.ProxyCell> proxyCells =
            new LinkedHashMap<>();''','''    private static Map<Long, TransformConstructionManager.ProxyCell> proxyCells =
            new LinkedHashMap<>();
    private static Map<Long, List<AABB>> doorPassages = Map.of();
    // The world-cell raw proxy changes by identity when a part is edited.
    // Cache the final global door cut per raw cell, not per collision query.
    private static final Map<Long, MaskedProxyCell> maskedProxyCells =
            new LinkedHashMap<>();
    private record MaskedProxyCell(
            TransformConstructionManager.ProxyCell raw,
            TransformConstructionManager.ProxyCell masked) { }

    private static void refreshDoorPassages() {
        doorPassages = TransformDoorwayCollision.indexPassages(groups);
        maskedProxyCells.clear();
    }''','client doorway index')
rewrite(client,'''        TransformAlarmClientRenderer.resetIndices();
        rebuildProxyCells();
        TransformAlarmAudioClient.sync(groups, surfaces);''','''        TransformAlarmClientRenderer.resetIndices();
        refreshDoorPassages();
        rebuildProxyCells();
        TransformAlarmAudioClient.sync(groups, surfaces);''','client full sync')
rewrite(client,'''        proxyCells = new LinkedHashMap<>();
        GROUP_PROXY_CONTRIBUTIONS.clear();''','''        proxyCells = new LinkedHashMap<>();
        doorPassages = Map.of();
        maskedProxyCells.clear();
        GROUP_PROXY_CONTRIBUTIONS.clear();''','client clear')
rewrite(client,'''    private static TransformConstructionManager.ProxyCell proxyCell(BlockPos pos) {
        return pos == null ? null : proxyCells.get(pos.asLong());
    }''','''    private static TransformConstructionManager.ProxyCell proxyCell(BlockPos pos) {
        if (pos == null) return null;
        long key = pos.asLong();
        TransformConstructionManager.ProxyCell raw = proxyCells.get(key);
        if (raw == null || !doorPassages.containsKey(key)) return raw;
        MaskedProxyCell cached = maskedProxyCells.get(key);
        if (cached != null && cached.raw() == raw) return cached.masked();
        TransformConstructionManager.ProxyCell masked =
                new TransformConstructionManager.ProxyCell(raw.selection(),
                        TransformDoorwayCollision.clipShape(raw.collision(),
                                pos, doorPassages),
                        TransformDoorwayCollision.clipShape(raw.groupCollision(),
                                pos, doorPassages),
                        TransformDoorwayCollision.clipShape(raw.surfaceCollision(),
                                pos, doorPassages),
                        raw.light(), raw.groupIds(), raw.surfaceIds());
        maskedProxyCells.put(key, new MaskedProxyCell(raw, masked));
        return masked;
    }''','client merged collision masking')
rewrite(client,'''            if (FacilityModule.isFacilityDoor(previous)
                    && FacilityModule.isFacilityDoor(state)
                    && FacilityModule.isDoorPassable(previous)
                            != FacilityModule.isDoorPassable(state)) {
                for (int dx''','''            if (FacilityModule.isFacilityDoor(previous)
                    && FacilityModule.isFacilityDoor(state)
                    && FacilityModule.isDoorPassable(previous)
                            != FacilityModule.isDoorPassable(state)) {
                refreshDoorPassages();
                for (int dx''','refresh door opening client cache')
rewrite(client,'''            groups = List.copyOf(next);
            TransformAlarmClientRenderer.groupCellChanged(groupId, cell, null);''','''            groups = List.copyOf(next);
            if (FacilityModule.isDoorPassable(current.cells().get(cell))) {
                refreshDoorPassages();
            }
            TransformAlarmClientRenderer.groupCellChanged(groupId, cell, null);''','client remove door')
rewrite(client,'''                groups = List.copyOf(next);
                rebuildGroupProxyCells(replacement.id());
                return;''','''                groups = List.copyOf(next);
                refreshDoorPassages();
                rebuildGroupProxyCells(replacement.id());
                return;''','client upsert existing group')
rewrite(client,'''        next.add(replacement);
        groups = List.copyOf(next);
        rebuildGroupProxyCells(replacement.id());''','''        next.add(replacement);
        groups = List.copyOf(next);
        refreshDoorPassages();
        rebuildGroupProxyCells(replacement.id());''','client upsert new group')
rewrite(client,'''                groups = List.copyOf(next);
                removeGroupProxyCells(id);''','''                groups = List.copyOf(next);
                refreshDoorPassages();
                removeGroupProxyCells(id);''','client remove group')
print('Global 45-degree passage masks now apply after merging all transformed owners on both client and server.')
