from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

screen_path = ROOT / "src/main/java/net/mcreator/scpadditions/client/FacilityDiagnosticsScreen.java"
roamer_path = ROOT / "src/main/java/net/mcreator/scpadditions/roamer/RoamerManager.java"
access_path = ROOT / "src/main/java/net/mcreator/scpadditions/facility/Scp079FacilityAccessManager.java"
changelog_path = ROOT / "CHANGELOG.md"

screen_path.write_text(r'''package net.mcreator.scpadditions.client;

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

    private final FacilityDiagnosticsPacket data;
    private int resetX;
    private int resetY;
    private int resetWidth;
    private boolean resetRequested;

    private FacilityDiagnosticsScreen(FacilityDiagnosticsPacket data) {
        super(Component.literal("ARC-Site-48 SCiPNET Diagnostics"));
        this.data = data;
    }

    public static void open(FacilityDiagnosticsPacket data) {
        Minecraft.getInstance().setScreen(new FacilityDiagnosticsScreen(data));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BACKDROP);

        int frameWidth = Math.min(570, width - 18);
        int frameHeight = Math.min(318, height - 18);
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
        int contentY = screenY + 72;
        int contentWidth = screenWidth - 24;
        if (data.auxiliaryPowerOnline()) {
            renderOnline(graphics, mouseX, mouseY, contentX, contentY,
                    contentWidth);
        } else {
            renderOffline(graphics, contentX, contentY, contentWidth);
        }

        line(graphics, "NODE ARC48-SYS-01 // ESC TO TERMINATE SESSION",
                contentX, screenY + screenHeight - 13, MUTED_BLUE);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 60, HEADER);
        graphics.fill(x, y + 57, x + width, y + 60, FOUNDATION_RED);
        graphics.blit(TERMINAL_LOGO, x + 10, y + 6, 0, 0,
                48, 48, 128, 128);

        line(graphics, "SCiPNET TERMINAL v3.2.6", x + 68, y + 10,
                OFF_WHITE);
        line(graphics, "ARC-SITE-48 // INTERNAL SYSTEMS NODE",
                x + 68, y + 24, METAL_GRAY);
        line(graphics, "ARMED RESEARCH & CONTAINMENT FACILITY",
                x + 68, y + 38, MUTED_BLUE);

        String site = "ARC-SITE-48";
        int badgeWidth = font.width(site) + 14;
        int badgeX = x + width - badgeWidth - 10;
        graphics.fill(badgeX, y + 8, badgeX + badgeWidth, y + 24,
                FOUNDATION_RED);
        centered(graphics, site, badgeX, y + 8, badgeWidth, 16, OFF_WHITE);
    }

    private void renderOnline(GuiGraphics graphics, int mouseX, int mouseY,
            int x, int y, int width) {
        sectionHeader(graphics, "ANOMALOUS CONTAINMENT INDEX", x, y, width);
        metric(graphics, "UNCONTAINED SCP SIGNATURES",
                twoDigits(data.uncontainedScps()), x, y + 20, width,
                data.uncontainedScps() == 0 ? SIGNAL_GOLD : FOUNDATION_RED);
        String integrity = data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        metric(graphics, "CONTAINMENT INTEGRITY", integrity,
                x, y + 34, width,
                data.uncontainedScps() == 0 ? SIGNAL_GOLD : FOUNDATION_RED);

        sectionHeader(graphics, "FACILITY DEFENSE TELEMETRY",
                x, y + 54, width);
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

        resetX = x;
        resetY = y + 166;
        resetWidth = width;
        boolean hovered = inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT);
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, hovered ? BUTTON_HOVER : BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                hovered ? FOUNDATION_RED : BEZEL_DARK);
        String label = resetRequested
                ? "REMOTE SESSION CACHE PURGE REQUESTED"
                : "PURGE REMOTE SESSION CACHE";
        centered(graphics, label, resetX, resetY, resetWidth, RESET_HEIGHT,
                resetRequested ? METAL_GRAY : OFF_WHITE);
    }

    private void renderOffline(GuiGraphics graphics, int x, int y, int width) {
        sectionHeader(graphics, "SCiPNET SERVICE AVAILABILITY", x, y, width);
        metric(graphics, "AUXILIARY POWER BUS", "OFFLINE",
                x, y + 22, width, FOUNDATION_RED);
        metric(graphics, "SCiPNET TELEMETRY LINK", "UNAVAILABLE",
                x, y + 38, width, METAL_GRAY);
        metric(graphics, "FACILITY DEFENSE DATA", "SUSPENDED",
                x, y + 54, width, METAL_GRAY);

        graphics.fill(x, y + 78, x + width, y + 124, HEADER);
        border(graphics, x, y + 78, width, 46, BEZEL_DARK);
        centered(graphics, "LIVE ARC-SITE-48 TELEMETRY CANNOT BE ACQUIRED",
                x, y + 84, width, 14, OFF_WHITE);
        centered(graphics, "RESTORE AUXILIARY POWER TO RESUME SCiPNET SERVICE",
                x, y + 100, width, 14, METAL_GRAY);

        statusStrip(graphics, "REMOTE ANOMALOUS ACCESS", "ISOLATED",
                x, y + 138, width, SIGNAL_GOLD);

        resetX = x;
        resetY = y + 166;
        resetWidth = width;
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                BEZEL_DARK);
        centered(graphics, "REMOTE SESSION CACHE PURGE UNAVAILABLE",
                resetX, resetY, resetWidth, RESET_HEIGHT, METAL_GRAY);
    }

    private void sectionHeader(GuiGraphics graphics, String title,
            int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 15, STEEL_BLUE);
        graphics.fill(x, y, x + 4, y + 15, FOUNDATION_RED);
        line(graphics, title, x + 9, y + 3, OFF_WHITE);
    }

    private void statusStrip(GuiGraphics graphics, String label, String value,
            int x, int y, int width, int valueColor) {
        graphics.fill(x, y, x + width, y + 19, HEADER);
        border(graphics, x, y, width, 19, BEZEL_DARK);
        line(graphics, label, x + 7, y + 5, METAL_GRAY);
        rightAligned(graphics, value, x + width - 7, y + 5, valueColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && data.auxiliaryPowerOnline() && !resetRequested
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
        String left = label + " ";
        String right = String.valueOf(value);
        int available = width - font.width(left) - font.width(right) - 8;
        int dots = Math.max(2, available / Math.max(1, font.width(".")));
        line(graphics, left + ".".repeat(dots), x, y, METAL_GRAY);
        rightAligned(graphics, right, x + width, y, valueColor);
    }

    private void rightAligned(GuiGraphics graphics, String text,
            int right, int y, int color) {
        graphics.drawString(font, text, right - font.width(text), y,
                color, false);
    }

    private void centered(GuiGraphics graphics, String text, int x, int y,
            int width, int height, int color) {
        graphics.drawString(font, text,
                x + Math.max(0, (width - font.width(text)) / 2),
                y + Math.max(0, (height - font.lineHeight) / 2),
                color, false);
    }

    private void line(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, text, x, y, color, false);
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
''', encoding="utf-8")

