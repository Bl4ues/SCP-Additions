package net.mcreator.scpadditions.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Makes every visible or invisible part behave as one Tesla Gate structure.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
            event.setCanceled(true);
            TeslaGateStructure.destroyFromCollision(level, event.getPos(), state,
                    !event.getPlayer().isCreative());
            return;
        }

        if (TeslaGateStructure.isController(state)) {
            TeslaGateStructure.removeCollisionParts(level, event.getPos(), state);
        }
    }
}
