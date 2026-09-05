package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Makes presentation modules personal while leaving simulation and world rules
 * under host/operator control.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class ClientPreferenceModulesUi {
    private static final String HOME_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterClient$HomeScreen";
    private static final String EXTENDED_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String CROSSHAIR_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$CrosshairScreen";
    private static final String DEBUG_TITLE = "Debug Tools";

    private static final int CLIENT_SCOPE = 0xFF79D58B;
    private static final int SERVER_SCOPE = 0xFFFFC56D;
    private static final int SERVER_LOCKED_SCOPE = 0xFFFF7373;
    private static final int BADGE_BACKGROUND = 0xE6081022;
    private static final float BADGE_SCALE = 0.74F;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();
    private static final Map<Screen, JsonObject> BASELINES =
            new WeakHashMap<>();
    private static final Map<Screen, Button> SAVE_REPLACEMENTS =
            new WeakHashMap<>();

    private ClientPreferenceModulesUi() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if ((!isExtended(screen) || isDebugTools(screen))
                && !isCrosshair(screen)) return;
        try {
            JsonObject working = working(screen);
            BASELINES.putIfAbsent(screen, working.deepCopy());
            ClientModulePreferences.applyTo(working);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not apply personal module preferences", exception);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInitPost(ScreenEvent.Init.Post event) {
        refresh(event.getScreen());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        refresh(screen);
        if (isExtended(screen) && !isDebugTools(screen)) {
            renderScopeBadges(event.getGuiGraphics(), screen);
        }
    }

    private static void refresh(Screen screen) {
        if (isHome(screen)) {
            applyHomePermissions(screen);
        } else if (isExtended(screen) && !isDebugTools(screen)) {
            enforceRowPermissions(screen);
            wireExtendedSave(screen);
        } else if (isCrosshair(screen)) {
            wireCrosshairSave(screen);
        }
    }

    private static boolean isHome(Screen screen) {
        return screen != null && HOME_SCREEN.equals(screen.getClass().getName());
    }

    private static boolean isExtended(Screen screen) {
        return screen != null
                && EXTENDED_SCREEN.equals(screen.getClass().getName());
    }

    private static boolean isCrosshair(Screen screen) {
        return screen != null
                && CROSSHAIR_SCREEN.equals(screen.getClass().getName());
    }

    private static boolean isDebugTools(Screen screen) {
        return isExtended(screen)
                && DEBUG_TITLE.equals(screen.getTitle().getString());
    }

    private static void applyHomePermissions(Screen screen) {
        boolean canEdit = canEditServer();
        setActive(screen, "General & Modules", true);
        setActive(screen, "Accessibility", true);
        setActive(screen, "Crosshair", true);
        setActive(screen, "Inventory, Equipment & Codex", canEdit);
        setActive(screen, "Contextual Interactions", canEdit);
        setActive(screen, "SCP-294 Drinks", canEdit);
        setActive(screen, "SCP-914 Recipes", canEdit);
        setActive(screen, "Debug Tools", canEdit);
    }

    private static void setActive(Screen screen, String label, boolean active) {
        for (var listener : screen.children()) {
            if (listener instanceof Button button
                    && label.equals(button.getMessage().getString())) {
                button.active = active;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void enforceRowPermissions(Screen screen) {
        try {
            Field buttonsField = screen.getClass().getDeclaredField("buttons");
            Field labelsField = screen.getClass().getDeclaredField("labels");
            buttonsField.setAccessible(true);
            labelsField.setAccessible(true);
            Object buttonsValue = buttonsField.get(screen);
            Object labelsValue = labelsField.get(screen);
            if (!(buttonsValue instanceof List<?> rawButtons)
                    || !(labelsValue instanceof Map<?, ?> rawLabels)) return;

            List<Button> buttons = (List<Button>) rawButtons;
            Map<Button, Component> labels = (Map<Button, Component>) rawLabels;
            Map<String, Boolean> scopes = rowScopes(screen);
            boolean canEdit = canEditServer();

            for (Button button : buttons) {
                Component label = labels.get(button);
                if (label == null || !button.visible) continue;
                String base = stripState(label.getString());

                Boolean personal = scopeForLabel(scopes, base);
                if (personal != null) {
                    button.active = (personal || canEdit)
                            && dependencyAvailable(screen, base);
                }
            }
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not apply module permission scopes", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void renderScopeBadges(GuiGraphics graphics,
            Screen screen) {
        try {
            Field buttonsField = screen.getClass().getDeclaredField("buttons");
            Field labelsField = screen.getClass().getDeclaredField("labels");
            buttonsField.setAccessible(true);
            labelsField.setAccessible(true);
            Object buttonsValue = buttonsField.get(screen);
            Object labelsValue = labelsField.get(screen);
            if (!(buttonsValue instanceof List<?> rawButtons)
                    || !(labelsValue instanceof Map<?, ?> rawLabels)) return;

            List<Button> buttons = (List<Button>) rawButtons;
            Map<Button, Component> labels = (Map<Button, Component>) rawLabels;
            Map<String, Boolean> scopes = rowScopes(screen);
            boolean canEdit = canEditServer();
            Font font = Minecraft.getInstance().font;

            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 1450.0F);
            for (Button button : buttons) {
                if (!button.visible) continue;
                Component label = labels.get(button);
                if (label == null) continue;
                String base = stripState(label.getString());
                if ("Test Voice".equals(base)) continue;
                Boolean personal = scopeForLabel(scopes, base);
                if (personal == null) continue;

                String text = personal ? "CLIENT" : "SERVER";
                Component badge = ScpFonts.roboto(text);
                int color = personal ? CLIENT_SCOPE
                        : canEdit ? SERVER_SCOPE : SERVER_LOCKED_SCOPE;
                int scaledWidth = Math.round(font.width(badge) * BADGE_SCALE);
                int scaledHeight = Math.max(6,
                        Math.round(font.lineHeight * BADGE_SCALE));
                int right = button.getX() + button.getWidth() - 58;
                int x = right - scaledWidth;
                int y = button.getY()
                        + Math.max(1, (button.getHeight() - scaledHeight) / 2);

                graphics.fill(x - 4, button.getY() + 2,
                        right + 3, button.getY() + button.getHeight() - 2,
                        BADGE_BACKGROUND);
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0.0F);
                graphics.pose().scale(BADGE_SCALE, BADGE_SCALE, 1.0F);
                graphics.drawString(font, badge, 0, 0, color, false);
                graphics.pose().popPose();
            }
            graphics.pose().popPose();
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not render module permission scopes", exception);
        }
    }

    private static boolean dependencyAvailable(Screen screen,
            String label) {
        if (!"Custom Item Interaction Sounds".equals(label)) return true;
        try {
            JsonObject modules = working(screen);
            if (!modules.has("inventory")
                    || !modules.get("inventory").isJsonObject()) return true;
            JsonObject inventory = modules.getAsJsonObject("inventory");
            return !inventory.has("enabled")
                    || inventory.get("enabled").getAsBoolean();
        } catch (Exception ignored) {
            return true;
        }
    }

    private static Boolean scopeForLabel(Map<String, Boolean> scopes,
            String label) {
        Boolean personal = scopes.get(label);
        return personal != null ? personal : specialPersonalControl(label);
    }

    @SuppressWarnings("unchecked")
    private static void wireExtendedSave(Screen screen) {
        try {
            Field buttonsField = screen.getClass().getDeclaredField("buttons");
            Field labelsField = screen.getClass().getDeclaredField("labels");
            buttonsField.setAccessible(true);
            labelsField.setAccessible(true);
            Object buttonsValue = buttonsField.get(screen);
            Object labelsValue = labelsField.get(screen);
            if (!(buttonsValue instanceof List<?> rawButtons)
                    || !(labelsValue instanceof Map<?, ?> rawLabels)) return;

            List<Button> buttons = (List<Button>) rawButtons;
            Map<Button, Component> labels = (Map<Button, Component>) rawLabels;
            Button existing = SAVE_REPLACEMENTS.get(screen);
            Button source = null;
            for (Map.Entry<Button, Component> entry : labels.entrySet()) {
                String text = entry.getValue().getString();
                if (entry.getKey() != existing
                        && ("Save & Reload".equals(text)
                        || "Saving...".equals(text))) {
                    source = entry.getKey();
                    break;
                }
            }
            if (source == null) return;

            boolean containsServerRows = containsServerRows(screen);
            boolean canEdit = canEditServer();
            String saveLabel = containsServerRows && canEdit
                    ? "Save All" : "Save Preferences";

            if (existing != null && buttons.contains(existing)) {
                source.visible = false;
                source.active = false;
                labels.put(existing, ScpFonts.roboto(saveLabel));
                existing.active = true;
                return;
            }

            source.visible = false;
            source.active = false;
            Button replacement = Button.builder(Component.empty(), button ->
                    saveExtended(screen, containsServerRows, canEdit))
                    .bounds(source.getX(), source.getY(),
                            source.getWidth(), source.getHeight()).build();
            labels.put(replacement, ScpFonts.roboto(saveLabel));
            buttons.add(replacement);
            addRenderableWidget(screen, replacement);
            SAVE_REPLACEMENTS.put(screen, replacement);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not wire personal module saving", exception);
        }
    }

    private static void saveExtended(Screen screen, boolean containsServerRows,
            boolean canEdit) {
        try {
            JsonObject working = working(screen);
            ClientModulePreferences.captureAndSave(working);

            if (containsServerRows && canEdit) {
                JsonObject baseline = BASELINES.getOrDefault(screen,
                        working.deepCopy());
                JsonObject serverSettings =
                        ClientModulePreferences.mergeServerSettings(
                                working, baseline);
                JsonObject payload = new JsonObject();
                payload.add(ConfigCenterService.MODULES, serverSettings);
                setBooleanField(screen, "saving", true);
                ModNetwork.CHANNEL.sendToServer(
                        new ConfigCenterNetwork.SaveRequest(
                                GSON.toJson(payload)));
                invokeNoArg(screen, "rebuildWidgets");
                return;
            }

            Minecraft.getInstance().setScreen(parent(screen));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not save personal module preferences", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static void wireCrosshairSave(Screen screen) {
        try {
            Field buttonsField = screen.getClass().getDeclaredField("buttons");
            Field labelsField = screen.getClass().getDeclaredField("labels");
            buttonsField.setAccessible(true);
            labelsField.setAccessible(true);
            Object buttonsValue = buttonsField.get(screen);
            Object labelsValue = labelsField.get(screen);
            if (!(buttonsValue instanceof List<?> rawButtons)
                    || !(labelsValue instanceof Map<?, ?> rawLabels)) return;

            List<Button> buttons = (List<Button>) rawButtons;
            Map<Button, Component> labels = (Map<Button, Component>) rawLabels;
            Button existing = SAVE_REPLACEMENTS.get(screen);
            Button source = null;
            for (Map.Entry<Button, Component> entry : labels.entrySet()) {
                String text = entry.getValue().getString();
                if (entry.getKey() != existing
                        && ("Save & Reload".equals(text)
                        || "Saving...".equals(text))) {
                    source = entry.getKey();
                    break;
                }
            }
            if (source == null) return;

            if (existing != null && buttons.contains(existing)) {
                source.visible = false;
                source.active = false;
                labels.put(existing, ScpFonts.roboto("Save Preference"));
                existing.active = true;
                return;
            }

            source.visible = false;
            source.active = false;
            Button replacement = Button.builder(Component.empty(), button -> {
                try {
                    ClientModulePreferences.captureAndSave(working(screen));
                    Minecraft.getInstance().setScreen(parent(screen));
                } catch (ReflectiveOperationException exception) {
                    ScpClassifiedDirectiveMod.LOGGER.warn(
                            "Could not save Crosshair preference", exception);
                }
            }).bounds(source.getX(), source.getY(),
                    source.getWidth(), source.getHeight()).build();
            labels.put(replacement, ScpFonts.roboto("Save Preference"));
            buttons.add(replacement);
            addRenderableWidget(screen, replacement);
            SAVE_REPLACEMENTS.put(screen, replacement);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not wire Crosshair preference saving", exception);
        }
    }

    private static Map<String, Boolean> rowScopes(Screen screen)
            throws ReflectiveOperationException {
        Field rowsField = screen.getClass().getDeclaredField("rows");
        rowsField.setAccessible(true);
        Object value = rowsField.get(screen);
        if (!(value instanceof List<?> rows)) return Map.of();

        Map<String, Boolean> scopes = new HashMap<>();
        for (Object row : rows) {
            Method isSection = row.getClass().getDeclaredMethod("isSection");
            Method label = row.getClass().getDeclaredMethod("label");
            Method group = row.getClass().getDeclaredMethod("group");
            Method key = row.getClass().getDeclaredMethod("key");
            isSection.setAccessible(true);
            label.setAccessible(true);
            group.setAccessible(true);
            key.setAccessible(true);
            if ((boolean) isSection.invoke(row)) continue;
            String rowLabel = String.valueOf(label.invoke(row));
            String rowGroup = String.valueOf(group.invoke(row));
            String rowKey = String.valueOf(key.invoke(row));
            scopes.put(rowLabel,
                    ClientModulePreferences.isClientPreference(
                            rowGroup, rowKey));
        }
        return scopes;
    }

    private static boolean containsServerRows(Screen screen)
            throws ReflectiveOperationException {
        for (Boolean personal : rowScopes(screen).values()) {
            if (!personal) return true;
        }
        return false;
    }

    private static Boolean specialPersonalControl(String label) {
        if (label.startsWith("Voice Profile A")
                || label.startsWith("Voice Profile B")
                || "Test Voice".equals(label)) {
            return true;
        }
        return null;
    }

    private static String stripState(String label) {
        if (label.endsWith(": ON")) {
            return label.substring(0, label.length() - 4);
        }
        if (label.endsWith(": OFF")) {
            return label.substring(0, label.length() - 5);
        }
        return label;
    }

    private static boolean canEditServer() {
        try {
            Class<?> client = Class.forName(
                    "com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterClient");
            Field filesField = client.getDeclaredField("files");
            filesField.setAccessible(true);
            Object value = filesField.get(null);
            if (!(value instanceof JsonObject files)
                    || !files.has("__permissions")
                    || !files.get("__permissions").isJsonObject()) {
                return false;
            }
            JsonObject permissions = files.getAsJsonObject("__permissions");
            return permissions.has("can_edit_server")
                    && permissions.get("can_edit_server").getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static JsonObject working(Screen screen)
            throws ReflectiveOperationException {
        Field field = screen.getClass().getDeclaredField("working");
        field.setAccessible(true);
        Object value = field.get(screen);
        if (value instanceof JsonObject working) return working;
        throw new IllegalStateException("Missing module working configuration");
    }

    private static Screen parent(Screen screen)
            throws ReflectiveOperationException {
        Field field = screen.getClass().getDeclaredField("parent");
        field.setAccessible(true);
        Object value = field.get(screen);
        return value instanceof Screen parent ? parent : null;
    }

    private static void setBooleanField(Screen screen, String name,
            boolean value) throws ReflectiveOperationException {
        Field field = screen.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setBoolean(screen, value);
    }

    private static void addRenderableWidget(Screen screen, Button button)
            throws ReflectiveOperationException {
        Method target = findMethod(screen.getClass(),
                "addRenderableWidget", 1);
        if (target == null) {
            throw new NoSuchMethodException("Screen.addRenderableWidget");
        }
        target.setAccessible(true);
        target.invoke(screen, button);
    }

    private static void invokeNoArg(Screen screen, String name)
            throws ReflectiveOperationException {
        Method target = findMethod(screen.getClass(), name, 0);
        if (target == null) throw new NoSuchMethodException(name);
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
}
