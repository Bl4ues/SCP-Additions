package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import net.minecraft.client.Minecraft;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public class ClientKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Keybinds.OPEN_SCP_INVENTORY.consumeClick()
                && ScpAdditionsModulesConfig.get().inventory.enabled) {
            ClientNetwork.requestInventorySync();
            Minecraft.getInstance().setScreen(new ScpInventoryScreen());
        }
    }
}
