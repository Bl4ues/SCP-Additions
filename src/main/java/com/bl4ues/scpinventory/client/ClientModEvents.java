package com.bl4ues.scpinventory.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "scp_additions", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("scp_additions", "scp_inventory_pickup_prompt"),
                (graphics, deltaTracker) -> {
                    if (Minecraft.getInstance().screen == null) {
                        PickupPromptClient.render(
                                graphics,
                                graphics.guiWidth(),
                                graphics.guiHeight(),
                                0.0F);
                    }
                });
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("scp_additions", "scp_inventory_context_prompt"),
                (graphics, deltaTracker) -> {
                    if (Minecraft.getInstance().screen == null) {
                        ContextPromptClient.render(
                                graphics,
                                graphics.guiWidth(),
                                graphics.guiHeight(),
                                0.0F);
                    }
                });
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath("scp_additions", "scp_inventory_full_notice"),
                (graphics, deltaTracker) -> InventoryFullOverlay.render(graphics));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_SCP_INVENTORY);
        event.register(Keybinds.CONTEXT_INTERACT);
        event.register(Keybinds.CONTEXT_CONFIG_SELECT);
    }
}
