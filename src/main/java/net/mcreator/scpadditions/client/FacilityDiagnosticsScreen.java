package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.network.FacilityDiagnosticsPacket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Read-only black-and-green Foundation diagnostic terminal. */
public final class FacilityDiagnosticsScreen extends Screen {
    private static final int GREEN = 0xFF62E17A;
    private static final int DIM_GREEN = 0xFF3F9850;
    private static final int BLACK = 0xFF020503;
    private static final int PANEL = 0xFF061009;
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FacilityDiagnosticsPacket data;

    private FacilityDiagnosticsScreen(FacilityDiagnosticsPacket data) {
        super(Component.literal("Foundation Facility Diagnostics"));
        this.data = data;
    }

    public static void open(FacilityDiagnosticsPacket data) {
        Minecraft.getInstance().setScreen(new FacilityDiagnosticsScreen(data));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, BLACK);
        int panelWidth = Math.min(360, width - 24);
        int panelHeight = Math.min(230, height - 24);
        int x = (width - panelWidth) / 2;
        int y = (height - panelHeight) / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL);
        border(graphics, x, y, panelWidth, panelHeight, DIM_GREEN);

        int tx = x + 12;
        int ty = y + 10;
        line(graphics, "SCP FOUNDATION // FACILITY SYSTEMS DIAGNOSTIC", tx, ty, GREEN);
        line(graphics, "SESSION CLASS: INTERNAL OPERATIONS", tx, ty + 12, DIM_GREEN);
        line(graphics, "REPORT TIME: " + LocalDateTime.now().format(TIME), tx, ty + 24, DIM_GREEN);
        rule(graphics, tx, ty + 38, panelWidth - 24);

        line(graphics, "[ ANOMALOUS CONTAINMENT TELEMETRY ]", tx, ty + 48, GREEN);
        metric(graphics, "UNCONTAINED SCP SIGNATURES", data.uncontainedScps(), tx, ty + 62,
                panelWidth - 24);
        String condition = data.uncontainedScps() == 0 ? "NOMINAL"
                : data.uncontainedScps() <= 2 ? "DEGRADED" : "CRITICAL";
        metric(graphics, "CONTAINMENT ASSURANCE", condition, tx, ty + 74,
                panelWidth - 24);

        line(graphics, "[ SECURITY INFRASTRUCTURE BUS ]", tx, ty + 94, GREEN);
        metric(graphics, "TESLA NODES ACTIVE", data.activeTeslaGates(), tx, ty + 108,
                panelWidth - 24);
        metric(graphics, "TESLA NODES REGISTERED", data.registeredTeslaGates(), tx, ty + 120,
                panelWidth - 24);
        metric(graphics, "TESLA MANUAL OVERRIDE",
                data.teslaOverride() ? "ENGAGED" : "CLEAR", tx, ty + 132,
                panelWidth - 24);
        metric(graphics, "REMOTE DOOR ENDPOINTS", data.connectedDoors(), tx, ty + 144,
                panelWidth - 24);

        rule(graphics, tx, ty + 164, panelWidth - 24);
        line(graphics, "AUXILIARY DIAGNOSTIC BUS: ONLINE", tx, ty + 174, GREEN);
        line(graphics, "DATA SCOPE: REGISTERED FOUNDATION ASSETS", tx, ty + 186, DIM_GREEN);
        line(graphics, "PRESS ESC TO TERMINATE SESSION", tx, ty + 204, DIM_GREEN);
        super.render(graphics, mouseX, mouseY, partialTick);
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

    private void line(GuiGraphics graphics, String text, int x, int y,
            int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    private void rule(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, DIM_GREEN);
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
