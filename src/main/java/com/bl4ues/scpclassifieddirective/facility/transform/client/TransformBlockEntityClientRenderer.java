package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformCameraGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurveillanceRuntime;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
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
    private static final Map<BlockEntity, Long> VIRTUAL_CAMERA_TICKS =
            new WeakHashMap<>();
    private static final Map<UUID, VirtualCameraPose> VIRTUAL_CAMERA_POSES =
            new HashMap<>();
    private static final ThreadLocal<TransformCameraGeometry.Frame>
            ACTIVE_CAMERA_FRAME = new ThreadLocal<>();

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
            VIRTUAL_CAMERA_POSES.clear();
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
        Set<SurfaceKey> currentSurfaces = new HashSet<>();
        for (ConstructionSurface surface : surfaces) {
            renderSurface(minecraft, event, pose, buffers, camera, surface,
                    currentSurfaces);
        }

        Set<CellKey> currentGroups = groups.stream().flatMap(group ->
                group.cells().entrySet().stream()
                        .filter(entry -> renderableEntity(entry.getValue()))
                        .map(entry -> new CellKey(group.id(), entry.getKey())))
                .collect(Collectors.toSet());
        GROUP_HOSTS.keySet().removeIf(key -> !currentGroups.contains(key));
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

            TransformCameraGeometry.Frame cameraFrame =
                    TransformCameraGeometry.frame(group, cell, state);
            UUID cameraId = cameraFrame == null ? null
                    : TransformSurveillanceRuntime.groupCameraId(
                            group.id(), cell);
            tickVirtualCamera(minecraft, host.entity(), state, cameraId);

            pose.pushPose();
            pose.translate(-camera.x, -camera.y, -camera.z);
            pose.translate(group.origin().x, group.origin().y,
                    group.origin().z);
            pose.mulPose(TransformMath.quaternion(group.rotationX(),
                    group.rotationY(), group.rotationZ()));
            pose.translate(cell.x() - 0.5D, cell.y() - 0.5D,
                    cell.z() - 0.5D);
            if (cameraFrame != null) ACTIVE_CAMERA_FRAME.set(cameraFrame);
            try {
                minecraft.getBlockEntityRenderDispatcher().render(host.entity(),
                        event.getPartialTick(), pose, buffers);
            } finally {
                if (cameraFrame != null) ACTIVE_CAMERA_FRAME.remove();
                pose.popPose();
            }
        }
    }

    private static void renderSurface(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            ConstructionSurface surface, Set<SurfaceKey> current) {
        for (Map.Entry<ConstructionSurface.SurfaceSlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.attachments().entrySet()) {
            renderSurfaceAttachment(minecraft, event, pose, buffers, camera,
                    surface, entry.getKey(), entry.getValue(),
                    TransformSurfaceGeometry.MAIN_SIDE, false, current);
        }
        for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.overlays().entrySet()) {
            // Overlay BlockEntities historically have their own adapters.
            // Cameras are the deliberate exception introduced here: they need
            // the generic Surface frame because their physical lens and feed
            // share that exact transform.
            if (!TransformCameraGeometry.isCamera(entry.getValue().state())) {
                continue;
            }
            renderSurfaceAttachment(minecraft, event, pose, buffers, camera,
                    surface, entry.getKey().slot(), entry.getValue(),
                    entry.getKey().normalSign(), true, current);
        }
    }

    private static void renderSurfaceAttachment(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment, int normalSign,
            boolean overlay, Set<SurfaceKey> current) {
        BlockState state = attachment.state();
        if (!renderableEntity(state)) return;
        EntityBlock entityBlock = (EntityBlock) state.getBlock();
        TransformCameraGeometry.Frame cameraFrame =
                TransformCameraGeometry.frame(surface, slot, state,
                        normalSign, overlay);
        Vec3 center = cameraFrame != null ? cameraFrame.center()
                : TransformSurfaceGeometry.cellCenter(surface, slot,
                        normalSign, overlay);
        if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;

        SurfaceKey key = new SurfaceKey(surface.id(), slot,
                normalSign < 0 ? -1 : 1, overlay);
        current.add(key);
        RenderHost host = host(minecraft, SURFACE_HOSTS.get(key), entityBlock,
                state, center);
        if (host == null) return;
        SURFACE_HOSTS.put(key, host);
        int cameraLayer = !overlay ? 0 : normalSign < 0 ? -1 : 1;
        UUID cameraId = cameraFrame == null ? null
                : TransformSurveillanceRuntime.cameraId(
                        surface.id(), slot, cameraLayer);
        tickVirtualCamera(minecraft, host.entity(), state, cameraId);

        Vec3 xAxis;
        Vec3 yAxis;
        Vec3 zAxis;
        if (cameraFrame != null) {
            xAxis = cameraFrame.xAxis();
            yAxis = cameraFrame.yAxis();
            zAxis = cameraFrame.zAxis();
        } else {
            int side = normalSign < 0 ? -1 : 1;
            double u = (slot.column() + 0.5D) / surface.columns();
            double v = (slot.row() + 0.5D) / surface.rows();
            zAxis = surface.gridNormal(u, v).scale(side).normalize();
            xAxis = surface.gridFrameTangent(u, v).scale(side).normalize();
            yAxis = TransformMath.safeNormalize(zAxis.cross(xAxis),
                    surface.gridVertical(u, v));
        }

        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        pose.translate(center.x, center.y, center.z);
        pose.mulPose(TransformMath.frameQuaternion(xAxis, yAxis, zAxis));
        pose.translate(-0.5D, -0.5D, -0.5D);
        if (cameraFrame != null) ACTIVE_CAMERA_FRAME.set(cameraFrame);
        try {
            minecraft.getBlockEntityRenderDispatcher().render(host.entity(),
                    event.getPartialTick(), pose, buffers);
        } finally {
            if (cameraFrame != null) ACTIVE_CAMERA_FRAME.remove();
            pose.popPose();
        }
    }

    private static void tickVirtualCamera(Minecraft minecraft,
            BlockEntity entity, BlockState state, UUID cameraId) {
        long tick = minecraft.level == null ? Long.MIN_VALUE
                : minecraft.level.getGameTime();
        if (VIRTUAL_CAMERA_TICKS.getOrDefault(entity, Long.MIN_VALUE) == tick) {
            return;
        }
        VIRTUAL_CAMERA_TICKS.put(entity, tick);
        VirtualCameraPose pose = cameraId == null ? null
                : VIRTUAL_CAMERA_POSES.get(cameraId);
        if (entity instanceof SurveillanceCameraPlaceholderModule
                .SurveillanceCameraBlockEntity wall) {
            if (pose != null) {
                wall.applyVirtualControl(pose.controlled(),
                        pose.yaw(), pose.pitch());
            }
            SurveillanceCameraPlaceholderModule.tickVirtualClient(
                    minecraft.level, entity.getBlockPos(), state, wall);
        } else if (entity instanceof CeilingCameraModule
                .CeilingCameraBlockEntity ceiling) {
            if (pose != null) {
                ceiling.applyVirtualControl(pose.controlled(),
                        pose.yaw(), pose.pitch());
            }
            CeilingCameraModule.tickVirtualClient(
                    minecraft.level, entity.getBlockPos(), state, ceiling);
        }
    }

    public static void applyVirtualCameraPose(UUID cameraId,
            boolean controlled, float yaw, float pitch) {
        if (cameraId == null) return;
        VIRTUAL_CAMERA_POSES.put(cameraId,
                new VirtualCameraPose(controlled, yaw, pitch));
    }

    public static TransformCameraGeometry.Frame activeCameraFrame() {
        return ACTIVE_CAMERA_FRAME.get();
    }

    public static TransformCameraGeometry.Angles virtualCameraPose(
            UUID cameraId) {
        if (cameraId == null) return null;
        VirtualCameraPose pose = VIRTUAL_CAMERA_POSES.get(cameraId);
        return pose == null ? null
                : new TransformCameraGeometry.Angles(
                        pose.yaw(), pose.pitch());
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
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            boolean overlay) {
    }

    private record RenderHost(BlockState state, BlockEntity entity) {
    }

    private record VirtualCameraPose(boolean controlled, float yaw,
            float pitch) {
    }
}
