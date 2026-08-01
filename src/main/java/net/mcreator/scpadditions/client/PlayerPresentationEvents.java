package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Client hooks for optional first-person and vanilla-HUD suppression. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class PlayerPresentationEvents {
    private PlayerPresentationEvents() {
    }

    @SubscribeEvent
    public static void beforeHand(RenderHandEvent event) {
        if (InventoryModuleRuntimeState.hideEmptyHandForClient()
                && event.getItemStack().isEmpty()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void beforeOverlay(RenderGuiOverlayEvent.Pre event) {
        if (InventoryModuleRuntimeState.disableExperienceBarForClient()
                && event.getOverlay().id().equals(
                        VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            event.setCanceled(true);
            return;
        }

        if (InventoryModuleRuntimeState.customOxygenBarForClient()
                && event.getOverlay().id().equals(
                        VanillaGuiOverlay.AIR_LEVEL.id())) {
            event.setCanceled(true);
            return;
        }

        if (customHotbarActive()
                && (event.getOverlay().id().equals(
                        VanillaGuiOverlay.HOTBAR.id())
                || event.getOverlay().id().equals(
                        VanillaGuiOverlay.ITEM_NAME.id()))) {
            event.setCanceled(true);
        }
    }

    private static boolean customHotbarActive() {
        Player player = Minecraft.getInstance().player;
        return player != null
                && !player.isCreative()
                && !player.isSpectator()
                && InventoryModuleRuntimeState.isEnabledForClient()
                && InventoryModuleRuntimeState.customHotbarForClient();
    }
}
