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

/** Physical WASD routing between cameras in the same or adjacent mapped rooms. */
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
    }

    public static Map<Move, NavigationTarget> targets() {
        EnumMap<Move, NavigationTarget> result = new EnumMap<>(Move.class);
        if (!Scp079PlayableClient.cameraMode()) return result;

        CameraNode current = currentNode();
        if (current == null) return result;
        FacilityRoomSnapshot currentRoom = room(current.roomId());
        if (currentRoom == null) return result;

        double yaw = Math.toRadians(current.baseYaw());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);

        for (CameraNode candidate : Scp079CameraNetworkClientState.nodes()) {
            if (candidate.cameraId().equals(current.cameraId())) continue;
            FacilityRoomSnapshot candidateRoom = room(candidate.roomId());
            if (candidateRoom == null) continue;
            boolean compatible = candidateRoom.id().equals(currentRoom.id())
                    || adjacent(currentRoom, candidateRoom);
            if (!compatible) continue;

            Vec3 delta = new Vec3(candidate.x() - current.x(), 0.0D,
                    candidate.z() - current.z());
            double distanceSqr = delta.lengthSqr();
            if (distanceSqr < 0.01D) continue;
            double localForward = delta.dot(forward);
            double localRight = delta.dot(right);
            Move move;
            if (Math.abs(localForward) >= Math.abs(localRight)) {
                move = localForward >= 0.0D ? Move.FORWARD : Move.BACK;
            } else {
                move = localRight >= 0.0D ? Move.RIGHT : Move.LEFT;
            }

            String roomName = candidateRoom.name().isBlank()
                    ? candidate.roomName() : candidateRoom.name();
            if (roomName == null || roomName.isBlank()) {
                roomName = "UNNAMED ROOM";
            }
            NavigationTarget next = new NavigationTarget(candidate.cameraId(),
                    candidate.roomId(), roomName.toUpperCase(Locale.ROOT),
                    distanceSqr);
            NavigationTarget previous = result.get(move);
            if (previous == null || next.distanceSqr() < previous.distanceSqr()) {
                result.put(move, next);
            }
        }
        return result;
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

    private static FacilityRoomSnapshot room(UUID id) {
        if (id == null) return null;
        List<FacilityRoomSnapshot> rooms = FacilityMappingClientState.rooms(
                Scp079PlayableClient.hostDimension());
        for (FacilityRoomSnapshot room : rooms) {
            if (id.equals(room.id())) return room;
        }
        return null;
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
        if (target != null) {
            Scp079CameraNavigationNetwork.requestCamera(target.cameraId());
        }
        return true;
    }
}
