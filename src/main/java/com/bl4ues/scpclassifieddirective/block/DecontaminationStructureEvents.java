package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityStructureBreakGuard;
import com.bl4ues.scpclassifieddirective.procedures.DecontaminationCheckpointController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Makes controller, helpers and both BLACK_DOOR endpoints one breakable structure. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DecontaminationStructureEvents {
    private DecontaminationStructureEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (state.getBlock() == DecontaminationStructureBlocks.collision()) {
            event.setCanceled(true);
            DecontaminationStructure.destroyFromCollision(level, pos, state,
                    !event.getPlayer().isCreative());
            return;
        }

        if (DecontaminationStructure.isOwnedDoor(level, pos, state)) {
            BlockPos controllerPos = DecontaminationStructure.controllerForDoor(
                    level, pos, state);
            event.setCanceled(true);
            if (controllerPos != null) {
                DecontaminationCheckpointController.forget(level, controllerPos);
                FacilityStructureBreakGuard.clear(level, controllerPos);
            }
            DecontaminationStructure.destroyFromDoor(level, pos, state,
                    !event.getPlayer().isCreative());
            return;
        }

        if (DecontaminationStructure.isController(state)) {
            DecontaminationStructure.removeStructureParts(level, pos, state);
            DecontaminationCheckpointController.forget(level, pos);
            FacilityStructureBreakGuard.clear(level, pos);
        }
    }
}
