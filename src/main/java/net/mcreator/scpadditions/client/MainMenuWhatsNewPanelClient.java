package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Compact, scrollable What's New panel for the SCP Additions title screen. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class MainMenuWhatsNewPanelClient {
    private static final int TEXT = 0xFFF5F6F7;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final int PANEL = 0xC70B0E12;
    private static final int TRACK = 0x553A4049;

    private static final int HEADER_HEIGHT = 54;
    private static final int ROW_HEIGHT = 19;

    private static final List<String> HIGHLIGHTS = List.of(
            "SCP-106",
            "Reworked SCP-079 facility control",
            "Core Room Elevator",
            "Reworked survival systems",
            "Custom hotbar and oxygen HUD",
            "Expanded SCP Inventory interfaces",
            "Custom crosshair",
            "Documents and expanded Codex",
            "Rebuilt contextual interactions",
            "Expanded Configuration Center",
            "Reworked main menu and loading screens"
    );

    private static final Map<CustomMainMenuScreen, Integer> SCROLL =
            new WeakHashMap<>();

    private MainMenuWhatsNewPanelClient() {
    }

    public static void render(CustomMainMenuScreen screen, GuiGraphics graphics) {
        if (screen.width < 720 || screen.height < 380) return;

        Panel panel = panel(screen);
        Font font = Minecraft.getInstance().font;

        graphics.fill(panel.x, panel.y,
                panel.x + panel.width, panel.y + panel.height, PANEL);
        graphics.fill(panel.x, panel.y,
                panel.x + panel.width, panel.y + 2, ACCENT);

        drawScaledText(graphics, font, ScpFonts.montserrat("WHAT'S NEW"),
                panel.x + 15, panel.y + 13, 1.04F, TEXT);
        drawScaledText(graphics, font,
                ScpFonts.titillium("VERSION " + majorVersion()),
                panel.x + 15, panel.y + 29, 0.96F, ACCENT_BRIGHT);

        int visibleRows = visibleRows(panel);
        int maxOffset = Math.max(0, HIGHLIGHTS.size() - visibleRows);
        int offset = Mth.clamp(SCROLL.getOrDefault(screen, 0), 0, maxOffset);
        if (offset != SCROLL.getOrDefault(screen, 0)) {
            SCROLL.put(screen, offset);
        }

        int listTop = panel.y + HEADER_HEIGHT;
        int listBottom = panel.y + panel.height - 9;
        int textWidth = panel.width - 52;

        graphics.enableScissor(panel.x + 10, listTop - 1,
                panel.x + panel.width - 10, listBottom + 1);

        for (int row = 0; row < visibleRows; row++) {
            int index = offset + row;
            if (index >= HIGHLIGHTS.size()) break;

            int lineY = listTop + row * ROW_HEIGHT;
            int bulletHeight = 7;
            int bulletY = lineY + Math.max(0,
                    (font.lineHeight - bulletHeight) / 2);
            graphics.fill(panel.x + 15, bulletY,
                    panel.x + 19, bulletY + bulletHeight, ACCENT);

            String text = compactToWidth(font, HIGHLIGHTS.get(index), textWidth);
            graphics.drawString(font, ScpFonts.roboto(text),
                    panel.x + 27, lineY, TEXT, false);
        }

        graphics.disableScissor();

        if (maxOffset > 0) {
            drawScrollbar(graphics, panel, visibleRows, offset, maxOffset);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof CustomMainMenuScreen screen)) return;
        if (!ClientModulePreferences.customMainMenuEnabled()) return;

        Panel panel = panel(screen);
        if (!panel.contains(event.getMouseX(), event.getMouseY())) return;

        int visibleRows = visibleRows(panel);
        int maxOffset = Math.max(0, HIGHLIGHTS.size() - visibleRows);
        if (maxOffset <= 0 || event.getScrollDelta() == 0.0D) return;

        int current = Mth.clamp(SCROLL.getOrDefault(screen, 0), 0, maxOffset);
        int direction = event.getScrollDelta() > 0.0D ? -1 : 1;
        int next = Mth.clamp(current + direction, 0, maxOffset);
        if (next != current) {
            SCROLL.put(screen, next);
        }
        event.setCanceled(true);
    }

    private static void drawScrollbar(GuiGraphics graphics, Panel panel,
            int visibleRows, int offset, int maxOffset) {
        int trackX = panel.x + panel.width - 8;
        int trackTop = panel.y + HEADER_HEIGHT;
        int trackBottom = panel.y + panel.height - 10;
        int trackHeight = Math.max(8, trackBottom - trackTop);

        graphics.fill(trackX, trackTop, trackX + 2, trackBottom, TRACK);

        float visibleFraction = visibleRows / (float) HIGHLIGHTS.size();
        int thumbHeight = Math.max(12, Math.round(trackHeight * visibleFraction));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackTop + Math.round(travel
                * (offset / (float) Math.max(1, maxOffset)));
        graphics.fill(trackX, thumbY, trackX + 2,
                thumbY + thumbHeight, ACCENT);
    }

    private static int visibleRows(Panel panel) {
        return Math.max(1, (panel.height - HEADER_HEIGHT - 9) / ROW_HEIGHT);
    }

    private static Panel panel(CustomMainMenuScreen screen) {
        int width = Mth.clamp(Math.round(screen.width * 0.30F), 248, 390);
        int height = Mth.clamp(Math.round(screen.height * 0.285F), 132, 176);
        int x = screen.width - width
                - Math.max(24, Math.round(screen.width * 0.034F));
        int y = screen.height - height
                - Math.max(26, Math.round(screen.height * 0.055F));
        return new Panel(x, y, width, height);
    }

    private static String majorVersion() {
        String version = ModList.get().getModContainerById(ScpAdditionsMod.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("4.0.0");
        String[] parts = version.split("\\.");
        return parts.length >= 2 ? parts[0] + "." + parts[1] : version;
    }

    private static String compactToWidth(Font font, String text, int maxWidth) {
        if (font.width(ScpFonts.roboto(text)) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = font.width(ScpFonts.roboto(suffix));
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            String candidate = result.toString() + text.charAt(index);
            if (font.width(ScpFonts.roboto(candidate)) + suffixWidth > maxWidth) {
                break;
            }
            result.append(text.charAt(index));
        }
        return result.append(suffix).toString();
    }

    private static void drawScaledText(GuiGraphics graphics, Font font,
            net.minecraft.network.chat.Component text,
            float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private record Panel(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }
}
