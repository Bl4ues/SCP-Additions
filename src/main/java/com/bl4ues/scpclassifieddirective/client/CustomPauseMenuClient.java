package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Replaces only the in-world pause screen when its client preference is enabled. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class CustomPauseMenuClient {
    private CustomPauseMenuClient() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!ClientModulePreferences.customPauseMenuEnabled()) return;

        if (PauseMenuSettingsPanelClient.shouldReplaceOptionsReturn(
                event.getScreen())) {
            event.setNewScreen(new CustomPauseMenuScreen());
            return;
        }

        if (event.getScreen() instanceof PauseScreen
                && !(event.getScreen() instanceof CustomPauseMenuScreen)) {
            event.setNewScreen(new CustomPauseMenuScreen());
        }
    }
}
