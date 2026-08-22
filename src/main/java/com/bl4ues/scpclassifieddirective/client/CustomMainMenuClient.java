package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Keeps the authored title presentation in control of title-menu navigation. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class CustomMainMenuClient {
    private CustomMainMenuClient() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (MainMenuSettingsPanelClient.shouldReplaceOptionsReturn(
                event.getScreen())) {
            Screen target = ClientModulePreferences.customMainMenuEnabled()
                    ? new CustomMainMenuScreen() : new TitleScreen();
            event.setNewScreen(target);
            return;
        }

        if (!(event.getScreen() instanceof TitleScreen)
                || event.getScreen() instanceof CustomMainMenuScreen) {
            return;
        }

        Screen title = ClientModulePreferences.customMainMenuEnabled()
                ? new CustomMainMenuScreen() : event.getScreen();
        if (Scp939VoiceSetupScreen.shouldOpenAutomatically()) {
            event.setNewScreen(new Scp939VoiceSetupScreen(title));
            return;
        }

        if (ClientModulePreferences.customMainMenuEnabled()) {
            event.setNewScreen(title);
        }
    }
}
