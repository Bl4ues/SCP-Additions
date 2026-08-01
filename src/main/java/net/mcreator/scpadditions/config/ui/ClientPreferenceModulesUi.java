package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.network.ModNetwork;
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
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.ClientModulePreferences;

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
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ClientPreferenceModulesUi {
    private static final String HOME_SCREEN =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient$HomeScreen";
    private static final String EXTENDED_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String CROSSHAIR_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$CrosshairScreen";

    private static final int PANEL = 0xEE111317;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int PERSONAL = 0xFF79D58B;
    private static final int HOST = 0xFFFFC56D;

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
        if (!isExtended(screen) && !isCrosshair(screen)) return;
        try {
            JsonObject working = working(screen);
            BASELINES.putIfAbsent(screen, working.deepCopy());
            ClientModulePreferences.applyTo(working);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
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
        if (isHome(screen)) renderHomeDisclaimer(event.getGuiGraphics(), screen);
        if (isExtended(screen)) {
            renderModuleDisclaimer(event.getGuiGraphics(), screen);
        }
    }

    private static void refresh(Screen screen) {
        if (isHome(screen)) {
            applyHomePermissions(screen);
        } else if (isExtended(screen)) {
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

    private static void applyHomePermissions(Screen screen) {
        boolean canEdit = canEditServer();
        setActive(screen, "General & Modules", true);
        setActive(screen, "Accessibility", true);
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
                String plain = label.getString();
                String base = stripState(plain);
                Boolean personal = scopes.get(base);
                if (personal == null) {
                    personal = specialPersonalControl(base);
                }
                if (personal != null) {
                    button.active = personal || canEdit;
                }
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not apply module permission scopes", exception);
        }
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
            ScpAdditionsMod.LOGGER.warn(
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
            ScpAdditionsMod.LOGGER.warn(
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
                    ScpAdditionsMod.LOGGER.warn(
                            "Could not save Crosshair preference", exception);
                }
            }).bounds(source.getX(), source.getY(),
                    source.getWidth(), source.getHeight()).build();
            labels.put(replacement, ScpFonts.roboto("Save Preference"));
            buttons.add(replacement);
            addRenderableWidget(screen, replacement);
            SAVE_REPLACEMENTS.put(screen, replacement);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
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
                    "net.mcreator.scpadditions.config.ui.ConfigCenterClient");
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

    private static void renderHomeDisclaimer(GuiGraphics graphics,
            Screen screen) {
        Font font = Minecraft.getInstance().font;
        int panelWidth = Math.min(420, screen.width - 20);
        int panelHeight = Math.min(310, screen.height - 20);
        int panelX = Math.max(8, (screen.width - panelWidth) / 2);
        int panelY = Math.max(10, (screen.height - panelHeight) / 2);
        graphics.fill(panelX + 10, panelY + 27,
                panelX + panelWidth - 10, panelY + 49, PANEL);
        graphics.drawString(font,
                ScpFonts.roboto("Personal preferences apply only to you."),
                panelX + 14, panelY + 29, PERSONAL, false);
        graphics.drawString(font,
                ScpFonts.roboto("World and gameplay rules require the host or an operator."),
                panelX + 14, panelY + 40, MUTED, false);
    }

    private static void renderModuleDisclaimer(GuiGraphics graphics,
            Screen screen) {
        Font font = Minecraft.getInstance().font;
        int panelWidth = Math.min(560, screen.width - 20);
        int rowCount;
        try {
            rowCount = rowScopes(screen).size();
        } catch (ReflectiveOperationException ignored) {
            rowCount = 8;
        }
        int preferredHeight = rowCount <= 4 ? 240 : 380;
        int panelHeight = Math.min(preferredHeight, screen.height - 16);
        int panelX = Math.max(8, (screen.width - panelWidth) / 2);
        int panelY = Math.max(8, (screen.height - panelHeight) / 2);
        graphics.fill(panelX + 10, panelY + 27,
                panelX + panelWidth - 10, panelY + 41, PANEL);
        graphics.drawString(font,
                ScpFonts.roboto("Personal: per-player presentation"),
                panelX + 16, panelY + 30, PERSONAL, false);
        String hostText = canEditServer()
                ? "Host: world and gameplay rules"
                : "Host: locked without operator permission";
        int hostWidth = font.width(ScpFonts.roboto(hostText));
        graphics.drawString(font, ScpFonts.roboto(hostText),
                panelX + panelWidth - hostWidth - 16,
                panelY + 30, HOST, false);
    }
}
