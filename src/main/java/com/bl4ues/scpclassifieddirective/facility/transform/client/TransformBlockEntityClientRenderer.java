package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Visual bridge for authored BlockEntity blocks placed on transformed geometry.
 * The renderer receives the exact local transform while the virtual BlockEntity
 * remains a render host only. Gameplay/ticking that depends on a vanilla
 * BlockPos still requires an explicit transformed runtime adapter.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformBlockEntityClientRenderer {
    private static final double MAX_DISTANCE_SQR = 128.0D * 128.0D;
    private static final Map<CellKey, RenderHost> GROUP_HOSTS = new HashMap<>();
    private static final Map<SurfaceKey, RenderHost> SURFACE_HOSTS =
            new HashMap<>();

    private TransformBlockEntityClientRenderer() {
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            GROUP_HOSTS.clear();
            SURFACE_HOSTS.clear();
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers()
                .bufferSource();
        var dimension = minecraft.level.dimension().location();
        var groups = TransformConstructionClientState.groups(dimension);
        var surfaces = TransformConstructionClientState.surfaces(dimension);

        for (TransformGroup group : groups) {
            renderGroup(minecraft, event, pose, buffers, camera, group);
        }
        for (ConstructionSurface surface : surfaces) {
            renderSurface(minecraft, event, pose, buffers, camera, surface);
        }

        Set<CellKey> currentGroups = groups.stream().flatMap(group ->
                group.cells().entrySet().stream()
                        .filter(entry -> renderableEntity(entry.getValue()))
                        .map(entry -> new CellKey(group.id(), entry.getKey())))
                .collect(Collectors.toSet());
        GROUP_HOSTS.keySet().removeIf(key -> !currentGroups.contains(key));

        Set<SurfaceKey> currentSurfaces = surfaces.stream().flatMap(surface ->
                surface.attachments().entrySet().stream()
                        .filter(entry -> renderableEntity(
                                entry.getValue().state()))
                        .map(entry -> new SurfaceKey(surface.id(),
                                entry.getKey())))
                .collect(Collectors.toSet());
        SURFACE_HOSTS.keySet().removeIf(key -> !currentSurfaces.contains(key));
    }

    private static void renderGroup(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            TransformGroup group) {
        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : group.cells().entrySet()) {
            BlockState state = entry.getValue();
            if (!renderableEntity(state)) continue;
            EntityBlock entityBlock = (EntityBlock) state.getBlock();
            TransformGroup.GridPos cell = entry.getKey();
            Vec3 center = group.cellCenter(cell);
            if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;
            CellKey key = new CellKey(group.id(), cell);
            RenderHost host = host(minecraft, GROUP_HOSTS.get(key), entityBlock,
                    state, center);
            if (host == null) continue;
            GROUP_HOSTS.put(key, host);

            pose.pushPose();
            pose.translate(-camera.x, -camera.y, -camera.z);
            pose.translate(group.origin().x, group.origin().y,
                    group.origin().z);
            pose.mulPose(TransformMath.quaternion(group.rotationX(),
                    group.rotationY(), group.rotationZ()));
            pose.translate(cell.x() - 0.5D, cell.y() - 0.5D,
                    cell.z() - 0.5D);
            minecraft.getBlockEntityRenderDispatcher().render(host.entity(),
                    event.getPartialTick(), pose, buffers);
            pose.popPose();
        }
    }

    private static void renderSurface(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            ConstructionSurface surface) {
        for (Map.Entry<ConstructionSurface.SurfaceSlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.attachments().entrySet()) {
            BlockState state = entry.getValue().state();
            if (!renderableEntity(state)) continue;
            EntityBlock entityBlock = (EntityBlock) state.getBlock();
            ConstructionSurface.SurfaceSlot slot = entry.getKey();
            double u = (slot.column() + 0.5D) / surface.columns();
            double v = (slot.row() + 0.5D) / surface.rows();
            Vec3 center = surface.gridPoint(u, v);
            if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;
            SurfaceKey key = new SurfaceKey(surface.id(), slot);
            RenderHost host = host(minecraft, SURFACE_HOSTS.get(key), entityBlock,
                    state, center);
            if (host == null) continue;
            SURFACE_HOSTS.put(key, host);

            Vec3 tangent = surface.gridTangent(u, v);
            Vec3 normal = surface.gridNormal(u, v);
            Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                    surface.gridVertical(u));
            pose.pushPose();
            pose.translate(-camera.x, -camera.y, -camera.z);
            pose.translate(center.x, center.y, center.z);
            pose.mulPose(TransformMath.frameQuaternion(tangent, vertical, normal));
            pose.translate(-0.5D, -0.5D, -0.5D);
            minecraft.getBlockEntityRenderDispatcher().render(host.entity(),
                    event.getPartialTick(), pose, buffers);
            pose.popPose();
        }
    }

    private static RenderHost host(Minecraft minecraft, RenderHost current,
            EntityBlock entityBlock, BlockState state, Vec3 center) {
        if (current != null && current.state().equals(state)
                && current.entity().getLevel() == minecraft.level) {
            return minecraft.getBlockEntityRenderDispatcher().getRenderer(
                    current.entity()) == null ? null : current;
        }
        BlockEntity entity = entityBlock.newBlockEntity(
                BlockPos.containing(center), state);
        if (entity == null) return null;
        entity.setLevel(minecraft.level);
        if (minecraft.getBlockEntityRenderDispatcher().getRenderer(entity) == null) {
            return null;
        }
        return new RenderHost(state, entity);
    }

    private static boolean renderableEntity(BlockState state) {
        return state != null && !state.isAir()
                && !AlarmModule.isController(state)
                && state.getBlock() instanceof EntityBlock;
    }

    private record CellKey(UUID groupId, TransformGroup.GridPos cell) {
    }

    private record SurfaceKey(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
    }

    private record RenderHost(BlockState state, BlockEntity entity) {
    }
}
