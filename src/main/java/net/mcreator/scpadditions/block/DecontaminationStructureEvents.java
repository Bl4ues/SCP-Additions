package net.mcreator.scpadditions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.FacilityStructureBreakGuard;
import net.mcreator.scpadditions.procedures.DecontaminationCheckpointController;

/**
 * Makes every visible or invisible checkpoint part behave as one structure.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DecontaminationStructureEvents {
    private DecontaminationStructureEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }

        BlockState state = event.getState();
        if (state.getBlock() == DecontaminationStructureBlocks.collision()) {
            BlockPos controllerPos = DecontaminationStructure
                    .controllerPosition(event.getPos(), state);
            event.setCanceled(true);
            DecontaminationCheckpointController.forget(level, controllerPos);
            FacilityStructureBreakGuard.clear(level, controllerPos);
            DecontaminationStructure.destroyFromCollision(level,
                    event.getPos(), state, !event.getPlayer().isCreative());
            return;
        }

        if (DecontaminationStructure.isController(state)) {
            DecontaminationStructure.removeCollisionParts(level,
                    event.getPos(), state);
            DecontaminationCheckpointController.forget(level, event.getPos());
            FacilityStructureBreakGuard.clear(level, event.getPos());
        }
    }
}
