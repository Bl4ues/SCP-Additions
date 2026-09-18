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

    private TransformAlarmRuntime() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % UPDATE_INTERVAL != 0) return;

        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                server);
        for (ServerLevel level : server.getAllLevels()) {
            updateGroups(level, data, tick);
            updateSurfaces(level, data, tick);
        }
    }

    private static boolean updateGroups(ServerLevel level,
            TransformConstructionSavedData data, int tick) {
        boolean changed = false;
        for (TransformGroup original : data.groups()) {
            if (!original.dimension().equals(level.dimension().location())) {
                continue;
            }
            TransformGroup current = original;
            List<TransformGroup.GridPos> changedCells = new ArrayList<>();
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : original.cells().entrySet()) {
                BlockState state = entry.getValue();
                if (!isAlarm(state)) continue;
                Vec3 center = original.cellCenter(entry.getKey());
                boolean active = shouldBeActive(level, center,
                        TransformPowerQuery.powered(level, original,
                                entry.getKey())
                                || adjacentOpenDoor(original, entry.getKey(),
                                        state));
                boolean wasActive = state.getValue(AlarmModule.ACTIVE);
                if (active != wasActive) {
                    BlockState updated = state.setValue(AlarmModule.ACTIVE, active);
                    current = current.withCell(entry.getKey(), updated);
                    TransformConstructionNetwork.broadcastGroupCell(level,
                            original.id(), entry.getKey(), updated);
                    changedCells.add(entry.getKey());
                }
                if (active && (!wasActive || tick % LOOP_INTERVAL == 0)) {
                    playLoop(level, center);
                }
            }
            if (!changedCells.isEmpty()) {
                data.putGroupState(current);
                for (TransformGroup.GridPos cell : changedCells) {
                    TransformConstructionManager.refreshGroupCellRuntime(
                            level.getServer(), original.id(), cell);
                }
                changed = true;
            }
        }
        return changed;
    }

    private static boolean updateSurfaces(ServerLevel level,
            TransformConstructionSavedData data, int tick) {
        boolean changed = false;
        for (ConstructionSurface original : data.surfaces()) {
            if (!original.dimension().equals(level.dimension().location())) {
                continue;
            }
            ConstructionSurface current = original;
            List<ConstructionSurface.SurfaceSlot> changedSlots =
                    new ArrayList<>();
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : original.attachments().entrySet()) {
                ConstructionSurface.SurfaceAttachment attachment =
                        entry.getValue();
                BlockState state = attachment.state();
                if (!isAlarm(state)) continue;
                ConstructionSurface.SurfaceSlot slot = entry.getKey();
                Vec3 center = surfaceCenter(original, slot);
                boolean active = shouldBeActive(level, center,
                        TransformPowerQuery.powered(level, original, slot)
                                || adjacentOpenDoor(original, slot));
                boolean wasActive = state.getValue(AlarmModule.ACTIVE);
                if (active != wasActive) {
                    BlockState updated = state.setValue(AlarmModule.ACTIVE, active);
                    current = current.withAttachment(slot, updated,
                            attachment.deform());
                    TransformConstructionNetwork.broadcastSurfaceSlot(level,
                            original.id(), slot, updated, attachment.deform());
                    changedSlots.add(slot);
                }
                if (active && (!wasActive || tick % LOOP_INTERVAL == 0)) {
                    playLoop(level, center);
                }
            }
            if (!changedSlots.isEmpty()) {
                data.putSurfaceState(current);
                for (ConstructionSurface.SurfaceSlot slot : changedSlots) {
                    TransformConstructionManager.refreshSurfaceSlotRuntime(
                            level.getServer(), original.id(), slot);
                }
                changed = true;
            }
        }
        return changed;
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
