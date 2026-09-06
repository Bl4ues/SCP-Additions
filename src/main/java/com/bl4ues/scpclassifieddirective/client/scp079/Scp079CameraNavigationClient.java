package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.network.Scp079CameraNavigationNetwork;
import com.bl4ues.scpclassifieddirective.network.Scp079CameraNavigationNetwork.CameraNode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * WASD navigation follows the authored facility-map topology. Camera placement
 * only decides which endpoint inside the chosen mapped room is used, so a camera
 * mounted backwards never makes the facility controls feel backwards as well.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079CameraNavigationClient {
    private static final int ADJACENT_ROOM_GAP = 2;

    private Scp079CameraNavigationClient() {
    }

    public enum Move {
        FORWARD,
        LEFT,
        BACK,
        RIGHT
    }

    public record NavigationTarget(UUID cameraId, UUID roomId,
            String roomName, double distanceSqr) {
        public boolean available() {
            return cameraId != null;
        }
    }

    public static Map<Move, NavigationTarget> targets() {
        EnumMap<Move, NavigationTarget> result = new EnumMap<>(Move.class);
        if (!Scp079PlayableClient.cameraMode()) return result;

        CameraNode current = currentNode();
        if (current == null) return result;
        FacilityRoomSnapshot currentRoom = room(current.roomId());
        RoomCenter currentCenter = center(currentRoom);
        if (currentRoom == null || currentCenter == null) return result;

        EnumMap<Move, FacilityRoomSnapshot> roomTargets =
                new EnumMap<>(Move.class);
        EnumMap<Move, Double> roomDistances = new EnumMap<>(Move.class);
        for (FacilityRoomSnapshot candidate : FacilityMappingClientState.rooms(
                Scp079PlayableClient.hostDimension())) {
            if (candidate.id().equals(currentRoom.id())
                    || !adjacent(currentRoom, candidate)) continue;
            RoomCenter candidateCenter = center(candidate);
            if (candidateCenter == null) continue;

            double dx = candidateCenter.x - currentCenter.x;
            double dz = candidateCenter.z - currentCenter.z;
            if (Math.abs(dx) < 0.001D && Math.abs(dz) < 0.001D) continue;
            Move move = Math.abs(dx) >= Math.abs(dz)
                    ? (dx >= 0.0D ? Move.RIGHT : Move.LEFT)
                    : (dz >= 0.0D ? Move.BACK : Move.FORWARD);
            double distanceSqr = dx * dx + dz * dz;
            Double previous = roomDistances.get(move);
            if (previous == null || distanceSqr < previous) {
                roomDistances.put(move, distanceSqr);
                roomTargets.put(move, candidate);
            }
        }

        for (Move move : Move.values()) {
            FacilityRoomSnapshot destination = roomTargets.get(move);
            if (destination == null) continue;
            RoomCenter destinationCenter = center(destination);
            CameraNode camera = cameraForRoom(destination.id(),
                    destinationCenter);
            String roomName = destination.name().isBlank()
                    ? directionalRoomName(move)
                    : destination.name().toUpperCase(Locale.ROOT);
            result.put(move, new NavigationTarget(
                    camera == null ? null : camera.cameraId(),
                    destination.id(), roomName,
                    roomDistances.getOrDefault(move, Double.MAX_VALUE)));
        }
        return result;
    }

    private static String directionalRoomName(Move move) {
        return switch (move) {
            case FORWARD -> "NORTH ROOM";
            case LEFT -> "WEST ROOM";
            case BACK -> "SOUTH ROOM";
            case RIGHT -> "EAST ROOM";
        };
    }

    private static CameraNode currentNode() {
        Vec3 view = Scp079PlayableClient.viewPosition();
        CameraNode best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CameraNode node : Scp079CameraNetworkClientState.nodes()) {
            double dx = node.x() - view.x;
            double dy = node.y() - view.y;
            double dz = node.z() - view.z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = node;
            }
        }
        return bestDistance <= 4.0D ? best : null;
    }

    private static CameraNode cameraForRoom(UUID roomId,
            RoomCenter center) {
        CameraNode best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CameraNode node : Scp079CameraNetworkClientState.nodes()) {
            if (!roomId.equals(node.roomId())) continue;
            double dx = center == null ? 0.0D : node.x() - center.x;
            double dz = center == null ? 0.0D : node.z() - center.z;
            double distance = dx * dx + dz * dz;
            if (best == null || distance < bestDistance
                    || (Math.abs(distance - bestDistance) < 0.0001D
                    && node.cameraId().toString().compareTo(
                    best.cameraId().toString()) < 0)) {
                best = node;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static FacilityRoomSnapshot room(UUID id) {
        if (id == null) return null;
        List<FacilityRoomSnapshot> rooms = FacilityMappingClientState.rooms(
                Scp079PlayableClient.hostDimension());
        for (FacilityRoomSnapshot room : rooms) {
            if (id.equals(room.id())) return room;
        }
        return null;
    }

    private static RoomCenter center(FacilityRoomSnapshot room) {
        if (room == null || room.patches().isEmpty()) return null;
        long weightedX = 0L;
        long weightedZ = 0L;
        long cells = 0L;
        for (FacilityFloorPatch patch : room.patches()) {
            long width = (long) patch.maxX() - patch.minX() + 1L;
            long depth = (long) patch.maxZ() - patch.minZ() + 1L;
            long area = Math.max(1L, width * depth);
            double cx = (patch.minX() + patch.maxX() + 1.0D) * 0.5D;
            double cz = (patch.minZ() + patch.maxZ() + 1.0D) * 0.5D;
            weightedX += Math.round(cx * area * 1024.0D);
            weightedZ += Math.round(cz * area * 1024.0D);
            cells += area;
        }
        if (cells <= 0L) return null;
        return new RoomCenter(weightedX / (cells * 1024.0D),
                weightedZ / (cells * 1024.0D));
    }

    private static boolean adjacent(FacilityRoomSnapshot a,
            FacilityRoomSnapshot b) {
        if (!a.floorLongLabel().equalsIgnoreCase(b.floorLongLabel())) return false;
        for (FacilityFloorPatch pa : a.patches()) {
            for (FacilityFloorPatch pb : b.patches()) {
                if (Math.abs(pa.y() - pb.y()) > 3) continue;
                int gapX = intervalGap(pa.minX(), pa.maxX(), pb.minX(), pb.maxX());
                int gapZ = intervalGap(pa.minZ(), pa.maxZ(), pb.minZ(), pb.maxZ());
                if (Math.max(gapX, gapZ) <= ADJACENT_ROOM_GAP) return true;
            }
        }
        return false;
    }

    private static int intervalGap(int amin, int amax, int bmin, int bmax) {
        if (amax < bmin) return bmin - amax - 1;
        if (bmax < amin) return amin - bmax - 1;
        return 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !Scp079PlayableClient.cameraMode()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;

        Map<Move, NavigationTarget> targets = targets();
        if (consumeAndNavigate(minecraft.options.keyUp,
                targets.get(Move.FORWARD))) return;
        if (consumeAndNavigate(minecraft.options.keyLeft,
                targets.get(Move.LEFT))) return;
        if (consumeAndNavigate(minecraft.options.keyDown,
                targets.get(Move.BACK))) return;
        consumeAndNavigate(minecraft.options.keyRight,
                targets.get(Move.RIGHT));
    }

    private static boolean consumeAndNavigate(KeyMapping key,
            NavigationTarget target) {
        boolean pressed = false;
        while (key.consumeClick()) pressed = true;
        if (!pressed) return false;
        if (target != null && target.available()) {
            Scp079CameraNavigationNetwork.requestCamera(target.cameraId());
        }
        return true;
    }

    private record RoomCenter(double x, double z) {
    }
}
