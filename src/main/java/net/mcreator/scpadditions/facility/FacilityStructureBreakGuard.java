package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.mcreator.scpadditions.block.DecontaminationStructure;
import net.mcreator.scpadditions.block.DecontaminationStructureBlocks;
import net.mcreator.scpadditions.block.TeslaGateStructure;
import net.mcreator.scpadditions.block.TeslaGateStructureBlocks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Animated facility structures swap their controller block while operating.
 * Mining progress is tied to the exact block state, so those swaps used to
 * reset breaking repeatedly. BreakSpeed fires continuously while a player is
 * mining; this guard briefly freezes transitions at the relevant controller.
 */
public final class FacilityStructureBreakGuard {
    private static final long MINING_GRACE_TICKS = 8L;
    private static final Map<StructureKey, Long> MINED_UNTIL =
            new ConcurrentHashMap<>();

    private FacilityStructureBreakGuard() {
    }

    public static void observeMining(Level level, BlockPos hitPos,
            BlockState hitState) {
        if (level == null || level.isClientSide || hitPos == null
                || hitState == null) {
            return;
        }
        BlockPos controllerPos = resolveController(hitPos, hitState);
        if (controllerPos == null) {
            return;
        }
        MINED_UNTIL.put(new StructureKey(level.dimension(),
                controllerPos.asLong()),
                level.getGameTime() + MINING_GRACE_TICKS);
    }

    public static boolean isBeingMined(LevelAccessor world,
            BlockPos controllerPos) {
        if (!(world instanceof ServerLevel level) || controllerPos == null) {
            return false;
        }
        StructureKey key = new StructureKey(level.dimension(),
                controllerPos.asLong());
        Long until = MINED_UNTIL.get(key);
        if (until == null) {
            return false;
        }
        if (until < level.getGameTime()) {
            MINED_UNTIL.remove(key, until);
            return false;
        }
        return true;
    }

    public static void clear(LevelAccessor world, BlockPos controllerPos) {
        if (!(world instanceof Level level) || controllerPos == null) {
            return;
        }
        MINED_UNTIL.remove(new StructureKey(level.dimension(),
                controllerPos.asLong()));
    }

    private static BlockPos resolveController(BlockPos hitPos,
            BlockState hitState) {
        if (TeslaGateStructure.isController(hitState)
                || DecontaminationStructure.isController(hitState)) {
            return hitPos.immutable();
        }
        if (hitState.getBlock() == TeslaGateStructureBlocks.collision()) {
            return TeslaGateStructure.controllerPosition(hitPos, hitState)
                    .immutable();
        }
        if (hitState.getBlock()
                == DecontaminationStructureBlocks.collision()) {
            return DecontaminationStructure.controllerPosition(hitPos,
                    hitState).immutable();
        }
        return null;
    }

    private record StructureKey(ResourceKey<Level> dimension, long pos) {
    }
}
