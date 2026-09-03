package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.client.gui.ScpInventoryScreen;
import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpEquipmentSlot;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.network.EquipmentActionPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.network.UsableSessionReturnPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.QuickSavePacket;

@Mod.EventBusSubscriber(modid = "scp_classified_directive", value = Dist.CLIENT)
public class ClientKeyHandler {

    /**
     * Consume the vanilla inventory mapping before Minecraft handles it. Polling
     * the KeyMapping rather than a raw keyboard event also honors mouse rebinds.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        openScpInventoryFromVanillaKey();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        while (Keybinds.STOW_HELD_ITEM.consumeClick()) {
            stowHeldItem();
        }

        while (Keybinds.QUICK_SAVE.consumeClick()) {
            quickSave();
        }
    }

    private static void openScpInventoryFromVanillaKey() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!InventoryModuleRuntimeState.isEnabledForClient()
                || player == null || minecraft.screen != null
                || !player.isAlive() || player.isSpectator()) {
            return;
        }

        boolean requested = false;
        while (minecraft.options.keyInventory.consumeClick()) {
            requested = true;
        }
        if (!requested) return;

        ClientNetwork.requestInventorySync();
        minecraft.setScreen(new ScpInventoryScreen());
    }

    private static void quickSave() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || !player.isAlive() || player.isSpectator()) {
            return;
        }
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(new QuickSavePacket());
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

        int selectedSlot = player.getInventory().selected;
        if (PlaceableHotbarSessionClient.returnSelectedSession(selectedSlot)) {
            return;
        }
        if (UsableHotbarSessionClient.returnSelectedSession(selectedSlot)) {
            return;
        }

        // Fallback for a server-tracked session that the client has not learned
        // about yet, for example immediately after joining or a late sync.
        ModNetwork.CHANNEL.sendToServer(new UsableSessionReturnPacket(selectedSlot));
    }
}
