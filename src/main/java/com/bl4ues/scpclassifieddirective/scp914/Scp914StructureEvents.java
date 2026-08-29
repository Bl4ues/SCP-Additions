package com.bl4ues.scpclassifieddirective.scp914;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Makes the SCP-914 controller and every visible helper one breakable structure. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp914StructureEvents {
    private Scp914StructureEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        boolean dropMachine = !event.getPlayer().isCreative();

        if (Scp914Structure.isHelper(state)) {
            event.setCanceled(true);
            Scp914Structure.destroyFromHelper(level, pos, state, dropMachine);
            return;
        }

        if (Scp914Structure.isController(state)) {
            event.setCanceled(true);
            Scp914Structure.destroyController(level, pos, dropMachine);
        }
    }
}
