package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.MainMenuSounds;

import java.util.List;

/** Global hover feedback for standard GUI buttons while the custom menu theme is enabled. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class CustomButtonSoundsClient {
    private static Screen trackedScreen;
    private static AbstractButton hoveredButton;

    private CustomButtonSoundsClient() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        if (!ClientModulePreferences.customMainMenuEnabled()) {
            reset();
            return;
        }

        Screen screen = event.getScreen();
        if (screen != trackedScreen) {
            trackedScreen = screen;
            hoveredButton = null;
        }

        AbstractButton current = hoveredButtonAt(screen,
                event.getMouseX(), event.getMouseY());
        if (current == hoveredButton) return;

        hoveredButton = current;
        if (current != null) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(MainMenuSounds.HOVER.get(), 1.0F));
        }
    }

    private static AbstractButton hoveredButtonAt(Screen screen,
            int mouseX, int mouseY) {
        List<? extends GuiEventListener> listeners = screen.children();
        for (int index = listeners.size() - 1; index >= 0; index--) {
            GuiEventListener listener = listeners.get(index);
            if (listener instanceof AbstractButton button
                    && button.visible
                    && button.active
                    && button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }

    private static void reset() {
        trackedScreen = null;
        hoveredButton = null;
    }
}
