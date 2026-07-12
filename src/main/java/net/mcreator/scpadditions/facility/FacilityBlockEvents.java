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
 * Break-time cleanup for facility multiblocks and manually assembled Unity
 * button pairs. Original and mirrored visual variants are equivalent here.
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

        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        boolean locked = isLockedButton(block);
        boolean functional = isFunctionalButton(block);
        if (!locked && !functional) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos counterpartPos = event.getPos().relative(facing.getOpposite(), 2);
        BlockState counterpartState = level.getBlockState(counterpartPos);
        Block counterpart = counterpartState.getBlock();

        boolean correctlyFacing = counterpartState.hasProperty(HorizontalDirectionalBlock.FACING)
                && counterpartState.getValue(HorizontalDirectionalBlock.FACING) == facing.getOpposite();
        boolean matchingCounterpart = correctlyFacing && (locked
                ? isLockedButton(counterpart)
                : isFunctionalButton(counterpart));

        if (matchingCounterpart) {
            level.destroyBlock(counterpartPos, false);
        }
    }

    private static boolean isLockedButton(Block block) {
        return block == FacilityModule.BUTTON_LOCKED.get()
                || MirroredDoorButtons.isLocked(block);
    }

    private static boolean isFunctionalButton(Block block) {
        return block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get()
                || MirroredDoorButtons.isFunctional(block);
    }
}
