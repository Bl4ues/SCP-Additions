package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Bridges the custom title scene into the New Game flow using the same long
 * spinner transition as the Configuration Center. The actual CreateWorldScreen
 * remains the world-creation backend so vanilla state, data packs and Forge
 * extension points are preserved for the custom presentation layer.
 */
public final class NewGameCreationClient {
    private static final Field CONFIGURATION_TRANSITION_FIELD;
    private static final Method BEGIN_SCREEN_TRANSITION_METHOD;

    static {
        Field field = null;
        Method method = null;
        try {
            field = CustomMainMenuScreen.class
                    .getDeclaredField("configurationTransition");
            field.setAccessible(true);
            method = CustomMainMenuScreen.class
                    .getDeclaredMethod("beginScreenTransition", Runnable.class);
            method.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not bind the animated New Game title transition",
                    exception);
        }
        CONFIGURATION_TRANSITION_FIELD = field;
        BEGIN_SCREEN_TRANSITION_METHOD = method;
    }

    private NewGameCreationClient() {
    }

    public static void openAnimated(Minecraft minecraft, Screen parent) {
        if (!(parent instanceof CustomMainMenuScreen menu)
                || CONFIGURATION_TRANSITION_FIELD == null
                || BEGIN_SCREEN_TRANSITION_METHOD == null) {
            prepareAndOpen(minecraft, parent);
            return;
        }

        Runnable open = () -> prepareAndOpen(minecraft, menu);
        try {
            CONFIGURATION_TRANSITION_FIELD.setBoolean(menu, true);
            BEGIN_SCREEN_TRANSITION_METHOD.invoke(menu, open);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not start the animated New Game transition; opening directly",
                    exception);
            prepareAndOpen(minecraft, menu);
        }
    }

    private static void prepareAndOpen(Minecraft minecraft, Screen parent) {
        if (parent instanceof CustomMainMenuScreen menu) {
            ConfigCenterVisuals.prepare(menu);
        }
        CreateWorldScreen.openFresh(minecraft, parent);
    }
}
