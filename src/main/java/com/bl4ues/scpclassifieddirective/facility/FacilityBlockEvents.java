package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * Break-time cleanup for facility multiblocks.
 *
 * Door buttons are deliberately excluded from ownership cleanup. Opposite
 * panels may synchronize interaction/state, but each panel remains an
 * independently placed block and breaking one must never remove the other.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

        if (block == FacilityModule.FACILITY_PROP_PART.get()) {
            BlockPos controllerPos =
                    FacilityLargePropStructure.controllerPosition(
                            event.getPos(), state);
            event.setCanceled(true);
            FacilityStructureBreakGuard.clear(level, controllerPos);
            FacilityLargePropStructure.destroyFromPart(level,
                    event.getPos(), state,
                    !event.getPlayer().isCreative());
            return;
        }

        if (block == FacilityModule.SIGN_SUPPORT.get()) {
            FacilityLargePropStructure.removeParts(level, event.getPos(),
                    FacilityLargePropStructure.Kind.SIGN_SUPPORT,
                    state.getValue(HorizontalDirectionalBlock.FACING));
        } else if (block == FacilityModule.TV.get()) {
            FacilityLargePropStructure.removeParts(level, event.getPos(),
                    FacilityLargePropStructure.Kind.TV,
                    state.getValue(DirectionalBlock.FACING));
        }

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
