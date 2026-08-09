package net.mcreator.scpadditions.client;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Keeps the authored title presentation in control of title-menu navigation. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CustomMainMenuClient {
    private CustomMainMenuClient() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!ClientModulePreferences.customMainMenuEnabled()) return;

        if (MainMenuSettingsPanelClient.shouldReplaceOptionsReturn(
                event.getScreen())) {
            event.setNewScreen(new CustomMainMenuScreen());
            return;
        }

        if (event.getScreen() instanceof TitleScreen
                && !(event.getScreen() instanceof CustomMainMenuScreen)) {
            event.setNewScreen(new CustomMainMenuScreen());
        }
    }
}
