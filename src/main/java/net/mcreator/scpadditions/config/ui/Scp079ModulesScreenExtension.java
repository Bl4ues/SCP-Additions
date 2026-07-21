package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.network.ModNetwork;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
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

import java.lang.reflect.Field;
import java.util.List;

/**
 * Extends the established General & Modules editor with a final Debug section
 * without exposing the private configuration-center screen implementation.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp079ModulesScreenExtension {
    private static final String MODULES_SCREEN =
            "net.mcreator.scpadditions.config.ui.ConfigCenterClient$ModulesScreen";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

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
                event.setNewScreen(new ExtendedModulesScreen(
                        event.getCurrentScreen(), working));
            }
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.warn(
                    "Could not attach the Debug section to the configuration center",
                    exception);
        }
    }

    private record Row(boolean section, String group, String key, String label,
                       String description, boolean fallback) {
        private static Row section(String label) {
            return new Row(true, "", "", label, "", false);
        }

        private static Row toggle(String group, String key, String label,
                                  String description, boolean fallback) {
            return new Row(false, group, key, label, description, fallback);
        }
    }

    private static final class ExtendedModulesScreen extends Screen {
        private static final int PANEL = 0xEE111317;
        private static final int HEADER = 0xEE24282E;
        private static final int TEXT = 0xFFE8E8E8;
        private static final int MUTED = 0xFF9FA6AD;
        private static final int ACCENT = 0xFF88DDEE;
        private static final int ROW_HEIGHT = 34;

        private static final List<Row> ROWS = List.of(
                Row.section("Modules"),
                Row.toggle("inventory", "enabled", "SCP Inventory",
                        "Enables the custom survival-horror inventory.", true),
                Row.toggle("inventory", "remember_ui_state", "Remember UI State",
                        "Remembers selected panels and scroll positions until leaving the world.", true),
                Row.toggle("interactions", "enabled", "Contextual Interactions",
                        "Enables SCP Unity-style interaction prompts.", true),
                Row.toggle("interactions", "disable_in_creative", "Hide Prompts in Creative",
                        "Disables custom prompts for Creative players.", false),
                Row.toggle("hud", "enabled", "Custom HUD",
                        "Shows the SCP Additions health, stamina and blink presentation.", true),
                Row.toggle("vitals", "custom_health_enabled", "Custom Health",
                        "Enables custom health behavior.", true),
                Row.toggle("vitals", "stamina_enabled", "Stamina",
                        "Enables stamina drain and regeneration.", true),
                Row.toggle("vitals", "horror_movement_enabled", "Survival-Horror Movement",
                        "Uses slower walking and committed sprinting.", true),
                Row.toggle("blink", "enabled", "Blink System",
                        "Enables automatic and manual blinking.", true),
                Row.toggle("scp_173", "enabled", "SCP-173",
                        "Enables SCP-173 behavior.", true),
                Row.toggle("scp_173", "natural_spawn_enabled", "SCP-173 Natural Spawning",
                        "Allows the configurable natural spawn system.", true),
                Row.section("Debug"),
                Row.toggle("debug", "show_scp_079_energy_hud", "SCP-079 Energy HUD",
                        "Shows SCP-079 processing power in the upper-right corner for testing.", false),
                Row.toggle("debug", "show_scp_spawn_timers_hud", "SCP Spawn Timers HUD",
                        "Shows natural-spawn timers and the latest scheduler result for SCP-173 and SCP-106.", false)
        );

        private final Screen parent;
        private final JsonObject working;
        private int scroll;
        private boolean saving;

        private ExtendedModulesScreen(Screen parent, JsonObject working) {
            super(Component.literal("General & Modules"));
            this.parent = parent;
            this.working = working;
        }

        @Override
        protected void init() {
            rebuildModuleWidgets();
        }

        private void rebuildModuleWidgets() {
            clearWidgets();
            int panelWidth = Math.min(560, width - 20);
            int panelHeight = Math.min(400, height - 16);
            int panelX = Math.max(8, (width - panelWidth) / 2);
            int panelY = Math.max(8, (height - panelHeight) / 2);
            int contentX = panelX + 16;
            int contentY = panelY + 44;
            int visible = visibleRows();
            int end = Math.min(ROWS.size(), scroll + visible);

            for (int i = scroll; i < end; i++) {
                Row row = ROWS.get(i);
                if (row.section()) continue;
                int rowY = contentY + (i - scroll) * ROW_HEIGHT;
                addRenderableWidget(Button.builder(
                        Component.literal(toggleLabel(row)), button -> {
                            JsonObject group = object(working, row.group());
                            group.addProperty(row.key(), !bool(group, row.key(),
                                    row.fallback()));
                            button.setMessage(Component.literal(toggleLabel(row)));
                        }).bounds(contentX, rowY, panelWidth - 32, 22).build());
            }

            int bottom = Math.min(height - 30,
                    contentY + visible * ROW_HEIGHT + 8);
            addRenderableWidget(Button.builder(Component.literal("Defaults"),
                    button -> resetDefaults())
                    .bounds(contentX, bottom, 90, 20).build());
            addRenderableWidget(Button.builder(
                    Component.literal(saving ? "Saving..." : "Save & Reload"),
                    button -> save())
                    .bounds(contentX + panelWidth - 230, bottom, 108, 20)
                    .build()).active = !saving;
            addRenderableWidget(Button.builder(Component.literal("Back"),
                    button -> goBack())
                    .bounds(contentX + panelWidth - 116, bottom, 84, 20).build());
        }

        private void save() {
            if (saving) return;
            saving = true;
            JsonObject payload = new JsonObject();
            payload.add(ConfigCenterService.MODULES, working);
            ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.SaveRequest(
                    GSON.toJson(payload)));
            rebuildModuleWidgets();
        }

        private void resetDefaults() {
            for (Row row : ROWS) {
                if (!row.section()) {
                    object(working, row.group()).addProperty(row.key(),
                            row.fallback());
                }
            }
            rebuildModuleWidgets();
        }

        private String toggleLabel(Row row) {
            return row.label() + ": "
                    + (bool(object(working, row.group()), row.key(),
                    row.fallback()) ? "ON" : "OFF");
        }

        private int visibleRows() {
            return Math.max(4, Math.min(9, (height - 118) / ROW_HEIGHT));
        }

        private void rebuild() {
            rebuildModuleWidgets();
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
            int max = Math.max(0, ROWS.size() - visibleRows());
            int next = Math.max(0, Math.min(max,
                    scroll + (delta < 0.0D ? 1 : -1)));
            if (next != scroll) {
                scroll = next;
                rebuild();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick) {
            renderBackground(graphics);
            int panelWidth = Math.min(560, width - 20);
            int panelHeight = Math.min(400, height - 16);
            int panelX = Math.max(8, (width - panelWidth) / 2);
            int panelY = Math.max(8, (height - panelHeight) / 2);
            int contentY = panelY + 44;

            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + panelHeight, PANEL);
            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + 26, HEADER);
            graphics.drawString(font, title, panelX + 10, panelY + 9,
                    TEXT, false);
            graphics.drawString(font, "Mouse wheel: scroll options",
                    panelX + panelWidth - 160, panelY + 31, MUTED, false);

            int end = Math.min(ROWS.size(), scroll + visibleRows());
            for (int i = scroll; i < end; i++) {
                Row row = ROWS.get(i);
                int rowY = contentY + (i - scroll) * ROW_HEIGHT;
                if (row.section()) {
                    graphics.drawString(font, row.label(), panelX + 18,
                            rowY + 7, ACCENT, false);
                    graphics.fill(panelX + 78, rowY + 11,
                            panelX + panelWidth - 18, rowY + 12, HEADER);
                } else {
                    graphics.drawString(font, compact(row.description(), 76),
                            panelX + 18, rowY + 24, MUTED, false);
                }
            }
            super.render(graphics, mouseX, mouseY, partialTick);
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

        private static String compact(String text, int max) {
            if (text == null) return "";
            return text.length() <= max ? text
                    : text.substring(0, Math.max(0, max - 3)) + "...";
        }
    }
}
