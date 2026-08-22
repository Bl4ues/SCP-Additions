package com.bl4ues.scpclassifieddirective.client;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.network.FacilityDiagnosticsPacket;
import com.bl4ues.scpclassifieddirective.network.FacilityDiagnosticsResetPacket;

/** ARC-Site-48 SCiPNET facility diagnostic terminal. */
public final class FacilityDiagnosticsScreen extends Screen {
    private static final ResourceLocation TERMINAL_LOGO = new ResourceLocation(
            "scp_classified_directive", "textures/screens/terminallogo.png");
    private static final ResourceLocation TERMINAL_TEXT = new ResourceLocation(
            "scp_classified_directive", "scipnet_terminal");
    private static final ResourceLocation TERMINAL_CAPTION = new ResourceLocation(
            "scp_classified_directive", "scipnet_caption");

    private static final int BACKDROP = 0xFF0B151D;
    private static final int SHADOW = 0xFF04080B;
    private static final int BEZEL = 0xFF91989B;
    private static final int BEZEL_LIGHT = 0xFFD7D8D4;
    private static final int BEZEL_DARK = 0xFF465663;
    private static final int SCREEN = 0xFF122532;
    private static final int HEADER = 0xFF1C3443;
    private static final int PANEL = 0xE61A303E;
    private static final int PANEL_ALT = 0xB5213A49;
    private static final int STEEL_BLUE = 0xFF2A4353;
    private static final int FOUNDATION_RED = 0xFFB94145;
    private static final int OFF_WHITE = 0xFFE9EDEB;
    private static final int METAL_GRAY = 0xFFA6AFB2;
    private static final int SIGNAL_GOLD = 0xFFC9A21A;
    private static final int MUTED_BLUE = 0xFF70838D;
    private static final int DIM_BLUE = 0xFF415966;
    private static final int BUTTON = 0xFF233C4B;
    private static final int BUTTON_HOVER = 0xFF315365;
    private static final int RESET_HEIGHT = 24;
    private static final int MAX_FRAME_WIDTH = 560;
    private static final int MAX_FRAME_HEIGHT = 420;
    private static final int CACHE_PURGE_TOTAL_TICKS = 5 * 60 * 20;

    private final FacilityDiagnosticsPacket data;
    private final long snapshotReceivedAtMillis;
    private int resetX;
    private int resetY;
    private int resetWidth;
    private boolean resetRequested;
    private boolean clickVariant;

    private FacilityDiagnosticsScreen(FacilityDiagnosticsPacket data) {
        super(ScpFonts.montserrat("ARC-Site-48 SCiPNET Diagnostics"));
        this.data = data;
        this.snapshotReceivedAtMillis = Util.getMillis();
    }

    public static void open(FacilityDiagnosticsPacket data) {
        Minecraft.getInstance().setScreen(new FacilityDiagnosticsScreen(data));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BACKDROP);

        int availableWidth = Math.max(4, width - 18);
        int availableHeight = Math.max(3, height - 18);
        int frameWidth = Math.min(Math.min(MAX_FRAME_WIDTH, availableWidth),
                Math.min(MAX_FRAME_HEIGHT, availableHeight) * 4 / 3);
        frameWidth = Math.max(4, frameWidth - frameWidth % 4);
        int frameHeight = frameWidth * 3 / 4;
        int frameX = (width - frameWidth) / 2;
        int frameY = (height - frameHeight) / 2;

        graphics.fill(frameX + 6, frameY + 8, frameX + frameWidth + 6,
                frameY + frameHeight + 8, SHADOW);
        graphics.fill(frameX, frameY, frameX + frameWidth,
                frameY + frameHeight, BEZEL_DARK);
        graphics.fill(frameX + 3, frameY + 3, frameX + frameWidth - 3,
                frameY + frameHeight - 3, BEZEL);
        graphics.fill(frameX + 6, frameY + 6, frameX + frameWidth - 6,
                frameY + frameHeight - 6, BEZEL_LIGHT);

        int screenX = frameX + 11;
        int screenY = frameY + 11;
        int screenWidth = frameWidth - 22;
        int screenHeight = frameHeight - 22;
        graphics.fill(screenX, screenY, screenX + screenWidth,
                screenY + screenHeight, SCREEN);
        drawScanlines(graphics, screenX, screenY, screenWidth, screenHeight);
        drawGrid(graphics, screenX, screenY, screenWidth, screenHeight);

