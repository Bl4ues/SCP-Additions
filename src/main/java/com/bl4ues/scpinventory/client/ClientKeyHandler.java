package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.network.EquipmentActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.UsableSessionReturnPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public class ClientKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Keybinds.OPEN_SCP_INVENTORY.consumeClick()
                && InventoryModuleRuntimeState.isEnabledForClient()) {
            ClientNetwork.requestInventorySync();
            Minecraft.getInstance().setScreen(new ScpInventoryScreen());
        }

        while (Keybinds.STOW_HELD_ITEM.consumeClick()) {
            stowHeldItem();
        }
    }

    private static void stowHeldItem() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null
                || !InventoryModuleRuntimeState.isEnabledForClient()) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;

        if (ScpItemClassifier.getEquipmentSlot(held).orElse(null)
                == ScpEquipmentSlot.WEAPON) {
            ModNetwork.CHANNEL.sendToServer(new EquipmentActionPacket(
                    ScpEquipmentSlot.WEAPON.name(),
                    EquipmentActionPacket.ACTION_UNEQUIP));
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new UsableSessionReturnPacket(
                player.getInventory().selected));
    }
}
