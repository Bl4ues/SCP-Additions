package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import net.mcreator.scpadditions.config.ConfigFilePersistence;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-only Configuration Center entry point used when no world is open.
 * World-owned editors remain available through the normal connected screen.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientPreferencesMenu {
    private static final Path PREFERENCES = FMLPaths.CONFIGDIR.get()
            .resolve("scpadditions").resolve("client_preferences.json");

    private static final int PANEL = 0xEE111317;
    private static final int HEADER = 0xEE24282E;
    private static final int NAVY = 0xFF081022;
    private static final int BORDER = 0xFF46536C;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int GOLD = 0xFFE5D49A;
    private static final int ON = 0xFF79D58B;
    private static final int OFF = 0xFFFF8B8B;

    private ClientPreferencesMenu() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            migrateMainMenuMusicDefault();
            MinecraftForge.registerConfigScreen(ClientPreferencesMenu::open);
        });
    }

    private static Screen open(Minecraft minecraft, Screen parent) {
        if (minecraft.player != null && minecraft.getConnection() != null) {
            ConfigCenterClient.requestOpen(parent);
            return minecraft.screen;
        }
        return new LocalHomeScreen(parent);
    }

    /**
     * Gson leaves newly-added primitive fields false in existing files. Migrate
     * the absent setting explicitly so the advertised default is actually true.
     */
    private static void migrateMainMenuMusicDefault() {
        try {
            if (!Files.exists(PREFERENCES)) {
                ClientModulePreferences.load();
                return;
            }

            JsonElement parsed = JsonParser.parseString(
                    Files.readString(PREFERENCES, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                ClientModulePreferences.load();
                return;
            }

            JsonObject root = parsed.getAsJsonObject();
            JsonObject audio;
            if (root.has("audio") && root.get("audio").isJsonObject()) {
                audio = root.getAsJsonObject("audio");
            } else {
                audio = new JsonObject();
                root.add("audio", audio);
            }

            if (!audio.has("mainMenuMusicEnabled")) {
                audio.addProperty("mainMenuMusicEnabled", true);
                ConfigFilePersistence.writeWithBackup(PREFERENCES,
                        root.toString() + System.lineSeparator());
            }
            ClientModulePreferences.load();
        } catch (Exception exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not migrate client menu-music preferences",
                    exception);
            ClientModulePreferences.load();
        }
    }

    private static JsonObject snapshot() {
        JsonObject modules = new JsonObject();
        ClientModulePreferences.applyTo(modules);
        return modules;
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
                || !root.get(key).isJsonPrimitive()) return fallback;
        try {
            return root.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double unit(JsonObject root, String key,
            double fallback) {
        if (root == null || !root.has(key)
                || !root.get(key).isJsonPrimitive()) return fallback;
        try {
            return Mth.clamp(root.get(key).getAsDouble(), 0.0D, 1.0D);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private abstract static class LocalScreen extends Screen {
        protected final Screen parent;

        protected LocalScreen(Screen parent, String title) {
            super(ScpFonts.roboto(title));
            this.parent = parent;
        }

        protected void back() {
            Minecraft.getInstance().setScreen(parent);
        }

        @Override
        public void onClose() {
            back();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        protected void panel(GuiGraphics graphics, int width, int height) {
            int x = (this.width - width) / 2;
            int y = (this.height - height) / 2;
            graphics.fill(x, y, x + width, y + height, PANEL);
            graphics.fill(x, y, x + width, y + 26, HEADER);
            graphics.drawString(font, title, x + 10, y + 9, WHITE, false);
        }
    }

    private static final class LocalHomeScreen extends LocalScreen {
        private LocalHomeScreen(Screen parent) {
            super(parent, "SCP Additions Configuration");
        }

        @Override
        protected void init() {
            int panelWidth = Math.min(430, width - 20);
            int x = (width - panelWidth) / 2 + 16;
            int y = (height - 270) / 2 + 58;
            int buttonWidth = panelWidth - 32;

            addRenderableWidget(Button.builder(
                    ScpFonts.roboto("Personal Modules & Audio"), button ->
                            minecraft.setScreen(new ToggleScreen(this)))
                    .bounds(x, y, buttonWidth, 24).build());
            addRenderableWidget(Button.builder(ScpFonts.roboto("Crosshair"),
                    button -> minecraft.setScreen(new CrosshairScreen(this)))
                    .bounds(x, y + 34, buttonWidth, 24).build());
            addRenderableWidget(Button.builder(
                    ScpFonts.roboto("Accessibility"), button ->
                            minecraft.setScreen(new AccessibilityScreen(this)))
                    .bounds(x, y + 68, buttonWidth, 24).build());
            addRenderableWidget(Button.builder(ScpFonts.roboto("Done"),
                    button -> back())
                    .bounds(x, y + 128, buttonWidth, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            panel(graphics, Math.min(430, width - 20), 270);
            int x = (width - Math.min(430, width - 20)) / 2 + 16;
            int y = (height - 270) / 2;
            graphics.drawString(font, ScpFonts.roboto(
                    "Local preferences are available without opening a world."),
                    x, y + 34, GOLD, false);
            graphics.drawString(font, ScpFonts.roboto(
                    "World and host settings remain locked until a world is open."),
                    x, y + 45, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private record Toggle(String group, String key, String label,
                          boolean fallback) {
    }

    private static final class ToggleScreen extends LocalScreen {
        private static final List<Toggle> OPTIONS = List.of(
                new Toggle("hud", "enabled", "Custom HUD", true),
                new Toggle("vitals", "custom_health_enabled",
                        "Custom Health", true),
                new Toggle("inventory", "custom_hotbar",
                        "Custom Hotbar", true),
                new Toggle("hud", "hide_active_effect_indicators",
                        "Hide Effect Indicators", true),
                new Toggle("hud", "hide_empty_hand",
                        "Hide Empty Hand", true),
                new Toggle("hud", "disable_experience_bar",
                        "Disable Experience Display", true),
                new Toggle("hud", "custom_oxygen_bar",
                        "Custom Oxygen Meter", true),
                new Toggle("hud", "action_bars_roboto",
                        "Roboto Action Bar", true),
                new Toggle("audio", "enter_sound_enabled",
                        "World Entry Sound", true),
                new Toggle("audio", "save_game_sound_enabled",
                        "Save Game Sound", true),
                new Toggle("audio", "replace_player_hurt_sounds",
                        "Replace Player Hurt Sounds", true),
                new Toggle("audio", "use_voice_profile_b",
                        "Use Voice Profile B", false),
                new Toggle("audio", "mute_non_player_hit_sounds",
                        "Remove Non-Player Hit Sounds", false),
                new Toggle("audio", "disable_vanilla_music",
                        "Disable Vanilla Music", true),
                new Toggle("audio", "main_menu_music_enabled",
                        "Main Menu Music", true)
        );

        private final JsonObject working = snapshot();
        private final List<Button> optionButtons = new ArrayList<>();
        private int scroll;

        private ToggleScreen(Screen parent) {
            super(parent, "Personal Modules & Audio");
        }

        @Override
        protected void init() {
            clearWidgets();
            optionButtons.clear();
            int panelWidth = Math.min(520, width - 20);
            int panelHeight = Math.min(390, height - 16);
            int x = (width - panelWidth) / 2 + 16;
            int y = (height - panelHeight) / 2 + 40;
            int visible = Math.max(1, (panelHeight - 104) / 30);
            int end = Math.min(OPTIONS.size(), scroll + visible);

            for (int i = scroll; i < end; i++) {
                Toggle option = OPTIONS.get(i);
                Button button = Button.builder(Component.empty(), clicked -> {
                    JsonObject group = object(working, option.group());
                    group.addProperty(option.key(), !bool(group,
                            option.key(), option.fallback()));
                    refreshLabels();
                }).bounds(x, y + (i - scroll) * 30,
                        panelWidth - 32, 22).build();
                optionButtons.add(addRenderableWidget(button));
            }

            int bottom = (height + panelHeight) / 2 - 30;
            addRenderableWidget(Button.builder(ScpFonts.roboto("Defaults"),
                    button -> {
                        ClientModulePreferences.resetDefaults(working);
                        refreshLabels();
                    }).bounds(x, bottom, 90, 20).build());
            addRenderableWidget(Button.builder(ScpFonts.roboto("Save"),
                    button -> {
                        ClientModulePreferences.captureAndSave(working);
                        back();
                    }).bounds(x + panelWidth - 232, bottom, 96, 20).build());
            addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                    button -> back())
                    .bounds(x + panelWidth - 128, bottom, 96, 20).build());
            refreshLabels();
        }

        private void refreshLabels() {
            for (int i = 0; i < optionButtons.size(); i++) {
                Toggle option = OPTIONS.get(scroll + i);
                boolean enabled = bool(object(working, option.group()),
                        option.key(), option.fallback());
                optionButtons.get(i).setMessage(ScpFonts.roboto(
                        option.label() + ": " + (enabled ? "ON" : "OFF")));
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY,
                double delta) {
            int panelHeight = Math.min(390, height - 16);
            int visible = Math.max(1, (panelHeight - 104) / 30);
            int next = Mth.clamp(scroll + (delta < 0 ? 1 : -1),
                    0, Math.max(0, OPTIONS.size() - visible));
            if (next != scroll) {
                scroll = next;
                init();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            int panelWidth = Math.min(520, width - 20);
            int panelHeight = Math.min(390, height - 16);
            panel(graphics, panelWidth, panelHeight);
            int x = (width - panelWidth) / 2 + 16;
            int y = (height - panelHeight) / 2 + 29;
            graphics.drawString(font, ScpFonts.roboto(
                    "These settings belong to this client, not to the world."),
                    x, y, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
            for (Button button : optionButtons) {
                String label = button.getMessage().getString();
                int stateColor = label.endsWith("ON") ? ON : OFF;
                String state = label.endsWith("ON") ? "ON" : "OFF";
                int stateWidth = font.width(ScpFonts.roboto(state));
                graphics.drawString(font, ScpFonts.roboto(state),
                        button.getX() + button.getWidth() - stateWidth - 10,
                        button.getY() + 7, stateColor, false);
            }
        }
    }

    private static final class AccessibilityScreen extends LocalScreen {
        private final JsonObject working = snapshot();
        private Button toggle;

        private AccessibilityScreen(Screen parent) {
            super(parent, "Accessibility");
        }

        @Override
        protected void init() {
            int panelWidth = Math.min(500, width - 20);
            int x = (width - panelWidth) / 2 + 16;
            int y = (height - 220) / 2 + 62;
            toggle = addRenderableWidget(Button.builder(Component.empty(), b -> {
                JsonObject accessibility = object(working, "accessibility");
                accessibility.addProperty("reduce_scp_012_visual_effects",
                        !bool(accessibility,
                                "reduce_scp_012_visual_effects", false));
                updateLabel();
            }).bounds(x, y, panelWidth - 32, 24).build());
            addRenderableWidget(Button.builder(ScpFonts.roboto("Save"), b -> {
                ClientModulePreferences.captureAndSave(working);
                back();
            }).bounds(x + panelWidth - 232, y + 70, 96, 20).build());
            addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                    b -> back()).bounds(x + panelWidth - 128,
                    y + 70, 96, 20).build());
            updateLabel();
        }

        private void updateLabel() {
            boolean enabled = bool(object(working, "accessibility"),
                    "reduce_scp_012_visual_effects", false);
            toggle.setMessage(ScpFonts.roboto(
                    "Reduce SCP-012 Visual Effects: "
                            + (enabled ? "ON" : "OFF")));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            int panelWidth = Math.min(500, width - 20);
            panel(graphics, panelWidth, 220);
            int x = (width - panelWidth) / 2 + 16;
            int y = (height - 220) / 2;
            graphics.drawString(font, ScpFonts.roboto(
                    "Photosensitive Epilepsy"), x, y + 36, GOLD, false);
            graphics.drawString(font, ScpFonts.roboto(
                    "Reduces flashing interference and subliminal images."),
                    x, y + 48, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class CrosshairScreen extends LocalScreen {
        private final JsonObject working = snapshot();
        private Button enabledButton;
        private Button visibleButton;

        private CrosshairScreen(Screen parent) {
            super(parent, "Crosshair");
        }

        @Override
        protected void init() {
            int panelWidth = Math.min(540, width - 20);
            int x = (width - panelWidth) / 2 + 16;
            int y = (height - 350) / 2 + 44;
            JsonObject crosshair = object(working, "crosshair");

            enabledButton = addRenderableWidget(Button.builder(Component.empty(),
                    b -> {
                        crosshair.addProperty("enabled",
                                !bool(crosshair, "enabled", true));
                        updateLabels();
                    }).bounds(x, y, panelWidth - 32, 22).build());
            visibleButton = addRenderableWidget(Button.builder(Component.empty(),
                    b -> {
                        crosshair.addProperty("in_game_enabled",
                                !bool(crosshair, "in_game_enabled", true));
                        updateLabels();
                    }).bounds(x, y + 30, panelWidth - 32, 22).build());

            addRenderableWidget(new ValueSlider(x, y + 82,
                    panelWidth - 32, "Red", crosshair, "red"));
            addRenderableWidget(new ValueSlider(x, y + 122,
                    panelWidth - 32, "Green", crosshair, "green"));
            addRenderableWidget(new ValueSlider(x, y + 162,
                    panelWidth - 32, "Blue", crosshair, "blue"));
            addRenderableWidget(new ValueSlider(x, y + 202,
                    panelWidth - 32, "Alpha", crosshair, "alpha"));

            addRenderableWidget(Button.builder(ScpFonts.roboto("Defaults"),
                    b -> {
                        crosshair.addProperty("enabled", true);
                        crosshair.addProperty("in_game_enabled", true);
                        crosshair.addProperty("red", 1.0D);
                        crosshair.addProperty("green", 1.0D);
                        crosshair.addProperty("blue", 1.0D);
                        crosshair.addProperty("alpha", 1.0D);
                        minecraft.setScreen(new CrosshairScreen(parent));
                    }).bounds(x, y + 250, 90, 20).build());
            addRenderableWidget(Button.builder(ScpFonts.roboto("Save"), b -> {
                ClientModulePreferences.captureAndSave(working);
                back();
            }).bounds(x + panelWidth - 232, y + 250, 96, 20).build());
            addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                    b -> back()).bounds(x + panelWidth - 128,
                    y + 250, 96, 20).build());
            updateLabels();
        }

        private void updateLabels() {
            JsonObject crosshair = object(working, "crosshair");
            enabledButton.setMessage(ScpFonts.roboto("Custom Crosshair: "
                    + (bool(crosshair, "enabled", true) ? "ON" : "OFF")));
            visibleButton.setMessage(ScpFonts.roboto("In-game Crosshair: "
                    + (bool(crosshair, "in_game_enabled", true)
                    ? "ON" : "OFF")));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            renderBackground(graphics);
            panel(graphics, Math.min(540, width - 20), 350);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final JsonObject settings;
        private final String key;

        private ValueSlider(int x, int y, int width, String label,
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
    }
}
