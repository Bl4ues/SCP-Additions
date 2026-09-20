package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.AlarmClient;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
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

    private static final Map<UUID, Set<TransformGroup.GridPos>> GROUP_ALARMS =
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

    // Construction meshes flush their depth at the default event priority.
    // Draw the translucent projection afterwards so both vanilla and
    // shader pipelines test it against the physical backing wall.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            resetIndices();
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

        // Rendering examines only Alarm addresses; updating an animated door
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

    private static void renderGroup(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            TransformGroup group) {
        for (TransformGroup.GridPos cell : groupAlarms(group)) {
            BlockState state = group.cells().get(cell);
            if (state == null || !AlarmModule.isController(state)) continue;
            Vec3 center = group.cellCenter(cell);
            if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) continue;
            // The render index is already restricted to Alarm controllers. A
            // visible active fixture also refreshes its audio key, covering
            // missed/late state deltas without scanning an entire facility.
            TransformAlarmAudioClient.groupCellChanged(group.id(), cell, state);

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
                Vec3 localCamera = TransformMath.worldToLocal(group.origin(),
                        camera, group.rotationX(), group.rotationY(),
                        group.rotationZ()).subtract(
                                cell.x() - 0.5D, cell.y() - 0.5D,
                                cell.z() - 0.5D);
                alarmRenderer.renderTransformed(alarm, event.getPartialTick(),
                        pose, buffers,
                        net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        localCamera);
            }
            pose.popPose();
            if (state.getValue(AlarmModule.ACTIVE)) {
                TransformAlarmPhysicalProjection.renderGroup(group, cell,
                        alarm, event.getPartialTick(), pose, buffers, camera);
            }
        }
    }

    private static void renderSurface(Minecraft minecraft,
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

    private static void renderSurfaceAlarm(Minecraft minecraft,
            RenderLevelStageEvent event, PoseStack pose,
            MultiBufferSource.BufferSource buffers, Vec3 camera,
            ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            boolean overlay, BlockState state) {
        if (state == null || !AlarmModule.isController(state)) return;
        int side = normalSign < 0 ? -1 : 1;
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        Vec3 normal = surface.gridNormal(u, v).scale(side);
        Vec3 center = TransformSurfaceGeometry.cellCenter(surface, slot,
                side, overlay);
        if (center.distanceToSqr(camera) > MAX_DISTANCE_SQR) return;
        TransformAlarmAudioClient.surfaceChanged(surface.id(), slot,
                overlay ? side : 0, state);

        SurfaceKey key = new SurfaceKey(surface.id(), slot, side, overlay);
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
            Vec3 delta = camera.subtract(center);
            Vec3 localCamera = new Vec3(
                    delta.dot(tangent) + 0.5D,
                    delta.dot(vertical) + 0.5D,
                    delta.dot(normal) + 0.5D);
            alarmRenderer.renderTransformed(alarm, event.getPartialTick(),
                    pose, buffers,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    net.minecraft.client.renderer.texture.OverlayTexture
                            .NO_OVERLAY, localCamera);
        }
        pose.popPose();
        if (state.getValue(AlarmModule.ACTIVE)) {
            TransformAlarmPhysicalProjection.renderSurface(surface, slot,
                    normalSign, overlay, alarm, event.getPartialTick(),
                    pose, buffers, camera);
        }
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
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            boolean overlay) {
    }
}
