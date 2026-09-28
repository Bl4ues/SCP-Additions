package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079ProcessingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
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
 * Server authority for surveillance cameras stored inside transformed Surface
 * cells. A Surface has no real camera BlockEntity in the world, so its registry
 * definition is rebuilt from the same physical frame used by rendering.
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

        TransformCameraGeometry.Frame frame = TransformCameraGeometry.frame(
                surface, slot, state, normalSign, overlay);
        if (frame == null) {
            FacilitySurveillanceRegistry.unregister(level, id);
            return;
        }

        TransformCameraGeometry.Angles neutral = frame.ceiling()
                ? frame.worldAngles(0.0F,
                        com.bl4ues.scpclassifieddirective.facility.surveillance
                                .CeilingCameraViewGeometry.DEFAULT_DOWN_PITCH)
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

        for (ConstructionSurface surface
                : TransformConstructionSavedData.get(server).surfaces()) {
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
        seen.add(id);
        int side = layer == 0 ? TransformSurfaceGeometry.MAIN_SIDE : layer;
        TransformCameraGeometry.Frame frame = TransformCameraGeometry.frame(
                surface, slot, state, side, overlay);
        if (frame == null) return;
        var definition = FacilitySurveillanceRegistry.camera(level, id);
        CameraPose pose = CameraPose.IDLE;

        if (definition != null && controllerHere
                && Scp079PlayableManager.isCameraMode(controller)
                && controller.position().distanceToSqr(
                        definition.eyePosition()) <= 0.36D) {
            TransformCameraGeometry.Angles local =
                    frame.localAngles(controller.getLookAngle());
            pose = directedPose(frame, local.yaw(), local.pitch(), false);
        } else if (definition != null && autonomous) {
            ServerPlayer target = trackingTarget(level, definition);
            if (target != null) {
                Vec3 direction = target.getEyePosition()
                        .subtract(definition.eyePosition());
                TransformCameraGeometry.Angles local =
                        frame.localAngles(direction);
                pose = directedPose(frame, local.yaw(), local.pitch(), true);
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
        float logicalPitch = physicalPitch;
        if (autonomous && !frame.ceiling()) {
            logicalPitch -= com.bl4ues.scpclassifieddirective.facility
                    .surveillance.SurveillanceCameraViewGeometry
                    .DEFAULT_DOWN_PITCH;
        }
        return new CameraPose(true,
                Mth.clamp(Mth.wrapDegrees(yaw), -limit, limit),
                Mth.clamp(logicalPitch, minPitch, maxPitch));
    }

    private static ServerPlayer trackingTarget(ServerLevel level,
            com.bl4ues.scpclassifieddirective.facility.surveillance
                    .FacilityCameraDefinition camera) {
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
