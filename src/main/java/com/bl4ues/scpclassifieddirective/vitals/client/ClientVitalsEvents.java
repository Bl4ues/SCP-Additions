package com.bl4ues.scpclassifieddirective.vitals.client;

import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.vitals.VitalsModule;

/** Client Forge-bus hooks for stamina prediction and vanilla HUD replacement. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class ClientVitalsEvents {
    private ClientVitalsEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PlayerVitalsClient.clientTick();
        }
    }

    @SubscribeEvent
    public static void beforeOverlay(RenderGuiOverlayEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        if (InventoryModuleRuntimeState.hideActiveEffectIndicatorsForClient()
                && event.getOverlay().id().equals(
                        VanillaGuiOverlay.POTION_ICONS.id())) {
            event.setCanceled(true);
            return;
        }
        if (InventoryModuleRuntimeState.hungerDisabledForClient()
                && event.getOverlay().id().equals(
                        VanillaGuiOverlay.FOOD_LEVEL.id())) {
            event.setCanceled(true);
            return;
        }
        if (!player.isCreative() && !player.isSpectator()
                && VitalsModule.healthHudEnabled()
                && (event.getOverlay().id().equals(
                        VanillaGuiOverlay.PLAYER_HEALTH.id())
                || event.getOverlay().id().equals(
                        VanillaGuiOverlay.ARMOR_LEVEL.id()))) {
            event.setCanceled(true);
        }
    }
}
