package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Compatibility facade retained for the existing pause-screen wiring. The
 * actual panels are native SCP: Classified Directive views in PauseMenuNativePanelsClient.
 */
public final class PauseMenuEmbeddedPanelsClient {
    private PauseMenuEmbeddedPanelsClient() {
    }

    public enum Mode {
        ACHIEVEMENTS,
        STATISTICS,
        OPEN_TO_LAN
    }

    public static boolean toggle(CustomPauseMenuScreen parent, Mode mode) {
        return PauseMenuNativePanelsClient.toggle(parent, nativeMode(mode));
    }

    public static void close(CustomPauseMenuScreen parent) {
        PauseMenuNativePanelsClient.close(parent);
    }

    public static boolean isOpen(CustomPauseMenuScreen parent) {
        return PauseMenuNativePanelsClient.isOpen(parent);
    }

    public static void tick(CustomPauseMenuScreen parent) {
        PauseMenuNativePanelsClient.tick(parent);
    }

    public static void render(CustomPauseMenuScreen parent,
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            long now, int baseX, int baseY, int menuWidth, int rowHeight,
            int gap) {
        PauseMenuNativePanelsClient.render(parent, graphics, mouseX, mouseY,
                partialTick, now, baseX, baseY, menuWidth, rowHeight, gap);
    }

    public static boolean mouseClicked(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        return PauseMenuNativePanelsClient.mouseClicked(parent,
                mouseX, mouseY, button);
    }

    public static boolean mouseReleased(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button) {
        return PauseMenuNativePanelsClient.mouseReleased(parent,
                mouseX, mouseY, button);
    }

    public static boolean mouseDragged(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        return PauseMenuNativePanelsClient.mouseDragged(parent,
                mouseX, mouseY, button, dragX, dragY);
    }

    public static boolean mouseScrolled(CustomPauseMenuScreen parent,
            double mouseX, double mouseY, double delta) {
        return PauseMenuNativePanelsClient.mouseScrolled(parent,
                mouseX, mouseY, delta);
    }

    public static boolean keyPressed(CustomPauseMenuScreen parent,
            int keyCode, int scanCode, int modifiers) {
        return PauseMenuNativePanelsClient.keyPressed(parent,
                keyCode, scanCode, modifiers);
    }

    public static boolean keyReleased(CustomPauseMenuScreen parent,
            int keyCode, int scanCode, int modifiers) {
        return PauseMenuNativePanelsClient.keyReleased(parent,
                keyCode, scanCode, modifiers);
    }

    public static boolean charTyped(CustomPauseMenuScreen parent,
            char codePoint, int modifiers) {
        return PauseMenuNativePanelsClient.charTyped(parent,
                codePoint, modifiers);
    }

    private static PauseMenuNativePanelsClient.Mode nativeMode(Mode mode) {
        return switch (mode) {
            case ACHIEVEMENTS -> PauseMenuNativePanelsClient.Mode.ACHIEVEMENTS;
            case STATISTICS -> PauseMenuNativePanelsClient.Mode.STATISTICS;
            case OPEN_TO_LAN -> PauseMenuNativePanelsClient.Mode.OPEN_TO_LAN;
        };
    }
}
