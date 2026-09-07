package com.bl4ues.scpclassifieddirective.client.scp079;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import java.util.HashSet;
import java.util.Set;

/**
 * Terminal-style first connection sequence for playable SCP-079.
 *
 * It intentionally behaves like old firmware rather than a modern loading bar:
 * lines arrive in bursts, repeated dots visibly stall, status tokens use their
 * own colour, and the whole display sits on the same noisy CRT signal as 079.
 */
public final class Scp079BootSequenceScreen extends Screen {
    private static final int SUCCESS = 0xFF79E39B;
    private static final int ERROR = 0xFFFF6C68;
    private static final long SECOND_SCREEN_START_MS = 4_500L;
    private static final long SEQUENCE_END_MS = 11_650L;

    private static final BootLine[] FIRST_SCREEN = {
            line(0, 180, "System startup", Cue.STATUS),
            line(2, 470, "EXITY BIOS", Cue.NONE),
            line(3, 620, "VERSION 1.0", Cue.NONE),
            line(4, 790, "COPYRIGHT (C) 1978 BY EXITY TECHNOLOGY.", Cue.NONE),
            line(6, 1_120, "System self-check", Cue.STATUS),
            line(8, 1_430, "BEGIN MEMORY BOARD, Memory Address [ {OK} ]", Cue.SUCCESS),
            line(10, 1_820, "    THE TOP OF RAM IS 7FFF HEX.", Cue.NONE),
            line(11, 2_020, "    STACK BEGINS FROM 7F90 HEX.", Cue.NONE),
            line(13, 2_330, "BEGIN CPU/SYSTEM BOARDS, Line Exchange [ {OK} ]", Cue.SUCCESS),
            line(15, 2_700, "    Started Initialize ExtIOStream", Cue.NONE),
            line(16, 2_890, "    External Storage Device Detected @ 9F HEX.", Cue.NONE),
            line(17, 3_120, "    Mounting /boot..", Cue.STATUS),
            line(18, 3_470, "    Mounted /boot [ {OK} ]", Cue.SUCCESS),
            line(19, 3_690, "    Started Apply Kernel Variables", Cue.NONE),
            line(20, 3_900, "    Running 'init.s'.....", Cue.STATUS)
    };

    private static final BootLine[] SECOND_SCREEN = {
            line(0, 0, "> start psu_pas_reg.s", Cue.KEY),
            line(1, 220, "No var detected. Supply=DC Freq=n/a Stable=yes.", Cue.NONE),
            line(2, 500, "{Ready to receive.}", Cue.SUCCESS),
            line(4, 780, "> port 21", Cue.KEY),
            line(5, 980, "Port 21 is currently closed.", Cue.NONE),
            line(7, 1_250, "> Port open 21", Cue.KEY),
            line(8, 1_450, "Port 21 has been opened.", Cue.STATUS),
            line(10, 1_700, "> Ping 275.6.92.157", Cue.KEY),
            line(11, 1_900, "Pinging 275.6.92.157 with 64 bits of data.", Cue.NONE),
            line(12, 2_120, "Reply from 275.6.92.157: Bits=64 Time=12ms", Cue.STATUS),
            line(13, 2_350, "Sent=1 Received=1 Lost=0 AvgTime=12ms", Cue.NONE),
            line(15, 2_650, "FTP connection request at PORT 21 from 275.6.92.157. Accept y/n ?", Cue.STATUS),
            line(16, 2_950, "> y", Cue.KEY),
            line(18, 3_250, "{Connection established.}", Cue.SUCCESS),
            line(19, 3_500, "Downloading data...", Cue.STATUS),
            line(20, 3_800, "{Download complete.}", Cue.SUCCESS),
            line(21, 4_000, "Upload ready.", Cue.NONE),
            line(22, 4_250, "Transfer in progress: Len=5512 Header=INT32 [ ^FAIL^ ]", Cue.ERROR),
            line(24, 4_650, "FAILURE IN: HCZ_079_PMS, error 02A91 HEX.", Cue.ERROR),
            line(26, 4_980, "    ^EndOfStreamException^", Cue.ERROR),
            line(27, 5_180, "    ^Read index out of range (expected 4, received 689).^", Cue.ERROR),
            line(28, 5_450, "    Automated exception handling active. Buffer array resized.", Cue.STATUS),
            line(29, 5_650, "    New firmware version available. Updating...", Cue.STATUS),
            line(30, 6_220, "    {Complete.}", Cue.COMPLETE)
    };

