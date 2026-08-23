package com.bl4ues.scpclassifieddirective.inventory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.client.MineZeroRestoreVisualClient;
import com.bl4ues.scpclassifieddirective.client.SaveGameClientState;
import com.bl4ues.scpclassifieddirective.client.Scp939ClientState;

@Mod.EventBusSubscriber(modid = "scp_classified_directive", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("scp_714_coma_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null
                            && !Scp939ClientState.pinned()) {
                        Scp714ComaPromptClient.render(graphics, width, height,
                                partialTick);
                    }
                });
        event.registerAboveAll("scp_inventory_pickup_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null
                            && !Scp939ClientState.pinned()
                            && !Scp714ComaPromptClient.blocksInteractionLayer()) {
                        PickupPromptClient.render(graphics, width, height,
                                partialTick);
                    }
                });
        event.registerAboveAll("scp_inventory_context_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null
                            && !Scp939ClientState.pinned()
                            && !Scp714ComaPromptClient.blocksInteractionLayer()) {
                        ContextPromptClient.render(graphics, width, height,
                                partialTick);
                    }
                });
        event.registerAboveAll("scp_inventory_full_notice",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        InventoryFullOverlay.render(graphics);
                    }
                });
        event.registerAboveAll("scp_save_notice",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        SaveGameClientState.render(graphics, width, height);
                    }
                });
        event.registerAboveAll("scp_restore_transition",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        MineZeroRestoreVisualClient.render(graphics,
                                width, height);
                    }
                });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_SCP_INVENTORY);
        event.register(Keybinds.CONTEXT_INTERACT);
        event.register(Keybinds.CONTEXT_CONFIG_SELECT);
        event.register(Keybinds.STOW_HELD_ITEM);
        event.register(Keybinds.QUICK_SAVE);
    }
}
