package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Applies the personal Roboto preference only to overlay/action-bar messages. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ActionBarFontEvents {
    private ActionBarFontEvents() {
    }

    @SubscribeEvent
    public static void onSystemMessage(ClientChatReceivedEvent.System event) {
        if (event.isOverlay()
                && InventoryModuleRuntimeState.actionBarsRobotoForClient()) {
            event.setMessage(ScpFonts.roboto(event.getMessage()));
        }
    }
}
