package net.mcreator.scpadditions.client;

import net.neoforged.neoforge.common.util.TriState;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.UnifiedReaderItems;
import net.mcreator.scpadditions.keycard.KeycardReaderLevels;
import net.mcreator.scpadditions.network.KeycardReaderApplySavedLevelPacket;
import net.mcreator.scpadditions.network.KeycardReaderCopyLevelPacket;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class KeycardReaderClientInteractionEvents {
    private KeycardReaderClientInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickReader(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        boolean applySavedLevel = Screen.hasControlDown();
        boolean copyCurrentLevel = Screen.hasShiftDown();
        if (!applySavedLevel && !copyCurrentLevel) return;
        boolean hasScrewdriver = event.getEntity().getMainHandItem().is(UnifiedReaderItems.SCREWDRIVER.get())
                || event.getEntity().getOffhandItem().is(UnifiedReaderItems.SCREWDRIVER.get());
        if (!hasScrewdriver || KeycardReaderLevels.describe(event.getLevel().getBlockState(event.getPos())) == null) return;

        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (applySavedLevel) {
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(new KeycardReaderApplySavedLevelPacket(event.getPos()));
        } else {
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(new KeycardReaderCopyLevelPacket(event.getPos()));
        }
    }
}
