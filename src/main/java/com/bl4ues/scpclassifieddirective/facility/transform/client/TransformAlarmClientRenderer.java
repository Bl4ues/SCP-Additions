package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.AlarmClient;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
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
 * Reuses the real Alarm BlockEntityRenderer for Alarm states stored in
 * transformed construction. The BlockEntity exists only as a client render
 * host; authoritative active state, sound and light come from
 * TransformAlarmRuntime.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformAlarmClientRenderer {
    private static final double MAX_DISTANCE_SQR = 96.0D * 96.0D;
    private static final Map<CellKey, AlarmModule.AlarmBlockEntity> GROUP_HOSTS =
            new HashMap<>();
    private static final Map<SurfaceKey, AlarmModule.AlarmBlockEntity>
            SURFACE_HOSTS = new HashMap<>();

    private TransformAlarmClientRenderer() {
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

        Set<CellKey> groupKeys = groups.stream().flatMap(group ->
                group.cells().entrySet().stream()
                        .filter(entry -> AlarmModule.isController(entry.getValue()))
                        .map(entry -> new CellKey(group.id(), entry.getKey())))
                .collect(Collectors.toSet());
        GROUP_HOSTS.keySet().removeIf(key -> !groupKeys.contains(key));

        Set<SurfaceKey> surfaceKeys = new java.util.HashSet<>();
        for (ConstructionSurface surface : surfaces) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (AlarmModule.isController(entry.getValue().state())) {
                    surfaceKeys.add(new SurfaceKey(surface.id(),
                            entry.getKey(), 1));
                }
            }
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                if (AlarmModule.isController(entry.getValue().state())) {
                    surfaceKeys.add(new SurfaceKey(surface.id(),
                            entry.getKey().slot(),
                            entry.getKey().normalSign() < 0 ? -1 : 1));
                }
            }
        }
        SURFACE_HOSTS.keySet().removeIf(key -> !surfaceKeys.contains(key));
    }

    private static void renderGroup(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            TransformGroup group) {
        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : group.cells().entrySet()) {
            BlockState state = entry.getValue();
            if (state == null || !AlarmModule.isController(state)) continue;
            TransformGroup.GridPos cell = entry.getKey();
            Vec3 center = group.cellCenter(cell);
            if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;

            CellKey key = new CellKey(group.id(), cell);
            AlarmModule.AlarmBlockEntity alarm = host(minecraft,
                    GROUP_HOSTS.get(key), center, state);
            if (alarm == null) continue;
            GROUP_HOSTS.put(key, alarm);

            pose.pushPose();
            pose.translate(-camera.x, -camera.y, -camera.z);
            pose.translate(group.origin().x, group.origin().y,
                    group.origin().z);
            pose.mulPose(TransformMath.quaternion(group.rotationX(),
                    group.rotationY(), group.rotationZ()));
            pose.translate(cell.x() - 0.5D, cell.y() - 0.5D,
                    cell.z() - 0.5D);
            var renderer = minecraft.getBlockEntityRenderDispatcher()
                    .getRenderer(alarm);
            if (renderer instanceof AlarmClient.BlockRenderer alarmRenderer) {
                alarmRenderer.renderTransformed(alarm, event.getPartialTick(),
                        pose, buffers, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            }
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
            renderSurfaceAlarm(minecraft, event, pose, buffers, camera,
                    surface, entry.getKey(), 1, entry.getValue().state());
        }
        for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                ConstructionSurface.SurfaceAttachment> entry
                : surface.overlays().entrySet()) {
            renderSurfaceAlarm(minecraft, event, pose, buffers, camera,
                    surface, entry.getKey().slot(),
                    entry.getKey().normalSign(), entry.getValue().state());
        }
    }

    private static void renderSurfaceAlarm(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            BlockState state) {
        if (state == null || !AlarmModule.isController(state)) return;
        int side = normalSign < 0 ? -1 : 1;
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 normal = surface.gridNormal(u, v).scale(side);
        Vec3 center = surface.gridPoint(u, v).add(normal.scale(0.5D));
        if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;

        SurfaceKey key = new SurfaceKey(surface.id(), slot, side);
        AlarmModule.AlarmBlockEntity alarm = host(minecraft,
                SURFACE_HOSTS.get(key), center, state);
        if (alarm == null) return;
        SURFACE_HOSTS.put(key, alarm);

        Vec3 tangent = surface.gridFrameTangent(u, v).scale(side);
        Vec3 vertical = TransformMath.safeNormalize(normal.cross(tangent),
                surface.gridVertical(u, v));
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        pose.translate(center.x, center.y, center.z);
        pose.mulPose(TransformMath.frameQuaternion(tangent, vertical, normal));
        pose.translate(-0.5D, -0.5D, -0.5D);
        var renderer = minecraft.getBlockEntityRenderDispatcher()
                .getRenderer(alarm);
        if (renderer instanceof AlarmClient.BlockRenderer alarmRenderer) {
            alarmRenderer.renderTransformed(alarm, event.getPartialTick(),
                    pose, buffers,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture
                            .NO_OVERLAY);
        }
        pose.popPose();
    }

    private static AlarmModule.AlarmBlockEntity host(Minecraft minecraft,
            AlarmModule.AlarmBlockEntity current, Vec3 center,
            BlockState state) {
        BlockPos hostPos = BlockPos.containing(center);
        if (current != null && current.getLevel() == minecraft.level
                && current.getBlockPos().equals(hostPos)
                && current.getBlockState().equals(state)) {
            return current;
        }
        AlarmModule.AlarmBlockEntity alarm = new AlarmModule.AlarmBlockEntity(
                hostPos, state);
        alarm.setLevel(minecraft.level);
        return minecraft.getBlockEntityRenderDispatcher().getRenderer(alarm) == null
                ? null : alarm;
    }

    private record CellKey(UUID groupId, TransformGroup.GridPos cell) {
    }

    private record SurfaceKey(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
    }
}
