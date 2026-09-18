package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.DecontaminationStructure;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.transform.network.TransformConstructionNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-side Alarm adapter for transformed construction. A transformed Alarm
 * is still represented by its real Alarm BlockState, but its physical location
 * is supplied by the off-grid/surface geometry rather than an integer BlockPos.
 * This keeps redstone, nearby-door activation, light emission and audio useful
 * without pretending that a rotated block entity lives in the vanilla grid.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformAlarmRuntime {
    private static final int UPDATE_INTERVAL = 2;
    private static final int LOOP_INTERVAL = 40;
    private static final double TRANSFORMED_DOOR_RANGE_SQR = 1.75D * 1.75D;
    private static final Map<MinecraftServer, AlarmIndex> INDEXES =
            new WeakHashMap<>();

    private TransformAlarmRuntime() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % UPDATE_INTERVAL != 0) return;

        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        AlarmIndex index = INDEXES.get(server);
        if (index == null || index.revision() != data.revision()) {
            index = AlarmIndex.build(data);
            INDEXES.put(server, index);
        }

        for (GroupAlarm alarm : index.groups()) {
            ServerLevel level = level(server, alarm.dimension());
            TransformGroup group = data.group(alarm.groupId());
            if (level == null || group == null) continue;
            BlockState state = group.cells().get(alarm.cell());
            if (!isAlarm(state)) continue;

            Vec3 center = group.cellCenter(alarm.cell());
            boolean active = shouldBeActive(level, center,
                    TransformPowerQuery.powered(level, group, alarm.cell())
                            || adjacentOpenDoor(group, alarm.cell(), state));
            boolean wasActive = state.getValue(AlarmModule.ACTIVE);
            if (active != wasActive) {
                BlockState updated = state.setValue(
                        AlarmModule.ACTIVE, active);
                data.putGroupState(group.withCell(alarm.cell(), updated));
                TransformConstructionNetwork.broadcastGroupCell(level,
                        group.id(), alarm.cell(), updated);
                TransformConstructionManager.refreshGroupCellRuntime(
                        server, group.id(), alarm.cell());
            }
            if (active && (!wasActive || tick % LOOP_INTERVAL == 0)) {
                playLoop(level, center);
            }
        }

        for (SurfaceAlarm alarm : index.surfaces()) {
            ServerLevel level = level(server, alarm.dimension());
            ConstructionSurface surface = data.surface(alarm.surfaceId());
            if (level == null || surface == null) continue;
            ConstructionSurface.SurfaceAttachment attachment =
                    alarm.normalSign() == 0
                            ? surface.attachments().get(alarm.slot())
                            : surface.overlay(alarm.slot(), alarm.normalSign());
            if (attachment == null || !isAlarm(attachment.state())) continue;

            double u = (alarm.slot().column() + 0.5D) / surface.columns();
            double v = (alarm.slot().row() + 0.5D) / surface.rows();
            int side = alarm.normalSign() == 0 ? 1 : alarm.normalSign();
            Vec3 center = surface.gridPoint(u, v)
                    .add(surface.gridNormal(u, v).scale(side * 0.5D));
            boolean active = shouldBeActive(level, center,
                    TransformPowerQuery.powered(level, surface, alarm.slot())
                            || adjacentOpenDoor(surface, alarm.slot()));
            BlockState state = attachment.state();
            boolean wasActive = state.getValue(AlarmModule.ACTIVE);
            if (active != wasActive) {
                BlockState updated = state.setValue(
                        AlarmModule.ACTIVE, active);
                ConstructionSurface next;
                if (alarm.normalSign() == 0) {
                    next = surface.withAttachment(alarm.slot(), updated,
                            attachment.deform());
                    TransformConstructionNetwork.broadcastSurfaceSlot(level,
                            surface.id(), alarm.slot(), updated,
                            attachment.deform());
                } else {
                    next = surface.withOverlay(alarm.slot(),
                            alarm.normalSign(), updated, attachment.deform());
                    TransformConstructionNetwork.broadcastSurfaceOverlay(level,
                            surface.id(), alarm.slot(), alarm.normalSign(),
                            updated, attachment.deform());
                }
                data.putSurfaceState(next);
                TransformConstructionManager.refreshSurfaceSlotRuntime(
                        server, surface.id(), alarm.slot());
            }
            if (active && (!wasActive || tick % LOOP_INTERVAL == 0)) {
                playLoop(level, center);
            }
        }
    }

    public static synchronized void structuralGroupCellChanged(
            MinecraftServer server, UUID groupId, TransformGroup.GridPos cell) {
        AlarmIndex index = INDEXES.get(server);
        if (server == null || index == null || groupId == null || cell == null) {
            return;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<GroupAlarm> groups = new ArrayList<>(index.groups());
        groups.removeIf(ref -> ref.groupId().equals(groupId)
                && ref.cell().equals(cell));
        TransformGroup group = data.group(groupId);
        if (group != null) {
            BlockState state = group.cells().get(cell);
            if (isAlarm(state)) {
                groups.add(new GroupAlarm(group.dimension(), groupId, cell));
            }
        }
        INDEXES.put(server, new AlarmIndex(data.revision(),
                List.copyOf(groups), index.surfaces()));
    }

    public static synchronized void structuralSurfaceSlotChanged(
            MinecraftServer server, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot) {
        AlarmIndex index = INDEXES.get(server);
        if (server == null || index == null || surfaceId == null || slot == null) {
            return;
        }
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<SurfaceAlarm> surfaces = new ArrayList<>(index.surfaces());
        surfaces.removeIf(ref -> ref.surfaceId().equals(surfaceId)
                && ref.slot().equals(slot));
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface != null) {
            ConstructionSurface.SurfaceAttachment main =
                    surface.attachments().get(slot);
            if (main != null && isAlarm(main.state())) {
                surfaces.add(new SurfaceAlarm(surface.dimension(), surfaceId,
                        slot, 0));
            }
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                if (entry.getKey().slot().equals(slot)
                        && isAlarm(entry.getValue().state())) {
                    surfaces.add(new SurfaceAlarm(surface.dimension(), surfaceId,
                            slot, entry.getKey().normalSign()));
                }
            }
        }
        INDEXES.put(server, new AlarmIndex(data.revision(), index.groups(),
                List.copyOf(surfaces)));
    }

    public static synchronized void structuralGroupChanged(
            MinecraftServer server, UUID groupId) {
        AlarmIndex index = INDEXES.get(server);
        if (server == null || index == null || groupId == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<GroupAlarm> groups = new ArrayList<>(index.groups());
        groups.removeIf(ref -> ref.groupId().equals(groupId));
        TransformGroup group = data.group(groupId);
        if (group != null) {
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (isAlarm(entry.getValue())) {
                    groups.add(new GroupAlarm(group.dimension(), groupId,
                            entry.getKey()));
                }
            }
        }
        INDEXES.put(server, new AlarmIndex(data.revision(),
                List.copyOf(groups), index.surfaces()));
    }

    public static synchronized void structuralSurfaceChanged(
            MinecraftServer server, UUID surfaceId) {
        AlarmIndex index = INDEXES.get(server);
        if (server == null || index == null || surfaceId == null) return;
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        List<SurfaceAlarm> surfaces = new ArrayList<>(index.surfaces());
        surfaces.removeIf(ref -> ref.surfaceId().equals(surfaceId));
        ConstructionSurface surface = data.surface(surfaceId);
        if (surface != null) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (isAlarm(entry.getValue().state())) {
                    surfaces.add(new SurfaceAlarm(surface.dimension(), surfaceId,
                            entry.getKey(), 0));
                }
            }
            for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.overlays().entrySet()) {
                if (isAlarm(entry.getValue().state())) {
                    surfaces.add(new SurfaceAlarm(surface.dimension(), surfaceId,
                            entry.getKey().slot(),
                            entry.getKey().normalSign()));
                }
            }
        }
        INDEXES.put(server, new AlarmIndex(data.revision(), index.groups(),
                List.copyOf(surfaces)));
    }

    public static synchronized void acknowledgeStructuralRevision(
            MinecraftServer server) {
        AlarmIndex index = INDEXES.get(server);
        if (server == null || index == null) return;
        long revision = TransformConstructionSavedData.get(server).revision();
        if (index.revision() != revision) {
            INDEXES.put(server, new AlarmIndex(revision,
                    index.groups(), index.surfaces()));
        }
    }

    private static ServerLevel level(MinecraftServer server,
            ResourceLocation dimension) {
        if (server == null || dimension == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (dimension.equals(level.dimension().location())) return level;
        }
        return null;
    }

    private record GroupAlarm(ResourceLocation dimension, UUID groupId,
            TransformGroup.GridPos cell) {
    }

    private record SurfaceAlarm(ResourceLocation dimension, UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
    }

    private record AlarmIndex(long revision, List<GroupAlarm> groups,
            List<SurfaceAlarm> surfaces) {
        private static AlarmIndex build(TransformConstructionSavedData data) {
            List<GroupAlarm> groups = new ArrayList<>();
            List<SurfaceAlarm> surfaces = new ArrayList<>();
            for (TransformGroup group : data.groups()) {
                for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                        : group.cells().entrySet()) {
                    if (isAlarm(entry.getValue())) {
                        groups.add(new GroupAlarm(group.dimension(), group.id(),
                                entry.getKey()));
                    }
                }
            }
            for (ConstructionSurface surface : data.surfaces()) {
                for (Map.Entry<ConstructionSurface.SurfaceSlot,
                        ConstructionSurface.SurfaceAttachment> entry
                        : surface.attachments().entrySet()) {
                    if (isAlarm(entry.getValue().state())) {
                        surfaces.add(new SurfaceAlarm(surface.dimension(),
                                surface.id(), entry.getKey(), 0));
                    }
                }
                for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                        ConstructionSurface.SurfaceAttachment> entry
                        : surface.overlays().entrySet()) {
                    if (isAlarm(entry.getValue().state())) {
                        surfaces.add(new SurfaceAlarm(surface.dimension(),
                                surface.id(), entry.getKey().slot(),
                                entry.getKey().normalSign()));
                    }
                }
            }
            return new AlarmIndex(data.revision(), List.copyOf(groups),
                    List.copyOf(surfaces));
        }
    }

    private static boolean adjacentOpenDoor(TransformGroup group,
            TransformGroup.GridPos cell, BlockState alarmState) {
        if (group == null || cell == null || alarmState == null) return false;

        // A wall-mounted Alarm lives in the air cell in front of its support.
        // Door adjacency is defined from that support cell, just as it is for
        // the real vanilla-grid Alarm footprint. This naturally catches an
        // Alarm above/beside a door without inventing room-wide range checks.
        TransformGroup.GridPos support = cell;
        if (alarmState.hasProperty(AlarmModule.FACING)) {
            Direction outward = alarmState.getValue(AlarmModule.FACING);
            support = cell.offset(-outward.getStepX(), -outward.getStepY(),
                    -outward.getStepZ());
        }
        if (electricOpenDoor(group.cells().get(support))) return true;
        for (Direction direction : Direction.values()) {
            TransformGroup.GridPos neighbor = support.offset(
                    direction.getStepX(), direction.getStepY(),
                    direction.getStepZ());
            if (electricOpenDoor(group.cells().get(neighbor))) return true;
        }
        return false;
    }

    private static boolean electricOpenDoor(BlockState state) {
        return state != null
                && FacilityModule.isElectricDoorOpenOrOpening(state);
    }

    private static boolean adjacentOpenDoor(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot) {
        int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] offset : offsets) {
            ConstructionSurface.SurfaceAttachment attachment =
                    surface.attachments().get(
                            new ConstructionSurface.SurfaceSlot(
                                    slot.column() + offset[0],
                                    slot.row() + offset[1]));
            if (attachment != null
                    && FacilityModule.isElectricDoorOpenOrOpening(
                            attachment.state())) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 surfaceCenter(ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot) {
        double u = (slot.column() + 0.5D) / surface.columns();
        double v = (slot.row() + 0.5D) / surface.rows();
        return surface.gridPoint(u, v)
                .add(surface.gridNormal(u, v).scale(0.5D));
    }

    private static boolean isAlarm(BlockState state) {
        return state != null && AlarmModule.isController(state)
                && state.hasProperty(AlarmModule.ACTIVE);
    }

    private static boolean shouldBeActive(ServerLevel level, Vec3 center,
            boolean logicalPower) {
        return logicalPower || hasVanillaDoor(level, center);
    }

    private static boolean hasVanillaDoor(ServerLevel level, Vec3 center) {
        BlockPos base = BlockPos.containing(center);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) continue;
                    BlockPos pos = base.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (DecontaminationStructure.isOwnedDoor(level, pos, state)) {
                        continue;
                    }
                    if (FacilityModule.isElectricDoorOpenOrOpening(state)) {
                        return true;
                    }
                    if (BlastDoorModule.isController(state)
                            && BlastDoorModule.isOpenOrOpening(level, pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void playLoop(ServerLevel level, Vec3 center) {
        level.playSound(null, center.x, center.y, center.z,
                AlarmModule.LOOP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }


}
