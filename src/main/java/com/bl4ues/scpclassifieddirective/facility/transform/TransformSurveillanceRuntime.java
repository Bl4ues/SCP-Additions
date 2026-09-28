package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079ProcessingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraViewGeometry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraViewGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server authority for surveillance cameras stored in transformed construction.
 *
 * Both rigid Off-Grid groups and curved Surfaces use the same physical camera
 * frame for registry, room association, autonomous tracking, playable control
 * and the virtual BlockEntity rendered on the client.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformSurveillanceRuntime {
    private static final Map<ServerLevel, Map<UUID, CameraPose>> CAMERA_POSES =
            new WeakHashMap<>();

    private TransformSurveillanceRuntime() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)) {
            return;
        }
        syncCameraPoses(level);
    }

    public static void refreshGroupCell(MinecraftServer server, UUID groupId,
            TransformGroup.GridPos cell) {
        if (server == null || groupId == null || cell == null) return;
        TransformGroup group = TransformConstructionSavedData.get(server)
                .group(groupId);
        if (group == null) return;
        ServerLevel level = level(server, group.dimension());
        if (level == null) return;
        sync(level, group, cell);
    }

    public static void refreshGroup(MinecraftServer server, UUID groupId) {
        if (server == null || groupId == null) return;
        TransformGroup group = TransformConstructionSavedData.get(server)
                .group(groupId);
        if (group == null) return;
        ServerLevel level = level(server, group.dimension());
        if (level == null) return;
        for (TransformGroup.GridPos cell : group.cells().keySet()) {
            sync(level, group, cell);
        }
    }

    public static void unregisterGroup(MinecraftServer server,
            TransformGroup group) {
        if (server == null || group == null) return;
        ServerLevel level = level(server, group.dimension());
        if (level == null) return;
        for (TransformGroup.GridPos cell : group.cells().keySet()) {
            FacilitySurveillanceRegistry.unregister(level,
                    groupCameraId(group.id(), cell));
        }
    }

    public static UUID groupCameraId(UUID groupId,
            TransformGroup.GridPos cell) {
        String key = ScpClassifiedDirectiveMod.MODID + ":group_camera:"
                + groupId + ":" + cell.x() + ":" + cell.y() + ":" + cell.z();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    public static void refreshSurfaceSlot(MinecraftServer server,
            UUID surfaceId, ConstructionSurface.SurfaceSlot slot) {
        if (server == null || surfaceId == null || slot == null) return;
        ConstructionSurface surface = TransformConstructionSavedData.get(server)
                .surface(surfaceId);
        if (surface == null) return;
        ServerLevel level = level(server, surface.dimension());
        if (level == null) return;
        sync(level, surface, slot, 0, false);
        sync(level, surface, slot, -1, true);
        sync(level, surface, slot, 1, true);
    }

    public static void refreshSurface(MinecraftServer server, UUID surfaceId) {
        if (server == null || surfaceId == null) return;
        ConstructionSurface surface = TransformConstructionSavedData.get(server)
                .surface(surfaceId);
        if (surface == null) return;
        for (int row = 0; row < surface.rows(); row++) {
            for (int column = 0; column < surface.columns(); column++) {
                refreshSurfaceSlot(server, surfaceId,
                        new ConstructionSurface.SurfaceSlot(column, row));
            }
        }
    }

    public static void unregisterSurface(MinecraftServer server,
            ConstructionSurface surface) {
        if (server == null || surface == null) return;
        ServerLevel level = level(server, surface.dimension());
        if (level == null) return;
        for (int row = 0; row < surface.rows(); row++) {
            for (int column = 0; column < surface.columns(); column++) {
                ConstructionSurface.SurfaceSlot slot =
                        new ConstructionSurface.SurfaceSlot(column, row);
                for (int layer : new int[]{0, -1, 1}) {
                    FacilitySurveillanceRegistry.unregister(level,
                            cameraId(surface.id(), slot, layer));
                }
            }
        }
    }

    public static UUID cameraId(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int layer) {
        String key = ScpClassifiedDirectiveMod.MODID + ":surface_camera:"
                + surfaceId + ":" + slot.column() + ":" + slot.row()
                + ":" + layer;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static void sync(ServerLevel level, TransformGroup group,
            TransformGroup.GridPos cell) {
        UUID id = groupCameraId(group.id(), cell);
        BlockState state = group.cells().get(cell);
        if (!TransformCameraGeometry.isCamera(state)) {
            FacilitySurveillanceRegistry.unregister(level, id);
            return;
        }
        register(level, id, state,
                TransformCameraGeometry.frame(group, cell, state));
    }

    private static void sync(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot, int layer, boolean overlay) {
        int normalSign = layer == 0 ? TransformSurfaceGeometry.MAIN_SIDE : layer;
        ConstructionSurface.SurfaceAttachment attachment = layer == 0
                ? surface.attachments().get(slot)
                : surface.overlay(slot, layer);
        UUID id = cameraId(surface.id(), slot, layer);
        BlockState state = attachment == null ? null : attachment.state();
        if (!TransformCameraGeometry.isCamera(state)) {
            FacilitySurveillanceRegistry.unregister(level, id);
            return;
        }
        register(level, id, state, TransformCameraGeometry.frame(
                surface, slot, state, normalSign, overlay));
    }

    private static void register(ServerLevel level, UUID id, BlockState state,
            TransformCameraGeometry.Frame frame) {
        if (frame == null || !TransformCameraGeometry.isCamera(state)) {
            FacilitySurveillanceRegistry.unregister(level, id);
            return;
        }
        TransformCameraGeometry.Angles neutral = frame.ceiling()
                ? frame.worldAngles(0.0F,
                        CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH)
                : frame.worldAngles(0.0F, 0.0F);
        BlockPos anchor = BlockPos.containing(frame.center());
        String name = (frame.ceiling() ? "Ceiling Camera " : "Camera ")
                + anchor.getX() + ", " + anchor.getY() + ", " + anchor.getZ();
        float yawLimit = frame.ceiling()
                ? CeilingCameraModule.MANUAL_YAW_LIMIT
                : SurveillanceCameraPlaceholderModule.MANUAL_YAW_LIMIT;
        float minPitch = frame.ceiling()
                ? CeilingCameraModule.MANUAL_MIN_PITCH
                : SurveillanceCameraPlaceholderModule.MANUAL_MIN_PITCH;
        float maxPitch = frame.ceiling()
                ? CeilingCameraModule.MANUAL_MAX_PITCH
                : SurveillanceCameraPlaceholderModule.MANUAL_MAX_PITCH;
        FacilitySurveillanceRegistry.register(level, id, anchor, frame.eye(),
                name, neutral.yaw(), neutral.pitch(), yawLimit,
                minPitch, maxPitch, 2.5F);
    }

    private static void syncCameraPoses(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ServerPlayer controller = Scp079PlayableManager.controller(server);
        boolean controllerHere = controller != null
                && controller.level().dimension().equals(level.dimension());
        boolean autonomous = controller == null
                && Scp079ProcessingManager.isActive(level)
                && Scp079FacilityAccessManager.hasFacilityAccess(level);
        Map<UUID, CameraPose> previous = CAMERA_POSES.computeIfAbsent(
                level, ignored -> new HashMap<>());
        Set<UUID> seen = new HashSet<>();
        boolean heartbeat = level.getGameTime() % 20L == 0L;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);

        for (TransformGroup group : data.groups()) {
            if (!group.dimension().equals(level.dimension().location())) continue;
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                BlockState state = entry.getValue();
                if (!TransformCameraGeometry.isCamera(state)) continue;
                UUID id = groupCameraId(group.id(), entry.getKey());
                TransformCameraGeometry.Frame frame =
                        TransformCameraGeometry.frame(group, entry.getKey(), state);
                syncCameraPose(level, id, frame, controller, controllerHere,
                        autonomous, heartbeat, previous, seen);
            }
        }

        for (ConstructionSurface surface : data.surfaces()) {
            if (!surface.dimension().equals(level.dimension().location())) {
                continue;
            }
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                syncCameraPose(level, surface, entry.getKey(),
                        entry.getValue(), 0, false, controller,
                        controllerHere, autonomous, heartbeat, previous, seen);
            }
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                int layer = entry.getKey().normalSign();
                if (Math.abs(layer) != 1) continue;
                syncCameraPose(level, surface, entry.getKey().slot(),
                        entry.getValue(), layer, true, controller,
                        controllerHere, autonomous, heartbeat, previous, seen);
            }
        }
        previous.keySet().removeIf(id -> !seen.contains(id));
    }

    private static void syncCameraPose(ServerLevel level,
            ConstructionSurface surface, ConstructionSurface.SurfaceSlot slot,
            ConstructionSurface.SurfaceAttachment attachment, int layer,
            boolean overlay, ServerPlayer controller, boolean controllerHere,
            boolean autonomous, boolean heartbeat,
            Map<UUID, CameraPose> previous, Set<UUID> seen) {
        BlockState state = attachment == null ? null : attachment.state();
        if (!TransformCameraGeometry.isCamera(state)) return;
        UUID id = cameraId(surface.id(), slot, layer);
        int side = layer == 0 ? TransformSurfaceGeometry.MAIN_SIDE : layer;
        syncCameraPose(level, id, TransformCameraGeometry.frame(
                        surface, slot, state, side, overlay),
                controller, controllerHere, autonomous, heartbeat,
                previous, seen);
    }

    private static void syncCameraPose(ServerLevel level, UUID id,
            TransformCameraGeometry.Frame frame, ServerPlayer controller,
            boolean controllerHere, boolean autonomous, boolean heartbeat,
            Map<UUID, CameraPose> previous, Set<UUID> seen) {
        if (frame == null) return;
        seen.add(id);
        FacilityCameraDefinition definition =
                FacilitySurveillanceRegistry.camera(level, id);
        CameraPose pose = CameraPose.IDLE;

        boolean operator = false;
        if (definition != null && controllerHere
                && Scp079PlayableManager.isCameraMode(controller)) {
            FacilityCameraDefinition current =
                    Scp079PlayableManager.currentCamera(controller);
            operator = current != null && id.equals(current.id());
        }

        if (operator) {
            TransformCameraGeometry.Angles local =
                    frame.localAngles(controller.getLookAngle());
            pose = directedPose(frame, local.yaw(), local.pitch(),
                    false);
        } else if (definition != null && autonomous) {
            ServerPlayer target = trackingTarget(level, definition);
            if (target != null) {
                // Use the live transformed frame as the mechanical
                // origin. The registry eye is a navigation snapshot and can
                // lag behind a Surface geometry edit; aiming from it makes the
                // virtual model look beside the player on curved walls.
                Vec3 direction = target.getEyePosition()
                        .subtract(frame.eye());
                TransformCameraGeometry.Angles local =
                        frame.localAngles(direction);
                pose = directedPose(frame, local.yaw(), local.pitch(),
                        true);
            }
        }

        CameraPose old = previous.put(id, pose);
        boolean changed = old == null || old.controlled() != pose.controlled()
                || pose.controlled()
                && (Math.abs(Mth.wrapDegrees(old.yaw() - pose.yaw())) > 0.08F
                    || Math.abs(old.pitch() - pose.pitch()) > 0.08F);
        if (changed || heartbeat && pose.controlled()) {
            TransformConstructionNetwork.broadcastSurfaceCameraPose(level, id,
                    pose.controlled(), pose.yaw(), pose.pitch());
        }
    }

    private static CameraPose directedPose(TransformCameraGeometry.Frame frame,
            float yaw, float physicalPitch, boolean autonomous) {
        float limit = frame.ceiling()
                ? CeilingCameraModule.MANUAL_YAW_LIMIT
                : SurveillanceCameraPlaceholderModule.MANUAL_YAW_LIMIT;
        float minPitch = frame.ceiling()
                ? CeilingCameraModule.MANUAL_MIN_PITCH
                : SurveillanceCameraPlaceholderModule.MANUAL_MIN_PITCH;
        float maxPitch = frame.ceiling()
                ? CeilingCameraModule.MANUAL_MAX_PITCH
                : SurveillanceCameraPlaceholderModule.MANUAL_MAX_PITCH;
        float logicalYaw = yaw;
        float logicalPitch = physicalPitch;
        if (autonomous && frame.ceiling()) {
            // The authored ceiling dome's procedural Y rotation has opposite
            // handedness to Minecraft yaw. Playable control already passes
            // through the client lens frame and must remain unmirrored.
            logicalYaw = -logicalYaw;
        } else if (autonomous) {
            // Wall-camera BlockEntities add the authored fifteen-degree
            // mechanical tilt while rendering. Autonomous targeting starts
            // from a physical world direction, so store the corresponding
            // logical pitch. Human control is already expressed in logical
            // camera pitch and must not subtract this a second time.
            logicalPitch -= SurveillanceCameraViewGeometry.DEFAULT_DOWN_PITCH;
        }
        return new CameraPose(true,
                Mth.clamp(Mth.wrapDegrees(logicalYaw), -limit, limit),
                Mth.clamp(logicalPitch, minPitch, maxPitch));
    }

    private static ServerPlayer trackingTarget(ServerLevel level,
            FacilityCameraDefinition camera) {
        FacilityRoomSnapshot room =
                FacilityMappingManager.roomSnapshotForCamera(level, camera);
        if (room == null) return null;
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()
                    || !room.containsColumn(player.blockPosition())) {
                continue;
            }
            double distance = player.position().distanceToSqr(
                    camera.eyePosition());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    private record CameraPose(boolean controlled, float yaw, float pitch) {
        private static final CameraPose IDLE =
                new CameraPose(false, 0.0F, 0.0F);
    }

    private static ServerLevel level(MinecraftServer server,
            ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) return level;
        }
        return null;
    }
}
