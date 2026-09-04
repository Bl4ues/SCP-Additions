package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Bridges the custom title scene into the New Game flow using the same spinner
 * transition as the Configuration Center. The actual CreateWorldScreen remains
 * the world-creation backend so vanilla state, data packs and Forge extension
 * points are preserved for the custom presentation layer.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NewGameCreationClient {
    private static final Field CONFIGURATION_TRANSITION_FIELD;
    private static final Method BEGIN_SCREEN_TRANSITION_METHOD;

    private static Screen pendingParent;
    private static boolean pendingCustomPresentation;
    private static boolean awaitingCreateWorld;

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

    /**
     * CreateWorldScreen.openFresh briefly opens GenericDirtMessageScreen while
     * its worldgen registries load. Keep that asynchronous vanilla work, but
     * replace only its presentation so the title backdrop/spinner never flashes
     * to Minecraft's dirt loading screen between the two custom interfaces.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!awaitingCreateWorld) return;
        Screen incoming = event.getNewScreen();
        if (incoming instanceof CreateWorldScreen) {
            awaitingCreateWorld = false;
            return;
        }
        if (incoming instanceof GenericDirtMessageScreen
                && !(incoming instanceof NewGameLoadingBridgeScreen)) {
            event.setNewScreen(new NewGameLoadingBridgeScreen());
        }
    }

    static boolean consumeCustomPresentation(CreateWorldScreen screen) {
        if (!pendingCustomPresentation || screen == null) return false;
        pendingCustomPresentation = false;
        return true;
    }

    static Screen consumePendingParent() {
        Screen parent = pendingParent;
        pendingParent = null;
        return parent;
    }

    private static void prepareAndOpen(Minecraft minecraft, Screen parent) {
        pendingParent = parent;
        pendingCustomPresentation = parent instanceof CustomMainMenuScreen;
        awaitingCreateWorld = pendingCustomPresentation;
        if (parent instanceof CustomMainMenuScreen menu) {
            ConfigCenterVisuals.prepare(menu);
        }
        try {
            CreateWorldScreen.openFresh(minecraft, parent);
        } catch (RuntimeException exception) {
            awaitingCreateWorld = false;
            pendingCustomPresentation = false;
            pendingParent = null;
            throw exception;
        }
    }

    private static final class NewGameLoadingBridgeScreen extends Screen {
        private NewGameLoadingBridgeScreen() {
            super(Component.literal("New Game"));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            ConfigCenterVisuals.renderBackdrop(this, graphics, mouseX, mouseY);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
