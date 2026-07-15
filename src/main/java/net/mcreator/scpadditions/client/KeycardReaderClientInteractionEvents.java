package net.mcreator.scpadditions.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.UnifiedReaderItems;
import net.mcreator.scpadditions.keycard.KeycardReaderLevels;
import net.mcreator.scpadditions.network.KeycardReaderApplySavedLevelPacket;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class KeycardReaderClientInteractionEvents {
    private KeycardReaderClientInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickReader(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !Screen.hasControlDown()) return;
        boolean hasScrewdriver = event.getEntity().getMainHandItem().is(UnifiedReaderItems.SCREWDRIVER.get())
                || event.getEntity().getOffhandItem().is(UnifiedReaderItems.SCREWDRIVER.get());
        if (!hasScrewdriver || KeycardReaderLevels.describe(event.getLevel().getBlockState(event.getPos())) == null) return;

        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(new KeycardReaderApplySavedLevelPacket(event.getPos()));
    }
}