roamer = roamer_path.read_text(encoding="utf-8")
anchor = '''    public static boolean isContained(MinecraftServer server,
            RoamerType type) {
        if (server == null || type == null) return false;
        synchronized (STATES) {
            return data(server, type).contained;
        }
    }
'''
replacement = anchor + '''
    /**
     * Returns whether this SCP profile is enabled and currently released from
     * containment, regardless of whether an entity is spawned at this moment.
     */
    public static boolean isOperationallyUncontained(MinecraftServer server,
            RoamerType type) {
        if (server == null || type == null || !type.spawnImplemented()
                || !moduleEnabled(type) || !isSpawnRuleEnabled(server, type)) {
            return false;
        }
        synchronized (STATES) {
            return !data(server, type).contained;
        }
    }
'''
if anchor not in roamer:
    raise SystemExit("RoamerManager containment anchor not found")
roamer_path.write_text(roamer.replace(anchor, replacement, 1), encoding="utf-8")

access = access_path.read_text(encoding="utf-8")
old_count = '''    private static int countUncontainedScps(MinecraftServer server) {
        int count = 0;
        for (RoamerType type : RoamerType.values()) {
            if (!RoamerManager.isContained(server, type)) {
                count += RoamerManager.activeCount(server, type);
            }
        }
        return count;
    }
'''
new_count = '''    private static int countUncontainedScps(MinecraftServer server) {
        int count = 0;
        for (RoamerType type : RoamerType.values()) {
            if (RoamerManager.isOperationallyUncontained(server, type)) {
                count++;
            }
        }
        return count;
    }
'''
if old_count not in access:
    raise SystemExit("SCP signature count anchor not found")
access_path.write_text(access.replace(old_count, new_count, 1), encoding="utf-8")

changelog = changelog_path.read_text(encoding="utf-8")
old_line = "- Added a black-and-green Foundation diagnostic interface reporting vague global containment, Tesla-grid, override, and connected-door telemetry; it remains accessible without auxiliary power to report system unavailability and includes a manual remote-session cache reset;"
new_line = "- Restyled the Facility Diagnostic Terminal as an ARC-Site-48 SCiPNET v3.2.6 CRT interface using the site crest palette, retained offline service reporting and remote-session cache purge controls, and corrected uncontained SCP signatures to represent enabled SCP profiles released from containment rather than only currently spawned entities;"
if old_line not in changelog:
    raise SystemExit("SCP-079 changelog anchor not found")
changelog_path.write_text(changelog.replace(old_line, new_line, 1), encoding="utf-8")
