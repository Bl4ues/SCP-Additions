package com.bl4ues.scpclassifieddirective.facility.blastdoor;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Makes every invisible Blast Door reservation cell break as one structure. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlastDoorStructureEvents {
    private BlastDoorStructureEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        BlockState state = event.getState();
        if (!BlastDoorModule.isPart(state)) return;

        BlockPos pos = event.getPos();
        event.setCanceled(true);
        BlastDoorStructure.destroyFromPart(level, pos, state,
                !event.getPlayer().isCreative());
    }
}
