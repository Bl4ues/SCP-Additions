package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Break-time cleanup for facility blocks that genuinely occupy a second
 * structural position. Unity door buttons are deliberately independent and
 * never remove another button when broken.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilityBlockEvents {
    private FacilityBlockEvents() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        Block block = event.getState().getBlock();

        // The upper wall-light block normally has no loot table because breaking
        // the lower half already drops the public item. Breaking the upper half
        // directly must still return exactly one wall light outside creative.
        if (block == FacilityModule.WALLLIGHT_2.get()) {
            if (!event.getPlayer().isCreative()) {
                Block.popResource(level, event.getPos(),
                        new ItemStack(FacilityModule.WALLLIGHT.get()));
            }
            BlockPos lower = event.getPos().below();
            if (level.getBlockState(lower).is(FacilityModule.WALLLIGHT.get())) {
                level.destroyBlock(lower, false);
            }
        }
    }
}
