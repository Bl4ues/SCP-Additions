package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps Crosshair with the client preferences in General & Modules instead of
 * occupying a dedicated Configuration Center home-screen category.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class CrosshairModulesPlacement {
    private static final String HOME_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterClient$HomeScreen";
    private static final String EXTENDED_MODULES_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String ROW_TYPE =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$Row";

    private static final String CROSSHAIR_LABEL = "Custom Crosshair";
    private static final String CROSSHAIR_DESCRIPTION =
            "Replaces Minecraft's crosshair with a configurable SCP: Classified Directive crosshair.";
    private static final int ROW_HEIGHT = 22;
    private static final int STATE_WIDTH = 92;
    private static final int GAP = 6;

    private static final int PANEL = 0xFF0B0E12;
    private static final int NAVY = 0xFF0D1116;
    private static final int NAVY_HOVER = 0xFF1A2028;
    private static final int ACCENT = 0xFFC99B18;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int CLIENT_SCOPE = 0xFF79D58B;
    private static final int BADGE_BACKGROUND = 0xE6081022;
    private static final int ON = 0xFF79D58B;
    private static final int OFF = 0xFFFF8B8B;
    private static final float BADGE_SCALE = 0.74F;

    private static final Map<Screen, RowControls> ROW_CONTROLS =
            new WeakHashMap<>();

    private CrosshairModulesPlacement() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPre(ScreenEvent.Init.Pre event) {
        Screen screen = event.getScreen();
        if (!isGeneralModulesScreen(screen)) return;
        injectCrosshairRow(screen);
        injectActionBarRow(screen);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (isHomeScreen(screen)) {
            compactHomeLayout(screen);
        } else if (isGeneralModulesScreen(screen)) {
            wireCrosshairRow(screen);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (isHomeScreen(screen)) {
            // Other presentation hooks may rebuild the home controls. Keep the
            // compact layout authoritative without creating another button.
            compactHomeLayout(screen);
        } else if (isGeneralModulesScreen(screen)) {
            wireCrosshairRow(screen);
            renderCrosshairRow(event.getGuiGraphics(), screen);
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

    /** Removes Crosshair from the home column and reclaims that entire row. */
    private static void compactHomeLayout(Screen screen) {
        Button general = findButton(screen, "General & Modules");
        if (general == null) return;

        Button crosshair = findButton(screen, "Crosshair");
        int startY = crosshair != null ? crosshair.getY() : general.getY();
        if (crosshair != null) {
            crosshair.visible = false;
            crosshair.active = false;
            crosshair.setX(-10_000);
        }

        int step = 27;
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
        if (screen == null) return null;
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

    /** Adds the Crosshair client preference immediately below Preferences. */
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
                String label = String.valueOf(labelMethod.invoke(currentRows.get(i)));
                if (CROSSHAIR_LABEL.equals(label) || "Crosshair".equals(label)) {
                    return;
                }
                if ("Preferences".equals(label)) preferencesIndex = i;
            }

            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class,
                    String.class, boolean.class);
            constructor.setAccessible(true);
            Object crosshairRow = constructor.newInstance(
                    "crosshair", "enabled", CROSSHAIR_LABEL,
                    CROSSHAIR_DESCRIPTION, true);

            List<Object> updated = new ArrayList<>(currentRows);
            int insertAt = preferencesIndex >= 0
                    ? preferencesIndex + 1 : Math.min(1, updated.size());
            updated.add(insertAt, crosshairRow);
            rowsField.set(screen, List.copyOf(updated));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not place Custom Crosshair in Preferences",
                    exception);
        }
    }

    /** Preserves the unrelated Action Bars preference historically housed here. */
    private static void injectActionBarRow(Screen screen) {
        try {
            Field rowsField = screen.getClass().getDeclaredField("rows");
            rowsField.setAccessible(true);
            Object value = rowsField.get(screen);
            if (!(value instanceof List<?> currentRows)) return;

            Class<?> rowType = Class.forName(ROW_TYPE);
            Method labelMethod = rowType.getDeclaredMethod("label");
            labelMethod.setAccessible(true);
            int insertAt = currentRows.size();
            for (int i = 0; i < currentRows.size(); i++) {
                String label = String.valueOf(labelMethod.invoke(currentRows.get(i)));
                if ("Action Bars in Roboto".equals(label)) return;
                if ("Hide Active Effect Indicators".equals(label)) {
                    insertAt = i + 1;
                }
            }

            Constructor<?> constructor = rowType.getDeclaredConstructor(
                    String.class, String.class, String.class,
                    String.class, boolean.class);
            constructor.setAccessible(true);
            Object row = constructor.newInstance(
                    "hud", "action_bars_roboto", "Action Bars in Roboto",
                    "Renders action-bar messages with the SCP Inventory Roboto font.",
                    true);

            List<Object> updated = new ArrayList<>(currentRows);
            updated.add(Math.min(insertAt, updated.size()), row);
            rowsField.set(screen, List.copyOf(updated));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not place Action Bars in Roboto in Preferences",
                    exception);
        }
    }

    /** Replaces the ordinary toggle hitbox with CONFIG and state hit areas. */
    @SuppressWarnings("unchecked")
    private static void wireCrosshairRow(Screen screen) {
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
            for (Map.Entry<Button, Component> entry : labels.entrySet()) {
                if (entry.getValue().getString().startsWith(CROSSHAIR_LABEL + ": ")) {
                    source = entry.getKey();
                    break;
                }
            }
            if (source == null) return;

            source.visible = false;
            source.active = false;

            RowControls existing = ROW_CONTROLS.get(screen);
            if (existing != null && existing.source() == source
                    && screen.children().contains(existing.editor())
                    && screen.children().contains(existing.state())) {
                return;
            }

            int editorWidth = Math.max(40,
                    source.getWidth() - STATE_WIDTH - GAP);
            HitAreaButton editor = new HitAreaButton(source.getX(), source.getY(),
                    editorWidth, ROW_HEIGHT, () -> openCrosshairEditor(screen));
            HitAreaButton state = new HitAreaButton(
                    source.getX() + source.getWidth() - STATE_WIDTH,
                    source.getY(), STATE_WIDTH, ROW_HEIGHT,
                    () -> toggleCrosshair(screen));
            addRenderableWidget(screen, editor);
            addRenderableWidget(screen, state);
            ROW_CONTROLS.put(screen, new RowControls(source, editor, state));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not wire Custom Crosshair preference row",
                    exception);
        }
    }

    private static void toggleCrosshair(Screen screen) {
        try {
            JsonObject settings = crosshairSettings(working(screen));
            settings.addProperty("enabled",
                    !bool(settings, "enabled", true));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not toggle Custom Crosshair preference",
                    exception);
        }
    }

    private static void openCrosshairEditor(Screen parent) {
        try {
            Minecraft.getInstance().setScreen(
                    new CrosshairEditorScreen(parent, working(parent)));
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not open Custom Crosshair configuration",
                    exception);
        }
    }

    private static void renderCrosshairRow(GuiGraphics graphics, Screen screen) {
        RowControls controls = ROW_CONTROLS.get(screen);
        if (controls == null || !screen.children().contains(controls.editor())
                || !screen.children().contains(controls.state())) {
            return;
        }

        Button source = controls.source();
        boolean editorHovered = controls.editor().isHoveredOrFocused();
        boolean hovered = editorHovered || controls.state().isHoveredOrFocused();
        boolean enabled;
        try {
            enabled = bool(crosshairSettings(working(screen)), "enabled", true);
        } catch (ReflectiveOperationException exception) {
            return;
        }

        int slide = ConfigCenterVisuals.contentOffsetX();
        int left = source.getX() + slide;
        int top = source.getY();
        int right = left + source.getWidth();
        int bottom = top + source.getHeight();
        Font font = Minecraft.getInstance().font;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 2100.0F);
        graphics.fill(left, top, right, bottom, hovered ? NAVY_HOVER : NAVY);
        graphics.fill(left, top, left + (hovered ? 6 : 4), bottom, ACCENT);

        Component title = ScpFonts.roboto(CROSSHAIR_LABEL);
        int textY = top + Math.max(1, (bottom - top - font.lineHeight) / 2);
        graphics.drawString(font, title, left + 16, textY, WHITE, false);

        Component scope = ScpFonts.roboto("CLIENT");
        int scaledWidth = Math.round(font.width(scope) * BADGE_SCALE);
        int scaledHeight = Math.max(6,
                Math.round(font.lineHeight * BADGE_SCALE));
        int badgeRight = right - 58;
        int badgeX = badgeRight - scaledWidth;
        int badgeY = top + Math.max(1, (bottom - top - scaledHeight) / 2);

        Component configure = ScpFonts.roboto("CONFIG >");
        int configureX = badgeX - 12 - font.width(configure);
        int titleEnd = left + 16 + font.width(title);
        if (configureX >= titleEnd + 12) {
            graphics.drawString(font, configure, configureX, textY,
                    editorHovered ? CLIENT_SCOPE : MUTED, false);
        }

        graphics.fill(badgeX - 4, top + 2, badgeRight + 3, bottom - 2,
                BADGE_BACKGROUND);
        graphics.pose().pushPose();
        graphics.pose().translate(badgeX, badgeY, 0.0F);
        graphics.pose().scale(BADGE_SCALE, BADGE_SCALE, 1.0F);
        graphics.drawString(font, scope, 0, 0, CLIENT_SCOPE, false);
        graphics.pose().popPose();

        Component state = ScpFonts.roboto(enabled ? "ON" : "OFF");
        graphics.drawString(font, state,
                right - 14 - font.width(state), textY,
                enabled ? ON : OFF, false);

        // Redraw the complete description in the same final pass so no other
        // scope renderer or row replacement can truncate it.
        graphics.fill(left, bottom, right, top + 34, PANEL);
        graphics.drawString(font, ScpFonts.roboto(CROSSHAIR_DESCRIPTION),
                left + 2, top + 24, MUTED, false);
        graphics.pose().popPose();
    }

    private static JsonObject working(Screen screen)
            throws ReflectiveOperationException {
        Field workingField = screen.getClass().getDeclaredField("working");
        workingField.setAccessible(true);
        Object value = workingField.get(screen);
        if (value instanceof JsonObject working) return working;
        throw new IllegalStateException("Missing module working configuration");
    }

    private static JsonObject crosshairSettings(JsonObject modules) {
        if (!modules.has("crosshair")
                || !modules.get("crosshair").isJsonObject()) {
            modules.add("crosshair", new JsonObject());
        }
        JsonObject settings = modules.getAsJsonObject("crosshair");
        if (!settings.has("enabled")) settings.addProperty("enabled", true);
        if (!settings.has("in_game_enabled")) {
            settings.addProperty("in_game_enabled", true);
        }
        if (!settings.has("red")) settings.addProperty("red", 1.0D);
        if (!settings.has("green")) settings.addProperty("green", 1.0D);
        if (!settings.has("blue")) settings.addProperty("blue", 1.0D);
        if (!settings.has("alpha")) settings.addProperty("alpha", 1.0D);
        return settings;
    }

    private static void addRenderableWidget(Screen screen, AbstractButton button)
            throws ReflectiveOperationException {
        Method target = findMethod(screen.getClass(), "addRenderableWidget", 1);
        if (target == null) {
            throw new NoSuchMethodException("Screen.addRenderableWidget");
        }
        target.setAccessible(true);
        target.invoke(screen, button);
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

    private static boolean bool(JsonObject root, String key,
            boolean fallback) {
        if (root == null || !root.has(key)
                || !root.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return root.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double unit(JsonObject root, String key, double fallback) {
        if (root == null || !root.has(key)
                || !root.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            double value = root.get(key).getAsDouble();
            if (!Double.isFinite(value)) return fallback;
            return Mth.clamp(value, 0.0D, 1.0D);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private record RowControls(Button source, HitAreaButton editor,
                               HitAreaButton state) {
    }

    /** Invisible interaction surface; the shared module visual is drawn above it. */
    private static final class HitAreaButton extends AbstractButton {
        private final Runnable action;

        private HitAreaButton(int x, int y, int width, int height,
                Runnable action) {
            super(x, y, width, height, Component.empty());
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            // Intentionally invisible. renderCrosshairRow owns the presentation.
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    /** Crosshair appearance editor. Enabling the module itself lives in Modules. */
    private static final class CrosshairEditorScreen extends Screen {
        private static final ResourceLocation CROSSHAIR_TEXTURE =
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                        "textures/gui/crosshair.png");
        private static final int TEXTURE_SIZE = 256;

        private final Screen parent;
        private final JsonObject sourceModules;
        private final JsonObject settings;
        private final List<Button> buttons = new ArrayList<>();
        private final Map<Button, Component> labels = new IdentityHashMap<>();

        private CrosshairEditorScreen(Screen parent, JsonObject sourceModules) {
            super(ScpFonts.roboto("Crosshair"));
            this.parent = parent;
            this.sourceModules = sourceModules;
            this.settings = crosshairSettings(sourceModules.deepCopy()).deepCopy();
        }

        @Override
        protected void init() {
            rebuildWidgets();
        }

        @Override
        protected void rebuildWidgets() {
            clearWidgets();
            buttons.clear();
            labels.clear();

            int panelWidth = Math.min(700, width - 20);
            int panelHeight = Math.min(340, height - 16);
            int panelX = ConfigCenterVisuals.contentLeft(width, panelWidth);
            int panelY = Math.max(8, (height - panelHeight) / 2);
            int contentX = panelX + 18;
            int contentWidth = panelWidth - 36;

            register(Button.builder(Component.empty(), button -> {
                settings.addProperty("in_game_enabled",
                        !bool(settings, "in_game_enabled", true));
                rebuildWidgets();
            }).bounds(contentX, panelY + 44, contentWidth, 22).build(),
                    inGameToggleLabel());

            int sliderX = panelX + 246;
            int sliderWidth = panelWidth - 270;
            int sliderY = panelY + 101;
            addRenderableWidget(new CrosshairSlider(sliderX, sliderY,
                    sliderWidth, "Red", settings, "red"));
            addRenderableWidget(new CrosshairSlider(sliderX, sliderY + 35,
                    sliderWidth, "Green", settings, "green"));
            addRenderableWidget(new CrosshairSlider(sliderX, sliderY + 70,
                    sliderWidth, "Blue", settings, "blue"));
            addRenderableWidget(new CrosshairSlider(sliderX, sliderY + 105,
                    sliderWidth, "Alpha", settings, "alpha"));

            int bottom = panelY + panelHeight - 30;
            register(Button.builder(Component.empty(), button -> resetDefaults())
                    .bounds(contentX, bottom, 90, 20).build(), "Defaults");
            register(Button.builder(Component.empty(), button -> save())
                    .bounds(panelX + panelWidth - 250, bottom, 112, 20).build(),
                    "Save Preference");
            register(Button.builder(Component.empty(), button -> goBack())
                    .bounds(panelX + panelWidth - 130, bottom, 112, 20).build(),
                    "Back");
        }

        private void register(Button button, String label) {
            labels.put(button, ScpFonts.roboto(label));
            button.setMessage(Component.empty());
            buttons.add(addRenderableWidget(button));
        }

        private void resetDefaults() {
            settings.addProperty("in_game_enabled", true);
            settings.addProperty("red", 1.0D);
            settings.addProperty("green", 1.0D);
            settings.addProperty("blue", 1.0D);
            settings.addProperty("alpha", 1.0D);
            rebuildWidgets();
        }

        private void save() {
            JsonObject savedSettings = settings.deepCopy();
            sourceModules.add("crosshair", savedSettings.deepCopy());

            // Save only the Crosshair values, not unrelated unsaved edits that
            // happen to be pending on the parent Modules screen.
            JsonObject localPreferences = new JsonObject();
            ClientModulePreferences.applyTo(localPreferences);
            localPreferences.add("crosshair", savedSettings.deepCopy());
            ClientModulePreferences.captureAndSave(localPreferences);
            goBack();
        }

        private void goBack() {
            Minecraft.getInstance().setScreen(parent);
        }

        @Override
        public void onClose() {
            goBack();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            ConfigCenterVisuals.renderBackdrop(this, graphics, mouseX, mouseY);
            int panelWidth = Math.min(700, width - 20);
            int panelHeight = Math.min(340, height - 16);
            int panelX = ConfigCenterVisuals.contentLeft(width, panelWidth);
            int panelY = Math.max(8, (height - panelHeight) / 2);

            ConfigCenterVisuals.drawPanel(graphics, font, panelX, panelY,
                    panelWidth, panelHeight, title.getString());
            graphics.drawString(font, ScpFonts.roboto(
                            "Configures visibility, color, and opacity for the custom crosshair."),
                    panelX + 18, panelY + 29, MUTED, false);

            if (!bool(settings, "in_game_enabled", true)) {
                graphics.drawCenteredString(font, ScpFonts.roboto(
                                "The in-game crosshair is completely hidden."),
                        panelX + 128, panelY + 153, OFF);
            } else {
                drawPreview(graphics, panelX + 96, panelY + 142);
                graphics.drawCenteredString(font, ScpFonts.roboto("Preview"),
                        panelX + 128, panelY + 100, 0xFFE3C865);
            }

            for (Button button : buttons) button.setMessage(Component.empty());
            super.render(graphics, mouseX, mouseY, partialTick);
            for (Button button : buttons) {
                if (button.visible) {
                    ConfigCenterVisuals.drawButton(graphics, font, button,
                            labels.getOrDefault(button, Component.empty()),
                            mouseX, mouseY);
                }
            }
            for (Button button : buttons) {
                Component label = labels.get(button);
                if (label != null) button.setMessage(label);
            }
        }

        private void drawPreview(GuiGraphics graphics, int x, int y) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(
                    (float) unit(settings, "red", 1.0D),
                    (float) unit(settings, "green", 1.0D),
                    (float) unit(settings, "blue", 1.0D),
                    (float) unit(settings, "alpha", 1.0D));
            graphics.blit(CROSSHAIR_TEXTURE, x, y, 64, 64,
                    0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE,
                    TEXTURE_SIZE, TEXTURE_SIZE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        private String inGameToggleLabel() {
            return "Enable In-Game Crosshair: "
                    + (bool(settings, "in_game_enabled", true) ? "ON" : "OFF");
        }
    }

    private static final class CrosshairSlider extends AbstractSliderButton {
        private final String label;
        private final JsonObject settings;
        private final String key;

        private CrosshairSlider(int x, int y, int width, String label,
                JsonObject settings, String key) {
            super(x, y, width, 22, Component.empty(),
                    unit(settings, key, 1.0D));
            this.label = label;
            this.settings = settings;
            this.key = key;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(ScpFonts.roboto(label + ": "
                    + String.format(java.util.Locale.ROOT, "%.2f", value)));
        }

        @Override
        protected void applyValue() {
            value = Mth.clamp(value, 0.0D, 1.0D);
            settings.addProperty(key, value);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int left = getX();
            int top = getY();
            int right = left + getWidth();
            int bottom = top + getHeight();
            int border = hovered ? 0xFFC99B18 : 0xFF3A424D;
            graphics.fill(left, top, right, bottom, 0xFF0D1116);
            graphics.fill(left, top, right, top + 1, border);
            graphics.fill(left, bottom - 1, right, bottom, border);
            graphics.fill(left, top, left + 1, bottom, border);
            graphics.fill(right - 1, top, right, bottom, border);

            int trackLeft = left + 9;
            int trackRight = right - 9;
            int trackY = top + 10;
            graphics.fill(trackLeft, trackY, trackRight, trackY + 2,
                    0xFF5A5E66);
            int knobX = trackLeft + (int) Math.round(
                    (trackRight - trackLeft - 4) * value);
            graphics.fill(knobX, top + 5, knobX + 4, bottom - 5,
                    hovered ? 0xFFE3C865 : WHITE);

            Component message = getMessage();
            int textX = left + Math.max(6,
                    (getWidth() - Minecraft.getInstance().font.width(message)) / 2);
            graphics.drawString(Minecraft.getInstance().font, message,
                    textX, top - 10, WHITE, false);
        }
    }
}
