package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;
import com.bl4ues.scpclassifieddirective.inventory.network.CopycatPanelRemovePacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Direct Screwdriver fallback for native copycat panels.
 *
 * Context Interaction normally owns the click, but the physical block must
 * remain editable when contextual prompts are disabled. Process once through
 * the main-hand event and accept the Screwdriver from either hand, matching
 * the reader/configuration interaction conventions used elsewhere in the mod.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WallPanelInteractionEvents {
    private WallPanelInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || KeycardReaderInteractionEvents.screwdriver(
                        event.getEntity()).isEmpty()) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        String interactionKey = null;

        if (state.is(WallPanelModule.BLOCK.get())) {
            WallPanelModule.Side side =
                    WallPanelModule.WallPanelBlock.sideForInteraction(
                            state, event.getPos(), event.getEntity(),
                            event.getHitVec().getDirection());
            if (side == null
                    || !(event.getLevel().getBlockEntity(event.getPos())
                        instanceof WallPanelModule.WallPanelBlockEntity panel)
                    || !panel.hasMaterial(side)) {
                return;
            }
            interactionKey = side == WallPanelModule.Side.FRONT
                    ? WallPanelModule.REMOVE_FRONT_INTERACTION
                    : WallPanelModule.REMOVE_BACK_INTERACTION;
        } else if (state.is(DoubleWallPanelModule.BLOCK.get())) {
            DoubleWallPanelModule.Side side =
                    DoubleWallPanelModule.DoubleWallPanelBlock
                            .sideForInteraction(state, event.getPos(),
                                    event.getHitVec(), event.getEntity());
            if (side == null
                    || !(event.getLevel().getBlockEntity(event.getPos())
                        instanceof DoubleWallPanelModule.DoubleWallPanelBlockEntity panel)
                    || !panel.hasMaterial(side)) {
                return;
            }
            interactionKey = side == DoubleWallPanelModule.Side.FRONT
                    ? DoubleWallPanelModule.REMOVE_FRONT_INTERACTION
                    : DoubleWallPanelModule.REMOVE_BACK_INTERACTION;
        } else {
            return;
        }

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(
                event.getLevel().isClientSide));

        if (event.getLevel().isClientSide) {
            ModNetwork.CHANNEL.sendToServer(new CopycatPanelRemovePacket(
                    event.getPos(), interactionKey));
        }
    }
}
