package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preserves the SCP Unity presentation for General & Modules and exposes
 * dedicated Crosshair, Accessibility, and developer-only Debug screens.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp079ModulesScreenExtension {
    private static final String MODULES_SCREEN =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient$ModulesScreen";
    private static final String HOME_SCREEN =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient$HomeScreen";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    private static final int PANEL = 0xEE111317;
    private static final int HEADER = 0xEE24282E;
    private static final int NAVY = 0xFF081022;
    private static final int NAVY_HOVER = 0xFF131E36;
    private static final int NAVY_DISABLED = 0xFF1B1E26;
    private static final int BORDER = 0xFF46536C;
    private static final int BORDER_HOVER = 0xFF73809A;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int ACCENT_SOFT = 0xFF8D711F;
    private static final int PALE_GOLD = 0xFFE5D49A;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int MODULE_ON = 0xFF79D58B;
    private static final int MODULE_OFF = 0xFFFF8B8B;

    private static final List<Row> GENERAL_ROWS = List.of(
            Row.section("Gameplay Features"),
            new Row("hud", "enabled", "Custom HUD",
                    "Shows the SCP Additions health, stamina and blink presentation.", true),
            new Row("vitals", "custom_health_enabled", "Custom Health",
                    "Enables custom health behavior.", true),
            new Row("vitals", "stamina_enabled", "Stamina",
                    "Enables stamina drain and regeneration.", true),
            new Row("hunger", "disabled", "Hunger System Replacement",
                    "Hides hunger, makes food restore health, and uses damage-delay regeneration.", true),
            new Row("vitals", "horror_movement_enabled", "Survival-Horror Movement",
                    "Uses slower walking and committed sprinting.", true),
            new Row("inventory", "enabled", "SCP Inventory",
                    "Enables the custom survival-horror inventory.", true),
            new Row("interactions", "enabled", "Contextual Interactions",
                    "Enables SCP Unity-style interaction prompts.", true),
            new Row("blink", "enabled", "Blink System",
                    "Enables automatic and manual blinking.", true),
            Row.section("Preferences"),
            new Row("inventory", "remember_ui_state", "Remember UI State",
                    "Remembers the selected panel, document and scroll positions until leaving the world.", true),
            new Row("interactions", "disable_in_creative", "Hide Prompts in Creative",
                    "Disables custom prompts for Creative players.", false),
            new Row("hud", "hide_active_effect_indicators",
                    "Hide Active Effect Indicators",
                    "Hides vanilla status-effect icons without changing Conditions displays.", true),
            new Row("audio", "enter_sound_enabled", "World Entry Sound",
                    "Plays enter.ogg after joining or opening a world.", true),
            new Row("audio", "save_game_sound_enabled", "Save Game Sound",
                    "Plays save_game.ogg when the player's respawn point is set.", true),
            new Row("audio", "disable_vanilla_music", "Disable Vanilla Music",
                    "Stops Minecraft's ambient soundtrack while preserving SCP Additions music.", false),
            new Row("audio", "replace_player_hurt_sounds",
                    "Replace Player Hurt Sounds",
                    "Replaces vanilla player damage sounds with the SCP Additions voice set.", true),
            new Row("audio", "mute_non_player_hit_sounds",
                    "Remove Non-Player Hit Sounds",
                    "Mutes vanilla attack, critical, and sweep impacts against non-player mobs.", false)
    );

    private static final List<Row> DEBUG_ROWS = List.of(
            new Row("debug", "show_scp_079_energy_hud", "SCP-079 Energy HUD",
                    "Shows SCP-079 processing power in the upper-right corner.", false),
            new Row("debug", "show_scp_079_decision_log_hud",
                    "SCP-079 Decision Log HUD",
                    "Shows recent SCP-079 decisions, costs, context, and manipulated devices.", false),
            new Row("debug", "show_scp_spawn_timers_hud",
                    "SCP Spawn Timers HUD",
                    "Shows each roamer's state, countdown, and latest scheduler result.", false)
    );

    private static final List<Row> ACCESSIBILITY_ROWS = List.of(
            new Row("accessibility", "reduce_scp_012_visual_effects",
                    "Reduce SCP-012 Visual Effects",
                    "Removes rapidly flashing interference and subliminal images during psychosis.",
                    false)
    );

    private Scp079ModulesScreenExtension() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen incoming = event.getNewScreen();
        if (incoming == null || !MODULES_SCREEN.equals(
                incoming.getClass().getName())) {
            return;
        }

        try {
            Field workingField = incoming.getClass().getDeclaredField("working");
            workingField.setAccessible(true);
            Object value = workingField.get(incoming);
            if (value instanceof JsonObject working) {
                event.setNewScreen(new ExtendedToggleScreen(
                        event.getCurrentScreen(), working,
                        "General & Modules", GENERAL_ROWS));
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not apply the extended General & Modules screen",
                    exception);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHomeScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen == null || !HOME_SCREEN.equals(screen.getClass().getName())) {
            return;
        }

        Map<String, Button> buttons = new LinkedHashMap<>();
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button) {
                buttons.put(button.getMessage().getString(), button);
            }
        }

        Button general = buttons.get("General & Modules");
        Button inventory = buttons.get("Inventory, Equipment & Codex");
        Button interactions = buttons.get("Contextual Interactions");
        Button drinks = buttons.get("SCP-294 Drinks");
        Button recipes = buttons.get("SCP-914 Recipes");
        Button reload = buttons.get("Reload Snapshot");
        Button done = buttons.get("Done");
        if (general == null || inventory == null || interactions == null
                || drinks == null || recipes == null || reload == null
                || done == null) {
            return;
        }

        int optionX = general.getX();
        int optionWidth = general.getWidth();
        int startY = general.getY();
        int step = 27;

        general.setY(startY + step);
        inventory.setY(startY + step * 2);
        interactions.setY(startY + step * 3);
        drinks.setY(startY + step * 4);
        recipes.setY(startY + step * 5);

        Button crosshair = Button.builder(ScpFonts.roboto("Crosshair"),
                button -> openCrosshairScreen(screen))
                .bounds(optionX, startY, optionWidth, 24).build();
        int splitWidth = (optionWidth - 6) / 2;
        Button accessibility = Button.builder(
                ScpFonts.roboto("Accessibility"),
                button -> openAccessibilityScreen(screen))
                .bounds(optionX, startY + step * 6, splitWidth, 24).build();
        Button debug = Button.builder(ScpFonts.roboto("Debug Tools"),
                button -> openDebugScreen(screen))
                .bounds(optionX + splitWidth + 6, startY + step * 6,
                        optionWidth - splitWidth - 6, 24).build();

        int bottomY = startY + step * 7 + 3;
        reload.setY(bottomY);
        done.setY(bottomY);
        event.addListener(crosshair);
        event.addListener(accessibility);
        event.addListener(debug);
    }

    private static void openCrosshairScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new CrosshairScreen(parent,
                moduleSnapshot(parent)));
    }

    private static void openAccessibilityScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new ExtendedToggleScreen(parent,
                moduleSnapshot(parent), "Accessibility",
                "Photosensitive Epilepsy", ACCESSIBILITY_ROWS));
    }

    private static void openDebugScreen(Screen parent) {
        Minecraft.getInstance().setScreen(new ExtendedToggleScreen(parent,
                moduleSnapshot(parent), "Debug Tools", DEBUG_ROWS));
    }

    private static JsonObject moduleSnapshot(Screen home) {
        Class<?> owner = home == null ? null : home.getClass().getDeclaringClass();
        if (owner == null) return new JsonObject();
        try {
            Field filesField = owner.getDeclaredField("files");
            filesField.setAccessible(true);
            Object value = filesField.get(null);
            if (value instanceof JsonObject files
                    && files.has(ConfigCenterService.MODULES)
                    && files.get(ConfigCenterService.MODULES).isJsonObject()) {
                return files.getAsJsonObject(
                        ConfigCenterService.MODULES).deepCopy();
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not read the module snapshot for the Configuration Center",
                    exception);
        }
        return new JsonObject();
    }

    private static void submitModules(JsonObject working) {
        JsonObject payload = new JsonObject();
        payload.add(ConfigCenterService.MODULES, working);
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.SaveRequest(
                GSON.toJson(payload)));
    }

    private record Row(String group, String key, String label,
                       String description, boolean fallback) {
        private static Row section(String title) {
            return new Row(null, null, title, "", false);
        }

        private boolean isSection() {
            return group == null;
        }
    }

    private static final class ExtendedToggleScreen extends Screen {
        private static final int ROW_HEIGHT = 34;

        private final Screen parent;
        private final JsonObject working;
        private final String sectionTitle;
        private final List<Row> rows;
        private final List<Button> buttons = new ArrayList<>();
        private final Map<Button, Component> labels = new IdentityHashMap<>();
        private int scroll;
        private boolean saving;

        private ExtendedToggleScreen(Screen parent, JsonObject working,
                String title, List<Row> rows) {
            this(parent, working, title, null, rows);
        }

        private ExtendedToggleScreen(Screen parent, JsonObject working,
                String title, String sectionTitle, List<Row> rows) {
            super(ScpFonts.roboto(title));
            this.parent = parent;
            this.working = working == null ? new JsonObject() : working;
            this.sectionTitle = sectionTitle;
            this.rows = rows == null ? List.of() : List.copyOf(rows);
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

            int panelWidth = Math.min(560, width - 20);
            int panelHeight = panelHeight();
            int panelX = Math.max(8, (width - panelWidth) / 2);
            int panelY = Math.max(8, (height - panelHeight) / 2);
            int contentX = panelX + 16;
            int contentY = panelY + (sectionTitle == null ? 44 : 57);
            int visible = visibleRows();
            int end = Math.min(rows.size(), scroll + visible);

            for (int i = scroll; i < end; i++) {
                Row row = rows.get(i);
                if (row.isSection()) continue;
                int rowY = contentY + (i - scroll) * ROW_HEIGHT;
                Button button = Button.builder(ScpFonts.roboto(toggleLabel(row)),
                        clicked -> {
                            JsonObject group = object(working, row.group());
                            group.addProperty(row.key(), !bool(group, row.key(),
                                    row.fallback()));
                            setLabel(clicked, toggleLabel(row));
                        }).bounds(contentX, rowY, panelWidth - 32, 22).build();
                register(button, toggleLabel(row));
            }

            int bottom = panelY + panelHeight - 30;
            register(Button.builder(ScpFonts.roboto("Defaults"),
                    button -> resetDefaults())
                    .bounds(contentX, bottom, 90, 20).build(), "Defaults");

            Button save = Button.builder(ScpFonts.roboto(
                            saving ? "Saving..." : "Save & Reload"),
                    button -> save())
                    .bounds(contentX + panelWidth - 230, bottom, 108, 20)
                    .build();
            save.active = !saving;
            register(save, saving ? "Saving..." : "Save & Reload");

            register(Button.builder(ScpFonts.roboto("Back"),
                    button -> goBack())
                    .bounds(contentX + panelWidth - 116, bottom, 84, 20)
                    .build(), "Back");
        }

        private void register(Button button, String label) {
            labels.put(button, ScpFonts.roboto(label));
            button.setMessage(Component.empty());
            buttons.add(addRenderableWidget(button));
        }

        private void setLabel(Button button, String label) {
            labels.put(button, ScpFonts.roboto(label));
            button.setMessage(Component.empty());
        }

        private void save() {
            if (saving) return;
            saving = true;
            submitModules(working);
            rebuildWidgets();
        }

        private void resetDefaults() {
            for (Row row : rows) {
                if (!row.isSection()) {
                    object(working, row.group()).addProperty(row.key(),
                            row.fallback());
                }
            }
            rebuildWidgets();
        }

        private String toggleLabel(Row row) {
            return row.label() + ": "
                    + (bool(object(working, row.group()), row.key(),
                    row.fallback()) ? "ON" : "OFF");
        }

        private int panelHeight() {
            int preferred = rows.size() <= 4 ? 240 : 380;
            return Math.min(preferred, height - 16);
        }

        private int visibleRows() {
            return Math.max(1, Math.min(8,
                    (panelHeight() - 100) / ROW_HEIGHT));
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
        public boolean mouseScrolled(double mouseX, double mouseY,
                                     double delta) {
            int max = Math.max(0, rows.size() - visibleRows());
            int next = Math.max(0, Math.min(max,
                    scroll + (delta < 0.0D ? 1 : -1)));
            if (next != scroll) {
                scroll = next;
                rebuildWidgets();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick) {
            renderBackground(graphics);
            int panelWidth = Math.min(560, width - 20);
            int panelHeight = panelHeight();
            int panelX = Math.max(8, (width - panelWidth) / 2);
            int panelY = Math.max(8, (height - panelHeight) / 2);

            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + panelHeight, PANEL);
            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + 26, HEADER);
            graphics.drawString(font, ScpFonts.roboto(title), panelX + 10,
                    panelY + 9, WHITE, false);

            if (sectionTitle != null) {
                graphics.drawString(font, ScpFonts.roboto(sectionTitle),
                        panelX + 16, panelY + 35, PALE_GOLD, false);
            }

            int visible = visibleRows();
            if (rows.size() > visible) {
                graphics.drawString(font,
                        ScpFonts.roboto("Mouse wheel: scroll options"),
                        panelX + panelWidth - 160, panelY + 31, MUTED, false);
            }
            int contentY = panelY + (sectionTitle == null ? 44 : 57);
            int end = Math.min(rows.size(), scroll + visible);
            for (int i = scroll; i < end; i++) {
                Row row = rows.get(i);
                int rowY = contentY + (i - scroll) * ROW_HEIGHT;
                if (row.isSection()) {
                    Component heading = ScpFonts.roboto(row.label());
                    graphics.drawString(font, heading, panelX + 18,
                            rowY + 10, PALE_GOLD, false);
                    int lineX = panelX + 26 + font.width(heading);
                    graphics.fill(lineX, rowY + 14,
                            panelX + panelWidth - 18, rowY + 15, BORDER);
                } else {
                    graphics.drawString(font,
                            ScpFonts.roboto(compact(row.description(), 80)),
                            panelX + 18, rowY + 24, MUTED, false);
                }
            }

            for (Button button : buttons) button.setMessage(Component.empty());
            super.render(graphics, mouseX, mouseY, partialTick);
            drawRegisteredButtons(graphics, font, buttons, labels, mouseX, mouseY);
            restoreLabels(buttons, labels);
        }
    }

    private static final class CrosshairScreen extends Screen {
        private static final ResourceLocation CROSSHAIR_TEXTURE =
                new ResourceLocation(ScpAdditionsMod.MODID,
                        "textures/gui/crosshair.png");
        private static final int TEXTURE_SIZE = 256;

        private final Screen parent;
        private final JsonObject working;
        private final List<Button> buttons = new ArrayList<>();
        private final Map<Button, Component> labels = new IdentityHashMap<>();
        private boolean saving;

        private CrosshairScreen(Screen parent, JsonObject working) {
            super(ScpFonts.roboto("Crosshair"));
            this.parent = parent;
            this.working = working == null ? new JsonObject() : working;
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

            JsonObject settings = object(working, "crosshair");
            int panelWidth = Math.min(650, width - 20);
            int panelHeight = Math.min(360, height - 16);
            int panelX = Math.max(8, (width - panelWidth) / 2);
            int panelY = Math.max(8, (height - panelHeight) / 2);
            int contentX = panelX + 18;
            int contentWidth = panelWidth - 36;

            Button replacement = Button.builder(Component.empty(), clicked -> {
                settings.addProperty("enabled",
                        !bool(settings, "enabled", false));
                rebuildWidgets();
            }).bounds(contentX, panelY + 42, contentWidth, 22).build();
            register(replacement, customToggleLabel(settings));

            boolean enabled = bool(settings, "enabled", false);
            boolean inGame = bool(settings, "in_game_enabled", true);
            if (enabled) {
                Button visible = Button.builder(Component.empty(), clicked -> {
                    settings.addProperty("in_game_enabled",
                            !bool(settings, "in_game_enabled", true));
                    rebuildWidgets();
                }).bounds(contentX, panelY + 73, contentWidth, 22).build();
                register(visible, inGameToggleLabel(settings));
            }

            if (enabled && inGame) {
                int sliderX = panelX + 246;
                int sliderWidth = panelWidth - 270;
                int sliderY = panelY + 112;
                addRenderableWidget(new CrosshairSlider(sliderX, sliderY,
                        sliderWidth, "Red", settings, "red"));
                addRenderableWidget(new CrosshairSlider(sliderX, sliderY + 35,
                        sliderWidth, "Green", settings, "green"));
                addRenderableWidget(new CrosshairSlider(sliderX, sliderY + 70,
                        sliderWidth, "Blue", settings, "blue"));
                addRenderableWidget(new CrosshairSlider(sliderX, sliderY + 105,
                        sliderWidth, "Alpha", settings, "alpha"));
            }

            int bottom = panelY + panelHeight - 30;
            register(Button.builder(Component.empty(), button -> resetDefaults())
                    .bounds(contentX, bottom, 90, 20).build(), "Defaults");

            Button save = Button.builder(Component.empty(), button -> save())
                    .bounds(panelX + panelWidth - 250, bottom, 112, 20)
                    .build();
            save.active = !saving;
            register(save, saving ? "Saving..." : "Save & Reload");

            register(Button.builder(Component.empty(), button -> cancel())
                    .bounds(panelX + panelWidth - 130, bottom, 112, 20)
                    .build(), "Cancel");
        }

        private void register(Button button, String label) {
            labels.put(button, ScpFonts.roboto(label));
            button.setMessage(Component.empty());
            buttons.add(addRenderableWidget(button));
        }

        private void resetDefaults() {
            JsonObject settings = object(working, "crosshair");
            settings.addProperty("enabled", false);
            settings.addProperty("in_game_enabled", true);
            settings.addProperty("red", 1.0D);
            settings.addProperty("green", 1.0D);
            settings.addProperty("blue", 1.0D);
            settings.addProperty("alpha", 1.0D);
            rebuildWidgets();
        }

        private void save() {
            if (saving) return;
            saving = true;
            submitModules(working);
            rebuildWidgets();
        }

        private void cancel() {
            Minecraft.getInstance().setScreen(parent);
        }

        @Override
        public void onClose() {
            cancel();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick) {
            renderBackground(graphics);
            int panelWidth = Math.min(650, width - 20);
            int panelHeight = Math.min(360, height - 16);
            int panelX = Math.max(8, (width - panelWidth) / 2);
            int panelY = Math.max(8, (height - panelHeight) / 2);
            JsonObject settings = object(working, "crosshair");

            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + panelHeight, PANEL);
            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + 26, HEADER);
            graphics.drawString(font, ScpFonts.roboto(title), panelX + 10,
                    panelY + 9, WHITE, false);

            graphics.drawString(font, ScpFonts.roboto(
                            "Replaces Minecraft's crosshair with the SCP Additions texture."),
                    panelX + 18, panelY + 29, MUTED, false);

            boolean enabled = bool(settings, "enabled", false);
            boolean inGame = bool(settings, "in_game_enabled", true);
            if (!enabled) {
                graphics.drawCenteredString(font, ScpFonts.roboto(
                                "The vanilla crosshair remains unchanged."),
                        panelX + panelWidth / 2, panelY + 128, MUTED);
            } else if (!inGame) {
                graphics.drawCenteredString(font, ScpFonts.roboto(
                                "The in-game crosshair is completely hidden."),
                        panelX + panelWidth / 2, panelY + 128, MODULE_OFF);
            } else {
                drawPreview(graphics, settings, panelX + 96, panelY + 165);
                graphics.drawCenteredString(font, ScpFonts.roboto("Preview"),
                        panelX + 128, panelY + 121, PALE_GOLD);
            }

            for (Button button : buttons) button.setMessage(Component.empty());
            super.render(graphics, mouseX, mouseY, partialTick);
            drawRegisteredButtons(graphics, font, buttons, labels, mouseX, mouseY);
            restoreLabels(buttons, labels);
        }

        private void drawPreview(GuiGraphics graphics, JsonObject settings,
                                 int x, int y) {
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

        private static String customToggleLabel(JsonObject settings) {
            return "Custom Crosshair: "
                    + (bool(settings, "enabled", false) ? "ON" : "OFF");
        }

        private static String inGameToggleLabel(JsonObject settings) {
            return "Enable in-game crosshair?: "
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
            int border = hovered ? BORDER_HOVER : BORDER;
            graphics.fill(left, top, right, bottom,
                    active ? NAVY : NAVY_DISABLED);
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
                    hovered ? PALE_GOLD : WHITE);

            Component message = getMessage();
            int textX = left + Math.max(6,
                    (getWidth() - Minecraft.getInstance().font.width(message)) / 2);
            graphics.drawString(Minecraft.getInstance().font, message,
                    textX, top - 10, WHITE, false);
        }
    }

    private static void drawRegisteredButtons(GuiGraphics graphics, Font font,
            List<Button> buttons, Map<Button, Component> labels,
            int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 440.0F);
        for (Button button : buttons) {
            if (button.visible) {
                drawButton(graphics, font, button,
                        labels.getOrDefault(button, Component.empty()),
                        mouseX, mouseY);
            }
        }
        graphics.pose().popPose();
    }

    private static void restoreLabels(List<Button> buttons,
                                      Map<Button, Component> labels) {
        for (Button button : buttons) {
            Component label = labels.get(button);
            if (label != null) button.setMessage(label);
        }
    }

    private static void drawButton(GuiGraphics graphics, Font font,
                                   Button button, Component label,
                                   int mouseX, int mouseY) {
        boolean hovered = contains(button, mouseX, mouseY);
        String plain = label.getString();
        boolean primary = "Save & Reload".equals(plain)
                || "Saving...".equals(plain);

        int background = !button.active ? NAVY_DISABLED
                : hovered ? NAVY_HOVER : NAVY;
        int border = hovered ? BORDER_HOVER : BORDER;
        int stripe = primary ? ACCENT
                : hovered ? ACCENT_SOFT : BORDER;
        int textColor = !button.active ? MUTED
                : primary ? PALE_GOLD : WHITE;

        int left = button.getX();
        int top = button.getY();
        int right = left + button.getWidth();
        int bottom = top + button.getHeight();
        graphics.fill(left, top, right, bottom, background);
        graphics.fill(left, top, right, top + 1, border);
        graphics.fill(left, bottom - 1, right, bottom, border);
        graphics.fill(left, top, left + 1, bottom, border);
        graphics.fill(right - 1, top, right, bottom, border);
        graphics.fill(left + 1, top + 1,
                left + (primary || hovered ? 4 : 2), bottom - 1, stripe);

        int textY = top + Math.max(1,
                (button.getHeight() - 8) / 2);
        int stateLength = plain.endsWith(": ON") ? 2
                : plain.endsWith(": OFF") ? 3 : 0;
        if (stateLength > 0) {
            String prefix = plain.substring(0, plain.length() - stateLength);
            String state = plain.substring(plain.length() - stateLength);
            Component prefixLabel = ScpFonts.roboto(prefix);
            Component stateLabel = ScpFonts.roboto(state);
            int totalWidth = font.width(prefixLabel) + font.width(stateLabel);
            int textX = left + Math.max(5,
                    (button.getWidth() - totalWidth) / 2);
            graphics.drawString(font, prefixLabel, textX, textY,
                    textColor, false);
            int stateColor = !button.active ? MUTED
                    : "ON".equals(state) ? MODULE_ON : MODULE_OFF;
            graphics.drawString(font, stateLabel,
                    textX + font.width(prefixLabel), textY,
                    stateColor, false);
        } else {
            int textX = left + Math.max(5,
                    (button.getWidth() - font.width(label)) / 2);
            graphics.drawString(font, label, textX, textY,
                    textColor, false);
        }
    }

    private static boolean contains(Button button, int mouseX,
                                    int mouseY) {
        return mouseX >= button.getX()
                && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY()
                && mouseY < button.getY() + button.getHeight();
    }

    private static JsonObject object(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonObject()) {
            root.add(key, new JsonObject());
        }
        return root.getAsJsonObject(key);
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

    private static String compact(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text
                : text.substring(0, Math.max(0, max - 3)) + "...";
    }
}
