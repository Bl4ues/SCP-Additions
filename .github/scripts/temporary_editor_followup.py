from pathlib import Path

root = Path('src/main/java/com/bl4ues/scpclassifieddirective')
render_path = root / 'facility/transform/client/TransformConstructionClientRenderer.java'
manager_path = root / 'facility/transform/TransformConstructionManager.java'
client_path = root / 'facility/transform/client/TransformConstructionClientState.java'
door_path = root / 'facility/transform/TransformDoorwayCollision.java'
map_path = root / 'client/scp079/Scp079FacilityMapScreen.java'

def replace_one(src, old, new, context):
    count = src.count(old)
    if count != 1:
        raise AssertionError(f'{context}: expected one match, found {count}')
    return src.replace(old, new, 1)

render = render_path.read_text()
# Keep edit handles in the same world/camera transform as the visible Surface.
start = render.index('    @SubscribeEvent\n    public static void renderEditorGizmos(')
end = render.index('    private static void renderGroup(', start)
old = render[start:end]
body = old[old.index('        VertexConsumer xray ='):old.index('        pose.popPose();')]
replacement = '''    private static void renderEditorGizmos(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera) {
        if (!minecraft.player.isCreative()) return;
        Selection selection = TransformConstructionClientState.selection();
        if (selection == null) return;
        boolean editor = minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getOffGridTool())
                || minecraft.player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getSurfaceTool());
        if (!editor) return;
''' + body + '''    }

'''
render = render[:start] + replacement + render[end:]
render = replace_one(render, '''        pose.popPose();
        buffers.endBatch();

        Set<UUID> current = surfaces.stream()''', '''        // Use precisely the same camera-relative pose as the Surface mesh.
        // The old AFTER_LEVEL callback supplied a different projection frame,
        // leaving axes apparently attached to the screen rather than the handle.
        renderEditorGizmos(minecraft, pose, buffers, camera);
        pose.popPose();
        buffers.endBatch();

        Set<UUID> current = surfaces.stream()''', 'gizmo world pose')
render = replace_one(render, '''        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column() + 1, slot.row()));
    }
''', '''        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column() + 1, slot.row()));
        // A changed border block can turn a previously unused matched seam on
        // in its neighbour. Rebuild only that neighbour, not every Surface.
        ConstructionSurface owner = TransformConstructionClientState.surface(id);
        if (owner != null && (slot.column() == 0
                || slot.column() == owner.columns() - 1
                || slot.row() == 0 || slot.row() == owner.rows() - 1)) {
            for (MatchedSurfaceEdge edge : SHARED_SURFACE_EDGES
                    .getOrDefault(id, Map.of()).values()) {
                SURFACE_MESHES.remove(edge.other().id());
            }
        }
    }
''', 'border invalidation')
render = replace_one(render, '''        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        // Both sides must be rebaked: the wall may predate the newly authored
        // ceiling, but its outer vertices and caps still need the same join.
        SURFACE_MESHES.clear();
        for (ConstructionSurface surface : surfaces) {''', '''        Set<UUID> affected = new java.util.HashSet<>();
        for (ConstructionSurface surface : surfaces) {
            if (!sameSurfaceGeometry(SURFACE_EDGE_GEOMETRIES.get(surface.id()),
                    surface)) affected.add(surface.id());
        }
        for (Map.Entry<UUID, Map<Integer, MatchedSurfaceEdge>> previous
                : SHARED_SURFACE_EDGES.entrySet()) {
            if (affected.contains(previous.getKey()) || surfaces.stream()
                    .noneMatch(surface -> surface.id().equals(previous.getKey()))) {
                for (MatchedSurfaceEdge edge : previous.getValue().values()) {
                    affected.add(edge.other().id());
                }
            }
        }
        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        for (ConstructionSurface surface : surfaces) {''', 'avoid global surface invalidation')
render = replace_one(render, '''            SHARED_SURFACE_EDGES.put(surface.id(), Map.copyOf(matches));
        }
    }
''', '''            SHARED_SURFACE_EDGES.put(surface.id(), Map.copyOf(matches));
            if (matches.values().stream().anyMatch(edge ->
                    affected.contains(edge.other().id()))) {
                affected.add(surface.id());
            }
        }
        SURFACE_MESHES.keySet().removeAll(affected);
    }
''', 'invalidate only changed and linked surfaces')
render_path.write_text(render)

# Keycard badges and their glyph use a single map-space transform already;
# shift only the font's ink downward, not the whole map-relative badge.
maptext = map_path.read_text()
maptext = replace_one(maptext, '''        graphics.drawString(font, value, -font.width(value) / 2, -4,
                color, false);''', '''        graphics.drawString(font, value, -font.width(value) / 2, -3,
                color, false);''', 'keycard glyph')
map_path.write_text(maptext)

# Enlarge search to include the adjacent cells whose 45-degree world AABBs
# overhang the doorway. Rebuild the *same* radius client and server on unlock.
door = door_path.read_text()
door = replace_one(door, 'private static final double CLEAR_HALF_WIDTH = 0.54D;', 'private static final double CLEAR_HALF_WIDTH = 0.59D;', 'diagonal pedestrian clearance')
door = replace_one(door, 'private static final int PASSAGE_STEPS = 7;', 'private static final int PASSAGE_STEPS = 9;', 'door corridor length')
door = replace_one(door, 'private static final double PASSAGE_STEP_LENGTH = 0.29D;', 'private static final double PASSAGE_STEP_LENGTH = 0.30D;', 'door corridor step')
door = replace_one(door, 'private static final double PASSAGE_HALF_DEPTH = 0.26D;', 'private static final double PASSAGE_HALF_DEPTH = 0.32D;', 'door corridor depth')
door = replace_one(door, 'for (int dx = -1; dx <= 1; dx++) {', 'for (int dx = -2; dx <= 2; dx++) {', 'door search x')
door = replace_one(door, 'for (int dz = -1; dz <= 1; dz++) {', 'for (int dz = -2; dz <= 2; dz++) {', 'door search z')
door_path.write_text(door)

