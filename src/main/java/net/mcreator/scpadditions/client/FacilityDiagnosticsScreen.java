package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.FacilityDiagnosticsPacket;
import net.mcreator.scpadditions.network.FacilityDiagnosticsResetPacket;

/** ARC-Site-48 SCiPNET facility diagnostic terminal. */
public final class FacilityDiagnosticsScreen extends Screen {
    private static final ResourceLocation TERMINAL_LOGO = new ResourceLocation(
            "scp_additions", "textures/screens/terminallogo.png");

    private static final int BACKDROP = 0xFF0C1720;
    private static final int SHADOW = 0xFF05090C;
    private static final int BEZEL = 0xFF9A9A9A;
    private static final int BEZEL_LIGHT = 0xFFD8D8D4;
    private static final int BEZEL_DARK = 0xFF485864;
    private static final int SCREEN = 0xFF142735;
    private static final int HEADER = 0xFF1D3443;
    private static final int STEEL_BLUE = 0xFF283C4A;
    private static final int FOUNDATION_RED = 0xFFB53F41;
    private static final int OFF_WHITE = 0xFFEEEEEE;
    private static final int METAL_GRAY = 0xFFA1A0A2;
    private static final int SIGNAL_GOLD = 0xFFC49916;
    private static final int MUTED_BLUE = 0xFF72828D;
    private static final int BUTTON = 0xFF213746;
    private static final int BUTTON_HOVER = 0xFF304D5E;
    private static final int RESET_HEIGHT = 20;
    private static final int MAX_FRAME_WIDTH = 560;
    private static final int MAX_FRAME_HEIGHT = 420;

    private final FacilityDiagnosticsPacket data;
    private final long snapshotReceivedAtMillis;
    private int resetX;
    private int resetY;
    private int resetWidth;
    private boolean resetRequested;

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

        renderHeader(graphics, screenX, screenY, screenWidth);

        int contentX = screenX + 12;
        int contentY = screenY + 70;
        int contentWidth = screenWidth - 24;
        if (data.auxiliaryPowerOnline()) {
            renderOnline(graphics, mouseX, mouseY, contentX, contentY,
                    contentWidth);
        } else {
            renderOffline(graphics, contentX, contentY, contentWidth);
        }

