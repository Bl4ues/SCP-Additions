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
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Keeps all Crosshair controls inside General & Modules preferences. */
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
    private static final Map<Screen, Button> DEFAULT_BUTTONS =
            new WeakHashMap<>();

    private CrosshairModulesPlacement() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if (isGeneralModulesScreen(screen)) {
            injectCrosshairRow(screen);
        } else if (isCrosshairScreen(screen)) {
            ensureCrosshairDefaultsPresent(screen);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen == null) return;

        if (isHomeScreen(screen)) {
            compactHomeLayout(screen);
        } else if (isGeneralModulesScreen(screen)) {
            wireCrosshairNavigation(screen);
        } else if (isCrosshairScreen(screen)) {
            wireEnabledDefaultsButton(screen);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (isHomeScreen(screen)) {
            // Another extension adds its widgets during Init.Post. Reapply this
            // every frame so the obsolete home entry can never reappear.
            compactHomeLayout(screen);
        } else if (isGeneralModulesScreen(screen)) {
            wireCrosshairNavigation(screen);
        } else if (isCrosshairScreen(screen)) {
            wireEnabledDefaultsButton(screen);
        }
    }

    private static boolean isHomeScreen(Screen screen) {
        return screen != null
                && HOME_SCREEN.equals(screen.getClass().getName());
    }

    private static boolean isGeneralModulesScreen(Screen screen) {
        return screen != null
                && EXTENDED_MODULES_SCREEN.equals(screen.getClass().getName())
                && "General & Modules".equals(screen.getTitle().getString());
    }

    private static boolean isCrosshairScreen(Screen screen) {
        return screen != null
                && CROSSHAIR_SCREEN.equals(screen.getClass().getName());
    }

    private static void compactHomeLayout(Screen screen) {
        Button crosshair = findButton(screen, "Crosshair");
        Button general = findButton(screen, "General & Modules");
        if (crosshair == null || general == null) return;

        int startY = crosshair.getY();
        int step = 27;
        crosshair.visible = false;
        crosshair.active = false;
        crosshair.setX(-10_000);

        general.setY(startY);
        setY(screen, "Inventory, Equipment & Codex", startY + step);
        setY(screen, "Contextual Interactions", startY + step * 2);
        setY(screen, "SCP-294 Drinks", startY + step * 3);
        setY(screen, "SCP-914 Recipes", startY + step * 4);
        setY(screen, "Accessibility", startY + step * 5);
        setY(screen, "Debug Tools", startY + step * 5);
        setY(screen, "Reload Snapshot", startY + step * 6 + 3);
        setY(screen, "Done", startY + step * 6 + 3);
    }

    private static Button findButton(Screen screen, String label) {
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof Button button
                    && label.equals(button.getMessage().getString())) {
                return button;
            }
        }
        return null;
    }

    private static void setY(Screen screen, String label, int y) {
        Button button = findButton(screen, label);
        if (button != null) button.setY(y);
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
            int preferencesIndex = -1;
            for (int i = 0; i < currentRows.size(); i++) {
                Object label = labelMethod.invoke(currentRows.get(i));
                if ("Crosshair".equals(label)) return;
                if ("Preferences".equals(label)) preferencesIndex = i;
            }

            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class,
                    String.class, boolean.class);
            constructor.setAccessible(true);
            Object crosshairRow = constructor.newInstance(
                    "crosshair", "enabled", "Crosshair",
                    "Configures the custom crosshair, visibility, color, and opacity.",
                    true);

            List<Object> updated = new ArrayList<>(currentRows);
            int insertAt = preferencesIndex >= 0
                    ? preferencesIndex + 1 : Math.min(1, updated.size());
            updated.add(insertAt, crosshairRow);
            rowsField.set(screen, List.copyOf(updated));
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not place Crosshair in Preferences",
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
            Button existing = NAVIGATION_BUTTONS.get(screen);
            Button source = null;
            Component sourceLabel = null;
            for (Map.Entry<Button, Component> entry : labels.entrySet()) {
                String label = entry.getValue().getString();
                if (entry.getKey() != existing
                        && label.startsWith("Crosshair: ")) {
                    source = entry.getKey();
                    sourceLabel = entry.getValue();
                    break;
                }
            }
            if (source == null || sourceLabel == null) return;

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
                    "Could not wire the Crosshair preference entry",
                    exception);
        }
    }

    private static void ensureCrosshairDefaultsPresent(Screen screen) {
        try {
            JsonObject settings = crosshairSettings(screen);
            if (!settings.has("enabled")) settings.addProperty("enabled", true);
            if (!settings.has("in_game_enabled")) {
                settings.addProperty("in_game_enabled", true);
            }
            if (!settings.has("red")) settings.addProperty("red", 1.0D);
            if (!settings.has("green")) settings.addProperty("green", 1.0D);
            if (!settings.has("blue")) settings.addProperty("blue", 1.0D);
            if (!settings.has("alpha")) settings.addProperty("alpha", 1.0D);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not apply Crosshair UI defaults",
                    exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void wireEnabledDefaultsButton(Screen screen) {
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
            Button existing = DEFAULT_BUTTONS.get(screen);
            Button source = null;
            Component sourceLabel = null;
            for (Map.Entry<Button, Component> entry : labels.entrySet()) {
                if (entry.getKey() != existing
                        && "Defaults".equals(entry.getValue().getString())) {
                    source = entry.getKey();
                    sourceLabel = entry.getValue();
                    break;
                }
            }
            if (source == null || sourceLabel == null) return;

            if (existing != null && buttons.contains(existing)) {
                source.visible = false;
                source.active = false;
                return;
            }

            source.visible = false;
            source.active = false;
            Button replacement = Button.builder(Component.empty(),
                    button -> resetCrosshairDefaults(screen))
                    .bounds(source.getX(), source.getY(),
                            source.getWidth(), source.getHeight())
                    .build();
            labels.put(replacement, sourceLabel);
            buttons.add(replacement);
            addRenderableWidget(screen, replacement);
            DEFAULT_BUTTONS.put(screen, replacement);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not wire enabled Crosshair defaults",
                    exception);
        }
    }

    private static void resetCrosshairDefaults(Screen screen) {
        try {
            JsonObject settings = crosshairSettings(screen);
            settings.addProperty("enabled", true);
            settings.addProperty("in_game_enabled", true);
            settings.addProperty("red", 1.0D);
            settings.addProperty("green", 1.0D);
            settings.addProperty("blue", 1.0D);
            settings.addProperty("alpha", 1.0D);
            invokeNoArg(screen, "rebuildWidgets");
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not restore Crosshair defaults",
                    exception);
        }
    }

    private static JsonObject crosshairSettings(Screen screen)
            throws ReflectiveOperationException {
        Field workingField = screen.getClass().getDeclaredField("working");
        workingField.setAccessible(true);
        Object value = workingField.get(screen);
        if (!(value instanceof JsonObject working)) {
            throw new IllegalStateException("Missing Crosshair working config");
        }
        if (!working.has("crosshair")
                || !working.get("crosshair").isJsonObject()) {
            working.add("crosshair", new JsonObject());
        }
        return working.getAsJsonObject("crosshair");
    }

    private static void addRenderableWidget(Screen screen, Button button)
            throws ReflectiveOperationException {
        Method target = findMethod(screen.getClass(), "addRenderableWidget", 1);
        if (target == null) {
            throw new NoSuchMethodException("Screen.addRenderableWidget");
        }
        target.setAccessible(true);
        target.invoke(screen, button);
    }

    private static void invokeNoArg(Screen screen, String methodName)
            throws ReflectiveOperationException {
        Method target = findMethod(screen.getClass(), methodName, 0);
        if (target == null) throw new NoSuchMethodException(methodName);
        target.setAccessible(true);
        target.invoke(screen);
    }

    private static Method findMethod(Class<?> start, String name,
            int parameterCount) {
        for (Class<?> type = start; type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (name.equals(method.getName())
                        && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
        }
        return null;
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
                    "Could not open Crosshair from Preferences",
                    exception);
        }
    }
}
