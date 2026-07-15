package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.client.gui.ItemConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared SCP Unity presentation for every native configuration screen.
 * Existing buttons keep their click behavior; this layer replaces only their
 * final rendering and standardizes Roboto text and user-facing tooltips.
 */
@Mod.EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class UnityConfigurationUiEvents {
    private static final int NAVY = 0xF000071F;
    private static final int NAVY_HOVER = 0xF0141E42;
    private static final int NAVY_DISABLED = 0xE01B1E26;
    private static final int GOLD = 0xFFE1A704;
    private static final int GOLD_SOFT = 0xFFB78B13;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int DANGER = 0xFFD46060;
    private static final Map<String, String> TOOLTIPS = buildTooltips();

    private UnityConfigurationUiEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!isConfigurationScreen(event.getScreen())) return;

        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractButton button) {
                button.setMessage(ScpFonts.roboto(button.getMessage()));
            } else if (listener instanceof EditBox editBox) {
                editBox.setFormatter((value, cursor) -> ScpFonts.roboto(value).getVisualOrderText());
                editBox.setTextColor(WHITE);
                editBox.setTextColorUneditable(MUTED);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isConfigurationScreen(screen)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        Font font = Minecraft.getInstance().font;
        Component hoveredTooltip = null;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 420.0F);
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractButton button) || !button.visible) continue;

            boolean hovered = contains(button, mouseX, mouseY);
            String plain = button.getMessage().getString();
            boolean danger = isDanger(plain);
            boolean primary = isPrimary(plain);

            int background = !button.active ? NAVY_DISABLED : hovered ? NAVY_HOVER : NAVY;
            int border = danger ? DANGER : hovered || primary ? GOLD : GOLD_SOFT;
            int text = !button.active ? MUTED : primary && !danger ? GOLD : WHITE;

            int x = button.getX();
            int y = button.getY();
            int right = x + button.getWidth();
            int bottom = y + button.getHeight();

            graphics.fill(x, y, right, bottom, background);
            graphics.fill(x, y, right, y + 1, border);
            graphics.fill(x, bottom - 1, right, bottom, border);
            graphics.fill(x, y, x + 1, bottom, border);
            graphics.fill(right - 1, y, right, bottom, border);
            graphics.fill(x + 1, y + 1, x + 4, bottom - 1, border);

            Component message = ScpFonts.roboto(button.getMessage());
            int textX = x + Math.max(5, (button.getWidth() - font.width(message)) / 2);
            int textY = y + Math.max(1, (button.getHeight() - 8) / 2);
            graphics.drawString(font, message, textX, textY, text, false);

            if (hovered) {
                String tooltip = tooltipFor(plain);
                if (!tooltip.isBlank()) hoveredTooltip = ScpFonts.roboto(tooltip);
            }
        }
        graphics.pose().popPose();

        if (hoveredTooltip != null) {
            graphics.renderTooltip(font, hoveredTooltip, mouseX, mouseY);
        }
    }

    private static boolean isConfigurationScreen(Screen screen) {
        if (screen == null) return false;
        if (screen instanceof ItemConfigScreen) return true;
        return screen.getClass().getName().startsWith(
                "net.mcreator.scpadditions.config.ui.ConfigCenterClient$");
    }

    private static boolean contains(AbstractButton button, int mouseX, int mouseY) {
        return mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
    }

    private static boolean isDanger(String label) {
        String value = normalize(label);
        return value.equals("x") || value.startsWith("delete") || value.startsWith("remove")
                || value.startsWith("forget") || value.startsWith("confirm");
    }

    private static boolean isPrimary(String label) {
        String value = normalize(label);
        return value.startsWith("save") || value.startsWith("done") || value.startsWith("create")
                || value.startsWith("+ add") || value.startsWith("add ") || value.startsWith("apply");
    }

    private static String tooltipFor(String label) {
        String normalized = normalize(label);
        for (Map.Entry<String, String> entry : TOOLTIPS.entrySet()) {
            if (normalized.equals(entry.getKey()) || normalized.startsWith(entry.getKey() + ":")
                    || normalized.startsWith(entry.getKey() + " ")) {
                return entry.getValue();
            }
        }
        if (normalized.startsWith("category")) return "Choose how this item is stored and used by SCP Inventory.";
        if (normalized.startsWith("type")) return "Choose the SCP Inventory category assigned to this item.";
        if (normalized.contains("on") || normalized.contains("off")) return "Toggle this setting. Changes are applied only after saving.";
        if (normalized.startsWith("x")) return "Remove this entry from the working configuration.";
        return "Open or modify this configuration entry.";
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> buildTooltips() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("general & modules", "Enable or disable gameplay, HUD, blink, interaction and SCP systems.");
        values.put("inventory, equipment & codex", "Edit item categories, equipment effects, Status filters and Codex documents.");
        values.put("contextual interactions", "Edit the prompts and right-click actions attached to blocks and entities.");
        values.put("scp-294 drinks", "Create, search and edit drinks, aliases, colors, effects and machine actions.");
        values.put("scp-914 recipes", "Create and edit intake, setting, output and advanced SCP-914 transformations.");
        values.put("item categories & equipment effects", "Classify items and assign supported equipment modifiers.");
        values.put("hidden status effects", "Choose effects that should not appear in the custom Conditions panel.");
        values.put("scp-173 entity targets", "Choose entities that participate in SCP-173 observation and targeting rules.");
        values.put("codex documents", "Create document definitions shown in the SCP Inventory Codex.");
        values.put("reload snapshot", "Discard unsaved local edits and request the current server configuration again.");
        values.put("save & reload", "Validate, save with a .bak backup and reload the affected system.");
        values.put("save all", "Validate and save every edited section in this configuration group.");
        values.put("save rule", "Save this item category and its equipment effects.");
        values.put("save", "Validate and save the current changes.");
        values.put("defaults", "Restore the default values in this screen before saving.");
        values.put("back", "Return to the previous configuration screen.");
        values.put("cancel", "Discard the unsaved changes on this screen.");
        values.put("done", "Close the configuration center.");
        values.put("forget", "Remove this item's explicit rule and return it to automatic classification.");
        values.put("confirm", "Confirm the destructive removal action.");
        values.put("+ add item", "Search the loaded item registry and create a new item rule.");
        values.put("+ add drink", "Create a new SCP-294 drink definition.");
        values.put("+ add recipe", "Create a new SCP-914 recipe in the in-game editor fragment.");
        values.put("duplicate", "Create an independent copy of this entry for faster editing.");
        values.put("additional recipe settings", "Show less common chance, NBT, action-bar and weighted-output options.");
        return Map.copyOf(values);
    }
}
