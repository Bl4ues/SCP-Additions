package com.bl4ues.scpclassifieddirective.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityStructureBreakGuard;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;

/**
 * Makes every visible or invisible part behave as one Tesla Gate structure.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TeslaGateStructureEvents {
    private TeslaGateStructureEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }

        BlockState state = event.getState();
        if (state.getBlock() == TeslaGateStructureBlocks.collision()) {
            BlockPos controllerPos = TeslaGateStructure.controllerPosition(
                    event.getPos(), state);
            event.setCanceled(true);
            FacilityStructureBreakGuard.clear(level, controllerPos);
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                Scp079FacilityAccessManager.unregisterTeslaGate(server, controllerPos);
            }
            TeslaGateStructure.destroyFromCollision(level, event.getPos(), state,
                    !event.getPlayer().isCreative());
            return;
        }

        if (TeslaGateStructure.isController(state)) {
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                Scp079FacilityAccessManager.unregisterTeslaGate(server, event.getPos());
            }
            FacilityStructureBreakGuard.clear(level, event.getPos());
            TeslaGateStructure.removeCollisionParts(level, event.getPos(), state);
        }
    }
}
