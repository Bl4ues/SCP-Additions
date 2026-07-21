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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds the developer HUD toggles to General & Modules while preserving the
 * exact SCP Unity presentation used by the native Configuration Center.
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
                    "Could not attach Debug settings to General & Modules",
                    exception);
        }
    }

    private record Row(String group, String key, String label,
                       String description, boolean fallback) {
    }

    private static final class ExtendedModulesScreen extends Screen {
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
        private static final int ROW_HEIGHT = 34;

        private static final List<Row> ROWS = List.of(
                new Row("inventory", "enabled", "SCP Inventory",
                        "Enables the custom survival-horror inventory.", true),
                new Row("inventory", "remember_ui_state", "Remember UI State",
                        "Remembers the selected panel, document and scroll positions until leaving the world.", true),
                new Row("interactions", "enabled", "Contextual Interactions",
                        "Enables SCP Unity-style interaction prompts.", true),
                new Row("interactions", "disable_in_creative", "Hide Prompts in Creative",
                        "Disables custom prompts for Creative players.", false),
                new Row("hud", "enabled", "Custom HUD",
                        "Shows the SCP Additions health, stamina and blink presentation.", true),
                new Row("vitals", "custom_health_enabled", "Custom Health",
                        "Enables custom health behavior.", true),
                new Row("vitals", "stamina_enabled", "Stamina",
                        "Enables stamina drain and regeneration.", true),
                new Row("vitals", "horror_movement_enabled", "Survival-Horror Movement",
                        "Uses slower walking and committed sprinting.", true),
                new Row("blink", "enabled", "Blink System",
                        "Enables automatic and manual blinking.", true),
                new Row("scp_173", "enabled", "SCP-173",
                        "Enables SCP-173 behavior. Natural spawning uses the 173spawn gamerule.", true),
                new Row("debug", "show_scp_079_energy_hud", "SCP-079 Energy HUD",
                        "Shows SCP-079 processing power in the upper-right corner for testing.", false),
                new Row("debug", "show_scp_spawn_timers_hud", "SCP Spawn Timers HUD",
                        "Shows each roamer's state, countdown and latest scheduler result.", false)
        );

        private final Screen parent;
        private final JsonObject working;
        private final List<Button> buttons = new ArrayList<>();
        private final Map<Button, Component> labels = new IdentityHashMap<>();
        private int scroll;
        private boolean saving;

        private ExtendedModulesScreen(Screen parent, JsonObject working) {
            super(ScpFonts.roboto("General & Modules"));
            this.parent = parent;
            this.working = working;
        }

        @Override
        protected void init() {
            rebuildModuleWidgets();
        }

        private void rebuildModuleWidgets() {
            clearWidgets();
            buttons.clear();
            labels.clear();

            int panelWidth = Math.min(560, width - 20);
            int panelHeight = Math.min(380, height - 16);
            int panelX = Math.max(8, (width - panelWidth) / 2);
            int panelY = Math.max(8, (height - panelHeight) / 2);
            int contentX = panelX + 16;
            int contentY = panelY + 44;
            int visible = visibleRows();
            int end = Math.min(ROWS.size(), scroll + visible);

            for (int i = scroll; i < end; i++) {
                Row row = ROWS.get(i);
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

            int bottom = Math.min(height - 30,
                    contentY + visible * ROW_HEIGHT + 8);
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
            JsonObject payload = new JsonObject();
            payload.add(ConfigCenterService.MODULES, working);
            ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.SaveRequest(
                    GSON.toJson(payload)));
            rebuildModuleWidgets();
        }

        private void resetDefaults() {
            for (Row row : ROWS) {
                object(working, row.group()).addProperty(row.key(),
                        row.fallback());
            }
            rebuildModuleWidgets();
        }

        private String toggleLabel(Row row) {
            return row.label() + ": "
                    + (bool(object(working, row.group()), row.key(),
                    row.fallback()) ? "ON" : "OFF");
        }

        private int visibleRows() {
            return Math.max(4, Math.min(8, (height - 118) / ROW_HEIGHT));
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
            int panelHeight = Math.min(380, height - 16);
            int panelX = Math.max(8, (width - panelWidth) / 2);
            int panelY = Math.max(8, (height - panelHeight) / 2);

            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + panelHeight, PANEL);
            graphics.fill(panelX, panelY, panelX + panelWidth,
                    panelY + 26, HEADER);
            graphics.drawString(font, ScpFonts.roboto(title), panelX + 10,
                    panelY + 9, WHITE, false);
            graphics.drawString(font,
                    ScpFonts.roboto("Mouse wheel: scroll options"),
                    panelX + panelWidth - 160, panelY + 31, MUTED, false);

            int visible = visibleRows();
            int startY = panelY + 68;
            int end = Math.min(ROWS.size(), scroll + visible);
            for (int i = scroll; i < end; i++) {
                graphics.drawString(font,
                        ScpFonts.roboto(compact(ROWS.get(i).description(), 80)),
                        panelX + 18,
                        startY + (i - scroll) * ROW_HEIGHT,
                        MUTED, false);
            }

            for (Button button : buttons) button.setMessage(Component.empty());
            super.render(graphics, mouseX, mouseY, partialTick);

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
            boolean primary = plain.startsWith("Save");

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

            int textX = left + Math.max(5,
                    (button.getWidth() - font.width(label)) / 2);
            int textY = top + Math.max(1,
                    (button.getHeight() - 8) / 2);
            graphics.drawString(font, label, textX, textY,
                    textColor, false);
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

        private static String compact(String text, int max) {
            if (text == null) return "";
            return text.length() <= max ? text
                    : text.substring(0, Math.max(0, max - 3)) + "...";
        }
    }
}