client = client_path.read_text()
needle = '''            if (FacilityModule.isFacilityDoor(previous)
                    && FacilityModule.isFacilityDoor(state)
                    && FacilityModule.isDoorPassable(previous)
                            != FacilityModule.isDoorPassable(state)) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {'''
replacement = needle.replace('dx = -1; dx <= 1', 'dx = -2; dx <= 2').replace('dz = -1; dz <= 1', 'dz = -2; dz <= 2')
client = replace_one(client, needle, replacement, 'client neighbouring doorway collision refresh')
client_path.write_text(client)

manager = manager_path.read_text()
manager = replace_one(manager, '''    /** Door state changes can also change the clipped collision of adjacent
     * rotated wall cells; refresh only those owners, never the whole group. */
    public static void refreshOpenDoorNeighbours(MinecraftServer server,
            UUID groupId, GridPos doorCell) {
        if (server == null || groupId == null || doorCell == null) return;
        TransformGroup group = TransformConstructionSavedData.get(server)
                .group(groupId);
        if (group == null) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {''', '''    /** Door state changes must rebuild all rotated neighbours whose world
     * AABBs can spill into the 45-degree doorway. */
    public static void refreshOpenDoorNeighbours(MinecraftServer server,
            UUID groupId, GridPos doorCell) {
        if (server == null || groupId == null || doorCell == null) return;
        TransformGroup group = TransformConstructionSavedData.get(server)
                .group(groupId);
        if (group == null) return;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {''', 'server neighbouring doorway collision refresh')

# Play the placed/removed block's own vanilla sound at its physical world
# position, only after the authoritative operation succeeds.
marker = '''    private static GridPos nearestGridCell(Vec3 local) {'''
manager = replace_one(manager, marker, '''    private static void playConstructionSound(ServerLevel level,
            BlockState state, Vec3 worldPosition, boolean placing) {
        if (state == null || worldPosition == null || state.isAir()) return;
        net.minecraft.world.level.block.SoundType sound = state.getSoundType();
        level.playSound(null, BlockPos.containing(worldPosition),
                placing ? sound.getPlaceSound() : sound.getBreakSound(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) * 0.5F,
                sound.getPitch() * (placing ? 0.8F : 1.0F));
    }

''' + marker, 'construction sound helper')
# Place methods have distinct success paths.
manager = replace_one(manager, '''        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;
    }

    private static void playConstructionSound(''', '''        playConstructionSound(level, payload, group.cellCenter(target), true);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;
    }

    private static void playConstructionSound(''', 'group place sound')
manager = replace_one(manager, '''        if (payload.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            FacilityPipeModule.refreshSurface(level, surfaceId);
        }
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;''', '''        if (payload.getBlock() instanceof FacilityPipeModule.PipeBlock) {
            FacilityPipeModule.refreshSurface(level, surfaceId);
        }
        playConstructionSound(level, payload, center, true);
        TransformConstructionNetwork.acknowledgeRevision(level.getServer());
        return true;''', 'surface place sound')
manager = replace_one(manager, '''        TransformConstructionNetwork.broadcastSurfaceOverlay(level, surfaceId,
                targetSlot, side, payload, deform);
        if (payload.getBlock() instanceof FacilityPipeModule.PipeBlock) {''', '''        TransformConstructionNetwork.broadcastSurfaceOverlay(level, surfaceId,
                targetSlot, side, payload, deform);
        playConstructionSound(level, payload, center.add(normal.scale(side * 0.5D)), true);
        if (payload.getBlock() instanceof FacilityPipeModule.PipeBlock) {''', 'overlay place sound')
manager = replace_one(manager, '''        TransformConstructionNetwork.broadcastSurfaceOverlayRemoved(level, id,
                slot, side);
        if (surface.overlay(slot, side).state().getBlock()''', '''        TransformConstructionNetwork.broadcastSurfaceOverlayRemoved(level, id,
                slot, side);
        playConstructionSound(level, surface.overlay(slot, side).state(), center, false);
        if (surface.overlay(slot, side).state().getBlock()''', 'overlay break sound')
manager = replace_one(manager, '''            data.removeGroup(id);
            refreshGroup(level.getServer(), id);
            // Deleting the owner itself still needs a structural snapshot.''', '''            data.removeGroup(id);
            refreshGroup(level.getServer(), id);
            playConstructionSound(level, state, group.cellCenter(cell), false);
            // Deleting the owner itself still needs a structural snapshot.''', 'last group block break sound')
manager = replace_one(manager, '''        TransformConstructionNetwork.broadcastGroupCellRemoved(level, id, cell);
        if (state.getBlock() instanceof FacilityPipeModule.PipeBlock) {''', '''        TransformConstructionNetwork.broadcastGroupCellRemoved(level, id, cell);
        playConstructionSound(level, state, group.cellCenter(cell), false);
        if (state.getBlock() instanceof FacilityPipeModule.PipeBlock) {''', 'group block break sound')
manager = replace_one(manager, '''        TransformConstructionNetwork.broadcastSurfaceSlotRemoved(
                level, id, slot);
        if (attachment.state().getBlock()''', '''        TransformConstructionNetwork.broadcastSurfaceSlotRemoved(
                level, id, slot);
        playConstructionSound(level, attachment.state(), center, false);
        if (attachment.state().getBlock()''', 'surface block break sound')
manager_path.write_text(manager)
print('Patched gizmo pose + incremental surface invalidation, clearance ink, 45-degree collision, and construction sounds.')