        drawBody(graphics, "NODE ARC48-SYS-01 // ESC TO TERMINATE SESSION",
                contentX, screenY + screenHeight - 13, MUTED_BLUE);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 60, HEADER);
        graphics.fill(x, y + 57, x + width, y + 60, FOUNDATION_RED);

        graphics.pose().pushPose();
        graphics.pose().translate(x + 10.0F, y + 6.0F, 0.0F);
        float logoScale = 48.0F / 128.0F;
        graphics.pose().scale(logoScale, logoScale, 1.0F);
        graphics.blit(TERMINAL_LOGO, 0, 0, 0, 0,
                128, 128, 128, 128);
        graphics.pose().popPose();

        drawHeading(graphics, "SCiPNET TERMINAL v3.2.6",
                x + 68, y + 9, OFF_WHITE);
        drawHeading(graphics, "ARC-SITE-48 // INTERNAL SYSTEMS NODE",
                x + 68, y + 23, METAL_GRAY);
        drawHeading(graphics, "ARMED RESEARCH & CONTAINMENT FACILITY",
                x + 68, y + 37, MUTED_BLUE);

        Component site = heading("ARC-SITE-48");
        int badgeWidth = font.width(site) + 14;
        int badgeX = x + width - badgeWidth - 10;
        graphics.fill(badgeX, y + 8, badgeX + badgeWidth, y + 24,
                FOUNDATION_RED);
        centered(graphics, site, badgeX, y + 8, badgeWidth, 16, OFF_WHITE);
    }

    private void renderOnline(GuiGraphics graphics, int mouseX, int mouseY,
            int x, int y, int width) {
        sectionHeader(graphics, "CONTAINMENT INDEX", x, y, width);
        metric(graphics, "UNCONTAINED SCP SIGNATURES",
                twoDigits(data.uncontainedScps()), x, y + 20, width,
                data.uncontainedScps() == 0 ? SIGNAL_GOLD : FOUNDATION_RED);
        String integrity = data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        metric(graphics, "CONTAINMENT INTEGRITY", integrity,
                x, y + 34, width,
                data.uncontainedScps() == 0 ? SIGNAL_GOLD : FOUNDATION_RED);

        sectionHeader(graphics, "FACILITY TELEMETRY", x, y + 54, width);
        metric(graphics, "TESLA GATE SYSTEMS ACTIVE",
                twoDigits(data.activeTeslaGates()), x, y + 74, width,
                OFF_WHITE);
        metric(graphics, "TESLA GATE SYSTEMS REGISTERED",
                twoDigits(data.registeredTeslaGates()), x, y + 88, width,
                METAL_GRAY);
        metric(graphics, "TESLA GATE SYSTEMS MANUAL OVERRIDE",
                data.teslaOverride() ? "ACTIVE" : "INACTIVE",
                x, y + 102, width,
                data.teslaOverride() ? FOUNDATION_RED : METAL_GRAY);
        metric(graphics, "DOOR SYSTEM ENDPOINTS",
                twoDigits(data.connectedDoors()), x, y + 116, width,
                OFF_WHITE);

        statusStrip(graphics, "AUXILIARY POWER BUS", "ONLINE",
                x, y + 138, width, SIGNAL_GOLD);
        renderPurgeButton(graphics, mouseX, mouseY, x, y + 166, width, true);
    }

    private void renderOffline(GuiGraphics graphics, int x, int y, int width) {
        sectionHeader(graphics, "CONTAINMENT INDEX", x, y, width);
        metric(graphics, "UNCONTAINED SCP SIGNATURES", "UNAVAILABLE",
                x, y + 20, width, METAL_GRAY);
        metric(graphics, "CONTAINMENT INTEGRITY", "UNAVAILABLE",
                x, y + 34, width, METAL_GRAY);

        sectionHeader(graphics, "FACILITY TELEMETRY", x, y + 54, width);
        metric(graphics, "TESLA GATE SYSTEMS ACTIVE", "UNAVAILABLE",
                x, y + 74, width, METAL_GRAY);
        metric(graphics, "TESLA GATE SYSTEMS REGISTERED", "UNAVAILABLE",
                x, y + 88, width, METAL_GRAY);
        metric(graphics, "TESLA GATE SYSTEMS MANUAL OVERRIDE", "UNAVAILABLE",
                x, y + 102, width, METAL_GRAY);
        metric(graphics, "DOOR SYSTEM ENDPOINTS", "UNAVAILABLE",
                x, y + 116, width, METAL_GRAY);

        statusStrip(graphics, "AUXILIARY POWER BUS", "OFFLINE",
                x, y + 138, width, FOUNDATION_RED);
        renderPurgeButton(graphics, 0, 0, x, y + 166, width, false);
    }

    private void renderPurgeButton(GuiGraphics graphics, int mouseX,
            int mouseY, int x, int y, int width, boolean powered) {
        resetX = x;
        resetY = y;
        resetWidth = width;
        int cooldown = cooldownRemainingTicks();
        boolean enabled = powered && cooldown <= 0 && !resetRequested;
        boolean hovered = enabled && inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT);
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, hovered ? BUTTON_HOVER : BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                hovered ? FOUNDATION_RED : BEZEL_DARK);

        String label;
        int color;
        if (!powered) {
            label = "REMOTE SESSION CACHE PURGE UNAVAILABLE // AUX POWER OFFLINE";
            color = METAL_GRAY;
        } else if (resetRequested) {
            label = "REMOTE SESSION CACHE PURGE REQUESTED";
            color = METAL_GRAY;
        } else if (cooldown > 0) {
            label = "REMOTE SESSION CACHE LOCKOUT // "
                    + formatCooldown(cooldown);
            color = SIGNAL_GOLD;
        } else {
            label = "PURGE REMOTE SESSION CACHE";
            color = OFF_WHITE;
        }
        centered(graphics, body(label), resetX, resetY, resetWidth,
                RESET_HEIGHT, color);
    }

    private void sectionHeader(GuiGraphics graphics, String title,
            int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 15, STEEL_BLUE);
        graphics.fill(x, y, x + 4, y + 15, FOUNDATION_RED);
        drawHeading(graphics, title, x + 9, y + 3, OFF_WHITE);
    }

    private void statusStrip(GuiGraphics graphics, String label, String value,
            int x, int y, int width, int valueColor) {
        graphics.fill(x, y, x + width, y + 19, HEADER);
        border(graphics, x, y, width, 19, BEZEL_DARK);
        drawBody(graphics, label, x + 7, y + 5, METAL_GRAY);
        rightAligned(graphics, body(value), x + width - 7, y + 5,
                valueColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && data.auxiliaryPowerOnline() && !resetRequested
                && cooldownRemainingTicks() <= 0
                && inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT)) {
            resetRequested = true;
            ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                    new FacilityDiagnosticsResetPacket(data.terminalPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void metric(GuiGraphics graphics, String label, Object value,
            int x, int y, int width, int valueColor) {
        Component left = body(label + " ");
        Component right = body(String.valueOf(value));
        Component dot = body(".");
        int available = width - font.width(left) - font.width(right) - 8;
        int dots = Math.max(2, available / Math.max(1, font.width(dot)));
        drawBody(graphics, label + " " + ".".repeat(dots),
                x, y, METAL_GRAY);
        rightAligned(graphics, right, x + width, y, valueColor);
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

    private void drawBody(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, body(text), x, y, color, false);
    }

    private void drawHeading(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, heading(text), x, y, color, false);
    }

    private static Component body(String text) {
        return ScpFonts.anonymousPro(text);
    }

    private static Component heading(String text) {
        return ScpFonts.montserrat(text);
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

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