        renderHeader(graphics, screenX, screenY, screenWidth);

        int contentX = screenX + 14;
        int contentY = screenY + 62;
        int contentWidth = screenWidth - 28;
        renderDashboard(graphics, mouseX, mouseY, contentX, contentY,
                contentWidth, data.auxiliaryPowerOnline());

        renderFooter(graphics, screenX, screenY, screenWidth, screenHeight);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 52, HEADER);
        graphics.fill(x, y + 49, x + width, y + 52, FOUNDATION_RED);

        graphics.pose().pushPose();
        graphics.pose().translate(x + 11.0F, y + 5.0F, 0.0F);
        float logoScale = 42.0F / 128.0F;
        graphics.pose().scale(logoScale, logoScale, 1.0F);
        graphics.blit(TERMINAL_LOGO, 0, 0, 0, 0,
                128, 128, 128, 128);
        graphics.pose().popPose();

        drawHeading(graphics, "SCiPNET FACILITY STATUS",
                x + 62, y + 8, OFF_WHITE);
        drawHeading(graphics, "ARC-SITE-48 // INTERNAL SYSTEMS NODE",
                x + 62, y + 23, METAL_GRAY);
        drawBody(graphics, "NODE ARC48-SYS-01", x + 62, y + 38, MUTED_BLUE);

        Component build = body("BUILD 3.2.6");
        int buildWidth = font.width(build) + 12;
        int buildX = x + width - buildWidth - 10;
        graphics.fill(buildX, y + 8, buildX + buildWidth, y + 24,
                0xAA233C4B);
        border(graphics, buildX, y + 8, buildWidth, 16, DIM_BLUE);
        centered(graphics, build, buildX, y + 8, buildWidth, 16, METAL_GRAY);
    }

    private void renderDashboard(GuiGraphics graphics, int mouseX,
            int mouseY, int x, int y, int width, boolean powered) {
        int gap = 8;
        int leftWidth = Math.max(190, width * 40 / 100);
        int rightWidth = width - leftWidth - gap;
        int panelHeight = 110;

        renderContainmentPanel(graphics, x, y, leftWidth, panelHeight,
                powered);
        renderFacilityPanel(graphics, x + leftWidth + gap, y, rightWidth,
                panelHeight, powered);

        int controlY = y + panelHeight + 10;
        renderOperationsPanel(graphics, mouseX, mouseY, x, controlY, width,
                powered);

        int logY = controlY + 102;
        renderSystemLog(graphics, x, logY, width, powered);
    }

    private void renderContainmentPanel(GuiGraphics graphics, int x, int y,
            int width, int height, boolean powered) {
        panel(graphics, x, y, width, height, "CONTAINMENT INDEX", "CI-01");
        boolean reindexing = powered && cooldownRemainingTicks() > 0;
        int reindex = reindexProgressPercent();
        String signatures = !powered ? "UNAVAILABLE"
                : reindexing ? "READING " + reindex + "%"
                : twoDigits(data.uncontainedScps());
        String integrity = !powered ? "UNAVAILABLE"
                : reindexing ? "REASSESSING"
                : data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        int threatColor = !powered ? METAL_GRAY
                : data.uncontainedScps() == 0 ? SIGNAL_GOLD : FOUNDATION_RED;

        panelRow(graphics, "UNCONTAINED SIGNATURES", signatures,
                x + 7, y + 25, width - 14, threatColor);
        panelRow(graphics, "INTEGRITY STATE", integrity,
                x + 7, y + 52, width - 14, threatColor);
        drawBody(graphics, !powered ? "REGISTRY LINK // SUSPENDED"
                        : reindexing ? "REGISTRY REINDEX // " + reindex + "%"
                        : "SCOPE // SITE-WIDE REGISTRY",
                x + 9, y + height - 15,
                reindexing ? SIGNAL_GOLD : MUTED_BLUE);
    }

    private void renderFacilityPanel(GuiGraphics graphics, int x, int y,
            int width, int height, boolean powered) {
        panel(graphics, x, y, width, height, "FACILITY TELEMETRY", "FT-02");
        boolean reindexing = powered && cooldownRemainingTicks() > 0;
        int reindex = reindexProgressPercent();
        String teslaGrid = !powered ? "UNAVAILABLE"
                : reindexing ? "READING " + reindex + "%"
                : twoDigits(data.activeTeslaGates()) + " / "
                        + twoDigits(data.registeredTeslaGates()) + " ONLINE";
        String override = !powered ? "UNAVAILABLE"
                : reindexing ? "VERIFYING"
                : data.teslaOverride() ? "ACTIVE" : "INACTIVE";
        String doors = !powered ? "UNAVAILABLE"
                : reindexing ? "INDEXING " + reindex + "%"
                : twoDigits(data.connectedDoors());

        panelRow(graphics, "TESLA GATE SYSTEMS", teslaGrid,
                x + 7, y + 25, width - 14,
                powered ? OFF_WHITE : METAL_GRAY);
        panelRow(graphics, "TESLA GATE SYSTEMS MANUAL OVERRIDE", override,
                x + 7, y + 48, width - 14,
                powered && data.teslaOverride() ? FOUNDATION_RED : METAL_GRAY);
        panelRow(graphics, "DOOR SYSTEM ENDPOINTS", doors,
                x + 7, y + 71, width - 14,
                powered ? OFF_WHITE : METAL_GRAY);
        drawBody(graphics, !powered ? "ENDPOINT BUS // SUSPENDED"
                        : reindexing ? "TELEMETRY REBUILD // " + reindex + "%"
                        : "LINK // ENDPOINT REGISTRY CURRENT",
                x + 9, y + height - 15,
                reindexing ? SIGNAL_GOLD : MUTED_BLUE);
    }

    private void renderOperationsPanel(GuiGraphics graphics, int mouseX,
            int mouseY, int x, int y, int width, boolean powered) {
        int height = 92;
        graphics.fill(x, y, x + width, y + height, PANEL);
        border(graphics, x, y, width, height, DIM_BLUE);
        graphics.fill(x, y, x + 4, y + height, FOUNDATION_RED);

        drawHeading(graphics, "SCiPNET OPERATIONS", x + 10, y + 7,
                OFF_WHITE);

        int cooldown = cooldownRemainingTicks();
        String powerState = powered ? "ONLINE" : "OFFLINE";
        int powerColor = powered ? SIGNAL_GOLD : FOUNDATION_RED;
        String cacheState = !powered ? "UNAVAILABLE"
                : cooldown > 0 ? "REINDEX " + formatCooldown(cooldown)
                : resetRequested ? "PURGE REQUESTED"
                : data.unusualNetworkActivity() ? "REVIEW ADVISED" : "READY";
        int cacheColor = !powered ? METAL_GRAY
                : cooldown > 0 || data.unusualNetworkActivity()
                        ? SIGNAL_GOLD : OFF_WHITE;

        drawBody(graphics, "AUXILIARY BUS", x + 10, y + 24, METAL_GRAY);
        drawBody(graphics, powerState, x + 92, y + 24, powerColor);
        drawBody(graphics, "SESSION CACHE", x + 10, y + 38, METAL_GRAY);
        drawBody(graphics, cacheState, x + 92, y + 38, cacheColor);

        int buttonWidth = Math.max(136, Math.min(190, width * 38 / 100));
        resetX = x + width - buttonWidth - 8;
        resetY = y + 18;
        resetWidth = buttonWidth;
        boolean enabled = powered && cooldown <= 0 && !resetRequested;
        boolean hovered = enabled && inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT);
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, hovered ? BUTTON_HOVER : BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                hovered ? FOUNDATION_RED : DIM_BLUE);

        String label = !powered ? "PURGE UNAVAILABLE"
                : cooldown > 0 ? "CACHE LOCKOUT ACTIVE"
                : resetRequested ? "PURGE REQUESTED"
                : "PURGE SESSION CACHE";
        centeredBody(graphics, label, resetX, resetY, resetWidth,
                RESET_HEIGHT, enabled ? OFF_WHITE : METAL_GRAY);

        String warningLineOne = cooldown > 0
                ? "REINDEX ACTIVE // TELEMETRY READING SLOWLY"
                : "WARNING // REMOTE TECHNICIAN SESSIONS END";
        String warningLineTwo = cooldown > 0
                ? "REMOTE TOKENS INVALID // EST. " + formatCooldown(cooldown)
                : "INDEX REBUILD REQUIRES APPROX. 05:00";
        centeredCaption(graphics, warningLineOne, resetX, y + 47,
                resetWidth, 9, cooldown > 0 ? SIGNAL_GOLD : FOUNDATION_RED);
        centeredCaption(graphics, warningLineTwo, resetX, y + 58,
                resetWidth, 9, MUTED_BLUE);
    }

    private void renderSystemLog(GuiGraphics graphics, int x, int y,
            int width, boolean powered) {
        int height = 88;
        graphics.fill(x, y, x + width, y + height, 0xB5142B38);
        border(graphics, x, y, width, height, DIM_BLUE);
        drawHeading(graphics, "SYSTEM LOG", x + 9, y + 7, METAL_GRAY);
        rightAligned(graphics, body("BUFFER 03"), x + width - 9,
                y + 7, MUTED_BLUE);
        graphics.fill(x + 8, y + 20, x + width - 8, y + 21, DIM_BLUE);

        int cooldown = cooldownRemainingTicks();
        int reindex = reindexProgressPercent();
        if (!powered) {
            logLine(graphics, "PWR/01", "AUXILIARY POWER BUS OFFLINE",
                    x + 9, y + 28, FOUNDATION_RED);
            logLine(graphics, "NET/12", "LIVE FACILITY TELEMETRY SUSPENDED",
                    x + 9, y + 43, METAL_GRAY);
            logLine(graphics, "SEC/07", "SESSION CACHE CONTROL UNAVAILABLE",
                    x + 9, y + 58, METAL_GRAY);
        } else if (cooldown > 0) {
            logLine(graphics, "IDX/03", "FACILITY INDEX REBUILD IN PROGRESS",
                    x + 9, y + 28, SIGNAL_GOLD);
            logLine(graphics, "TEL/06",
                    "TELEMETRY CHANNELS READING // " + reindex + "%",
                    x + 9, y + 43, METAL_GRAY);
            logLine(graphics, "SEC/07",
                    "REMOTE TECHNICIAN TOKENS INVALIDATED",
                    x + 9, y + 58, METAL_GRAY);
        } else if (data.unusualNetworkActivity()) {
            logLine(graphics, "SYS/00", "FACILITY SNAPSHOT ACQUIRED",
                    x + 9, y + 28, OFF_WHITE);
            logLine(graphics, "NET/47", "UNUSUAL NETWORK ACTIVITY DETECTED",
                    x + 9, y + 43, SIGNAL_GOLD);
            logLine(graphics, "SEC/12",
                    "CONTACT SITE NETWORK INTEGRITY FOR REVIEW",
                    x + 9, y + 58, METAL_GRAY);
        } else {
            logLine(graphics, "SYS/00", "FACILITY SNAPSHOT ACQUIRED",
                    x + 9, y + 28, OFF_WHITE);
            logLine(graphics, "NET/12", "ENDPOINT REGISTRY SYNCHRONIZED",
                    x + 9, y + 43, METAL_GRAY);
            logLine(graphics, "SEC/07", "REMOTE SESSION CACHE READY",
                    x + 9, y + 58, METAL_GRAY);
        }

        Component prompt = body("ARC48:SCIPNET>");
        graphics.drawString(font, prompt, x + 9, y + 74, MUTED_BLUE, false);
        if ((Util.getMillis() / 500L) % 2L == 0L) {
            int cursorX = x + 13 + font.width(prompt);
            graphics.fill(cursorX, y + 75, cursorX + 5, y + 83,
                    METAL_GRAY);
        }
    }

    private void renderFooter(GuiGraphics graphics, int x, int y, int width,
            int height) {
        drawBody(graphics, "FOUNDATION INTERNAL // SESSION ACTIVE",
                x + 14, y + height - 13, MUTED_BLUE);
        Component close = body("ESC // CLOSE");
        rightAligned(graphics, close, x + width - 14, y + height - 13,
                MUTED_BLUE);
    }

    private void panel(GuiGraphics graphics, int x, int y, int width,
            int height, String title, String code) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        border(graphics, x, y, width, height, DIM_BLUE);
        graphics.fill(x, y, x + width, y + 18, STEEL_BLUE);
        graphics.fill(x, y, x + 4, y + 18, FOUNDATION_RED);
        drawHeading(graphics, title, x + 9, y + 4, OFF_WHITE);
        Component codeText = body(code);
        rightAligned(graphics, codeText, x + width - 8, y + 5, MUTED_BLUE);
    }

    private void panelRow(GuiGraphics graphics, String label, String value,
            int x, int y, int width, int valueColor) {
        graphics.fill(x, y, x + width, y + 21, PANEL_ALT);
        graphics.fill(x, y + 20, x + width, y + 21, DIM_BLUE);
        drawBody(graphics, label, x + 5, y + 6, METAL_GRAY);

        Component valueText = body(value);
        int valueWidth = Math.max(34, font.width(valueText) + 12);
        int valueX = x + width - valueWidth - 4;
        graphics.fill(valueX, y + 3, x + width - 4, y + 18, 0x88122532);
        centered(graphics, valueText, valueX, y + 3, valueWidth, 15,
                valueColor);
    }

    private void logLine(GuiGraphics graphics, String channel, String text,
            int x, int y, int color) {
        drawBody(graphics, channel, x, y, MUTED_BLUE);
        drawBody(graphics, text, x + 44, y, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && data.auxiliaryPowerOnline()) {
            playRandomClick();
        }
        if (button == 0 && data.auxiliaryPowerOnline() && !resetRequested
                && cooldownRemainingTicks() <= 0
                && inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT)) {
            resetRequested = true;
            playSelect();
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                    new FacilityDiagnosticsResetPacket(data.terminalPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void playRandomClick() {
        clickVariant = !clickVariant;
        String id = clickVariant ? "click_1" : "click_2";
        float pitch = 0.90F + (float) (Math.random() * 0.20D);
        playBlockSound(id, pitch, 0.2F);
    }

    private void playSelect() {
        playBlockSound("select", 1.0F, 1.0F);
    }

    private void playBlockSound(String soundId, float pitch, float volume) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(
                new ResourceLocation("scp_classified_directive", soundId));
        if (sound == null) return;
        BlockPos pos = data.terminalPos();
        level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, sound, SoundSource.BLOCKS,
                volume, pitch, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rightAligned(GuiGraphics graphics, Component text,
            int right, int y, int color) {
        graphics.drawString(font, text, right - font.width(text), y,
                color, false);
    }

    private void centered(GuiGraphics graphics, Component text, int x, int y,
            int width, int height, int color) {
        graphics.drawString(font, text,
                x + Math.max(0, (width - font.width(text)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2),
                color, false);
    }

    private void centeredBody(GuiGraphics graphics, String text, int x,
            int y, int width, int height, int color) {
        Component component = body(text);
        graphics.drawString(font, component,
                x + Math.max(0, (width - font.width(component)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2),
                color, false);
    }

    private void drawBody(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, body(text), x, y, color, false);
    }

    private void centeredCaption(GuiGraphics graphics, String text, int x,
            int y, int width, int height, int color) {
        Component component = caption(text);
        graphics.drawString(font, component,
                x + Math.max(0, (width - font.width(component)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2),
                color, false);
    }

    private void drawHeading(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, heading(text), x, y, color, false);
    }

    private static Component body(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(TERMINAL_TEXT));
    }

    private static Component caption(String text) {
        return Component.literal(text == null ? "" : text)
                .withStyle(style -> style.withFont(TERMINAL_CAPTION));
    }

    private static Component heading(String text) {
        return ScpFonts.montserrat(text);
    }

    private int reindexProgressPercent() {
        int remaining = cooldownRemainingTicks();
        if (remaining <= 0) return 100;
        double completed = 1.0D - Math.min(1.0D,
                remaining / (double) CACHE_PURGE_TOTAL_TICKS);
        return Math.max(0, Math.min(99,
                (int) Math.floor(completed * 100.0D)));
    }

    private int cooldownRemainingTicks() {
        long elapsedMillis = Math.max(0L,
                Util.getMillis() - snapshotReceivedAtMillis);
        int elapsedTicks = (int) Math.min(Integer.MAX_VALUE,
                elapsedMillis / 50L);
        return Math.max(0, data.cachePurgeCooldownTicks() - elapsedTicks);
    }

    private static String formatCooldown(int ticks) {
        int totalSeconds = Math.max(0, (ticks + 19) / 20);
        return String.format("%02d:%02d", totalSeconds / 60,
                totalSeconds % 60);
    }

    private static String twoDigits(int value) {
        return String.format("%02d", Math.max(0, value));
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static void drawScanlines(GuiGraphics graphics, int x, int y,
            int width, int height) {
        for (int lineY = y + 1; lineY < y + height; lineY += 4) {
            graphics.fill(x, lineY, x + width, lineY + 1, 0x12000000);
        }
    }

    private static void drawGrid(GuiGraphics graphics, int x, int y,
            int width, int height) {
        for (int lineX = x + 32; lineX < x + width; lineX += 32) {
            graphics.fill(lineX, y, lineX + 1, y + height, 0x08000000);
        }
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
