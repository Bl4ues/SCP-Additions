package net.mcreator.scpadditions.config.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * The Configuration Center carries descriptions in its own layout. Mouse-anchored
 * vanilla tooltips duplicate that information, cover controls and clash with the
 * rest of the presentation, so they are intentionally suppressed on every
 * Configuration Center screen.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ConfigCenterTooltipSuppression {
    private ConfigCenterTooltipSuppression() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTooltip(RenderTooltipEvent.Pre event) {
        if (isConfigurationScreen(Minecraft.getInstance().screen)) {
            event.setCanceled(true);
        }
    }

    private static boolean isConfigurationScreen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        if (name.startsWith("net.mcreator.scpadditions.config.ui.ConfigCenterClient$")
                || name.startsWith(
                "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$")
                || name.startsWith(
                "net.mcreator.scpadditions.client.RoombaConfigCenterEnhancements$")
                || name.equals(
                "net.mcreator.scpadditions.config.ui.ModCompatibilitiesUi$ModCompatibilitiesScreen")) {
            return true;
        }
        String simple = screen.getClass().getSimpleName();
        return "ItemConfigScreen".equals(simple)
                || "ContextConfigScreen".equals(simple)
                || "UnityColorPickerScreen".equals(simple)
                || "CodexImageDropScreen".equals(simple)
                || "CodexTextEditorScreen".equals(simple);
    }
}
