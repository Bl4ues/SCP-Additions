package com.bl4ues.scpinventory.client;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import net.minecraft.client.Minecraft;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public class ClientKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Keybinds.OPEN_SCP_INVENTORY.consumeClick()
                && InventoryModuleRuntimeState.isEnabledForClient()) {
            ClientNetwork.requestInventorySync();
            Minecraft.getInstance().setScreen(new ScpInventoryScreen());
        }
    }
}
