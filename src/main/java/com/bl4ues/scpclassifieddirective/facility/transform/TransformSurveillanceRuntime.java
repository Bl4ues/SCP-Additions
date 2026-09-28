package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.surveillance.CeilingCameraModule;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server authority for surveillance cameras stored inside transformed Surface
 * cells. A Surface has no real camera BlockEntity in the world, so its registry
 * definition is rebuilt from the same physical frame used by rendering.
 */
public final class TransformSurveillanceRuntime {
    private TransformSurveillanceRuntime() {
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

    private static ServerLevel level(MinecraftServer server,
            ResourceLocation dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) return level;
        }
        return null;
    }
}
