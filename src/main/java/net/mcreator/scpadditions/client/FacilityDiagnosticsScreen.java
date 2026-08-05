package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.network.FacilityDiagnosticsPacket;
import net.mcreator.scpadditions.network.FacilityDiagnosticsResetPacket;

/** Black-and-green SCiPNET facility diagnostic terminal. */
public final class FacilityDiagnosticsScreen extends Screen {
    private static final int GREEN = 0xFF62E17A;
    private static final int DIM_GREEN = 0xFF3F9850;
    private static final int BLACK = 0xFF020503;
    private static final int PANEL = 0xFF061009;
    private static final int BUTTON = 0xFF0A1B0D;
    private static final int BUTTON_HOVER = 0xFF10351A;

    private final FacilityDiagnosticsPacket data;
    private int resetX;
    private int resetY;
    private int resetWidth;
    private static final int RESET_HEIGHT = 18;
    private boolean resetRequested;

    private FacilityDiagnosticsScreen(FacilityDiagnosticsPacket data) {
        super(Component.literal("SCiPNET Facility Diagnostics"));
        this.data = data;
    }

    public static void open(FacilityDiagnosticsPacket data) {
        Minecraft.getInstance().setScreen(new FacilityDiagnosticsScreen(data));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BLACK);
        int panelWidth = Math.min(410, width - 24);
        int panelHeight = Math.min(246, height - 24);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        border(graphics, x, y, panelWidth, panelHeight, DIM_GREEN);

        int tx = x + 12;
        int ty = y + 10;
        line(graphics, "SCiPNET TERMINAL v3.2.6", tx, ty, GREEN);
        line(graphics, "FOUNDATION INTERNAL DIAGNOSTIC NODE",
                tx, ty + 12, DIM_GREEN);
        line(graphics, "ACCESS CONTEXT: INTERNAL OPERATIONS",
                tx, ty + 24, DIM_GREEN);
        rule(graphics, tx, ty + 38, panelWidth - 24);

        if (data.auxiliaryPowerOnline()) {
            renderOnline(graphics, mouseX, mouseY, tx, ty, panelWidth);
        } else {
            renderOffline(graphics, tx, ty, panelWidth);
        }

        line(graphics, "ESC // TERMINATE SCiPNET SESSION", tx, ty + 224,
                DIM_GREEN);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderOnline(GuiGraphics graphics, int mouseX, int mouseY,
            int tx, int ty, int panelWidth) {
        line(graphics, "[ ANOMALOUS SIGNATURE REGISTRY ]",
                tx, ty + 48, GREEN);
        metric(graphics, "UNCONTAINED SCP SIGNATURES",
                data.uncontainedScps(), tx, ty + 62, panelWidth - 24);
        String condition = data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        metric(graphics, "CONTAINMENT INTEGRITY", condition,
                tx, ty + 74, panelWidth - 24);

        line(graphics, "[ FACILITY DEFENSE TELEMETRY ]",
                tx, ty + 94, GREEN);
        metric(graphics, "TESLA GRID NODES ACTIVE", data.activeTeslaGates(),
                tx, ty + 108, panelWidth - 24);
        metric(graphics, "TESLA GRID NODES REGISTERED",
                data.registeredTeslaGates(), tx, ty + 120,
                panelWidth - 24);
        metric(graphics, "TESLA GRID OVERRIDE",
                data.teslaOverride() ? "ACTIVE" : "INACTIVE",
                tx, ty + 132, panelWidth - 24);
        metric(graphics, "DOOR SYSTEM ENDPOINTS", data.connectedDoors(),
                tx, ty + 144, panelWidth - 24);

        rule(graphics, tx, ty + 162, panelWidth - 24);
        line(graphics, "AUXILIARY POWER BUS: ONLINE",
                tx, ty + 172, GREEN);
        line(graphics, "TELEMETRY SCOPE: REGISTERED FOUNDATION ASSETS",
                tx, ty + 184, DIM_GREEN);

        resetX = tx;
        resetY = ty + 198;
        resetWidth = panelWidth - 24;
        boolean hovered = inside(mouseX, mouseY, resetX, resetY,
                resetWidth, RESET_HEIGHT);
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, hovered ? BUTTON_HOVER : BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                hovered ? GREEN : DIM_GREEN);
        String label = resetRequested
                ? "REMOTE CACHE PURGE REQUESTED"
                : "PURGE REMOTE SESSION CACHE";
        centered(graphics, label, resetX, resetY, resetWidth, RESET_HEIGHT,
                resetRequested ? DIM_GREEN : GREEN);
    }

    private void renderOffline(GuiGraphics graphics, int tx, int ty,
            int panelWidth) {
        line(graphics, "[ SCiPNET SERVICE STATUS ]", tx, ty + 48, GREEN);
        metric(graphics, "AUXILIARY POWER BUS", "OFFLINE",
                tx, ty + 66, panelWidth - 24);
        metric(graphics, "SCiPNET TELEMETRY LINK", "UNAVAILABLE",
                tx, ty + 78, panelWidth - 24);
        metric(graphics, "FACILITY DEFENSE DATA", "SUSPENDED",
                tx, ty + 90, panelWidth - 24);

        rule(graphics, tx, ty + 112, panelWidth - 24);
        line(graphics, "LIVE FOUNDATION TELEMETRY CANNOT BE ACQUIRED.",
                tx, ty + 126, DIM_GREEN);
        line(graphics, "RESTORE AUXILIARY POWER BUS TO RESUME SERVICE.",
                tx, ty + 140, DIM_GREEN);
        line(graphics, "REMOTE ANOMALOUS ACCESS: ISOLATED",
                tx, ty + 162, GREEN);

        resetX = tx;
        resetY = ty + 198;
        resetWidth = panelWidth - 24;
        graphics.fill(resetX, resetY, resetX + resetWidth,
                resetY + RESET_HEIGHT, BUTTON);
        border(graphics, resetX, resetY, resetWidth, RESET_HEIGHT,
                DIM_GREEN);
        centered(graphics, "REMOTE CACHE PURGE UNAVAILABLE",
                resetX, resetY, resetWidth, RESET_HEIGHT, DIM_GREEN);
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
            int x, int y, int width) {
        String left = label + " ";
        String right = String.valueOf(value);
        int dots = Math.max(2, (width - font.width(left) - font.width(right))
                / Math.max(1, font.width(".")));
        line(graphics, left + ".".repeat(dots) + right, x, y, GREEN);
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

    private void rule(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, DIM_GREEN);
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width
                && mouseY >= y && mouseY < y + height;
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
