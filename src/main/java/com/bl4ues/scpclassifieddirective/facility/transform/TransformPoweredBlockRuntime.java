package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * Conservative redstone adapter for ordinary transformed blocks whose state is
 * represented by POWERED (and optionally LIT). Door-like OPEN blocks and Alarm
 * are handled by their dedicated runtimes so their authored timing/animation is
 * not flattened into a generic boolean update.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TransformPoweredBlockRuntime {
    private static final int UPDATE_INTERVAL = 2;

    private TransformPoweredBlockRuntime() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % UPDATE_INTERVAL != 0) return;
        TransformConstructionSavedData data = TransformConstructionSavedData.get(
                server);
        boolean changed = false;
        for (ServerLevel level : server.getAllLevels()) {
            changed |= updateGroups(level, data);
            changed |= updateSurfaces(level, data);
        }
        if (changed) TransformConstructionManager.refresh(server);
    }

    private static boolean updateGroups(ServerLevel level,
            TransformConstructionSavedData data) {
        boolean changed = false;
        for (TransformGroup original : data.groups()) {
            if (!original.dimension().equals(level.dimension().location())) continue;
            TransformGroup next = original;
            boolean localChanged = false;
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : original.cells().entrySet()) {
                BlockState state = entry.getValue();
                if (!eligible(state)) continue;
                boolean powered = hasSignal(level,
                        original.cellCenter(entry.getKey()));
                BlockState updated = poweredState(state, powered);
                if (updated.equals(state)) continue;
                next = next.withCell(entry.getKey(), updated);
                localChanged = true;
            }
            if (localChanged) {
                data.putGroup(next);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean updateSurfaces(ServerLevel level,
            TransformConstructionSavedData data) {
        boolean changed = false;
        for (ConstructionSurface original : data.surfaces()) {
            if (!original.dimension().equals(level.dimension().location())) continue;
            ConstructionSurface next = original;
            boolean localChanged = false;
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : original.attachments().entrySet()) {
                BlockState state = entry.getValue().state();
                if (!eligible(state)) continue;
                ConstructionSurface.SurfaceSlot slot = entry.getKey();
                Vec3 center = original.gridPoint(
                        (slot.column() + 0.5D) / original.columns(),
                        (slot.row() + 0.5D) / original.rows());
                boolean powered = hasSignal(level, center);
                BlockState updated = poweredState(state, powered);
                if (updated.equals(state)) continue;
                next = next.withAttachment(slot, updated,
                        entry.getValue().deform());
                localChanged = true;
            }
            if (localChanged) {
                data.putSurface(next);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean eligible(BlockState state) {
        return state != null
                && state.hasProperty(BlockStateProperties.POWERED)
                && !state.hasProperty(BlockStateProperties.OPEN)
                && !AlarmModule.isController(state);
    }

    private static BlockState poweredState(BlockState state, boolean powered) {
        BlockState updated = state.setValue(BlockStateProperties.POWERED, powered);
        if (updated.hasProperty(BlockStateProperties.LIT)) {
            updated = updated.setValue(BlockStateProperties.LIT, powered);
        }
        return updated;
    }

    private static boolean hasSignal(ServerLevel level, Vec3 center) {
        BlockPos base = BlockPos.containing(center);
        if (level.hasNeighborSignal(base)) return true;
        // A rotated object's physical body can cross the immediately adjacent
        // vanilla cell. Keep this local so redstone does not become wireless.
        for (Direction direction : Direction.values()) {
            if (level.hasNeighborSignal(base.relative(direction))) return true;
        }
        return false;
    }
}
