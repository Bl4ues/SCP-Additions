package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
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
import net.minecraft.world.level.block.state.BlockState;
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
    private static final double GROUP_BATCH_RADIUS =
            Math.sqrt(3.0D) * GROUP_BATCH_SIZE * 0.5D;
    private static final Direction[] SIDES = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, null
    };
    private static final Map<UUID, CachedSurface> SURFACE_MESHES =
            new HashMap<>();
    private static final Map<UUID, CachedGroup> GROUP_MESHES =
            new HashMap<>();

    private TransformConstructionClientRenderer() {
    }

    public static void clearSurfaceCache() {
        SURFACE_MESHES.clear();
        GROUP_MESHES.clear();
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
        for (ConstructionSurface surface : surfaces) {
            renderSurfacePayloads(minecraft, pose, buffers, surface, camera);
        }

        boolean offGridTool = minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getOffGridTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getOffGridTool());
        boolean surfaceTool = minecraft.player.getMainHandItem().is(
                TransformConstructionModule.getSurfaceTool())
                || minecraft.player.getOffhandItem().is(
                        TransformConstructionModule.getSurfaceTool());
        boolean mappingTool = minecraft.player.getMainHandItem().is(
                FacilityMappingItems.getTool())
                || minecraft.player.getOffhandItem().is(
                        FacilityMappingItems.getTool());
        Selection selection = TransformConstructionClientState.selection();
        boolean placingBlock = minecraft.player.getMainHandItem().getItem()
                instanceof BlockItem;
        boolean showSelectedGroup = placingBlock && selection != null
                && selection.type() == SelectionType.GROUP;
        boolean showSelectedSurface = placingBlock && selection != null
                && selection.type() == SelectionType.SURFACE;
        TransformGroupPlacementClient.Target placementTarget =
                placingBlock ? TransformGroupPlacementClient.findTarget(
                        minecraft.player) : null;
        TransformGroupPlacementClient.PayloadTarget interactionTarget =
                placingBlock ? null
                        : TransformGroupPlacementClient.findPayloadTarget(
                                minecraft.player);
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
                        interactionTarget.cell(),
                        0.78F, 0.93F, 1.0F, 0.96F);
            }
            if (surfaceTarget != null) {
                renderLogicalSurfaceSlot(pose, lines,
                        surfaceTarget.surface(), surfaceTarget.slot(),
                        0.24F, 1.0F, 0.38F, 0.98F);
            }
            if (surfaceTool) {
                for (ConstructionSurface surface : surfaces) {
                    if (selection != null
                            && selection.type() == SelectionType.SURFACE
                            && surface.id().equals(selection.id())) {
                        renderSurfaceGrid(pose, lines, surface, camera);
                    } else {
                        renderSurfaceOutline(pose, lines, surface, camera);
                    }
                }
            } else if (showSelectedSurface
                    && selection != null
                    && selection.type() == SelectionType.SURFACE) {
                ConstructionSurface selectedSurface =
                        TransformConstructionClientState.surface(selection.id());
                if (selectedSurface != null) {
                    renderSurfaceGrid(pose, lines, selectedSurface, camera);
                }
            } else if (placingBlock) {
                for (ConstructionSurface surface : surfaces) {
                    Vec3 center = surface.gridPoint(0.5D, 0.5D);
                    if (center.distanceToSqr(camera) <= 40.0D * 40.0D
                            && !surface.attachments().isEmpty()) {
                        renderSurfaceGrid(pose, lines, surface, camera);
                    }
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
        pose.popPose();
        buffers.endBatch();

        Set<UUID> current = surfaces.stream().map(ConstructionSurface::id)
                .collect(Collectors.toSet());
        SURFACE_MESHES.keySet().removeIf(id -> !current.contains(id));
        Set<UUID> currentGroups = groups.stream().map(TransformGroup::id)
                .collect(Collectors.toSet());
        GROUP_MESHES.keySet().removeIf(id -> !currentGroups.contains(id));
    }

    private static void renderGroup(Minecraft minecraft, PoseStack pose,
            MultiBufferSource.BufferSource buffers, TransformGroup group,
            Vec3 camera) {
        CachedGroup cached = GROUP_MESHES.get(group.id());
        if (cached == null) {
            cached = buildGroupMesh(minecraft, group);
            GROUP_MESHES.put(group.id(), cached);
        } else if (!cached.cells().equals(group.cells())) {
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
            for (Map.Entry<RenderType, List<PreparedVertex>> layer
                    : batch.layers().entrySet()) {
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
        pose.popPose();
    }

    private static CachedGroup updateGroupMesh(Minecraft minecraft,
            TransformGroup group, CachedGroup cached) {
        Set<GroupBatchKey> dirty = new java.util.HashSet<>();
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
            return new CachedGroup(Map.copyOf(group.cells()), cached.batches());
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
        return new CachedGroup(Map.copyOf(group.cells()),
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
        return new CachedGroup(Map.copyOf(group.cells()),
                List.copyOf(batches));
    }

    private static CachedGroupBatch buildGroupBatch(Minecraft minecraft,
            TransformGroup group, GroupBatchKey key) {
        Map<RenderType, List<PreparedVertex>> layers = new LinkedHashMap<>();
        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : group.cells().entrySet()) {
            TransformGroup.GridPos cell = entry.getKey();
            if (!key.equals(GroupBatchKey.of(cell))) continue;
            BlockState state = entry.getValue();
            if (state == null || state.isAir()
                    || state.getRenderShape() != RenderShape.MODEL) continue;

            BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
            RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
            List<PreparedVertex> output = layers.computeIfAbsent(renderType,
                    ignored -> new ArrayList<>());
            Vec3 center = group.cellCenter(cell);
            BlockPos lightPos = BlockPos.containing(center);
            int packedLight = minecraft.level.hasChunkAt(lightPos)
                    ? LevelRenderer.getLightColor(minecraft.level, state, lightPos)
                    : 0x00F000F0;
            RandomSource random = RandomSource.create(
                    42L ^ cell.hashCode() * 31L);
            for (Direction side : SIDES) {
                random.setSeed(42L ^ cell.hashCode() * 31L);
                for (BakedQuad quad : model.getQuads(state, side, random,
                        ModelData.EMPTY, null)) {
                    appendGroupQuad(minecraft, output, group, cell, state,
                            quad, lightPos, packedLight);
                }
            }
        }
        if (layers.isEmpty()) return null;
        Map<RenderType, List<PreparedVertex>> immutable = new LinkedHashMap<>();
        layers.forEach((type, vertices) ->
                immutable.put(type, List.copyOf(vertices)));
        return new CachedGroupBatch(key, key.center(), Map.copyOf(immutable));
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
            Vec3 camera) {
        Vec3 center = surface.gridPoint(0.5D, 0.5D);
        double radius = Math.max(surface.width(), surface.height()) * 0.75D + 2.0D;
        if (center.distanceToSqr(camera)
                > (192.0D + radius) * (192.0D + radius)) return;

        CachedSurface cached = SURFACE_MESHES.get(surface.id());
        if (cached == null || !sameSurfaceGeometry(cached.surface(), surface)) {
            cached = buildSurfaceMesh(minecraft, surface);
            SURFACE_MESHES.put(surface.id(), cached);
        } else if (!cached.surface().attachments().equals(
                surface.attachments())) {
            cached = updateSurfaceMesh(minecraft, surface, cached);
            SURFACE_MESHES.put(surface.id(), cached);
        }
        for (CachedSurfaceSlot slot : cached.slots().values()) {
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
        Set<ConstructionSurface.SurfaceSlot> dirty =
                new java.util.LinkedHashSet<>();
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

        for (ConstructionSurface.SurfaceSlot slot : dirty) {
            ConstructionSurface.SurfaceAttachment attachment =
                    surface.attachments().get(slot);
            if (attachment == null || attachment.state().isAir()) {
                slots.remove(slot);
                continue;
            }
            CachedSurfaceSlot rebuilt = buildSurfaceSlot(minecraft, surface,
                    slot, attachment);
            if (rebuilt == null) slots.remove(slot);
            else slots.put(slot, rebuilt);
        }
        return new CachedSurface(surface, Map.copyOf(slots));
    }

    private static CachedSurface buildSurfaceMesh(Minecraft minecraft,
            ConstructionSurface surface) {
        Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots =
                new LinkedHashMap<>();
        for (Map.Entry<ConstructionSurface.SurfaceSlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.attachments().entrySet()) {
            CachedSurfaceSlot slot = buildSurfaceSlot(minecraft, surface,
                    entry.getKey(), entry.getValue());
            if (slot != null) slots.put(entry.getKey(), slot);
        }
        return new CachedSurface(surface, Map.copyOf(slots));
    }

    private static CachedSurfaceSlot buildSurfaceSlot(Minecraft minecraft,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment) {
        BlockState state = attachment.state();
        if (state == null || state.isAir()
                || state.getRenderShape() != RenderShape.MODEL) return null;
        Map<RenderType, List<PreparedVertex>> layers = new LinkedHashMap<>();
        RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
        List<PreparedVertex> output = layers.computeIfAbsent(renderType,
                ignored -> new ArrayList<>());
        appendSurfaceBlock(minecraft, output, surface, slot, attachment);
        Map<RenderType, List<PreparedVertex>> immutable = new LinkedHashMap<>();
        layers.forEach((type, vertices) ->
                immutable.put(type, List.copyOf(vertices)));
        return new CachedSurfaceSlot(Map.copyOf(immutable));
    }

    private static void appendSurfaceBlock(Minecraft minecraft,
            List<PreparedVertex> output, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment) {
        BlockState state = attachment.state();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create(42L);
        BlockPos lightPos = BlockPos.containing(surface.gridPoint(
                (slot.column() + 0.5D) / surface.columns(),
                (slot.row() + 0.5D) / surface.rows()));
        int packedLight = LevelRenderer.getLightColor(minecraft.level, state,
                lightPos);
        for (Direction side : SIDES) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, side, random,
                    ModelData.EMPTY, null)) {
                prepareQuad(minecraft, output, surface, slot, attachment, quad,
                        lightPos, packedLight);
            }
        }
    }

    private static void prepareQuad(Minecraft minecraft,
            List<PreparedVertex> output, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment, BakedQuad quad,
            BlockPos lightPos, int fallbackLight) {
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

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float u = stride > 4 ? Float.intBitsToFloat(vertices[offset + 4]) : 0.0F;
            float v = stride > 5 ? Float.intBitsToFloat(vertices[offset + 5]) : 0.0F;
            VertexFrame frame = attachment.deform()
                    ? deformedFrame(surface, slot, x, y, z, quadNormal)
                    : rigidFrame(surface, slot, x, y, z, quadNormal);
            output.add(new PreparedVertex(attachment.state(), frame.position(),
                    frame.normal(), u, v, red, green, blue, fallbackLight));
        }
    }

    private static VertexFrame deformedFrame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double x, double y, double z,
            Vec3 localNormal) {
        double localX = surface.flipped() ? 1.0D - x : x;
        double u = (slot.column() + localX) / surface.columns();
        double v = (slot.row() + y) / surface.rows();
        Vec3 tangent = surface.gridFrameTangent(u, v);
        Vec3 normal = surface.gridNormal(u, v);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));
        Vec3 position = surface.gridPoint(u, v).add(normal.scale(z));
        Vec3 transformedNormal = TransformMath.safeNormalize(
                tangent.scale(localNormal.x)
                        .add(vertical.scale(localNormal.y))
                        .add(normal.scale(localNormal.z)), normal);
        return new VertexFrame(position, transformedNormal);
    }

    private static VertexFrame rigidFrame(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, double x, double y, double z,
            Vec3 localNormal) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 tangent = surface.gridFrameTangent(u, v);
        Vec3 normal = surface.gridNormal(u, v);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));
        Vec3 position = surface.gridPoint(u, v)
                .add(tangent.scale(x - 0.5D))
                .add(vertical.scale(y - 0.5D))
                .add(normal.scale(z));
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
        Selection selection = TransformConstructionClientState.selection();
        if (selection != null && selection.type() == SelectionType.SURFACE) {
            ConstructionSurface surface =
                    TransformConstructionClientState.surface(selection.id());
            if (surface != null) {
                return TransformSurfaceRaycast.target(player, surface);
            }
        }
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
                    Vec3 local = new Vec3(cell.x() + xi - 0.5D,
                            cell.y() + yi - 0.5D,
                            cell.z() + zi - 0.5D);
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
                        Vec3 local = new Vec3(cell.x() + (xi - 0.5D),
                                cell.y() + (yi - 0.5D),
                                cell.z() + (zi - 0.5D));
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
        if (active) renderGroupGizmo(pose, lines, group);
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
        for (int column = 0; column <= columns; column++) {
            double u = column / (double) columns;
            Vec3 previous = surface.gridPoint(u, 0.0D);
            int samples = Math.max(4, rows * 2);
            for (int sample = 1; sample <= samples; sample++) {
                double v = sample / (double) samples;
                Vec3 current = surface.gridPoint(u, v);
                line(pose, lines, previous, current, red, green, blue, 0.84F);
                previous = current;
            }
        }
        for (int row = 0; row <= rows; row++) {
            double v = row / (double) rows;
            Vec3 previous = surface.gridPoint(0.0D, v);
            int samples = Math.max(8, columns * 3);
            for (int sample = 1; sample <= samples; sample++) {
                double u = sample / (double) samples;
                Vec3 current = surface.gridPoint(u, v);
                line(pose, lines, previous, current, red, green, blue, 0.84F);
                previous = current;
            }
        }
        if (active) {
            renderAlignedBoundary(pose, lines, surface);
            renderHandles(pose, lines, surface, selected.handle());
            renderSurfaceGizmo(pose, lines, surface, selected.handle());
            renderSurfaceSides(pose, lines, surface);
        }
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
            Map<ConstructionSurface.SurfaceSlot, CachedSurfaceSlot> slots) {
    }

    private record CachedSurfaceSlot(
            Map<RenderType, List<PreparedVertex>> layers) {
    }

    private record CachedGroup(
            Map<TransformGroup.GridPos, BlockState> cells,
            List<CachedGroupBatch> batches) {
    }

    private record CachedGroupBatch(GroupBatchKey key,
            Vec3 localCenter,
            Map<RenderType, List<PreparedVertex>> layers) {
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
