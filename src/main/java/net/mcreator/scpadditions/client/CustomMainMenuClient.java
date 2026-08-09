package net.mcreator.scpadditions.client;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Replaces only the vanilla title presentation; underlying menu actions stay intact. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CustomMainMenuClient {
    private CustomMainMenuClient() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!ClientModulePreferences.customMainMenuEnabled()) return;
        if (event.getScreen() instanceof TitleScreen
                && !(event.getScreen() instanceof CustomMainMenuScreen)) {
            event.setNewScreen(new CustomMainMenuScreen());
        }
    }
}
