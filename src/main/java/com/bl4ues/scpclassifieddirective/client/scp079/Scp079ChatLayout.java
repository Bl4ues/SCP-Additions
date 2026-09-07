package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;

/**
 * Fixed terminal chat presentation for playable SCP-079.
 *
 * This deliberately ignores the ordinary facility-chat preference and vanilla
 * chat geometry. The old computer owns its own terminal, including a block
 * cursor and a raised lower-left anchor that stays clear of gameplay status
 * readouts such as night vision.
 */
public final class Scp079ChatLayout {
    public static final int LEFT = 22;
    public static final int BOTTOM_CLEARANCE = 72;
    public static final int INPUT_HEIGHT = 21;
    public static final int HISTORY_GAP = 7;
    public static final int HISTORY_LINES = 7;
    public static final int HISTORY_LIFETIME_TICKS = 220;
    public static final int HISTORY_FADE_START_TICKS = 135;

    private static final int PROMPT_WIDTH = 42;
    private static final int MIN_INPUT_WIDTH = 250;
    private static final int MAX_INPUT_WIDTH = 500;
    private static final int PANEL = 0xD6060D12;
    private static final int PANEL_EDGE = 0xC94D7180;
    private static final int PANEL_ACCENT = 0xE7AEE7FA;
    private static final int CURSOR = 0xF0C8F2FF;
    private static final float TEXT_SCALE = 1.04F;
    // PF Videotext has substantial visual top padding. The normal mathematical
    // baseline made the glyphs sit above the deliberately chunky block caret.
    private static final int TEXT_Y_OFFSET = 10;

    private Scp079ChatLayout() {
    }

    public static int panelWidth(int screenWidth) {
        return Mth.clamp((int) Math.round(screenWidth * 0.36D),
                MIN_INPUT_WIDTH, MAX_INPUT_WIDTH);
    }

    public static int inputX() {
        return LEFT + PROMPT_WIDTH;
    }

    public static int inputY(int screenHeight) {
        return screenHeight - BOTTOM_CLEARANCE - INPUT_HEIGHT;
    }

    public static int inputWidth(int screenWidth) {
        return Math.max(80, panelWidth(screenWidth) - PROMPT_WIDTH - 10);
    }

    public static int historyBottom(int screenHeight) {
        return inputY(screenHeight) - HISTORY_GAP;
    }

    public static int historyWidth(int screenWidth) {
        return panelWidth(screenWidth);
    }

    /** Bottom edge used by vanilla's upward-growing command suggestion list. */
    public static int suggestionAnchor(EditBox input) {
        return Math.max(8, input.getY() - 4);
    }

    public static int suggestionTop(EditBox input, int screenHeight,
            int suggestionLineLimit) {
        int lineHeight = 12;
        int listHeight = Math.max(1, suggestionLineLimit) * lineHeight + 4;
        return Math.max(8, suggestionAnchor(input) - listHeight);
    }

    public static void positionInput(EditBox input, int screenWidth,
            int screenHeight) {
        if (input == null) return;
        input.setX(inputX());
        input.setY(inputY(screenHeight));
        input.setWidth(inputWidth(screenWidth));
    }

    /** Paints over vanilla's EditBox with the authored SCP-079 terminal input. */
    public static void renderInput(GuiGraphics graphics, EditBox input,
            Minecraft minecraft) {
        if (graphics == null || input == null || minecraft == null) return;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = LEFT;
        int y = inputY(minecraft.getWindow().getGuiScaledHeight());
        int width = panelWidth(screenWidth);
        int bottom = y + INPUT_HEIGHT;

        graphics.fill(x, y, x + width, bottom, PANEL);
        graphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
        graphics.fill(x, bottom - 1, x + width, bottom, PANEL_EDGE);
        graphics.fill(x, y, x + 2, bottom, PANEL_ACCENT);

        Scp079UiTheme.draw(graphics, minecraft.font, "079>",
                x + 8, y + TEXT_Y_OFFSET, TEXT_SCALE, Scp079UiTheme.ACCENT);

        Font font = minecraft.font;
        String value = input.getValue();
        int cursor = Mth.clamp(input.getCursorPosition(), 0, value.length());
        int textX = inputX();
        int available = Math.max(24, inputWidth(screenWidth) - 5);
        Window window = visibleWindow(font, value, cursor, available);

        int visibleCursor = Mth.clamp(cursor, window.start, window.end);
        String shown = value.substring(window.start, window.end);
        Scp079UiTheme.draw(graphics, font, shown,
                textX, y + TEXT_Y_OFFSET, TEXT_SCALE, Scp079UiTheme.TEXT);

        // Old terminals did not apologize with a delicate one-pixel caret.
        // A blunt block cursor makes the input look like a CRT terminal rather
        // than a modern text field wearing a retro font as a costume.
        if (input.isFocused() && (System.currentTimeMillis() / 420L & 1L) == 0L) {
            int cursorX = textX + width(font,
                    value.substring(window.start, visibleCursor));
            int cursorWidth = 5;
            if (visibleCursor < window.end) {
                cursorWidth = Math.max(4, Math.min(8, width(font,
                        value.substring(visibleCursor, visibleCursor + 1))));
            }
            graphics.fill(cursorX, y + 5,
                    Math.min(x + width - 6, cursorX + cursorWidth),
                    bottom - 5, CURSOR);
        }
    }

    public static Component terminalText(Component value) {
        return Component.empty().append(value == null ? Component.empty() : value)
                .withStyle(style -> style.withFont(ScpFonts.PF_VIDEOTEXT));
    }

    public static Component terminalText(String value) {
        return Component.literal(value == null ? "" : value)
                .withStyle(style -> style.withFont(ScpFonts.PF_VIDEOTEXT));
    }

    public static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private static Window visibleWindow(Font font, String value, int cursor,
            int available) {
        if (StringUtil.isNullOrEmpty(value)) return new Window(0, 0);
        int start = cursor;
        while (start > 0) {
            String candidate = value.substring(start - 1, cursor);
            if (width(font, candidate) > available * 2 / 3) break;
            start--;
        }
        int end = cursor;
        while (end < value.length()) {
            String candidate = value.substring(start, end + 1);
            if (width(font, candidate) > available) break;
            end++;
        }
        while (start > 0 && width(font, value.substring(start - 1, end))
                <= available) {
            start--;
        }
        return new Window(start, Math.max(start, end));
    }

    private static int width(Font font, String value) {
        return Math.round(font.width(terminalText(value)) * TEXT_SCALE);
    }

    private record Window(int start, int end) {
    }
}
