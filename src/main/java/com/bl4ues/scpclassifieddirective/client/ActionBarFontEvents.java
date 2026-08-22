package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Applies the personal Roboto preference only to overlay/action-bar messages. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
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
