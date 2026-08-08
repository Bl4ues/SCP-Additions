package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.client.gui.ScpStorageContainerScreen;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpinventory.container.StorageContainerSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces only plain slot-based storage screens while the SCP Inventory module
 * is active. Processing, crafting, equipment and other semantic menus keep
 * their native screens.
 */
@Mod.EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class StorageContainerScreenEvents {
    private StorageContainerScreenEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!InventoryModuleRuntimeState.isEnabledForClient()) {
            return;
        }

        Screen incoming = event.getNewScreen();
        if (!(incoming instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        if (!StorageContainerSupport.isSupported(
                containerScreen.getMenu(), player.getInventory())) {
            return;
        }

        ClientNetwork.requestInventorySync();
        event.setNewScreen(new ScpStorageContainerScreen(
                containerScreen.getMenu(), containerScreen.getTitle()));
    }
}