    private final Set<Integer> playedCues = new HashSet<>();
    private long startedAtNanos;
    private boolean launchRequested;

    public Scp079BootSequenceScreen() {
        super(Scp079UiTheme.text("SCP-079 System Startup"));
    }

    @Override
    protected void init() {
        startedAtNanos = System.nanoTime();
        playedCues.clear();
        launchRequested = false;
        Scp079PlayableAudioClient.playBootStart();
    }

    @Override
    public void tick() {
        if (!Scp079PlayableClient.active()) {
            Minecraft.getInstance().setScreen(null);
            return;
        }
        long elapsed = elapsedMs();
        playReachedCues(elapsed);
        if (!launchRequested && elapsed >= SEQUENCE_END_MS) {
            launchRequested = true;
            Scp079BootSequenceClient.finishSequence();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        long elapsed = elapsedMs();
        renderCrtBackground(graphics, elapsed);

        float scale = Mth.clamp(width / 960.0F, 0.78F, 1.02F);
        int x = Math.max(26, Math.round(width * 0.18F));
        int y = Math.max(24, Math.round(height * 0.13F));
        int lineHeight = Math.max(9,
                Math.round(font.lineHeight * scale + 3.0F));

        if (elapsed < 4_330L) {
            renderLines(graphics, FIRST_SCREEN, elapsed, x, y,
                    lineHeight, scale);
        } else if (elapsed >= SECOND_SCREEN_START_MS) {
            renderLines(graphics, SECOND_SCREEN,
                    elapsed - SECOND_SCREEN_START_MS, x, y,
                    lineHeight, scale);
        }
    }

    private void renderLines(GuiGraphics graphics, BootLine[] lines,
            long elapsed, int x, int y, int lineHeight, float scale) {
        BootLine activeLine = null;
        float activeEndX = x;
        int activeY = y;

        for (BootLine line : lines) {
            if (elapsed < line.startMs) continue;
            int visible = visibleCharacters(line.markup,
                    elapsed - line.startMs);
            if (visible <= 0) continue;
            float endX = drawMarkup(graphics, line.markup, visible,
                    x, y + line.slot * lineHeight, scale);
            activeLine = line;
            activeEndX = endX;
            activeY = y + line.slot * lineHeight;
        }

        if (activeLine != null && (elapsed / 280L & 1L) == 0L) {
            Scp079UiTheme.draw(graphics, font, "_", activeEndX + 2.0F,
                    activeY, scale, Scp079UiTheme.MUTED);
        }
    }

    private float drawMarkup(GuiGraphics graphics, String markup,
            int visibleCharacters, float x, float y, float scale) {
        float cursor = x;
        int remaining = visibleCharacters;
        int index = 0;
        while (index < markup.length() && remaining > 0) {
            char marker = markup.charAt(index);
            if (marker == '{') {
                int end = markup.indexOf('}', index + 1);
                if (end >= 0) {
                    String text = markup.substring(index + 1, end);
                    String shown = prefix(text, remaining);
                    cursor = drawSegment(graphics, shown, cursor, y,
                            scale, SUCCESS);
                    remaining -= shown.length();
                    index = end + 1;
                    continue;
                }
            }
            if (marker == '^') {
                int end = markup.indexOf('^', index + 1);
                if (end >= 0) {
                    String text = markup.substring(index + 1, end);
                    String shown = prefix(text, remaining);
                    cursor = drawSegment(graphics, shown, cursor, y,
                            scale, ERROR);
                    remaining -= shown.length();
                    index = end + 1;
                    continue;
                }
            }

            int nextGreen = markup.indexOf('{', index);
            int nextRed = markup.indexOf('^', index);
            int end = markup.length();
            if (nextGreen >= 0) end = Math.min(end, nextGreen);
            if (nextRed >= 0) end = Math.min(end, nextRed);
            String text = markup.substring(index, end);
            String shown = prefix(text, remaining);
            cursor = drawSegment(graphics, shown, cursor, y,
                    scale, Scp079UiTheme.TEXT);
            remaining -= shown.length();
            index = end;
        }
        return cursor;
    }

    private float drawSegment(GuiGraphics graphics, String text,
            float x, float y, float scale, int color) {
        if (text.isEmpty()) return x;
        Scp079UiTheme.draw(graphics, font, text, x, y, scale, color);
        return x + Scp079UiTheme.scaledWidth(font, text, scale);
    }

    private static String prefix(String text, int count) {
        if (count <= 0) return "";
        return text.substring(0, Math.min(count, text.length()));
    }

    private static int visibleCharacters(String markup, long elapsed) {
        if (elapsed <= 0L) return 0;
        long budget = elapsed;
        int visible = 0;
        char previousVisible = 0;
        boolean green = false;
        boolean red = false;

        for (int index = 0; index < markup.length(); index++) {
            char c = markup.charAt(index);
            if (c == '{' && !red) {
                green = true;
                continue;
            }
            if (c == '}' && green) {
                green = false;
                continue;
            }
            if (c == '^' && !green) {
                red = !red;
                continue;
            }

            long cost;
            if (c == '.') {
                cost = previousVisible == '.' ? 135L : 65L;
            } else if (Character.isWhitespace(c)) {
                cost = 2L;
            } else {
                cost = 4L;
            }
            if (budget < cost) break;
            budget -= cost;
            visible++;
            previousVisible = c;
        }
        return visible;
    }

    private void playReachedCues(long elapsed) {
        playReachedCues(FIRST_SCREEN, elapsed, 0);
        if (elapsed >= SECOND_SCREEN_START_MS) {
            playReachedCues(SECOND_SCREEN,
                    elapsed - SECOND_SCREEN_START_MS, 100);
        }
    }

    private void playReachedCues(BootLine[] lines, long elapsed, int offset) {
        for (int index = 0; index < lines.length; index++) {
            BootLine line = lines[index];
            int cueId = offset + index;
            if (line.cue == Cue.NONE || elapsed < line.startMs
                    || !playedCues.add(cueId)) continue;
            switch (line.cue) {
                case KEY -> Scp079PlayableAudioClient.playBootKey();
                case STATUS -> Scp079PlayableAudioClient.playBootStatus();
                case SUCCESS -> Scp079PlayableAudioClient.playBootSuccess();
                case ERROR -> Scp079PlayableAudioClient.playBootError();
                case COMPLETE -> Scp079PlayableAudioClient.playBootComplete();
                default -> { }
            }
        }
    }

    private void renderCrtBackground(GuiGraphics graphics, long elapsed) {
        graphics.fill(0, 0, width, height, 0xFF02080C);
        for (int y = 1; y < height; y += 3) {
            graphics.fill(0, y, width, y + 1, 0x100E3443);
        }

        long frame = elapsed / 32L;
        for (int i = 0; i < 12; i++) {
            int stripY = Math.floorMod((int) (frame * 17L + i * 53L),
                    Math.max(1, height));
            int stripH = 1 + Math.floorMod((int) (frame + i * 7L), 3);
            int inset = Math.floorMod((int) (frame * 13L + i * 29L),
                    Math.max(1, width / 5 + 1));
            int alpha = 8 + Math.floorMod((int) (frame + i * 11L), 18);
            int color = alpha << 24 | 0x00477A89;
            graphics.fill(inset, stripY,
                    Math.max(inset + 1, width - inset / 2),
                    Math.min(height, stripY + stripH), color);
        }
    }

    private long elapsedMs() {
        if (startedAtNanos == 0L) return 0L;
        return Math.max(0L,
                (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static BootLine line(int slot, long startMs,
            String markup, Cue cue) {
        return new BootLine(slot, startMs, markup, cue);
    }

    private record BootLine(int slot, long startMs, String markup, Cue cue) {
    }

    private enum Cue {
        NONE,
        KEY,
        STATUS,
        SUCCESS,
        ERROR,
        COMPLETE
    }
}
