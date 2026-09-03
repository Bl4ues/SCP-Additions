package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.client.MineZeroRestoreVisualClient;
import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import com.bl4ues.scpclassifieddirective.client.SaveGameClientState;
import com.bl4ues.scpclassifieddirective.client.Scp939ClientState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        Scp714ComaPromptClient.render(graphics,
                                                virtualWidth, virtualHeight,
                                                partialTick));
                    }
                });
        event.registerAboveAll("scp_inventory_pickup_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null
                            && !Scp939ClientState.pinned()
                            && !Scp714ComaPromptClient.blocksInteractionLayer()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        PickupPromptClient.render(graphics,
                                                virtualWidth, virtualHeight,
                                                partialTick));
                    }
                });
        event.registerAboveAll("scp_inventory_context_prompt",
                (gui, graphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null
                            && !Scp939ClientState.pinned()
                            && !Scp714ComaPromptClient.blocksInteractionLayer()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        ContextPromptClient.render(graphics,
                                                virtualWidth, virtualHeight,
                                                partialTick));
                    }
                });
        event.registerAboveAll("scp_inventory_full_notice",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        InventoryFullOverlay.render(graphics));
                    }
                });
        event.registerAboveAll("scp_save_notice",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        SaveGameClientState.render(graphics,
                                                virtualWidth, virtualHeight));
                    }
                });
        event.registerAboveAll("scp_restore_transition",
                (gui, graphics, partialTick, width, height) -> {
                    if (!Scp939ClientState.pinned()) {
                        ResponsiveUiScale.renderHud(graphics, width, height,
                                (virtualWidth, virtualHeight) ->
                                        MineZeroRestoreVisualClient.render(
                                                graphics, virtualWidth,
                                                virtualHeight));
                    }
                });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.CONTEXT_CONFIG_SELECT);
        event.register(Keybinds.STOW_HELD_ITEM);
        event.register(Keybinds.QUICK_SAVE);
    }
}
