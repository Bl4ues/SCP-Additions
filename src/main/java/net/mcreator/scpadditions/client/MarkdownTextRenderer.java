package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

/**
 * Small Markdown subset shared by structured documents and legacy Codex text.
 *
 * <p>Supported syntax: {@code **bold**}, {@code *italic*},
 * {@code ***bold italic***}, {@code ==highlight==}, {@code ---}, and
 * {@code [[redacted]]}.</p>
 */
public final class MarkdownTextRenderer {
    private static final int HIGHLIGHT_COLOR = 0x99E8D75F;

    private MarkdownTextRenderer() {
    }

    public static int measureLines(String markdown, int width,
                                   float textScale, Font font) {
        return layout(markdown, 0, Integer.MAX_VALUE, 1,
                textScale, ignored -> Math.max(1, width),
                font, null, 0, 0, false);
    }

    public static int render(GuiGraphics graphics, String markdown,
                             int x, int startY, int maxY,
                             int lineHeight, float textScale,
                             IntUnaryOperator widthAtY, int color) {
        Font font = Minecraft.getInstance().font;
        return layout(markdown, x, maxY, lineHeight,
                textScale, widthAtY, font, graphics,
                startY, color, true);
    }

    private static int layout(String markdown, int startX, int maxY,
                              int lineHeight, float textScale,
                              IntUnaryOperator widthAtY, Font font,
                              GuiGraphics graphics, int startY,
                              int color, boolean draw) {
        String normalized = markdown == null ? ""
                : markdown.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);
        int y = startY;
        int paragraphGap = Math.max(3,
                Math.round(lineHeight * 0.28F));

        for (int paragraphIndex = 0;
             paragraphIndex < paragraphs.length;
             paragraphIndex++) {
            String paragraph = paragraphs[paragraphIndex];
            int available = Math.max(1, widthAtY.applyAsInt(y));

            if (paragraph.trim().equals("---")) {
                if (draw && y + lineHeight >= 0 && y <= maxY) {
                    int center = y + Math.max(1, lineHeight / 2);
                    graphics.fill(startX, center,
                            startX + available,
                            center + Math.max(1,
                                    Math.round(textScale * 0.7F)),
                            0xAA202020);
                }
                y += lineHeight;
                if (paragraphIndex + 1 < paragraphs.length) {
                    y += paragraphGap;
                }
                if (draw && y > maxY) {
                    return heightInLines(startY, y, lineHeight);
                }
                continue;
            }

            if (paragraph.isEmpty()) {
                y += lineHeight;
                if (draw && y > maxY) {
                    return heightInLines(startY, y, lineHeight);
                }
                continue;
            }

            List<Word> words = toWords(parse(paragraph));
            int cursor = 0;
            while (cursor < words.size()) {
                available = Math.max(1, widthAtY.applyAsInt(y));
                List<LineWord> line = new ArrayList<>();
                int naturalWidth = 0;
                int spaceCount = 0;

                while (cursor < words.size()) {
                    Word word = words.get(cursor);
                    int wordWidth = wordWidth(word, font, textScale);
                    boolean spaced = !line.isEmpty() && word.spaceBefore();
                    int spaceWidth = spaced
                            ? scaledSpaceWidth(font, textScale) : 0;

                    if (!line.isEmpty()
                            && naturalWidth + spaceWidth + wordWidth
                            > available) {
                        break;
                    }

                    line.add(new LineWord(word, spaced, wordWidth));
                    naturalWidth += spaceWidth + wordWidth;
                    if (spaced) spaceCount++;
                    cursor++;

                    if (line.size() == 1 && naturalWidth > available) {
                        break;
                    }
                }

                if (line.isEmpty()) {
                    Word word = words.get(cursor++);
                    int wordWidth = wordWidth(word, font, textScale);
                    line.add(new LineWord(word, false, wordWidth));
                    naturalWidth = wordWidth;
                }

                boolean justify = cursor < words.size()
                        && spaceCount > 0;
                if (draw && y + lineHeight >= 0 && y <= maxY) {
                    renderLine(graphics, font, line, startX, y,
                            available, naturalWidth, spaceCount,
                            justify, lineHeight, textScale, color);
                }

                y += lineHeight;
                if (draw && y > maxY) {
                    return heightInLines(startY, y, lineHeight);
                }
            }

            if (paragraphIndex + 1 < paragraphs.length) {
                y += paragraphGap;
                if (draw && y > maxY) {
                    return heightInLines(startY, y, lineHeight);
                }
            }
        }

