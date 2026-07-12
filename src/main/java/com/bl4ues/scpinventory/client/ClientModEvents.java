package com.bl4ues.scpinventory.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("scp_inventory_pickup_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null) {
                        PickupPromptClient.render(graphics, width, height, partialTick);
                    }
                });
        event.registerAboveAll("scp_inventory_context_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null) {
                        ContextPromptClient.render(graphics, width, height, partialTick);
                    }
                });
        event.registerAboveAll("scp_inventory_full_notice",
                (gui, graphics, partialTick, width, height) -> InventoryFullOverlay.render(graphics));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_SCP_INVENTORY);
        event.register(Keybinds.CONTEXT_INTERACT);
        event.register(Keybinds.CONTEXT_CONFIG_SELECT);
    }
}
