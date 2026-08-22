package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;
import com.bl4ues.scpclassifieddirective.network.KeycardReaderApplySavedLevelPacket;
import com.bl4ues.scpclassifieddirective.network.KeycardReaderCopyLevelPacket;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (applySavedLevel) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(new KeycardReaderApplySavedLevelPacket(event.getPos()));
        } else {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(new KeycardReaderCopyLevelPacket(event.getPos()));
        }
    }
}
