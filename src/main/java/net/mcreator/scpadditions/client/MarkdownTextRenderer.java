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

/** Small Markdown subset shared by document pages and legacy Codex text. */
public final class MarkdownTextRenderer {
    private MarkdownTextRenderer() {
    }

    public static int measureLines(String markdown, int width, float textScale,
                                   Font font) {
        return layout(markdown, 0, Integer.MAX_VALUE, 1, textScale,
                ignored -> Math.max(1, width), font, null, 0, 0, false);
    }

    public static int render(GuiGraphics graphics, String markdown,
                             int x, int startY, int maxY, int lineHeight,
                             float textScale, IntUnaryOperator widthAtY,
                             int color) {
        Font font = Minecraft.getInstance().font;
        return layout(markdown, x, maxY, lineHeight, textScale, widthAtY,
                font, graphics, startY, color, true);
    }

    private static int layout(String markdown, int startX, int maxY,
                              int lineHeight, float textScale,
                              IntUnaryOperator widthAtY, Font font,
                              GuiGraphics graphics, int startY, int color,
                              boolean draw) {
        String normalized = markdown == null ? ""
                : markdown.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);
        int y = startY;
        int lineCount = 0;

        for (String paragraph : paragraphs) {
            int available = Math.max(1, widthAtY.applyAsInt(y));
            if (paragraph.trim().equals("---")) {
                if (draw && y + lineHeight >= 0 && y <= maxY) {
                    int center = y + Math.max(1, lineHeight / 2);
                    graphics.fill(startX, center, startX + available,
                            center + Math.max(1, Math.round(textScale)),
                            0xAA202020);
                }
                y += lineHeight;
                lineCount++;
                if (draw && y > maxY) break;
                continue;
            }

            if (paragraph.isEmpty()) {
                y += lineHeight;
                lineCount++;
                if (draw && y > maxY) break;
                continue;
            }

            List<Piece> pieces = toWords(parse(paragraph));
            int x = startX;
            boolean lineHasContent = false;
            for (Piece piece : pieces) {
                int currentWidth = Math.max(1, widthAtY.applyAsInt(y));
                int pieceWidth = Math.max(1,
                        Math.round(font.width(piece.component()) * textScale));
                if (lineHasContent && x + pieceWidth > startX + currentWidth) {
                    x = startX;
                    y += lineHeight;
                    lineCount++;
                    lineHasContent = false;
                    if (draw && y > maxY) return Math.max(1, lineCount);
                }

                if (draw && y + lineHeight >= 0 && y <= maxY) {
                    if (piece.redacted()) {
                        int barHeight = Math.max(2,
                                Math.round(font.lineHeight * textScale * 0.72F));
                        graphics.fill(x, y + Math.max(0,
                                        (lineHeight - barHeight) / 2),
                                x + pieceWidth, y + Math.max(0,
                                        (lineHeight - barHeight) / 2) + barHeight,
                                0xFF101010);
                    } else {
                        drawScaled(graphics, piece.component(), x, y,
                                color, textScale);
                    }
                }
                x += pieceWidth;
                lineHasContent = true;
            }
            y += lineHeight;
            lineCount++;
            if (draw && y > maxY) break;
        }
        return Math.max(1, lineCount);
    }

    private static void drawScaled(GuiGraphics graphics, Component component,
                                   int x, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(Minecraft.getInstance().font, component,
                0, 0, color, false);
        graphics.pose().popPose();
    }

    private static List<Piece> toWords(List<Run> runs) {
        List<Piece> pieces = new ArrayList<>();
        for (Run run : runs) {
            String text = run.text();
            if (text.isEmpty()) continue;
            int cursor = 0;
            while (cursor < text.length()) {
                int nextSpace = text.indexOf(' ', cursor);
                String word;
                if (nextSpace < 0) {
                    word = text.substring(cursor);
                    cursor = text.length();
                } else {
                    word = text.substring(cursor, nextSpace + 1);
                    cursor = nextSpace + 1;
                }
                MutableComponent component = ScpFonts.roboto(word)
                        .withStyle(style -> style.withBold(run.bold())
                                .withItalic(run.italic()));
                pieces.add(new Piece(component, run.redacted()));
            }
        }
        return pieces;
    }

    private static List<Run> parse(String line) {
        List<Run> runs = new ArrayList<>();
        int cursor = 0;
        while (cursor < line.length()) {
            Marker marker = nextMarker(line, cursor);
            if (marker == null) {
                runs.add(new Run(line.substring(cursor), false, false, false));
                break;
            }
            if (marker.index() > cursor) {
                runs.add(new Run(line.substring(cursor, marker.index()),
                        false, false, false));
            }
            int end = line.indexOf(marker.close(), marker.index() + marker.open().length());
            if (end < 0) {
                runs.add(new Run(line.substring(marker.index()),
                        false, false, false));
                break;
            }
            String content = line.substring(marker.index() + marker.open().length(), end);
            runs.add(new Run(content, marker.bold(), marker.italic(), marker.redacted()));
            cursor = end + marker.close().length();
        }
        return runs;
    }

    private static Marker nextMarker(String line, int start) {
        Marker best = null;
        for (Marker candidate : List.of(
                new Marker(line.indexOf("[[", start), "[[", "]]", false, false, true),
                new Marker(line.indexOf("***", start), "***", "***", true, true, false),
                new Marker(line.indexOf("**", start), "**", "**", true, false, false),
                new Marker(line.indexOf("*", start), "*", "*", false, true, false))) {
            if (candidate.index() < 0) continue;
            if (best == null || candidate.index() < best.index()
                    || (candidate.index() == best.index()
                    && candidate.open().length() > best.open().length())) {
                best = candidate;
            }
        }
        return best;
    }

    private record Run(String text, boolean bold, boolean italic,
                       boolean redacted) {
    }

    private record Piece(Component component, boolean redacted) {
    }

    private record Marker(int index, String open, String close,
                          boolean bold, boolean italic, boolean redacted) {
    }
}
