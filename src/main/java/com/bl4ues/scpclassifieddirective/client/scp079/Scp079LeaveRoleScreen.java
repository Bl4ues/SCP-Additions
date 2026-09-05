package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/** Confirmation screen available even when SCP-079 has no Auxiliary Power. */
public final class Scp079LeaveRoleScreen extends Screen {
    private Scp079LeaveRoleScreen() {
        super(Scp079UiTheme.text("Leave SCP Role"));
    }

    public static void open() {
        if (!Scp079PlayableClient.active()) return;
        Minecraft.getInstance().setScreen(new Scp079LeaveRoleScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF06121A);
        Scp079UiTheme.renderFrame(graphics, width, height);

        int w = Math.min(370, width - 56);
        int h = 142;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, 0xE9081820);
        border(graphics, x, y, w, h, 0xFF597F8E);
        Scp079UiTheme.drawCentered(graphics, font, "LEAVE SCP ROLE?",
                x + w * 0.5F, y + 23, 1.28F, 0xFFFFFFFF);
        Scp079UiTheme.drawCentered(graphics, font,
                "RETURN TO YOUR ORIGINAL PLAYER STATE.",
                x + w * 0.5F, y + 51, 1.04F, Scp079UiTheme.MUTED);

        int gap = 12;
        int bw = (w - 44 - gap) / 2;
        int by = y + h - 43;
        int cancelX = x + 22;
        int leaveX = cancelX + bw + gap;
        drawButton(graphics, cancelX, by, bw, 29, "CANCEL",
                inside(mouseX, mouseY, cancelX, by, bw, 29), false);
        drawButton(graphics, leaveX, by, bw, 29, "LEAVE ROLE",
                inside(mouseX, mouseY, leaveX, by, bw, 29), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int w = Math.min(370, width - 56);
        int h = 142;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int gap = 12;
        int bw = (w - 44 - gap) / 2;
        int by = y + h - 43;
        int cancelX = x + 22;
        int leaveX = cancelX + bw + gap;
        if (inside(mouseX, mouseY, cancelX, by, bw, 29)) {
            onClose();
            return true;
        }
        if (inside(mouseX, mouseY, leaveX, by, bw, 29)) {
            ScpRoleSelectorNetwork.requestRole(
                    ScpRoleSelectorNetwork.Role.HUMAN);
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            ScpRoleSelectorNetwork.requestRole(
                    ScpRoleSelectorNetwork.Role.HUMAN);
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void drawButton(GuiGraphics graphics, int x, int y,
            int w, int h, String text, boolean hover, boolean danger) {
        int fill = danger ? (hover ? 0xE0642828 : 0xD53D2022)
                : (hover ? 0xE52A5868 : 0xD518303A);
        int line = danger ? 0xFFE28A7F : 0xFF75B7CC;
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        Scp079UiTheme.drawCentered(graphics, font, text,
                x + w * 0.5F, y + 11, 1.08F, 0xFFFFFFFF);
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