        return heightInLines(startY, y, lineHeight);
    }

    private static int heightInLines(int startY, int endY,
                                     int lineHeight) {
        int height = Math.max(lineHeight, endY - startY);
        return Math.max(1,
                (height + Math.max(1, lineHeight) - 1)
                        / Math.max(1, lineHeight));
    }

    private static void renderLine(GuiGraphics graphics, Font font,
                                   List<LineWord> line,
                                   int startX, int y,
                                   int available, int naturalWidth,
                                   int spaceCount, boolean justify,
                                   int lineHeight, float textScale,
                                   int color) {
        int extra = justify
                ? Math.max(0, available - naturalWidth) : 0;
        int distributed = 0;
        int encounteredSpaces = 0;
        int x = startX;
        int baseSpace = scaledSpaceWidth(font, textScale);
        boolean previousHighlighted = false;

        for (LineWord placed : line) {
            int gapStart = x;
            if (placed.spaceBefore()) {
                int bonus = 0;
                if (justify && spaceCount > 0) {
                    encounteredSpaces++;
                    int target = Math.round(
                            extra * (encounteredSpaces
                                    / (float) spaceCount));
                    bonus = target - distributed;
                    distributed = target;
                }
                x += baseSpace + bonus;
            }

            Word word = placed.word();
            if (word.redacted()) {
                int desiredHeight = Math.max(5, Math.round(
                        font.lineHeight * textScale * 1.08F));
                int barHeight = Math.min(
                        Math.max(5, lineHeight - 1),
                        desiredHeight);
                int barY = y + Math.max(0,
                        (lineHeight - barHeight) / 2);
                graphics.fill(x, barY,
                        x + placed.width(), barY + barHeight,
                        0xFF101010);
            } else {
                if (word.highlighted()) {
                    int markHeight = Math.max(3, Math.round(
                            font.lineHeight * textScale * 0.78F));
                    int markY = y + Math.max(0, Math.round(
                            font.lineHeight * textScale * 0.20F));
                    int markX = previousHighlighted
                            && placed.spaceBefore() ? gapStart : x - 1;
                    graphics.fill(markX, markY,
                            x + placed.width() + 1,
                            markY + markHeight,
                            HIGHLIGHT_COLOR);
                }
                drawScaled(graphics, word.component(),
                        x, y, color, textScale, word.bold());
            }
            x += placed.width();
            previousHighlighted = word.highlighted();
        }
    }

    private static void drawScaled(GuiGraphics graphics,
                                   Component component,
                                   int x, int y, int color,
                                   float scale, boolean bold) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font,
                component, 0, 0, color, false);
        if (bold) {
            graphics.pose().translate(0.34F, 0.0F, 0.01F);
            graphics.drawString(Minecraft.getInstance().font,
                    component, 0, 0, color, false);
        }
        graphics.pose().popPose();
    }

    private static int wordWidth(Word word, Font font,
                                 float textScale) {
        int width = Math.max(1, Math.round(
                font.width(word.component()) * textScale));
        if (word.bold()) {
            width += Math.max(1,
                    Math.round(textScale * 0.34F));
        }
        return width;
    }

    private static int scaledSpaceWidth(Font font, float textScale) {
        return Math.max(1,
                Math.round(font.width(" ") * textScale));
    }

    private static List<Word> toWords(List<Run> runs) {
        List<Word> words = new ArrayList<>();
        boolean pendingSpace = false;

        for (Run run : runs) {
            String text = run.text();
            if (text.isEmpty()) continue;

            if (run.redacted()) {
                boolean leadingSpace = Character.isWhitespace(
                        text.charAt(0));
                boolean trailingSpace = Character.isWhitespace(
                        text.charAt(text.length() - 1));
                String content = text.trim();
                if (!content.isEmpty()) {
                    MutableComponent component =
                            ScpFonts.roboto(content)
                                    .withStyle(style ->
                                            style.withItalic(
                                                    run.italic()));
                    words.add(new Word(component, run.bold(),
                            true, false,
                            pendingSpace || leadingSpace));
                }
                pendingSpace = trailingSpace;
                continue;
            }

            int cursor = 0;
            while (cursor < text.length()) {
                if (Character.isWhitespace(text.charAt(cursor))) {
                    pendingSpace = true;
                    cursor++;
                    continue;
                }

                int end = cursor + 1;
                while (end < text.length()
                        && !Character.isWhitespace(
                                text.charAt(end))) {
                    end++;
                }

                String wordText = text.substring(cursor, end);
                MutableComponent component =
                        ScpFonts.roboto(wordText)
                                .withStyle(style ->
                                        style.withItalic(run.italic()));
                words.add(new Word(component, run.bold(),
                        false, run.highlighted(), pendingSpace));
                pendingSpace = false;
                cursor = end;
            }
        }

        return words;
    }

    private static List<Run> parse(String line) {
        List<Run> runs = new ArrayList<>();
        int cursor = 0;

        while (cursor < line.length()) {
            Marker marker = nextMarker(line, cursor);
            if (marker == null) {
                runs.add(new Run(line.substring(cursor),
                        false, false, false, false));
                break;
            }

            if (marker.index() > cursor) {
                runs.add(new Run(
                        line.substring(cursor, marker.index()),
                        false, false, false, false));
            }

            int contentStart = marker.index()
                    + marker.open().length();
            int end = line.indexOf(marker.close(), contentStart);
            if (end < 0) {
                runs.add(new Run(line.substring(marker.index()),
                        false, false, false, false));
                break;
            }

            String content = line.substring(contentStart, end);
            runs.add(new Run(content, marker.bold(),
                    marker.italic(), marker.redacted(),
                    marker.highlighted()));
            cursor = end + marker.close().length();
        }

        if (runs.isEmpty() && !line.isEmpty()) {
            runs.add(new Run(line,
                    false, false, false, false));
        }
        return runs;
    }

    private static Marker nextMarker(String line, int start) {
        Marker best = null;
        for (Marker candidate : List.of(
                new Marker(line.indexOf("[[", start),
                        "[[", "]]", false, false, true, false),
                new Marker(line.indexOf("***", start),
                        "***", "***", true, true, false, false),
                new Marker(line.indexOf("**", start),
                        "**", "**", true, false, false, false),
                new Marker(line.indexOf("==", start),
                        "==", "==", false, false, false, true),
                new Marker(line.indexOf("*", start),
                        "*", "*", false, true, false, false))) {
            if (candidate.index() < 0) continue;
            if (best == null
                    || candidate.index() < best.index()
                    || (candidate.index() == best.index()
                    && candidate.open().length()
                    > best.open().length())) {
                best = candidate;
            }
        }
        return best;
    }

    private record Run(String text, boolean bold,
                       boolean italic, boolean redacted,
                       boolean highlighted) {
    }

    private record Word(Component component, boolean bold,
                        boolean redacted, boolean highlighted,
                        boolean spaceBefore) {
    }

    private record LineWord(Word word, boolean spaceBefore,
                            int width) {
    }

    private record Marker(int index, String open, String close,
                          boolean bold, boolean italic,
                          boolean redacted, boolean highlighted) {
    }
}
