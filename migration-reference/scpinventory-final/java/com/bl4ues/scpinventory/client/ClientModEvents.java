package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scpinventory", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("blink_vignette_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    BlinkClient.renderVignette(guiGraphics, width, height);
                });

        event.registerAboveAll("blink_blackout_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    BlinkClient.renderBlackout(guiGraphics, width, height);
                });

        event.registerAboveAll("player_vitals_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().player != null
                            && !gui.getMinecraft().player.isCreative()
                            && !gui.getMinecraft().player.isSpectator()) {
                        PlayerVitalsOverlay.render(guiGraphics, width, height, partialTick);
                    }
                });

        event.registerAboveAll("blink_meter_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    BlinkClient.renderHud(guiGraphics, width, height, partialTick);
                });

        event.registerAboveAll("pickup_prompt_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null) {
                        PickupPromptClient.render(guiGraphics, width, height, partialTick);
                    }
                });

        event.registerAboveAll("context_prompt_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    if (gui.getMinecraft().screen == null) {
                        ContextPromptClient.render(guiGraphics, width, height, partialTick);
                    }
                });

        event.registerAboveAll("inventory_full_overlay",
                (gui, guiGraphics, partialTick, width, height) -> {
                    InventoryFullOverlay.render(guiGraphics);
                });
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybinds.OPEN_SCP_INVENTORY);
        event.register(Keybinds.CONTEXT_INTERACT);
        event.register(Keybinds.CONTEXT_CONFIG_SELECT);
        event.register(Keybinds.BLINK);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SCP_173.get(), Scp173Renderer::new);
        event.registerEntityRenderer(ModEntities.SCP_131_A.get(), Scp131ARenderer::new);
        event.registerEntityRenderer(ModEntities.SCP_131_B.get(), Scp131BRenderer::new);
    }
}
