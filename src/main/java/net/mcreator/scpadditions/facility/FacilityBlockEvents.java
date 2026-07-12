package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Break-time cleanup for the few facility blocks that occupy or create a
 * second logical position. Counterparts are removed without a second drop.
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

        BlockState state = event.getState();
        Block block = state.getBlock();

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
            return;
        }

        if (!isDoorButton(block) || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos counterpart = event.getPos().relative(facing.getOpposite(), 2);
        if (isDoorButton(level.getBlockState(counterpart).getBlock())) {
            level.destroyBlock(counterpart, false);
        }
    }

    private static boolean isDoorButton(Block block) {
        return block == FacilityModule.BUTTON_LOCKED.get()
                || block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get();
    }
}
