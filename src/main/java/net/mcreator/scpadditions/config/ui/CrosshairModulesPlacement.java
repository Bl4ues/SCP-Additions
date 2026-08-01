package net.mcreator.scpadditions.config.ui;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Places Crosshair as the first Gameplay Features entry instead of a home-screen category. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CrosshairModulesPlacement {
    private static final String HOME_SCREEN =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient$HomeScreen";
    private static final String EXTENDED_MODULES_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String CROSSHAIR_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$CrosshairScreen";
    private static final String ROW_TYPE =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$Row";

    private static final Map<Screen, Button> NAVIGATION_BUTTONS =
            new WeakHashMap<>();

    private CrosshairModulesPlacement() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if (isGeneralModulesScreen(screen)) {
            injectCrosshairRow(screen);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen == null) return;

        if (HOME_SCREEN.equals(screen.getClass().getName())) {
            restoreCompactHomeLayout(event);
        } else if (isGeneralModulesScreen(screen)) {
            wireCrosshairNavigation(screen);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (isGeneralModulesScreen(screen)) {
            // The module screen rebuilds its widgets after toggles and scrolling.
            // Rewire the navigation entry whenever that happens.
            wireCrosshairNavigation(screen);
        }
    }

    private static boolean isGeneralModulesScreen(Screen screen) {
        return screen != null
                && EXTENDED_MODULES_SCREEN.equals(screen.getClass().getName())
                && "General & Modules".equals(screen.getTitle().getString());
    }

    private static void restoreCompactHomeLayout(ScreenEvent.Init.Post event) {
        Map<String, Button> buttons = new LinkedHashMap<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button) {
                buttons.put(button.getMessage().getString(), button);
            }
        }

        Button crosshair = buttons.get("Crosshair");
        Button general = buttons.get("General & Modules");
        Button inventory = buttons.get("Inventory, Equipment & Codex");
        Button interactions = buttons.get("Contextual Interactions");
        Button drinks = buttons.get("SCP-294 Drinks");
        Button recipes = buttons.get("SCP-914 Recipes");
        Button accessibility = buttons.get("Accessibility");
        Button debug = buttons.get("Debug Tools");
        Button reload = buttons.get("Reload Snapshot");
        Button done = buttons.get("Done");
        if (crosshair == null || general == null || inventory == null
                || interactions == null || drinks == null || recipes == null
                || accessibility == null || debug == null || reload == null
                || done == null) {
            return;
        }

        int startY = crosshair.getY();
        int step = 27;
        crosshair.visible = false;
        crosshair.active = false;

        general.setY(startY);
        inventory.setY(startY + step);
        interactions.setY(startY + step * 2);
        drinks.setY(startY + step * 3);
        recipes.setY(startY + step * 4);
        accessibility.setY(startY + step * 5);
        debug.setY(startY + step * 5);

        int bottomY = startY + step * 6 + 3;
        reload.setY(bottomY);
        done.setY(bottomY);
    }

    private static void injectCrosshairRow(Screen screen) {
        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            Object value = rowsField.get(screen);
            if (!(value instanceof List<?> currentRows)) return;

            Class<?> rowType = Class.forName(ROW_TYPE);
            Method labelMethod = rowType.getDeclaredMethod("label");
            labelMethod.setAccessible(true);
            for (Object row : currentRows) {
                Object label = labelMethod.invoke(row);
                if ("Crosshair".equals(label)) return;
            }

            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class,
                    String.class, boolean.class);
            constructor.setAccessible(true);
            Object crosshairRow = constructor.newInstance(
                    "crosshair", "enabled", "Crosshair",
                    "Configures the custom crosshair, visibility, color, and opacity.",
                    false);

            List<Object> updated = new ArrayList<>(currentRows);
            int insertAt = Math.min(1, updated.size());
            updated.add(insertAt, crosshairRow);
            rowsField.set(screen, List.copyOf(updated));
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not place Crosshair in Gameplay Features",
                    exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void wireCrosshairNavigation(Screen screen) {
        try {
            Field buttonsField = screen.getClass().getDeclaredField("buttons");
            Field labelsField = screen.getClass().getDeclaredField("labels");
            buttonsField.setAccessible(true);
            labelsField.setAccessible(true);

            Object buttonsValue = buttonsField.get(screen);
            Object labelsValue = labelsField.get(screen);
            if (!(buttonsValue instanceof List<?> rawButtons)
                    || !(labelsValue instanceof Map<?, ?> rawLabels)) {
                return;
            }

            List<Button> buttons = (List<Button>) rawButtons;
            Map<Button, Component> labels = (Map<Button, Component>) rawLabels;
            Button source = null;
            Component sourceLabel = null;
            for (Map.Entry<Button, Component> entry : labels.entrySet()) {
                String label = entry.getValue().getString();
                if (label.startsWith("Crosshair: ")) {
                    source = entry.getKey();
                    sourceLabel = entry.getValue();
                    break;
                }
            }
            if (source == null || sourceLabel == null) return;

            Button existing = NAVIGATION_BUTTONS.get(screen);
            if (existing != null && buttons.contains(existing)) {
                source.visible = false;
                source.active = false;
                return;
            }

            source.visible = false;
            source.active = false;
            Button navigation = Button.builder(Component.empty(),
                    button -> openCrosshair(screen))
                    .bounds(source.getX(), source.getY(),
                            source.getWidth(), source.getHeight())
                    .build();
            labels.put(navigation, sourceLabel);
            buttons.add(navigation);
            addRenderableWidget(screen, navigation);
            NAVIGATION_BUTTONS.put(screen, navigation);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not wire the Crosshair gameplay entry",
                    exception);
        }
    }

    private static void addRenderableWidget(Screen screen, Button button)
            throws ReflectiveOperationException {
        Method target = null;
        for (Class<?> type = screen.getClass(); type != null && target == null;
                type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if ("addRenderableWidget".equals(method.getName())
                        && method.getParameterCount() == 1) {
                    target = method;
                    break;
                }
            }
        }
        if (target == null) {
            throw new NoSuchMethodException("Screen.addRenderableWidget");
        }
        target.setAccessible(true);
        target.invoke(screen, button);
    }

    private static void openCrosshair(Screen parent) {
        try {
            Field workingField = parent.getClass().getDeclaredField("working");
            workingField.setAccessible(true);
            Object value = workingField.get(parent);
            if (!(value instanceof JsonObject working)) return;

            Class<?> screenType = Class.forName(CROSSHAIR_SCREEN);
            Constructor<?> constructor = screenType.getDeclaredConstructor(
                    Screen.class, JsonObject.class);
            constructor.setAccessible(true);
            Object created = constructor.newInstance(parent, working);
            if (created instanceof Screen screen) {
                Minecraft.getInstance().setScreen(screen);
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not open Crosshair from General & Modules",
                    exception);
        }
    }
}
