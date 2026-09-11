package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.block.TeslaGateStructure;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.network.Scp079CameraNavigationNetwork.CameraNode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Keeps physical SCP-079 prompts scoped to the room the active feed belongs to.
 * Facility devices stay local; only cameras in genuinely adjacent rooms may be
 * exposed as cross-room physical switch targets.
 */
public final class Scp079InteractionScopeClient {
    private static final int DEVICE_BORDER = 3;
    private static final int ROOM_COLUMN_HEIGHT = 12;
    private static final int ADJACENT_ROOM_GAP = 2;

    private Scp079InteractionScopeClient() {
    }

    public static boolean allow(BlockPos target) {
        if (target == null || !Scp079PlayableClient.cameraMode()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;

        List<FacilityRoomSnapshot> rooms = FacilityMappingClientState.rooms(
                Scp079PlayableClient.hostDimension());
        FacilityRoomSnapshot current = currentRoom(rooms);
        if (current == null) return false;

        FacilityRoomSnapshot targetCameraRoom = cameraRoom(rooms, target);
        if (targetCameraRoom != null) {
            return !current.id().equals(targetCameraRoom.id())
                    && adjacent(current, targetCameraRoom);
        }

        BlockState state = minecraft.level.getBlockState(target);
        BlockPos resolvedTarget = target;
        boolean door = FacilityModule.isFacilityDoor(state);
        if (BlastDoorModule.isStructureState(state)) {
            BlockPos controller = BlastDoorModule.controllerPosition(
                    minecraft.level, target, state);
            if (controller != null) {
                resolvedTarget = controller;
                door = BlastDoorModule.hasRedstoneConnection(
                        minecraft.level, controller);
            }
        }
        boolean tesla = TeslaGateStructure.isController(state);
        if (!door && !tesla) return false;

        FacilityRoomSnapshot owner = owningRoom(
                rooms, resolvedTarget, current);
        return owner != null && current.id().equals(owner.id());
    }

    private static FacilityRoomSnapshot currentRoom(
            List<FacilityRoomSnapshot> rooms) {
        Vec3 viewPosition = Scp079PlayableClient.viewPosition();

        // Prefer the topology node itself. A wall/ceiling camera can sit in the
        // expanded border of multiple rooms, while its authored topology has one
        // unambiguous owner. Using roomAt() first reintroduced the old boundary
        // flicker whenever iteration order happened to choose the neighbour.
        CameraNode nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (CameraNode node : Scp079CameraNetworkClientState.nodes()) {
            double dx = node.x() - viewPosition.x;
            double dy = node.y() - viewPosition.y;
            double dz = node.z() - viewPosition.z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = node;
            }
        }
        if (nearest != null && nearestDistance <= 4.0D) {
            FacilityRoomSnapshot topologyRoom = roomById(rooms, nearest.roomId());
            if (topologyRoom != null) return topologyRoom;
        }

        BlockPos view = BlockPos.containing(viewPosition);
        for (FacilityRoomSnapshot room : rooms) {
            if (room.containsColumn(view)) return room;
        }
        for (FacilityRoomSnapshot room : rooms) {
            if (withinExpandedFloor(room, view, 1)) return room;
        }
        return null;
    }

    private static FacilityRoomSnapshot cameraRoom(
            List<FacilityRoomSnapshot> rooms, BlockPos target) {
        CameraNode best = null;
        double bestDistance = Double.MAX_VALUE;
        for (CameraNode node : Scp079CameraNetworkClientState.nodes()) {
            double distance = node.anchorPos().distSqr(target);
            if (distance <= 2.0D && distance < bestDistance) {
                bestDistance = distance;
                best = node;
            }
        }
        return best == null ? null : roomById(rooms, best.roomId());
    }

    private static FacilityRoomSnapshot owningRoom(
            List<FacilityRoomSnapshot> rooms, BlockPos target,
            FacilityRoomSnapshot current) {
        if (current.containsColumn(target)) return current;

        // A device physically inside another authored floor belongs to that
        // room even when the current room's structural border also reaches it.
        for (FacilityRoomSnapshot room : rooms) {
            if (!room.id().equals(current.id()) && room.containsColumn(target)) {
                return room;
            }
        }

        // Doors commonly live just outside the walkable floor. Preserve that
        // structural allowance only when no neighbouring room claims the block.
        if (withinExpandedFloor(current, target, DEVICE_BORDER)) return current;
        return null;
    }

    private static FacilityRoomSnapshot roomById(
            List<FacilityRoomSnapshot> rooms, UUID id) {
        if (id == null) return null;
        for (FacilityRoomSnapshot room : rooms) {
            if (id.equals(room.id())) return room;
        }
        return null;
    }

    private static boolean adjacent(FacilityRoomSnapshot a,
            FacilityRoomSnapshot b) {
        if (a == null || b == null || a.id().equals(b.id())
                || !a.floorLongLabel().equalsIgnoreCase(b.floorLongLabel())) {
            return false;
        }
        for (FacilityFloorPatch pa : a.patches()) {
            for (FacilityFloorPatch pb : b.patches()) {
                if (Math.abs(pa.y() - pb.y()) > 3) continue;
                int gapX = intervalGap(pa.minX(), pa.maxX(),
                        pb.minX(), pb.maxX());
                int gapZ = intervalGap(pa.minZ(), pa.maxZ(),
                        pb.minZ(), pb.maxZ());
                if (Math.max(gapX, gapZ) <= ADJACENT_ROOM_GAP) return true;
            }
        }
        return false;
    }

    private static boolean withinExpandedFloor(FacilityRoomSnapshot room,
            BlockPos target, int border) {
        int extra = Math.max(0, border);
        for (FacilityFloorPatch patch : room.patches()) {
            if (target.getX() < patch.minX() - extra
                    || target.getX() > patch.maxX() + extra
                    || target.getZ() < patch.minZ() - extra
                    || target.getZ() > patch.maxZ() + extra) continue;
            if (target.getY() >= patch.y() - 1
                    && target.getY() <= patch.y() + ROOM_COLUMN_HEIGHT) {
                return true;
            }
        }
        return false;
    }

    private static int intervalGap(int amin, int amax, int bmin, int bmax) {
        if (amax < bmin) return bmin - amax - 1;
        if (bmax < amin) return amin - bmax - 1;
        return 0;
    }
}
