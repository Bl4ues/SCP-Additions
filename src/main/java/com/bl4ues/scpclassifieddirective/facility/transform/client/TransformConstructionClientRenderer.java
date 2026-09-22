package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityPipeModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceBridge;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Axis;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.EditMode;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.Selection;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SelectionType;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState.SurfaceHandle;
import com.bl4ues.scpclassifieddirective.init.FacilityMappingItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * World-space rendering for off-grid cells and curved surface payloads. Static
 * surface models are deformed vertex-by-vertex; rigid attachments keep their
 * shape and inherit the local surface frame. Surface geometry is cached until
 * the authored surface snapshot changes, so a large curved facility does not
 * repeatedly rebuild baked-model quads and spline transforms every frame.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformConstructionClientRenderer {
    private static final double MAX_RENDER_DISTANCE_SQR = 192.0D * 192.0D;
    private static final int GROUP_BATCH_SIZE = 8;
    private static final int SURFACE_BATCH_SIZE = 8;
    private static final double GROUP_BATCH_RADIUS =
            Math.sqrt(3.0D) * GROUP_BATCH_SIZE * 0.5D;
    private static final Direction[] SIDES = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, null
    };
    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();
    private static final Map<BlockState, Boolean> FULL_SURFACE_CELLS =
            new java.util.IdentityHashMap<>();
    // Neighbour relationships depend on geometry, not individual block states.
    // Resolve each shared edge once per committed geometry change rather than
    // searching every other Surface for every vertex of a long curved corridor.
    private static final Map<UUID, ConstructionSurface> SURFACE_EDGE_GEOMETRIES =
            new HashMap<>();
    private static final Map<UUID, Map<Integer, MatchedSurfaceEdge>>
            SHARED_SURFACE_EDGES = new HashMap<>();
    // An unchanged curved border is expensive to resample. Keep its arc and
    // coarse bounds across unrelated gizmo edits, instead of traversing every
    // room's 4x49 geometry again on each committed handle movement.
    private static final Map<UUID, List<List<Vec3>>> SURFACE_EDGE_SAMPLES =
            new HashMap<>();
    private static final Map<UUID, List<AABB>> SURFACE_EDGE_BOUNDS =
            new HashMap<>();
    // Capture the actual world-space camera transform during the solid pass;
    // AFTER_LEVEL uses a different PoseStack under some shader pipelines.
    private static org.joml.Matrix4f editorWorldPose;
    private static org.joml.Matrix3f editorWorldNormal;
    private static final Map<UUID, CachedGroup> GROUP_MESHES =
            new HashMap<>();
    private static final Map<UUID, Set<TransformGroup.GridPos>>
            DIRTY_GROUP_CELLS = new HashMap<>();
    private static final Map<UUID, Set<ConstructionSurface.SurfaceSlot>>
            DIRTY_SURFACE_SLOTS = new HashMap<>();

    private TransformConstructionClientRenderer() {
    }

    public static void clearSurfaceCache() {
        SURFACE_MESHES.clear();
        FULL_SURFACE_CELLS.clear();
        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        SURFACE_EDGE_SAMPLES.clear();
        SURFACE_EDGE_BOUNDS.clear();
        GROUP_MESHES.clear();
        DIRTY_GROUP_CELLS.clear();
        DIRTY_SURFACE_SLOTS.clear();
        editorWorldPose = null;
        editorWorldNormal = null;
    }

    static void markGroupCellDirty(UUID id, TransformGroup.GridPos cell) {
        if (id == null || cell == null) return;
        DIRTY_GROUP_CELLS.computeIfAbsent(id,
                ignored -> new java.util.LinkedHashSet<>()).add(cell);
    }

    static void markSurfaceSlotDirty(UUID id,
            ConstructionSurface.SurfaceSlot slot) {
        if (id == null || slot == null) return;
        Set<ConstructionSurface.SurfaceSlot> dirty =
                DIRTY_SURFACE_SLOTS.computeIfAbsent(id,
                        ignored -> new java.util.LinkedHashSet<>());
        dirty.add(slot);
        // A pipe's exposed end face depends on the next segment even if the
        // bracket state on the existing segment stays unchanged. Only rebake
        // its two immediate neighbours, never the full curved wall.
        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column() - 1, slot.row()));
        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column() + 1, slot.row()));
        // Breaking a full block also exposes the DOWN/UP faces of the two
        // vertical neighbours. Rebuild all four immediate neighbours so
        // formerly culled faces return without invalidating the whole wall.
        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column(), slot.row() - 1));
        dirty.add(new ConstructionSurface.SurfaceSlot(
                slot.column(), slot.row() + 1));
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

    static void invalidateGroup(UUID id) {
        if (id != null) GROUP_MESHES.remove(id);
    }

    @SubscribeEvent
    public static void hideProxyVanillaOutline(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || event.getTarget() == null) return;
        BlockPos pos = event.getTarget().getBlockPos();
        boolean proxy = minecraft.level.getBlockState(pos).is(
                TransformConstructionModule.getProxy());
        boolean logicalGroup = minecraft.player != null
                && (TransformGroupPlacementClient.findTarget(minecraft.player)
                        != null
                || TransformGroupPlacementClient.findPayloadTarget(
                        minecraft.player) != null);
        boolean logicalSurface = false;
        if (minecraft.player != null) {
            TransformSurfaceRaycast.Target target =
                    aimedSurfaceTarget(minecraft.player);
            if (target != null) {
                boolean placing = minecraft.player.getMainHandItem().getItem()
                        instanceof BlockItem;
                boolean editing = minecraft.player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool())
                        || minecraft.player.getOffhandItem().is(
                                TransformConstructionModule.getSurfaceTool());
                logicalSurface = placing || editing
                        || target.surface().attachments().containsKey(
                                target.slot());
            }
        }
        if (proxy || logicalGroup || logicalSurface) {
            // Proxy/world AABBs are only broad-phase bridges. The authored
            // local cell is the selection authority and is rendered below in
            // the transformed frame.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        var dimension = minecraft.level.dimension().location();
        var groups = TransformConstructionClientState.groups(dimension);
        var surfaces = TransformConstructionClientState.surfaces(dimension);
        if (groups.isEmpty() && surfaces.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (TransformGroup group : groups) {
            renderGroup(minecraft, pose, buffers, group, camera);
        }
        updateSharedSurfaceEdges(surfaces);
        for (ConstructionSurface surface : surfaces) {
            renderSurfacePayloads(minecraft, pose, buffers, surface, camera,
                    event.getFrustum());
        }

        boolean creativeAuthoring = minecraft.player.isCreative();
        boolean offGridTool = creativeAuthoring
                && (minecraft.player.getMainHandItem().is(
                        TransformConstructionModule.getOffGridTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getOffGridTool()));
        boolean surfaceTool = creativeAuthoring
                && (minecraft.player.getMainHandItem().is(
                        TransformConstructionModule.getSurfaceTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getSurfaceTool()));
        boolean mappingTool = minecraft.player.getMainHandItem().is(
                FacilityMappingItems.getTool())
                || minecraft.player.getOffhandItem().is(
                        FacilityMappingItems.getTool());
        Selection selection = TransformConstructionClientState.selection();
        boolean placingBlock = creativeAuthoring
                && minecraft.player.getMainHandItem().getItem()
                instanceof BlockItem;
        boolean showSelectedGroup = placingBlock && selection != null
                && selection.type() == SelectionType.GROUP;
        boolean showSelectedSurface = placingBlock && selection != null
                && selection.type() == SelectionType.SURFACE;
        TransformGroupPlacementClient.Target placementTarget =
                placingBlock ? TransformGroupPlacementClient.findTarget(
                        minecraft.player) : null;
        TransformGroupPlacementClient.PayloadTarget interactionTarget =
                offGridTool && !placingBlock
                        ? TransformGroupPlacementClient.findPayloadTarget(
                                minecraft.player) : null;
        TransformSurfaceRaycast.Target surfaceTarget =
                placingBlock ? aimedSurfaceTarget(minecraft.player) : null;
        if (offGridTool || surfaceTool || mappingTool || showSelectedGroup
                || showSelectedSurface || placementTarget != null
                || interactionTarget != null || surfaceTarget != null
                || placingBlock && !surfaces.isEmpty()) {
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            if (offGridTool) {
                for (TransformGroup group : groups) {
                    if (selection != null
                            && selection.type() == SelectionType.GROUP
                            && group.id().equals(selection.id())) {
                        renderGroupGrid(pose, lines, group, camera);
                    } else {
                        renderGroupOutline(pose, lines, group, camera);
                    }
                }
            } else if (showSelectedGroup
                    && selection != null
                    && selection.type() == SelectionType.GROUP) {
                TransformGroup selectedGroup =
                        TransformConstructionClientState.group(selection.id());
                if (selectedGroup != null) {
                    renderGroupGrid(pose, lines, selectedGroup, camera);
                }
            }
            if (placementTarget != null
                    && !(showSelectedGroup && selection != null
                    && placementTarget.group().id().equals(selection.id()))) {
                // Building against transformed construction should expose the
                // same local cell grid the raycast uses. The vanilla proxy AABB
                // is only broad-phase plumbing and must never be the builder's
                // visual reference.
                renderGroupGrid(pose, lines, placementTarget.group(), camera);
                renderLogicalGroupCell(pose, lines, placementTarget.group(),
                        placementTarget.source(),
                        0.72F, 0.88F, 0.96F, 0.94F);
                renderLogicalGroupCell(pose, lines, placementTarget.group(),
                        placementTarget.adjacentCell(),
                        0.24F, 1.0F, 0.38F, 0.98F);
            }
            if (interactionTarget != null
                    && !(selection != null
                    && selection.type() == SelectionType.GROUP
                    && interactionTarget.group().id().equals(selection.id())
                    && offGridTool)) {
                renderLogicalGroupCell(pose, lines, interactionTarget.group(),
                        interactionTarget.visualCell(),
                        0.78F, 0.93F, 1.0F, 0.96F);
            }
            if (surfaceTarget != null) {
                if (!(showSelectedSurface && selection != null
                        && surfaceTarget.surface().id().equals(selection.id()))) {
                    renderSurfaceGrid(pose, lines,
                            surfaceTarget.surface(), camera);
                }
                renderLogicalSurfaceSlot(pose, lines,
                        surfaceTarget.surface(), surfaceTarget.visualSlot(),
                        0.24F, 1.0F, 0.38F, 0.98F);
            }
            if (surfaceTool) {
                for (ConstructionSurface surface : surfaces) {
                    if (selection != null
                            && selection.type() == SelectionType.SURFACE
                            && surface.id().equals(selection.id())) {
                        renderSurfaceGrid(pose, lines, surface, camera);
                    } else {
                        Vec3 outlineCenter = surface.gridPoint(0.5D, 0.5D);
                        if (outlineCenter.distanceToSqr(camera) <= 64.0D * 64.0D) {
                            renderSurfaceOutline(pose, lines, surface, camera);
                        }
                    }
                }
                renderLinkedSurfaceAuthoring(pose, lines, camera);
            } else if (showSelectedSurface
                    && selection != null
                    && selection.type() == SelectionType.SURFACE) {
                ConstructionSurface selectedSurface =
                        TransformConstructionClientState.surface(selection.id());
                if (selectedSurface != null) {
                    renderSurfaceGrid(pose, lines, selectedSurface, camera);
                }
            } else if (mappingTool) {
                for (ConstructionSurface surface : surfaces) {
                    renderSurfaceMappingGuide(pose, lines, surface, camera);
                }
            }
            buffers.endBatch(RenderType.lines());
        }
        BlockPos blocked = TransformConstructionClientState.blockedPlacement();
        if (blocked != null && minecraft.level.hasChunkAt(blocked)) {
            VertexConsumer warningLines = buffers.getBuffer(RenderType.lines());
            float pulse = (float) (0.45D + 0.55D * Math.abs(
                    Math.sin(System.currentTimeMillis() / 90.0D)));
            LevelRenderer.renderLineBox(pose, warningLines,
                    new AABB(blocked).inflate(0.012D),
                    1.0F, 0.08F, 0.04F, pulse);
            buffers.endBatch(RenderType.lines());
        }
        // Save the known-good world transform for the final editor pass.
        // Render only after *all* world layers: translucent/deferred shaders
        // otherwise paint over the gizmos drawn at AFTER_SOLID_BLOCKS.
        if (minecraft.player.isCreative()
                && TransformConstructionClientState.selection() != null) {
            editorWorldPose = new org.joml.Matrix4f(pose.last().pose());
            editorWorldNormal = new org.joml.Matrix3f(pose.last().normal());
        } else {
            editorWorldPose = null;
            editorWorldNormal = null;
        }
        pose.popPose();
        buffers.endBatch();

        Set<UUID> current = surfaces.stream().map(ConstructionSurface::id)
                .collect(Collectors.toSet());
        SURFACE_MESHES.keySet().removeIf(id -> !current.contains(id));
        Set<UUID> currentGroups = groups.stream().map(TransformGroup::id)
                .collect(Collectors.toSet());
        GROUP_MESHES.keySet().removeIf(id -> !currentGroups.contains(id));
    }

    @SubscribeEvent
    public static void renderEditorGizmosAfterWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL
                || editorWorldPose == null || editorWorldNormal == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        PoseStack pose = new PoseStack();
        pose.last().pose().set(editorWorldPose);
        pose.last().normal().set(editorWorldNormal);
        editorWorldPose = null;
        editorWorldNormal = null;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        // Some shader pipelines restore their own depth state around the final
        // world composite. Explicitly disable depth for this tiny editor pass
        // so an axis remains visible even when its origin is inside a wall.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        renderEditorGizmos(minecraft, pose, buffers,
                event.getCamera().getPosition());
        buffers.endBatch(TransformEditorRenderTypes.GIZMO_LINES);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void renderEditorGizmos(Minecraft minecraft, PoseStack pose,
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
        VertexConsumer xray = buffers.getBuffer(TransformEditorRenderTypes.GIZMO_LINES);
        if (selection.type() == SelectionType.GROUP) {
            TransformGroup group = TransformConstructionClientState.group(
                    selection.id());
            if (group != null && group.origin().distanceToSqr(camera)
                    < MAX_RENDER_DISTANCE_SQR) renderGroupGizmo(pose, xray, group);
        } else {
            ConstructionSurface surface = TransformConstructionClientState.surface(
                    selection.id());
            if (surface != null) {
                double[] uv = switch (selection.handle()) {
                    case BOTTOM_START -> new double[]{0, 0};
                    case BOTTOM_END -> new double[]{1, 0};
                    case TOP_START -> new double[]{0, 1};
                    case TOP_END -> new double[]{1, 1};
                    case BOTTOM_EDGE -> new double[]{0.5, 0};
                    case TOP_EDGE -> new double[]{0.5, 1};
                    case START_EDGE -> new double[]{0, 0.5};
                    case END_EDGE -> new double[]{1, 0.5};
                    case CENTER -> new double[]{0.5, 0.5};
                };
                if (surface.gridPoint(uv[0], uv[1]).distanceToSqr(camera)
                        < MAX_RENDER_DISTANCE_SQR) {
                    renderHandles(pose, xray, surface, selection.handle());
                    renderSurfaceGizmo(pose, xray, surface, selection.handle());
                }
            }
        }
        buffers.endBatch(TransformEditorRenderTypes.GIZMO_LINES);
    }

    private static void renderGroup(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, TransformGroup group,
            Vec3 camera) {
        CachedGroup cached = GROUP_MESHES.get(group.id());
        if (cached == null) {
            cached = buildGroupMesh(minecraft, group);
            GROUP_MESHES.put(group.id(), cached);
        } else if (cached.source() != group) {
            cached = updateGroupMesh(minecraft, group, cached);
            GROUP_MESHES.put(group.id(), cached);
        }
        pose.pushPose();
        pose.translate(group.origin().x, group.origin().y, group.origin().z);
        pose.mulPose(TransformMath.quaternion(group.rotationX(),
                group.rotationY(), group.rotationZ()));
        for (CachedGroupBatch batch : cached.batches()) {
            Vec3 worldBatchCenter = TransformMath.localToWorld(group.origin(),
                    batch.localCenter(), group.rotationX(), group.rotationY(),
                    group.rotationZ());
            double maxDistance = 192.0D + GROUP_BATCH_RADIUS;
            if (worldBatchCenter.distanceToSqr(camera)
                    > maxDistance * maxDistance) continue;
            for (Map<RenderType, List<PreparedVertex>> cell
                    : batch.cells().values()) {
                for (Map.Entry<RenderType, List<PreparedVertex>> layer
                        : cell.entrySet()) {
                    VertexConsumer consumer = buffers.getBuffer(layer.getKey());
                    for (PreparedVertex vertex : layer.getValue()) {
                        consumer.vertex(pose.last().pose(),
                                        (float) vertex.position().x,
                                        (float) vertex.position().y,
                                        (float) vertex.position().z)
                                .color(vertex.red(), vertex.green(),
                                        vertex.blue(), 255)
                                .uv(vertex.u(), vertex.v())
                                .overlayCoords(OverlayTexture.NO_OVERLAY)
                                .uv2(vertex.fallbackLight())
                                .normal(pose.last().normal(),
                                        (float) vertex.normal().x,
                                        (float) vertex.normal().y,
                                        (float) vertex.normal().z)
                                .endVertex();
                    }
                }
            }
        }
        pose.popPose();
    }

    private static CachedGroup updateGroupMesh(Minecraft minecraft,
            TransformGroup group, CachedGroup cached) {
        Set<TransformGroup.GridPos> hinted =
                DIRTY_GROUP_CELLS.remove(group.id());
        if (hinted != null && !hinted.isEmpty()) {
            Map<GroupBatchKey, CachedGroupBatch> batches =
                    new LinkedHashMap<>();
            for (CachedGroupBatch batch : cached.batches()) {
                batches.put(batch.key(), batch);
            }
            for (TransformGroup.GridPos cell : hinted) {
                GroupBatchKey key = GroupBatchKey.of(cell);
                CachedGroupBatch batch = batches.get(key);
                Map<TransformGroup.GridPos,
                        Map<RenderType, List<PreparedVertex>>> cells =
                        batch == null ? new LinkedHashMap<>()
                                : new LinkedHashMap<>(batch.cells());
                Map<RenderType, List<PreparedVertex>> rebuilt =
                        buildGroupCell(minecraft, group, cell);
                if (rebuilt.isEmpty()) cells.remove(cell);
                else cells.put(cell, rebuilt);
                if (cells.isEmpty()) {
                    batches.remove(key);
                } else {
                    batches.put(key, new CachedGroupBatch(key, key.center(),
                            Map.copyOf(cells)));
                }
            }
            return new CachedGroup(group, Map.copyOf(group.cells()),
                    List.copyOf(batches.values()));
        }

        // Structural snapshots or geometry edits are uncommon. They may alter
        // many cells at once, so rebuild only the batches containing actual
        // state differences rather than trying to infer one runtime cell hint.
        Set<GroupBatchKey> dirty = new java.util.LinkedHashSet<>();
        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : cached.cells().entrySet()) {
            BlockState next = group.cells().get(entry.getKey());
            if (!java.util.Objects.equals(entry.getValue(), next)) {
                dirty.add(GroupBatchKey.of(entry.getKey()));
            }
        }
        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : group.cells().entrySet()) {
            BlockState previous = cached.cells().get(entry.getKey());
            if (!java.util.Objects.equals(previous, entry.getValue())) {
                dirty.add(GroupBatchKey.of(entry.getKey()));
            }
        }
        if (dirty.isEmpty()) {
            return new CachedGroup(group, Map.copyOf(group.cells()),
                    cached.batches());
        }

        Map<GroupBatchKey, CachedGroupBatch> batches = new LinkedHashMap<>();
        for (CachedGroupBatch batch : cached.batches()) {
            batches.put(batch.key(), batch);
        }
        for (GroupBatchKey key : dirty) {
            CachedGroupBatch rebuilt = buildGroupBatch(minecraft, group, key);
            if (rebuilt == null) batches.remove(key);
            else batches.put(key, rebuilt);
        }
        return new CachedGroup(group, Map.copyOf(group.cells()),
                List.copyOf(batches.values()));
    }

    private static CachedGroup buildGroupMesh(Minecraft minecraft,
            TransformGroup group) {
        Set<GroupBatchKey> keys = new java.util.LinkedHashSet<>();
        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : group.cells().entrySet()) {
            BlockState state = entry.getValue();
            if (state == null || state.isAir()
                    || state.getRenderShape() != RenderShape.MODEL) continue;
            keys.add(GroupBatchKey.of(entry.getKey()));
        }
        List<CachedGroupBatch> batches = new ArrayList<>(keys.size());
        for (GroupBatchKey key : keys) {
            CachedGroupBatch batch = buildGroupBatch(minecraft, group, key);
            if (batch != null) batches.add(batch);
        }
        return new CachedGroup(group, Map.copyOf(group.cells()),
                List.copyOf(batches));
    }

    private static CachedGroupBatch buildGroupBatch(Minecraft minecraft,
            TransformGroup group, GroupBatchKey key) {
        Map<TransformGroup.GridPos, Map<RenderType, List<PreparedVertex>>> cells =
                new LinkedHashMap<>();
        int baseX = key.x() * GROUP_BATCH_SIZE;
        int baseY = key.y() * GROUP_BATCH_SIZE;
        int baseZ = key.z() * GROUP_BATCH_SIZE;
        for (int x = baseX; x < baseX + GROUP_BATCH_SIZE; x++) {
            for (int y = baseY; y < baseY + GROUP_BATCH_SIZE; y++) {
                for (int z = baseZ; z < baseZ + GROUP_BATCH_SIZE; z++) {
                    TransformGroup.GridPos cell =
                            new TransformGroup.GridPos(x, y, z);
                    Map<RenderType, List<PreparedVertex>> rendered =
                            buildGroupCell(minecraft, group, cell);
                    if (!rendered.isEmpty()) cells.put(cell, rendered);
                }
            }
        }
        return cells.isEmpty() ? null
                : new CachedGroupBatch(key, key.center(), Map.copyOf(cells));
    }

    private static Map<RenderType, List<PreparedVertex>> buildGroupCell(
            Minecraft minecraft, TransformGroup group,
            TransformGroup.GridPos cell) {
        BlockState state = group.cells().get(cell);
        if (state == null || state.isAir()
                || state.getRenderShape() != RenderShape.MODEL) {
            return Map.of();
        }

        Map<RenderType, List<PreparedVertex>> layers = new LinkedHashMap<>();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
        List<PreparedVertex> output = layers.computeIfAbsent(renderType,
                ignored -> new ArrayList<>());
        Vec3 center = group.cellCenter(cell);
        BlockPos lightPos = BlockPos.containing(center);
        int packedLight = minecraft.level.hasChunkAt(lightPos)
                ? LevelRenderer.getLightColor(minecraft.level, state, lightPos)
                : 0x00F000F0;
        long seed = 42L ^ cell.hashCode() * 31L;
        RandomSource random = RandomSource.create(seed);
        for (Direction side : SIDES) {
            random.setSeed(seed);
            for (BakedQuad quad : model.getQuads(state, side,
                    random, ModelData.EMPTY, null)) {
                appendGroupQuad(minecraft, output, group, cell,
                        state, quad, lightPos, packedLight);
            }
        }
        Map<RenderType, List<PreparedVertex>> immutable = new LinkedHashMap<>();
        layers.forEach((type, vertices) ->
                immutable.put(type, List.copyOf(vertices)));
        return Map.copyOf(immutable);
    }

    private static void appendGroupQuad(Minecraft minecraft,
            List<PreparedVertex> output, TransformGroup group,
            TransformGroup.GridPos cell, BlockState state, BakedQuad quad,
            BlockPos lightPos, int packedLight) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        int tint = quad.isTinted() ? minecraft.getBlockColors().getColor(
                state, minecraft.level, lightPos, quad.getTintIndex())
                : 0xFFFFFF;
        int red = tint < 0 ? 255 : tint >> 16 & 0xFF;
        int green = tint < 0 ? 255 : tint >> 8 & 0xFF;
        int blue = tint < 0 ? 255 : tint & 0xFF;
        Vec3 localNormal = new Vec3(quad.getDirection().getStepX(),
                quad.getDirection().getStepY(),
                quad.getDirection().getStepZ()).normalize();

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            double x = Float.intBitsToFloat(vertices[offset]);
            double y = Float.intBitsToFloat(vertices[offset + 1]);
            double z = Float.intBitsToFloat(vertices[offset + 2]);
            float u = stride > 4
                    ? Float.intBitsToFloat(vertices[offset + 4]) : 0.0F;
            float v = stride > 5
                    ? Float.intBitsToFloat(vertices[offset + 5]) : 0.0F;
            Vec3 local = new Vec3(cell.x() - 0.5D + x,
                    cell.y() - 0.5D + y, cell.z() - 0.5D + z);
            output.add(new PreparedVertex(state, local, localNormal, u, v,
                    red, green, blue, packedLight));
        }
    }

    private static void renderSurfacePayloads(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, ConstructionSurface surface,
            Vec3 camera, net.minecraft.client.renderer.culling.Frustum frustum) {
        Vec3 center = surface.gridPoint(0.5D, 0.5D);
        double radius = Math.max(surface.width(), surface.height())
                + surface.curveOffset().length()
                + surface.heightCurveOffset().length() + 3.0D;
        if (center.distanceToSqr(camera)
                > (192.0D + radius) * (192.0D + radius)) return;
        if (frustum != null && !frustum.isVisible(new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius))) return;

        CachedSurface cached = SURFACE_MESHES.get(surface.id());
        if (cached == null) {
            cached = buildSurfaceMesh(minecraft, surface);
            SURFACE_MESHES.put(surface.id(), cached);
        } else if (cached.surface() != surface
                && !TransformConstructionClientControls.previewingSurface(
                        surface.id())) {
            if (!sameSurfaceGeometry(cached.surface(), surface)) {
                cached = buildSurfaceMesh(minecraft, surface);
            } else {
                cached = updateSurfaceMesh(minecraft, surface, cached);
            }
            SURFACE_MESHES.put(surface.id(), cached);
        }
        // One huge Surface can span several rooms: a surface-level frustum
        // check previously submitted its entire mesh when only one corner was
        // visible. Keep immutable 8x8 logical-region batches and cull each
        // by its actual world-space geometry bounds before visiting vertices.
        for (CachedSurfaceBatch batch : cached.batches()) {
            if (batch.bounds() != null && frustum != null
                    && !frustum.isVisible(batch.bounds())) continue;
            if (batch.bounds() != null) {
                double reach = 192.0D + batch.bounds().getSize() * 0.5D;
                if (batch.bounds().getCenter().distanceToSqr(camera)
                        > reach * reach) continue;
            }
            renderSurfaceLayers(pose, buffers, batch.layers());
        }
    }

    private static void renderSurfaceLayers(PoseStack pose,
            MultiBufferSource.BufferSource buffers,
            Map<RenderType, List<PreparedVertex>> layers) {
        for (Map.Entry<RenderType, List<PreparedVertex>> layer
                : layers.entrySet()) {
            VertexConsumer consumer = buffers.getBuffer(layer.getKey());
            for (PreparedVertex vertex : layer.getValue()) {
                consumer.vertex(pose.last().pose(),
                                (float) vertex.position().x,
                                (float) vertex.position().y,
                                (float) vertex.position().z)
                        .color(vertex.red(), vertex.green(),
                                vertex.blue(), 255)
                        .uv(vertex.u(), vertex.v())
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(vertex.fallbackLight())
                        .normal(pose.last().normal(),
                                (float) vertex.normal().x,
                                (float) vertex.normal().y,
                                (float) vertex.normal().z)
                        .endVertex();
            }
        }
    }

    private static void renderSurfaceCache(PoseStack pose,
            MultiBufferSource.BufferSource buffers,
            Iterable<CachedSurfaceSlot> cachedSlots,
            net.minecraft.client.renderer.culling.Frustum frustum) {
        for (CachedSurfaceSlot slot : cachedSlots) {
            if (frustum != null && slot.bounds() != null
                    && !frustum.isVisible(slot.bounds())) continue;
            for (Map.Entry<RenderType, List<PreparedVertex>> layer
                    : slot.layers().entrySet()) {
                VertexConsumer consumer = buffers.getBuffer(layer.getKey());
                for (PreparedVertex vertex : layer.getValue()) {
                    consumer.vertex(pose.last().pose(),
                                    (float) vertex.position().x,
                                    (float) vertex.position().y,
                                    (float) vertex.position().z)
                            .color(vertex.red(), vertex.green(),
                                    vertex.blue(), 255)
                            .uv(vertex.u(), vertex.v())
                            .overlayCoords(OverlayTexture.NO_OVERLAY)
                            .uv2(vertex.fallbackLight())
                            .normal(pose.last().normal(),
                                    (float) vertex.normal().x,
                                    (float) vertex.normal().y,
                                    (float) vertex.normal().z)
                            .endVertex();
                }
            }
        }
    }

    private static boolean sameSurfaceGeometry(ConstructionSurface a,
            ConstructionSurface b) {
        return a != null && b != null
                && java.util.Objects.equals(a.id(), b.id())
                && java.util.Objects.equals(a.dimension(), b.dimension())
                && java.util.Objects.equals(a.bottomStart(), b.bottomStart())
                && java.util.Objects.equals(a.bottomEnd(), b.bottomEnd())
                && java.util.Objects.equals(a.topStart(), b.topStart())
                && java.util.Objects.equals(a.topEnd(), b.topEnd())
                && java.util.Objects.equals(a.curveOffset(), b.curveOffset())
                && java.util.Objects.equals(a.heightCurveOffset(),
                        b.heightCurveOffset())
                && a.flipped() == b.flipped();
    }

    private static CachedSurface updateSurfaceMesh(Minecraft minecraft,
            ConstructionSurface surface, CachedSurface cached) {
        Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots =
                new LinkedHashMap<>(cached.slots());
        Map<ConstructionSurface.SurfaceOverlaySlot, CachedSurfaceSlot> overlays =
                new LinkedHashMap<>(cached.overlays());

        Set<ConstructionSurface.SurfaceSlot> hinted =
                DIRTY_SURFACE_SLOTS.remove(surface.id());
        Set<ConstructionSurface.SurfaceSlot> dirty = hinted == null
                ? new java.util.LinkedHashSet<>()
                : new java.util.LinkedHashSet<>(hinted);

        if (hinted == null) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : cached.surface().attachments().entrySet()) {
                if (!java.util.Objects.equals(entry.getValue(),
                        surface.attachments().get(entry.getKey()))) {
                    dirty.add(entry.getKey());
                }
            }
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (!java.util.Objects.equals(entry.getValue(),
                        cached.surface().attachments().get(entry.getKey()))) {
                    dirty.add(entry.getKey());
                }
            }
            for (ConstructionSurface.SurfaceOverlaySlot key
                    : cached.surface().overlays().keySet()) {
                if (!java.util.Objects.equals(cached.surface().overlays().get(key),
                        surface.overlays().get(key))) {
                    dirty.add(key.slot());
                }
            }
            for (ConstructionSurface.SurfaceOverlaySlot key
                    : surface.overlays().keySet()) {
                if (!java.util.Objects.equals(surface.overlays().get(key),
                        cached.surface().overlays().get(key))) {
                    dirty.add(key.slot());
                }
            }
        }

        for (ConstructionSurface.SurfaceSlot slot : dirty) {
            ConstructionSurface.SurfaceAttachment attachment =
                    surface.attachments().get(slot);
            if (attachment == null || attachment.state().isAir()) {
                slots.remove(slot);
            } else {
                CachedSurfaceSlot rebuilt = buildSurfaceSlot(minecraft, surface,
                        slot, attachment,
                        TransformSurfaceGeometry.MAIN_SIDE, false);
                if (rebuilt == null) slots.remove(slot);
                else slots.put(slot, rebuilt);
            }

            overlays.keySet().removeIf(key -> key.slot().equals(slot));
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                if (!entry.getKey().slot().equals(slot)) continue;
                ConstructionSurface.SurfaceAttachment overlay =
                        entry.getValue();
                if (overlay == null || overlay.state().isAir()) continue;
                CachedSurfaceSlot rebuilt = buildSurfaceSlot(minecraft, surface,
                        slot, overlay, entry.getKey().normalSign(), true);
                if (rebuilt != null) overlays.put(entry.getKey(), rebuilt);
            }
        }
        Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> frozenSlots =
                Map.copyOf(slots);
        Map<ConstructionSurface.SurfaceOverlaySlot, CachedSurfaceSlot>
                frozenOverlays = Map.copyOf(overlays);
        return new CachedSurface(surface, frozenSlots, frozenOverlays,
                buildSurfaceBatches(frozenSlots, frozenOverlays));
    }

    private static CachedSurface buildSurfaceMesh(Minecraft minecraft,
            ConstructionSurface surface) {
        Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots =
                new LinkedHashMap<>();
        for (Map.Entry<ConstructionSurface.SurfaceSlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.attachments().entrySet()) {
            CachedSurfaceSlot slot = buildSurfaceSlot(minecraft, surface,
                    entry.getKey(), entry.getValue(),
                    TransformSurfaceGeometry.MAIN_SIDE, false);
            if (slot != null) slots.put(entry.getKey(), slot);
        }
        Map<ConstructionSurface.SurfaceOverlaySlot, CachedSurfaceSlot> overlays =
                new LinkedHashMap<>();
        for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.overlays().entrySet()) {
            CachedSurfaceSlot overlay = buildSurfaceSlot(minecraft, surface,
                    entry.getKey().slot(), entry.getValue(),
                    entry.getKey().normalSign(), true);
            if (overlay != null) overlays.put(entry.getKey(), overlay);
        }
        Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> frozenSlots =
                Map.copyOf(slots);
        Map<ConstructionSurface.SurfaceOverlaySlot, CachedSurfaceSlot>
                frozenOverlays = Map.copyOf(overlays);
        return new CachedSurface(surface, frozenSlots, frozenOverlays,
                buildSurfaceBatches(frozenSlots, frozenOverlays));
    }

    private static long surfaceBatchKey(ConstructionSurface.SurfaceSlot slot) {
        long column = Math.floorDiv(slot.column(), SURFACE_BATCH_SIZE);
        long row = Math.floorDiv(slot.row(), SURFACE_BATCH_SIZE);
        return (column << 32) ^ (row & 0xffffffffL);
    }

    private static List<CachedSurfaceBatch> buildSurfaceBatches(
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots,
            Map<ConstructionSurface.SurfaceOverlaySlot, CachedSurfaceSlot> overlays) {
        Map<Long, List<CachedSurfaceSlot>> regions = new LinkedHashMap<>();
        slots.forEach((slot, cached) -> regions.computeIfAbsent(
                surfaceBatchKey(slot), ignored -> new ArrayList<>()).add(cached));
        overlays.forEach((slot, cached) -> regions.computeIfAbsent(
                surfaceBatchKey(slot.slot()),
                ignored -> new ArrayList<>()).add(cached));
        List<CachedSurfaceBatch> result = new ArrayList<>(regions.size());
        for (List<CachedSurfaceSlot> region : regions.values()) {
            Map<RenderType, List<PreparedVertex>> layers = new LinkedHashMap<>();
            AABB bounds = null;
            for (CachedSurfaceSlot cached : region) {
                cached.layers().forEach((type, vertices) -> layers
                        .computeIfAbsent(type, ignored -> new ArrayList<>())
                        .addAll(vertices));
                if (cached.bounds() != null) bounds = bounds == null
                        ? cached.bounds() : bounds.minmax(cached.bounds());
            }
            Map<RenderType, List<PreparedVertex>> frozen = new LinkedHashMap<>();
            layers.forEach((type, vertices) ->
                    frozen.put(type, List.copyOf(vertices)));
            result.add(new CachedSurfaceBatch(Map.copyOf(frozen),
                    bounds == null ? null : bounds.inflate(0.06D)));
        }
        return List.copyOf(result);
    }

    /** Draw exactly the cached world-space payload of a Surface slot into the
     * context mask. Rebuilding a second local-frame version of an offset door
     * button put its outline one cell away on curved walls. Using the same
     * prepared vertices as the visible Surface guarantees matching geometry. */
    public static boolean renderSurfaceContextOutline(Minecraft minecraft,
            TransformContextTargetClient.Target target, PoseStack pose,
            MultiBufferSource buffers, Vec3 camera) {
        if (target == null || target.surfaceSlot() == null
                || minecraft.level == null) return false;
        ConstructionSurface surface = TransformConstructionClientState.surface(
                target.surfaceId());
        if (surface == null) return false;
        boolean overlay = target.kind()
                == TransformContextTargetClient.Kind.SURFACE_OVERLAY;
        int side = overlay ? target.normalSign()
                : TransformSurfaceGeometry.MAIN_SIDE;
        ConstructionSurface.SurfaceAttachment attachment = overlay
                ? surface.overlay(target.surfaceSlot(), side)
                : surface.attachments().get(target.surfaceSlot());
        if (attachment == null || attachment.state().isAir()) return false;
        CachedSurface current = SURFACE_MESHES.get(surface.id());
        CachedSurfaceSlot mesh = current != null && current.surface() == surface
                ? overlay
                    ? current.overlays().get(new ConstructionSurface.SurfaceOverlaySlot(
                            target.surfaceSlot(), side))
                    : current.slots().get(target.surfaceSlot())
                : null;
        if (mesh == null) {
            mesh = buildSurfaceSlot(minecraft, surface,
                    target.surfaceSlot(), attachment, side, overlay);
        }
        if (mesh == null) return false;
        for (Map.Entry<RenderType, List<PreparedVertex>> layer
                : mesh.layers().entrySet()) {
            VertexConsumer consumer = buffers.getBuffer(layer.getKey());
            for (PreparedVertex vertex : layer.getValue()) {
                Vec3 world = vertex.position().subtract(camera);
                consumer.vertex(pose.last().pose(), (float) world.x,
                                (float) world.y, (float) world.z)
                        .color(255, 255, 255, 255)
                        .uv(vertex.u(), vertex.v())
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT)
                        .normal(pose.last().normal(),
                                (float) vertex.normal().x,
                                (float) vertex.normal().y,
                                (float) vertex.normal().z)
                        .endVertex();
            }
        }
        return true;
    }

    private static CachedSurfaceSlot buildSurfaceSlot(Minecraft minecraft,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment,
            int normalSign, boolean overlay) {
        BlockState state = attachment.state();
        if (state == null || state.isAir()
                || state.getRenderShape() != RenderShape.MODEL) return null;
        Map<RenderType, List<PreparedVertex>> layers = new LinkedHashMap<>();
        RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
        List<PreparedVertex> output = layers.computeIfAbsent(renderType,
                ignored -> new ArrayList<>());
        appendSurfaceBlock(minecraft, output, surface, slot, attachment,
                normalSign, overlay);
        Map<RenderType, List<PreparedVertex>> immutable = new LinkedHashMap<>();
        layers.forEach((type, vertices) ->
                immutable.put(type, List.copyOf(vertices)));
        AABB bounds = null;
        for (List<PreparedVertex> vertices : immutable.values()) {
            for (PreparedVertex vertex : vertices) {
                Vec3 point = vertex.position();
                AABB position = new AABB(point, point);
                bounds = bounds == null ? position : bounds.minmax(position);
            }
        }
        return new CachedSurfaceSlot(Map.copyOf(immutable),
                bounds == null ? null : bounds.inflate(0.02D));
    }

    private static void appendSurfaceBlock(Minecraft minecraft,
            List<PreparedVertex> output, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment,
            int normalSign, boolean overlay) {
        BlockState state = attachment.state();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create(42L);
        BlockPos lightPos = BlockPos.containing(surface.gridPoint(
                (slot.column() + 0.5D) / surface.columns(),
                (slot.row() + 0.5D) / surface.rows()));
        int packedLight = LevelRenderer.getLightColor(minecraft.level, state,
                lightPos);
        // The authored local grid has its own neighbours; the parent-world
        // BlockPos is not a valid source of vanilla face-culling information.
        // Shared full-cube faces otherwise overlap after deformation, causing
        // the bright/dark razor-thin seams visible even within one Surface.
        boolean solidCell = fullSurfaceCell(state);
        for (Direction side : SIDES) {
            if (solidCell && internalSurfaceFace(surface, slot,
                    normalSign, overlay, side)) continue;
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, side, random,
                    ModelData.EMPTY, null)) {
                if (side == null && solidCell && internalSurfaceFace(
                        surface, slot, normalSign, overlay,
                        quad.getDirection())) continue;
                prepareQuad(minecraft, output, surface, slot, attachment,
                        normalSign, overlay, quad, lightPos, packedLight);
            }
        }
    }

    private static boolean internalSurfaceFace(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            boolean overlay, Direction face) {
        if (face == null || face.getAxis() == Direction.Axis.Z) return false;
        int side = normalSign < 0 ? -1 : 1;
        int frameSign = (surface.flipped() ? -1 : 1) * side;
        int dc = face == Direction.EAST ? frameSign
                : face == Direction.WEST ? -frameSign : 0;
        int dr = face == Direction.UP ? 1
                : face == Direction.DOWN ? -1 : 0;
        ConstructionSurface.SurfaceSlot neighbour =
                new ConstructionSurface.SurfaceSlot(slot.column() + dc,
                        slot.row() + dr);
        if (neighbour.column() < 0 || neighbour.column() >= surface.columns()
                || neighbour.row() < 0 || neighbour.row() >= surface.rows()) {
            // Another Surface is NOT the same as a neighbouring solid cell.
            // Their parametric border vertices may differ slightly, even if
            // a geometric join exists. Culling this cap exposed the sky along
            // the entire wall/ceiling seam. The cap is needed as a watertight
            // fallback for unmatched subdivisions and partial-length joins.
            return false;
        }
        ConstructionSurface.SurfaceAttachment other = overlay
                ? surface.overlay(neighbour, normalSign)
                : surface.attachments().get(neighbour);
        return other != null && fullSurfaceCell(other.state());
    }

    private static boolean fullSurfaceCell(BlockState state) {
        if (state == null || state.isAir() || state.hasBlockEntity()
                || state.getBlock() instanceof FacilityPipeModule.PipeBlock) return false;
        return FULL_SURFACE_CELLS.computeIfAbsent(state,
                TransformConstructionClientRenderer::computeFullSurfaceCell);
    }

    private static boolean computeFullSurfaceCell(BlockState state) {
        List<AABB> boxes = state.getCollisionShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                CollisionContext.empty()).toAabbs();
        if (boxes.size() != 1) return false;
        AABB box = boxes.get(0);
        return box.minX >= -1.0E-6D && box.minY >= -1.0E-6D
                && box.minZ >= -1.0E-6D && box.maxX <= 1.000001D
                && box.maxY <= 1.000001D && box.maxZ <= 1.000001D
                && box.minX < 1.0E-6D && box.minY < 1.0E-6D
                && box.minZ < 1.0E-6D && box.maxX > 0.999999D
                && box.maxY > 0.999999D && box.maxZ > 0.999999D;
    }

    private static void prepareQuad(Minecraft minecraft,
            List<PreparedVertex> output, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment, int normalSign,
            boolean overlay, BakedQuad quad, BlockPos lightPos,
            int fallbackLight) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        int tint = quad.isTinted() ? minecraft.getBlockColors().getColor(
                attachment.state(), minecraft.level, lightPos,
                quad.getTintIndex()) : 0xFFFFFF;
        int red = tint < 0 ? 255 : (tint >> 16) & 0xFF;
        int green = tint < 0 ? 255 : (tint >> 8) & 0xFF;
        int blue = tint < 0 ? 255 : tint & 0xFF;
        Vec3 quadNormal = new Vec3(quad.getDirection().getStepX(),
                quad.getDirection().getStepY(), quad.getDirection().getStepZ());

        Vec3[] points = new Vec3[4];
        float[] us = new float[4];
        float[] vs = new float[4];
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            points[vertex] = new Vec3(x, y, z);
            us[vertex] = stride > 4
                    ? Float.intBitsToFloat(vertices[offset + 4]) : 0.0F;
            vs[vertex] = stride > 5
                    ? Float.intBitsToFloat(vertices[offset + 5]) : 0.0F;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        // Adjoining pipe end caps lie on the exact same deformed cell seam.
        // Drawing both internal caps there causes flicker, even when the
        // tubular side faces have perfectly matching vertices. Keep the cap
        // only at the exposed end of a run, across all three pipe finishes.
        if (attachment.state().getBlock() instanceof FacilityPipeModule.PipeBlock
                && maxX - minX < 1.0E-4D
                && (Math.abs(minX) < 1.0E-4D
                    || Math.abs(minX - 1.0D) < 1.0E-4D)
                && pipeNeighbor(surface, slot, attachment.state(),
                        normalSign, overlay, minX > 0.5D)) {
            return;
        }

        // Adjacent tube faces already use the same parametric end vertices and
        // omit internal caps. Finer subdivisions only on curved pipe payloads
        // reduce long polygon chords and pixel-sized cracks at tight bends.
        // The tessellation is cached per slot; it never rebuilds every pipe
        // or the entire Surface when one neighbouring cell changes.
        boolean deform = TransformSurfaceGeometry.effectiveDeform(attachment);
        boolean curvedPipe = deform
                && attachment.state().getBlock() instanceof FacilityPipeModule.PipeBlock
                && surface.curveOffset().lengthSqr() > 1.0E-8D;
        // Give only structural perimeter tiles additional curve samples.
        // Two independently gridded quadratic planes need enough vertices on
        // their shared edge to avoid a visible sawtooth between their chords.
        // Interior tiles retain their cheap four-step tessellation.
        boolean perimeter = !overlay && fullSurfaceCell(attachment.state())
                && (slot.column() == 0 && minX < 1.0E-5D
                || slot.column() == surface.columns() - 1
                        && maxX > 0.99999D
                || slot.row() == 0 && minY < 1.0E-5D
                || slot.row() == surface.rows() - 1
                        && maxY > 0.99999D);
        // Interior structural tiles do not need the same tessellation as a
        // visible seam. Keep shared boundaries dense so independent planes
        // still weld cleanly, while cutting the normal curved-wall vertex
        // count roughly in half. Pipes retain extra samples for their profile.
        // Tessellate ALONG an exposed border, not 12x12 across every block
        // merely touching any border. The old cross-product made even short
        // two-axis curved corridors generate thousands of redundant quads.
        boolean horizontalEdge = perimeter && (slot.row() == 0
                || slot.row() == surface.rows() - 1);
        boolean verticalEdge = perimeter && (slot.column() == 0
                || slot.column() == surface.columns() - 1);
        int xSteps = deform
                && surface.curveOffset().lengthSqr() > 1.0E-8D
                && maxX - minX > 0.20D
                ? curvedPipe ? 12 : horizontalEdge ? 24 : 2 : 1;
        int ySteps = deform
                && surface.heightCurveOffset().lengthSqr() > 1.0E-8D
                && maxY - minY > 0.20D
                ? verticalEdge ? 24 : 2 : 1;
        for (int ix = 0; ix < xSteps; ix++) {
            double s0 = ix / (double) xSteps;
            double s1 = (ix + 1.0D) / xSteps;
            for (int iy = 0; iy < ySteps; iy++) {
                double t0 = iy / (double) ySteps;
                double t1 = (iy + 1.0D) / ySteps;
                emitSurfaceVertex(output, surface, slot, attachment, normalSign,
                        overlay, bilerp(points, s0, t0), quadNormal,
                        bilerp(us, s0, t0), bilerp(vs, s0, t0),
                        red, green, blue, fallbackLight);
                emitSurfaceVertex(output, surface, slot, attachment, normalSign,
                        overlay, bilerp(points, s1, t0), quadNormal,
                        bilerp(us, s1, t0), bilerp(vs, s1, t0),
                        red, green, blue, fallbackLight);
                emitSurfaceVertex(output, surface, slot, attachment, normalSign,
                        overlay, bilerp(points, s1, t1), quadNormal,
                        bilerp(us, s1, t1), bilerp(vs, s1, t1),
                        red, green, blue, fallbackLight);
                emitSurfaceVertex(output, surface, slot, attachment, normalSign,
                        overlay, bilerp(points, s0, t1), quadNormal,
                        bilerp(us, s0, t1), bilerp(vs, s0, t1),
                        red, green, blue, fallbackLight);
            }
        }
    }

    private static boolean pipeNeighbor(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, BlockState state,
            int normalSign, boolean overlay, boolean localRight) {
        int side = normalSign < 0 ? -1 : 1;
        int physicalDirection = (surface.flipped() ? -1 : 1) * side;
        ConstructionSurface.SurfaceSlot adjacent =
                new ConstructionSurface.SurfaceSlot(
                        slot.column() + (localRight ? 1 : -1)
                                * physicalDirection,
                        slot.row());
        ConstructionSurface.SurfaceAttachment other = overlay
                ? surface.overlay(adjacent, normalSign)
                : surface.attachments().get(adjacent);
        return other != null
                && other.state().getBlock()
                        instanceof FacilityPipeModule.PipeBlock
                && other.state().getValue(HorizontalDirectionalBlock.FACING)
                        == state.getValue(HorizontalDirectionalBlock.FACING);
    }

    private static void emitSurfaceVertex(List<PreparedVertex> output,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment, int normalSign,
            boolean overlay, Vec3 point, Vec3 localNormal, float u, float v,
            int red, int green, int blue, int light) {
        double depthOffset = overlay
                && (normalSign < 0 ? -1 : 1)
                == TransformSurfaceGeometry.MAIN_SIDE ? 1.0D : 0.0D;
        // A ceiling meets a wall at the latter's TOP/BOTTOM border, not just
        // its START/END border. All four physical edges are eligible; internal
        // tile borders, fixtures and overlays never get mitered.
        // The authored X axis can be mirrored by Surface.flip. Never infer
        // the world-space border from an unmapped local x=0/1: a flipped first
        // column reaches the physical border at x=1, not x=0. The weld method
        // below checks the final world-side (u,v) before doing any projection.
        boolean seamEligible = !overlay
                && normalSign == TransformSurfaceGeometry.MAIN_SIDE
                && fullSurfaceCell(attachment.state());
        boolean joinEdge = seamEligible
                && (((slot.column() == 0
                        || slot.column() == surface.columns() - 1)
                        && (Math.abs(point.x) < 1.0E-6D
                        || Math.abs(point.x - 1.0D) < 1.0E-6D))
                    || (slot.row() == 0
                        && Math.abs(point.y) < 1.0E-6D)
                    || (slot.row() == surface.rows() - 1
                        && Math.abs(point.y - 1.0D) < 1.0E-6D));
        VertexFrame frame = TransformSurfaceGeometry.effectiveDeform(attachment)
                ? deformedFrame(surface, slot, point.x, point.y, point.z,
                        localNormal, normalSign, depthOffset, seamEligible)
                : rigidFrame(surface, slot, point.x, point.y, point.z,
                        localNormal, normalSign, depthOffset);
        output.add(new PreparedVertex(attachment.state(), frame.position(),
                frame.normal(), u, v, red, green, blue, light));
    }

    private static Vec3 bilerp(Vec3[] p, double s, double t) {
        return p[0].scale((1.0D - s) * (1.0D - t))
                .add(p[1].scale(s * (1.0D - t)))
                .add(p[2].scale(s * t))
                .add(p[3].scale((1.0D - s) * t));
    }

    private static float bilerp(float[] p, double s, double t) {
        return (float) (p[0] * (1.0D - s) * (1.0D - t)
                + p[1] * s * (1.0D - t)
                + p[2] * s * t
                + p[3] * (1.0D - s) * t);
    }

    private static VertexFrame deformedFrame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double x, double y, double z,
            Vec3 localNormal, int normalSign, double depthOffset,
            boolean seamEligible) {
        int side = normalSign < 0 ? -1 : 1;
        double baseX = surface.flipped() ? 1.0D - x : x;
        double localX = side < 0 ? 1.0D - baseX : baseX;
        double u = (slot.column() + localX) / surface.columns();
        double v = (slot.row() + y) / surface.rows();
        Vec3 tangent = surface.gridFrameTangent(u, v).scale(side);
        Vec3 normal = surface.gridNormal(u, v).scale(side);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));
        Vec3 position = surface.gridPoint(u, v)
                .add(normal.scale(z + depthOffset));
        if (seamEligible && z >= 0.0D && z <= 1.000001D) {
            Vec3 welded = smoothJoinedSurfacePosition(surface, u, v, z,
                    position);
            if (welded != null) position = welded;
        }
        Vec3 transformedNormal = TransformMath.safeNormalize(
                tangent.scale(localNormal.x)
                        .add(vertical.scale(localNormal.y))
                        .add(normal.scale(localNormal.z)), normal);
        return new VertexFrame(position, transformedNormal);
    }

    /**
     * Pull the first physical cell next to a matched seam toward the same
     * canonical border used by its neighbour. Moving only the terminal row of
     * vertices closes the mathematical edge, but leaves a visible lip whenever
     * two old Surfaces approach the join with different tangents. This one-cell
     * smoothstep keeps the authored curves intact away from the seam while the
     * local grid reorganises itself into a continuous transition.
     */
    private static Vec3 smoothJoinedSurfacePosition(ConstructionSurface surface,
            double u, double v, double depth, Vec3 original) {
        Map<Integer, MatchedSurfaceEdge> matches =
                SHARED_SURFACE_EDGES.get(surface.id());
        if (matches == null || matches.isEmpty()) return null;

        Vec3 totalDelta = Vec3.ZERO;
        double totalWeight = 0.0D;
        for (int edge = 0; edge < 4; edge++) {
            if (!matches.containsKey(edge)) continue;
            double cellsFromEdge = switch (edge) {
                case 0 -> u * surface.columns();
                case 1 -> (1.0D - u) * surface.columns();
                case 2 -> v * surface.rows();
                default -> (1.0D - v) * surface.rows();
            };
            if (cellsFromEdge < -1.0E-6D || cellsFromEdge > 1.0D) continue;

            double boundaryU = edge == 0 ? 0.0D
                    : edge == 1 ? 1.0D : u;
            double boundaryV = edge == 2 ? 0.0D
                    : edge == 3 ? 1.0D : v;
            Vec3 joined = joinedSurfaceEdge(surface, boundaryU, boundaryV,
                    depth);
            if (joined == null) continue;
            Vec3 base = surface.gridPoint(boundaryU, boundaryV)
                    .add(surface.gridNormal(boundaryU, boundaryV)
                            .scale(depth));
            Vec3 delta = joined.subtract(base);

            double t = 1.0D - Math.max(0.0D,
                    Math.min(1.0D, cellsFromEdge));
            // C1 smoothstep. The derivative is zero both at the seam and at
            // the end of the one-cell blend band, avoiding a second visible
            // crease immediately beside the join.
            double weight = t * t * (3.0D - 2.0D * t);
            totalDelta = totalDelta.add(delta.scale(weight));
            totalWeight += weight;
        }
        return totalWeight <= 1.0E-9D ? null : original.add(totalDelta);
    }

    /** Endpoint agreement is checked for both directions and at the center,
     * so independent curved planes only meet when their physical edges do. */
    private record MatchedSurfaceEdge(ConstructionSurface other,
            int otherEdge, boolean reversed, boolean partial,
            List<Vec3> samples, Map<Long, Double> projectionCache) { }

    /** Cached arc points let a short wall edge meet an interior subsection of
     * a longer ceiling edge even when their slot counts and arc parameters
     * differ. Only neighbouring physical edges may be considered. */
    private static List<Vec3> sampleEdge(ConstructionSurface surface,
            int edge) {
        List<Vec3> points = new ArrayList<>(49);
        for (int i = 0; i <= 48; i++) {
            points.add(edgePoint(surface, edge, i / 48.0D));
        }
        return List.copyOf(points);
    }

    private static double closestEdgeFraction(List<Vec3> samples, Vec3 point) {
        double best = 0.72D * 0.72D;
        double fraction = Double.NaN;
        for (int i = 1; i < samples.size(); i++) {
            Vec3 a = samples.get(i - 1);
            Vec3 span = samples.get(i).subtract(a);
            double length2 = span.lengthSqr();
            double t = length2 < 1.0E-12D ? 0.0D
                    : net.minecraft.util.Mth.clamp(
                            point.subtract(a).dot(span) / length2,
                            0.0D, 1.0D);
            double distance = a.add(span.scale(t)).distanceToSqr(point);
            if (distance < best) {
                best = distance;
                fraction = (i - 1 + t) / (samples.size() - 1.0D);
            }
        }
        return fraction;
    }

    private static AABB sampledEdgeBounds(List<Vec3> samples) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 point : samples) {
            minX = Math.min(minX, point.x); minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z); maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y); maxZ = Math.max(maxZ, point.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Vec3 edgePoint(ConstructionSurface surface,
            int edge, double fraction) {
        return switch (edge) {
            case 0 -> surface.gridPoint(0.0D, fraction);
            case 1 -> surface.gridPoint(1.0D, fraction);
            case 2 -> surface.gridPoint(fraction, 0.0D);
            default -> surface.gridPoint(fraction, 1.0D);
        };
    }

    /** Refresh neighbours only after a committed edit. A temporary drag
     * changes its authoring guides but must not rebake the entire facility. */
    private static void updateSharedSurfaceEdges(
            List<ConstructionSurface> surfaces) {
        boolean changed = SURFACE_EDGE_GEOMETRIES.size() != surfaces.size();
        for (ConstructionSurface surface : surfaces) {
            if (!sameSurfaceGeometry(
                    SURFACE_EDGE_GEOMETRIES.get(surface.id()), surface)) {
                changed = true;
            }
        }
        if (!changed || surfaces.stream().anyMatch(surface ->
                TransformConstructionClientControls.previewingSurface(
                        surface.id()))) return;
        Set<UUID> affected = new java.util.HashSet<>();
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
        Set<UUID> currentIds = new java.util.HashSet<>();
        for (ConstructionSurface surface : surfaces) currentIds.add(surface.id());
        SURFACE_EDGE_SAMPLES.keySet().retainAll(currentIds);
        SURFACE_EDGE_BOUNDS.keySet().retainAll(currentIds);
        for (ConstructionSurface surface : surfaces) {
            if (SURFACE_EDGE_SAMPLES.containsKey(surface.id())
                    && !affected.contains(surface.id())) continue;
            List<List<Vec3>> samples = List.of(sampleEdge(surface, 0),
                    sampleEdge(surface, 1), sampleEdge(surface, 2),
                    sampleEdge(surface, 3));
            SURFACE_EDGE_SAMPLES.put(surface.id(), samples);
            SURFACE_EDGE_BOUNDS.put(surface.id(), List.of(
                    sampledEdgeBounds(samples.get(0)),
                    sampledEdgeBounds(samples.get(1)),
                    sampledEdgeBounds(samples.get(2)),
                    sampledEdgeBounds(samples.get(3))));
        }
        SURFACE_EDGE_GEOMETRIES.clear();
        SHARED_SURFACE_EDGES.clear();
        Map<UUID, List<List<Vec3>>> edgeSamples = SURFACE_EDGE_SAMPLES;
        for (ConstructionSurface surface : surfaces) {
            SURFACE_EDGE_GEOMETRIES.put(surface.id(), surface);
            Map<Integer, MatchedSurfaceEdge> matches = new HashMap<>();
            for (int edge = 0; edge < 4; edge++) {
                List<Vec3> sourceSamples = edgeSamples.get(surface.id())
                        .get(edge);
                // Broad-phase only. A partially overlapping old wall/roof
                // must not be rejected because its endpoints do not meet.
                AABB sourceBounds = SURFACE_EDGE_BOUNDS.get(surface.id())
                        .get(edge).inflate(0.55D);
                MatchedSurfaceEdge match = null;
                double bestScore = Double.POSITIVE_INFINITY;
                for (ConstructionSurface other : surfaces) {
                    if (other.id().equals(surface.id())) continue;
                    for (int otherEdge = 0; otherEdge < 4; otherEdge++) {
                        List<Vec3> samples = edgeSamples.get(other.id())
                                .get(otherEdge);
                        if (!sourceBounds.intersects(SURFACE_EDGE_BOUNDS
                                .get(other.id()).get(otherEdge).inflate(0.55D))) {
                            continue;
                        }
                        double otherU = otherEdge == 0 ? 0.0D
                                : otherEdge == 1 ? 1.0D : 0.5D;
                        double otherV = otherEdge == 2 ? 0.0D
                                : otherEdge == 3 ? 1.0D : 0.5D;
                        double dot = surface.gridNormal(
                                edge < 2 ? edge : 0.5D,
                                edge == 2 ? 0.0D : edge == 3 ? 1.0D : 0.5D)
                                .dot(other.gridNormal(otherU, otherV));
                        if (dot <= -0.2D) continue;

                        // Existing surfaces need not have identical lengths or
                        // perfectly matching endpoints. Only weld the contiguous
                        // portions whose *physical* edges are already nearby.
                        // Sample comparisons happen after committed geometry
                        // changes and are never run in the per-frame vertex pass.
                        int close = 0;
                        int run = 0;
                        int longestRun = 0;
                        double separation = 0.0D;
                        double firstFraction = Double.NaN;
                        double lastFraction = Double.NaN;
                        for (int sample = 0; sample <= 48; sample += 4) {
                            Vec3 point = sourceSamples.get(sample);
                            double mapped = closestEdgeFraction(samples, point);
                            if (!Double.isFinite(mapped)) {
                                run = 0;
                                continue;
                            }
                            double distance = point.distanceToSqr(
                                    edgePoint(other, otherEdge, mapped));
                            if (distance > 0.45D * 0.45D) {
                                run = 0;
                                continue;
                            }
                            close++;
                            longestRun = Math.max(longestRun, ++run);
                            separation += distance;
                            if (!Double.isFinite(firstFraction)) {
                                firstFraction = mapped;
                            }
                            lastFraction = mapped;
                        }
                        // A one-point intersection between unrelated edges is
                        // not a seam; require an actual shared arc interval.
                        if (close < 3 || longestRun < 3
                                || Math.abs(lastFraction - firstFraction)
                                        < 0.035D) continue;
                        double score = separation / close
                                + (13 - close) * 0.004D;
                        if (score >= bestScore) continue;
                        bestScore = score;
                        boolean partial = close < 12;
                        match = new MatchedSurfaceEdge(other, otherEdge,
                                lastFraction < firstFraction, partial, samples,
                                new HashMap<>());
                    }
                }
                if (match != null) matches.put(edge, match);
            }
            SHARED_SURFACE_EDGES.put(surface.id(), Map.copyOf(matches));
            if (matches.values().stream().anyMatch(edge ->
                    affected.contains(edge.other().id()))) {
                affected.add(surface.id());
            }
        }
        SURFACE_MESHES.keySet().removeAll(affected);
    }

    /** Use the same physical miter point for both meshes. The shared-edge
     * cache supports wall/ceiling joins even when one edge is horizontal in
     * authoring space and the other is vertical. */
    private static Vec3 joinedSurfaceEdge(ConstructionSurface surface,
            double u, double v, double depth) {
        Map<Integer, MatchedSurfaceEdge> matches =
                SHARED_SURFACE_EDGES.get(surface.id());
        if (matches == null || matches.isEmpty()) return null;
        Vec3 sourcePoint = surface.gridPoint(u, v);
        Vec3 sourceNormal = surface.gridNormal(u, v);
        Vec3 result = null;
        int found = 0;
        for (int edge = 0; edge < 4; edge++) {
            if (edge == 0 && Math.abs(u) >= 1.0E-6D
                    || edge == 1 && Math.abs(u - 1.0D) >= 1.0E-6D
                    || edge == 2 && Math.abs(v) >= 1.0E-6D
                    || edge == 3 && Math.abs(v - 1.0D) >= 1.0E-6D) continue;
            MatchedSurfaceEdge match = matches.get(edge);
            if (match == null) continue;
            ConstructionSurface other = match.other();
            double fraction = edge < 2 ? v : u;
            // Grid arc-length distributions can differ at an otherwise exact
            // physical edge. Project this vertex to the neighbour's edge
            // rather than assuming the same grid fraction on both meshes.
            // Each boundary coordinate is shared by several model quads and
            // both depth faces. Project it once per committed seam geometry.
            double otherFraction = match.partial()
                    ? match.projectionCache().computeIfAbsent(
                            Math.round(fraction * 1_000_000.0D), ignored ->
                                    closestEdgeFraction(match.samples(),
                                            sourcePoint))
                    : (match.reversed() ? 1.0D - fraction : fraction);
            if (!Double.isFinite(otherFraction)) continue;
            double otherU = match.otherEdge() == 0 ? 0.0D
                    : match.otherEdge() == 1 ? 1.0D : otherFraction;
            double otherV = match.otherEdge() == 2 ? 0.0D
                    : match.otherEdge() == 3 ? 1.0D : otherFraction;
            Vec3 otherPoint = other.gridPoint(otherU, otherV);
            if (sourcePoint.distanceToSqr(otherPoint) >= 0.72D * 0.72D)
                continue;
            int column = Math.min(other.columns() - 1, Math.max(0,
                    (int) Math.floor(otherU * other.columns())));
            int row = Math.min(other.rows() - 1, Math.max(0,
                    (int) Math.floor(otherV * other.rows())));
            ConstructionSurface.SurfaceAttachment attached =
                    other.attachments().get(
                            new ConstructionSurface.SurfaceSlot(column, row));
            if (attached == null || !fullSurfaceCell(attached.state())) continue;
            Vec3 otherNormal = other.gridNormal(otherU, otherV);
            double dot = sourceNormal.dot(otherNormal);
            if (dot <= -0.2D) continue;
            // Weld both authored borders to one canonical miter curve. The old
            // implementation solved the intersection independently from each
            // Surface, so slightly different arc parameterization produced two
            // almost-equal edges and a bright slit. Midpoint + bisector is
            // symmetric: both meshes reach the exact same world-space vertex
            // while their original curves remain unchanged away from the edge.
            // Partial joins are common in old corridors where a long roof
            // edge overlaps only part of a shorter wall. Averaging independently
            // from both directions is not symmetric and can leave two nearly
            // identical polylines separated by a bright slit. Elect one edge
            // deterministically as the seam master so both meshes converge onto
            // the exact same authored curve.
            Vec3 sharedBase;
            if (match.partial()) {
                boolean sourceMaster = surface.id().toString().compareTo(
                        other.id().toString()) <= 0;
                sharedBase = sourceMaster ? sourcePoint : otherPoint;
            } else {
                sharedBase = canonicalSharedEdgePoint(surface, edge, other,
                        match.otherEdge(), match.reversed(), fraction);
            }
            Vec3 bisector = TransformMath.safeNormalize(
                    sourceNormal.add(otherNormal), sourceNormal);
            double projection = Math.abs(bisector.dot(sourceNormal));
            if (projection < 0.12D) continue;
            Vec3 candidate = sharedBase.add(
                    bisector.scale(depth / projection));
            if (candidate.distanceToSqr(sharedBase) > 3.0D) continue;
            result = result == null ? candidate : result.add(candidate);
            found++;
        }
        if (found == 0) return null;
        // Do not extend a seam beyond either authored plane. That old overlap
        // hid pinholes but produced the visible "raised" lips in tight curves.
        // Full matches now share one deterministic piecewise-linear border,
        // so both independently subdivided grids land on the same seam.
        return result.scale(1.0D / found);
    }

    private static Vec3 canonicalSharedEdgePoint(ConstructionSurface first,
            int firstEdge, ConstructionSurface second, int secondEdge,
            boolean reversed, double fraction) {
        final int segments = 128;
        double scaled = Math.max(0.0D, Math.min(1.0D, fraction)) * segments;
        int index = Math.min(segments - 1, (int) Math.floor(scaled));
        double local = scaled - index;
        double a = index / (double) segments;
        double b = (index + 1.0D) / segments;
        Vec3 p0 = edgePoint(first, firstEdge, a).add(
                edgePoint(second, secondEdge,
                        reversed ? 1.0D - a : a)).scale(0.5D);
        Vec3 p1 = edgePoint(first, firstEdge, b).add(
                edgePoint(second, secondEdge,
                        reversed ? 1.0D - b : b)).scale(0.5D);
        return p0.lerp(p1, local);
    }

    private static VertexFrame rigidFrame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double x, double y, double z,
            Vec3 localNormal, int normalSign, double depthOffset) {
        int side = normalSign < 0 ? -1 : 1;
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 tangent = surface.gridFrameTangent(u, v).scale(side);
        Vec3 normal = surface.gridNormal(u, v).scale(side);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));
        Vec3 position = surface.gridPoint(u, v)
                .add(tangent.scale(x - 0.5D))
                .add(vertical.scale(y - 0.5D))
                .add(normal.scale(z + depthOffset));
        Vec3 transformedNormal = TransformMath.safeNormalize(
                tangent.scale(localNormal.x)
                        .add(vertical.scale(localNormal.y))
                        .add(normal.scale(localNormal.z)), normal);
        return new VertexFrame(position, transformedNormal);
    }

    private static TransformSurfaceRaycast.Target aimedSurfaceTarget(
            LocalPlayer player) {
        if (player == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        // Editor selection must not override the closest logical hit.
        return TransformSurfaceRaycast.target(player,
                TransformConstructionClientState.surfaces(
                        minecraft.level.dimension().location()));
    }

    private static void renderLogicalSurfaceSlot(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, float red, float green,
            float blue, float alpha) {
        int columns = surface.columns();
        int rows = surface.rows();
        double u0 = slot.column() / (double) columns;
        double u1 = (slot.column() + 1.0D) / columns;
        double v0 = slot.row() / (double) rows;
        double v1 = (slot.row() + 1.0D) / rows;
        int samples = 4;
        renderSurfaceSlotEdge(pose, lines, surface, u0, v0, u1, v0,
                samples, red, green, blue, alpha);
        renderSurfaceSlotEdge(pose, lines, surface, u0, v1, u1, v1,
                samples, red, green, blue, alpha);
        renderSurfaceSlotEdge(pose, lines, surface, u0, v0, u0, v1,
                samples, red, green, blue, alpha);
        renderSurfaceSlotEdge(pose, lines, surface, u1, v0, u1, v1,
                samples, red, green, blue, alpha);
    }

    private static void renderSurfaceSlotEdge(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface, double u0,
            double v0, double u1, double v1, int samples, float red,
            float green, float blue, float alpha) {
        Vec3 previous = surface.gridPoint(u0, v0);
        for (int index = 1; index <= samples; index++) {
            double t = index / (double) samples;
            Vec3 current = surface.gridPoint(
                    u0 + (u1 - u0) * t,
                    v0 + (v1 - v0) * t);
            line(pose, lines, previous, current, red, green, blue, alpha);
            previous = current;
        }
    }

    private static void renderGroupOutline(PoseStack pose,
            VertexConsumer lines, TransformGroup group, Vec3 camera) {
        if (group.cells().isEmpty()) return;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (TransformGroup.GridPos cell : group.cells().keySet()) {
            minX = Math.min(minX, cell.x() - 0.5D);
            minY = Math.min(minY, cell.y() - 0.5D);
            minZ = Math.min(minZ, cell.z() - 0.5D);
            maxX = Math.max(maxX, cell.x() + 0.5D);
            maxY = Math.max(maxY, cell.y() + 0.5D);
            maxZ = Math.max(maxZ, cell.z() + 0.5D);
        }
        Vec3 localCenter = new Vec3((minX + maxX) * 0.5D,
                (minY + maxY) * 0.5D, (minZ + maxZ) * 0.5D);
        Vec3 worldCenter = TransformMath.localToWorld(group.origin(),
                localCenter, group.rotationX(), group.rotationY(),
                group.rotationZ());
        if (worldCenter.distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) return;

        Vec3[] corners = new Vec3[8];
        int index = 0;
        for (int yi = 0; yi < 2; yi++) {
            for (int zi = 0; zi < 2; zi++) {
                for (int xi = 0; xi < 2; xi++) {
                    Vec3 local = new Vec3(xi == 0 ? minX : maxX,
                            yi == 0 ? minY : maxY,
                            zi == 0 ? minZ : maxZ);
                    corners[index++] = TransformMath.localToWorld(
                            group.origin(), local, group.rotationX(),
                            group.rotationY(), group.rotationZ());
                }
            }
        }
        int[][] edges = {
                {0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},
                {0,4},{1,5},{2,6},{3,7}
        };
        for (int[] edge : edges) {
            line(pose, lines, corners[edge[0]], corners[edge[1]],
                    0.12F, 1.0F, 0.28F, 0.72F);
        }
    }

    private static void renderLogicalGroupCell(PoseStack pose,
            VertexConsumer lines, TransformGroup group,
            TransformGroup.GridPos cell, float red, float green,
            float blue, float alpha) {
        Vec3[] corners = new Vec3[8];
        int index = 0;
        for (int yi = 0; yi < 2; yi++) {
            for (int zi = 0; zi < 2; zi++) {
                for (int xi = 0; xi < 2; xi++) {
                    double edge = 0.506D;
                    Vec3 local = new Vec3(
                            cell.x() + (xi == 0 ? -edge : edge),
                            cell.y() + (yi == 0 ? -edge : edge),
                            cell.z() + (zi == 0 ? -edge : edge));
                    corners[index++] = TransformMath.localToWorld(
                            group.origin(), local, group.rotationX(),
                            group.rotationY(), group.rotationZ());
                }
            }
        }
        int[][] edges = {
                {0,1},{2,3},{4,5},{6,7},
                {0,2},{1,3},{4,6},{5,7},
                {0,4},{1,5},{2,6},{3,7}
        };
        for (int[] edge : edges) {
            line(pose, lines, corners[edge[0]], corners[edge[1]],
                    red, green, blue, alpha);
        }
    }

    private static void renderGroupGrid(PoseStack pose, VertexConsumer lines,
            TransformGroup group, Vec3 camera) {
        if (group.origin().distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) return;
        Selection selected = TransformConstructionClientState.selection();
        boolean active = selected != null && selected.type() == SelectionType.GROUP
                && group.id().equals(selected.id());
        float r = active ? 1.0F : 0.12F;
        float g = active ? 0.82F : 1.0F;
        float b = active ? 0.12F : 0.28F;
        for (TransformGroup.GridPos cell : group.cells().keySet()) {
            Vec3[] corners = new Vec3[8];
            int index = 0;
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    for (int xi = 0; xi < 2; xi++) {
                        double edge = 0.506D;
                        Vec3 local = new Vec3(
                                cell.x() + (xi == 0 ? -edge : edge),
                                cell.y() + (yi == 0 ? -edge : edge),
                                cell.z() + (zi == 0 ? -edge : edge));
                        corners[index++] = TransformMath.localToWorld(
                                group.origin(), local, group.rotationX(),
                                group.rotationY(), group.rotationZ());
                    }
                }
            }
            int[][] edges = {
                    {0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},
                    {0,4},{1,5},{2,6},{3,7}
            };
            for (int[] edge : edges) {
                line(pose, lines, corners[edge[0]], corners[edge[1]],
                        r, g, b, 0.95F);
            }
        }
        // Gizmos have their own depth-free pass after the level is rendered.
    }

    private static void renderGroupGizmo(PoseStack pose, VertexConsumer lines,
            TransformGroup group) {
        Minecraft minecraft = Minecraft.getInstance();
        Axis hot = minecraft.player == null ? null
                : TransformConstructionClientControls.hoveredGroupGizmoAxis(
                        minecraft.player);
        if (TransformConstructionClientState.mode() == EditMode.ROTATE) {
            for (Axis axis : Axis.values()) {
                renderRotationRing(pose, lines, group.origin(),
                        TransformConstructionClientControls.gizmoAxisDirection(
                                TransformConstructionClientState.selection(),
                                axis), axis, axis == hot);
            }
        } else {
            for (Axis axis : Axis.values()) {
                renderMoveAxis(pose, lines, group.origin(),
                        TransformConstructionClientControls.gizmoAxisDirection(
                                TransformConstructionClientState.selection(),
                                axis), axis, axis == hot);
            }
        }
    }

    private static void renderMoveAxis(PoseStack pose, VertexConsumer lines,
            Vec3 origin, Vec3 direction, Axis axis, boolean hot) {
        direction = TransformMath.safeNormalize(direction,
                new Vec3(1.0D, 0.0D, 0.0D));
        float[] color = axisColor(axis, hot);
        Vec3 tip = origin.add(direction.scale(1.18D));
        line(pose, lines, origin, tip, color[0], color[1], color[2], 1.0F);
        Vec3 helper = Math.abs(direction.y) < 0.8D
                ? new Vec3(0.0D, 1.0D, 0.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 side = direction.cross(helper).normalize().scale(0.10D);
        Vec3 back = tip.subtract(direction.scale(0.20D));
        line(pose, lines, tip, back.add(side),
                color[0], color[1], color[2], 1.0F);
        line(pose, lines, tip, back.subtract(side),
                color[0], color[1], color[2], 1.0F);
    }

    private static void renderRotationRing(PoseStack pose, VertexConsumer lines,
            Vec3 center, Vec3 normal, Axis axis, boolean hot) {
        normal = TransformMath.safeNormalize(normal,
                new Vec3(0.0D, 1.0D, 0.0D));
        Vec3 basisA = Math.abs(normal.y) < 0.85D
                ? normal.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize()
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 basisB = normal.cross(basisA).normalize();
        float[] color = axisColor(axis, hot);
        final int segments = 48;
        final double radius = 0.92D;
        Vec3 previous = center.add(basisA.scale(radius));
        for (int index = 1; index <= segments; index++) {
            double angle = Math.PI * 2.0D * index / segments;
            Vec3 current = center.add(basisA.scale(Math.cos(angle) * radius))
                    .add(basisB.scale(Math.sin(angle) * radius));
            line(pose, lines, previous, current,
                    color[0], color[1], color[2], 1.0F);
            previous = current;
        }
    }

    private static float[] axisColor(Axis axis, boolean hot) {
        float boost = hot ? 1.0F : 0.78F;
        return switch (axis) {
            case X -> new float[]{1.0F, hot ? 0.42F : 0.16F,
                    hot ? 0.38F : 0.14F};
            case Y -> new float[]{hot ? 0.42F : 0.14F, 1.0F,
                    hot ? 0.42F : 0.18F};
            case Z -> new float[]{hot ? 0.42F : 0.18F,
                    hot ? 0.58F : 0.32F, boost};
        };
    }

    private static void renderLinkedSurfaceAuthoring(PoseStack pose,
            VertexConsumer lines, Vec3 camera) {
        if (!TransformConstructionClientControls.linkedSurfaceMode()) return;
        TransformConstructionClientControls.LinkedSurfaceEdge first =
                TransformConstructionClientControls.linkedFirstEdge();
        TransformConstructionClientControls.LinkedSurfaceEdge hovered =
                TransformConstructionClientControls.linkedHoveredEdge();
        if (first != null) {
            ConstructionSurface surface =
                    TransformConstructionClientState.surface(first.surfaceId());
            if (surface != null) {
                renderLinkedSurfaceEdge(pose, lines, surface, first.edge(),
                        0.20F, 0.85F, 1.0F, 1.0F);
            }
        }
        if (hovered != null) {
            ConstructionSurface surface =
                    TransformConstructionClientState.surface(hovered.surfaceId());
            if (surface != null) {
                renderLinkedSurfaceEdge(pose, lines, surface, hovered.edge(),
                        1.0F, 0.78F, 0.16F, 1.0F);
            }
        }
        ConstructionSurface preview =
                TransformConstructionClientControls.linkedPreview();
        if (preview != null
                && preview.gridPoint(0.5D, 0.5D).distanceToSqr(camera)
                        <= MAX_RENDER_DISTANCE_SQR) {
            renderSurfaceGrid(pose, lines, preview, camera);
            Vec3 a = preview.gridPoint(0.5D, 0.0D);
            Vec3 crown = preview.gridPoint(0.5D, 0.5D);
            Vec3 b = preview.gridPoint(0.5D, 1.0D);
            line(pose, lines, a, crown, 0.35F, 0.92F, 1.0F, 0.96F);
            line(pose, lines, crown, b, 0.35F, 0.92F, 1.0F, 0.96F);
        }
    }

    private static void renderLinkedSurfaceEdge(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface, int edge,
            float red, float green, float blue, float alpha) {
        Vec3 previous = TransformConstructionClientControls.linkedEdgePoint(
                surface, edge, 0.0D);
        for (int sample = 1; sample <= 48; sample++) {
            double t = sample / 48.0D;
            Vec3 current = TransformConstructionClientControls.linkedEdgePoint(
                    surface, edge, t);
            line(pose, lines, previous, current, red, green, blue, alpha);
            previous = current;
        }
    }

    private static void renderSurfaceOutline(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface, Vec3 camera) {
        Vec3 center = surface.gridPoint(0.5D, 0.5D);
        double radius = Math.max(surface.width(), surface.height()) * 0.75D + 2.0D;
        if (center.distanceToSqr(camera)
                > (192.0D + radius) * (192.0D + radius)) return;

        int widthSamples = Math.max(8, Math.min(48, surface.columns() * 2));
        int heightSamples = Math.max(4, Math.min(24, surface.rows() * 2));
        renderSurfaceEdge(pose, lines, surface, true, 0.0D, widthSamples);
        renderSurfaceEdge(pose, lines, surface, true, 1.0D, widthSamples);
        renderSurfaceEdge(pose, lines, surface, false, 0.0D, heightSamples);
        renderSurfaceEdge(pose, lines, surface, false, 1.0D, heightSamples);
    }

    private static void renderSurfaceEdge(PoseStack pose, VertexConsumer lines,
            ConstructionSurface surface, boolean horizontal, double fixed,
            int samples) {
        Vec3 previous = horizontal
                ? surface.gridPoint(0.0D, fixed)
                : surface.gridPoint(fixed, 0.0D);
        for (int sample = 1; sample <= samples; sample++) {
            double t = sample / (double) samples;
            Vec3 current = horizontal
                    ? surface.gridPoint(t, fixed)
                    : surface.gridPoint(fixed, t);
            line(pose, lines, previous, current,
                    0.12F, 1.0F, 0.28F, 0.72F);
            previous = current;
        }
    }

    private static void renderSurfaceMappingGuide(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface, Vec3 camera) {
        Vec3 center = surface.gridPoint(0.5D, 0.0D);
        if (center.distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) return;

        int samples = Math.max(8, Math.min(64, surface.columns() * 3));
        Vec3 previous = surface.gridPoint(0.0D, 0.0D);
        for (int sample = 1; sample <= samples; sample++) {
            double u = sample / (double) samples;
            Vec3 current = surface.gridPoint(u, 0.0D);
            line(pose, lines, previous, current,
                    1.0F, 0.80F, 0.15F, 0.92F);
            previous = current;
        }

        Vec3 normal = surface.gridNormal(0.5D, 0.0D).normalize();
        Vec3 interior = normal.scale(-1.0D);
        Vec3 insideEnd = center.add(interior.scale(0.72D));
        line(pose, lines, center, insideEnd,
                0.18F, 1.0F, 0.30F, 1.0F);

        Vec3 tangent = surface.gridTangent(0.5D, 0.0D).normalize();
        Vec3 left = insideEnd.subtract(interior.scale(0.18D))
                .add(tangent.scale(0.10D));
        Vec3 right = insideEnd.subtract(interior.scale(0.18D))
                .subtract(tangent.scale(0.10D));
        line(pose, lines, insideEnd, left,
                0.18F, 1.0F, 0.30F, 1.0F);
        line(pose, lines, insideEnd, right,
                0.18F, 1.0F, 0.30F, 1.0F);
    }

    private static void renderSurfaceGrid(PoseStack pose, VertexConsumer lines,
            ConstructionSurface surface, Vec3 camera) {
        if (surface.gridPoint(0.5D, 0.5D).distanceToSqr(camera)
                > MAX_RENDER_DISTANCE_SQR) return;
        Selection selected = TransformConstructionClientState.selection();
        boolean active = selected != null
                && selected.type() == SelectionType.SURFACE
                && surface.id().equals(selected.id());
        float red = active ? 1.0F : 0.12F;
        float green = active ? 0.82F : 1.0F;
        float blue = active ? 0.12F : 0.28F;
        int columns = surface.columns();
        int rows = surface.rows();
        boolean preview = TransformConstructionClientControls.previewingSurface(
                surface.id());
        // Guides need not draw hundreds of lines at editor distance.
        // Cap their cost even on first selection, before drag/preview starts.
        int columnStep = Math.max(1, (columns + (preview ? 11 : 23))
                / (preview ? 12 : 24));
        int rowStep = Math.max(1, (rows + (preview ? 7 : 11))
                / (preview ? 8 : 12));
        for (int column = 0; column <= columns; column += columnStep) {
            double u = column / (double) columns;
            Vec3 previous = visibleSurfaceGridPoint(surface, u, 0.0D,
                    camera);
            int samples = preview ? Math.max(4, Math.min(12, rows))
                    : Math.max(4, Math.min(40, rows * 2));
            for (int sample = 1; sample <= samples; sample++) {
                double v = sample / (double) samples;
                Vec3 current = visibleSurfaceGridPoint(surface, u, v,
                        camera);
                line(pose, lines, previous, current, red, green, blue, 0.84F);
                previous = current;
            }
        }
        for (int row = 0; row <= rows; row += rowStep) {
            double v = row / (double) rows;
            Vec3 previous = visibleSurfaceGridPoint(surface, 0.0D, v,
                    camera);
            int samples = preview ? Math.max(8, Math.min(24, columns))
                    : Math.max(8, Math.min(64, columns * 2));
            for (int sample = 1; sample <= samples; sample++) {
                double u = sample / (double) samples;
                Vec3 current = visibleSurfaceGridPoint(surface, u, v,
                        camera);
                line(pose, lines, previous, current, red, green, blue, 0.84F);
                previous = current;
            }
        }
        if (active) {
            renderAlignedBoundary(pose, lines, surface);
            renderSurfaceSides(pose, lines, surface);
            // Handle spheres and axes are rendered after the world so walls
            // never hide the controls that are intentionally selectable through it.
        }
    }

    private static Vec3 visibleSurfaceGridPoint(
            ConstructionSurface surface, double u, double v,
            Vec3 camera) {
        Vec3 point = surface.gridPoint(u, v);
        boolean border = Math.abs(u) < 1.0E-8D
                || Math.abs(u - 1.0D) < 1.0E-8D
                || Math.abs(v) < 1.0E-8D
                || Math.abs(v - 1.0D) < 1.0E-8D;
        if (border) {
            Vec3 joined = joinedSurfaceEdge(surface, u, v, 0.0D);
            if (joined != null) point = joined;
        }
        Vec3 normal = TransformMath.safeNormalize(surface.gridNormal(u, v),
                new Vec3(0.0D, 0.0D, 1.0D));
        double cameraSide = camera.subtract(point).dot(normal);
        // Draw just off the authored guide to avoid line z-fighting.
        // A placed block must never move the edit grid a whole cell.
        return point.add(normal.scale(cameraSide >= 0.0D
                ? 0.008D : -0.008D));
    }

    private static void renderAlignedBoundary(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface) {
        boundaryLine(pose, lines, surface.bottomStart(), surface.bottomEnd(),
                surface.curveOffset().lengthSqr() < 1.0E-8D);
        boundaryLine(pose, lines, surface.topStart(), surface.topEnd(),
                surface.curveOffset().lengthSqr() < 1.0E-8D);
        boundaryLine(pose, lines, surface.bottomStart(), surface.topStart(), true);
        boundaryLine(pose, lines, surface.bottomEnd(), surface.topEnd(), true);
    }

    private static void boundaryLine(PoseStack pose, VertexConsumer lines,
            Vec3 a, Vec3 b, boolean straight) {
        boolean aligned = straight && principalAxisAligned(b.subtract(a));
        line(pose, lines, a, b, aligned ? 0.20F : 1.0F,
                aligned ? 1.0F : 0.82F, aligned ? 0.28F : 0.12F, 1.0F);
    }

    private static boolean principalAxisAligned(Vec3 delta) {
        if (delta.lengthSqr() < 1.0E-8D) return false;
        Vec3 n = delta.normalize();
        double max = Math.max(Math.abs(n.x),
                Math.max(Math.abs(n.y), Math.abs(n.z)));
        return max >= 0.9995D;
    }

    private static void renderSurfaceSides(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface) {
        Vec3 center = surface.gridPoint(0.5D, 0.5D);
        Vec3 normal = surface.gridNormal(0.5D, 0.5D);
        Vec3 placement = center.add(normal.scale(0.55D));
        Vec3 interior = center.subtract(normal.scale(0.55D));
        line(pose, lines, center, placement, 1.0F, 0.30F, 0.18F, 0.95F);
        line(pose, lines, center, interior, 0.20F, 1.0F, 0.30F, 0.95F);
    }

    private static void renderSurfaceGizmo(PoseStack pose,
            VertexConsumer lines, ConstructionSurface surface,
            SurfaceHandle handle) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 origin = TransformConstructionClientControls.handlePosition(
                surface, handle);
        Axis hot = minecraft.player == null ? null
                : TransformConstructionClientControls.hoveredSurfaceGizmoAxis(
                        minecraft.player);
        for (Axis axis : Axis.values()) {
            if (surface.bridge() != null && axis != Axis.Y) continue;
            renderMoveAxis(pose, lines, origin,
                    TransformConstructionClientControls.gizmoAxisDirection(
                            TransformConstructionClientState.selection(), axis),
                    axis, axis == hot);
        }
    }

    private static void renderHandles(PoseStack pose, VertexConsumer lines,
            ConstructionSurface surface, SurfaceHandle selected) {
        UUID hoveredId = TransformConstructionClientState.hoveredSurfaceId();
        SurfaceHandle hovered = hoveredId != null
                && hoveredId.equals(surface.id())
                ? TransformConstructionClientState.hoveredSurfaceHandle() : null;
        for (SurfaceHandle handle : SurfaceHandle.values()) {
            if (surface.bridge() != null && handle != SurfaceHandle.CENTER) {
                continue;
            }
            Vec3 point = TransformConstructionClientControls.handlePosition(
                    surface, handle);
            boolean active = handle == selected;
            boolean hot = handle == hovered;
            float red = active ? 1.0F : hot ? 0.30F : 0.22F;
            float green = active ? 0.35F : hot ? 0.70F : 1.0F;
            float blue = active ? 0.18F : hot ? 1.0F : 0.18F;
            if (handle == SurfaceHandle.BOTTOM_EDGE
                    || handle == SurfaceHandle.TOP_EDGE
                    || handle == SurfaceHandle.START_EDGE
                    || handle == SurfaceHandle.END_EDGE) {
                Vec3 tangent = surface.gridTangent(0.5D, 0.5D);
                Vec3 vertical = surface.gridVertical(0.5D);
                double size = active ? 0.15D : 0.105D;
                Vec3 a = point.add(tangent.scale(size));
                Vec3 b = point.add(vertical.scale(size));
                Vec3 c = point.subtract(tangent.scale(size));
                Vec3 d = point.subtract(vertical.scale(size));
                line(pose, lines, a, b, red, green, blue, 1.0F);
                line(pose, lines, b, c, red, green, blue, 1.0F);
                line(pose, lines, c, d, red, green, blue, 1.0F);
                line(pose, lines, d, a, red, green, blue, 1.0F);
            } else {
                double size = active ? 0.13D : 0.09D;
                AABB box = new AABB(point.x - size, point.y - size,
                        point.z - size, point.x + size, point.y + size,
                        point.z + size);
                LevelRenderer.renderLineBox(pose, lines, box,
                        red, green, blue, 1.0F);
            }
        }
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 from,
            Vec3 to, float red, float green, float blue, float alpha) {
        Vec3 direction = to.subtract(from);
        if (direction.lengthSqr() < 1.0E-9D) return;
        direction = direction.normalize();
        PoseStack.Pose current = pose.last();
        int r = Math.round(red * 255.0F);
        int g = Math.round(green * 255.0F);
        int b = Math.round(blue * 255.0F);
        int a = Math.round(alpha * 255.0F);
        lines.vertex(current.pose(), (float) from.x, (float) from.y,
                        (float) from.z)
                .color(r, g, b, a)
                .normal(current.normal(), (float) direction.x,
                        (float) direction.y, (float) direction.z).endVertex();
        lines.vertex(current.pose(), (float) to.x, (float) to.y,
                        (float) to.z)
                .color(r, g, b, a)
                .normal(current.normal(), (float) direction.x,
                        (float) direction.y, (float) direction.z).endVertex();
    }

    private record VertexFrame(Vec3 position, Vec3 normal) {
    }

    private record PreparedVertex(BlockState state, Vec3 position, Vec3 normal,
            float u, float v, int red, int green, int blue,
            int fallbackLight) {
    }

    private record CachedSurface(ConstructionSurface surface,
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots,
            Map<ConstructionSurface.SurfaceOverlaySlot,
                    CachedSurfaceSlot> overlays,
            List<CachedSurfaceBatch> batches) {
    }

    private record CachedSurfaceBatch(
            Map<RenderType, List<PreparedVertex>> layers, AABB bounds) {
    }

    private record CachedSurfaceSlot(
            Map<RenderType, List<PreparedVertex>> layers, AABB bounds) {
    }

    private record CachedGroup(TransformGroup source,
            Map<TransformGroup.GridPos, BlockState> cells,
            List<CachedGroupBatch> batches) {
    }

    private record CachedGroupBatch(GroupBatchKey key,
            Vec3 localCenter,
            Map<TransformGroup.GridPos,
                    Map<RenderType, List<PreparedVertex>>> cells) {
    }

    private record GroupBatchKey(int x, int y, int z) {
        static GroupBatchKey of(TransformGroup.GridPos cell) {
            return new GroupBatchKey(
                    Math.floorDiv(cell.x(), GROUP_BATCH_SIZE),
                    Math.floorDiv(cell.y(), GROUP_BATCH_SIZE),
                    Math.floorDiv(cell.z(), GROUP_BATCH_SIZE));
        }

        Vec3 center() {
            double offset = (GROUP_BATCH_SIZE - 1) * 0.5D;
            return new Vec3(x * GROUP_BATCH_SIZE + offset,
                    y * GROUP_BATCH_SIZE + offset,
                    z * GROUP_BATCH_SIZE + offset);
        }
    }
}
