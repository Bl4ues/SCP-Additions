package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Small confirmation screen available even when SCP-079 has no Auxiliary Power. */
public final class Scp079LeaveRoleScreen extends Screen {
    private Scp079LeaveRoleScreen() {
        super(Component.literal("Leave SCP Role"));
    }

    public static void open() {
        if (!Scp079PlayableClient.active()) return;
        Minecraft.getInstance().setScreen(new Scp079LeaveRoleScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA9000000);
        int w = Math.min(350, width - 40);
        int h = 132;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, 0xF2081820);
        border(graphics, x, y, w, h, 0xFF597F8E);
        graphics.drawCenteredString(font, ScpFonts.montserrat("LEAVE SCP ROLE?"),
                x + w / 2, y + 22, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                ScpFonts.roboto("Return to your original player state."),
                x + w / 2, y + 47, 0xFF8EAEB9);

        int gap = 10;
        int bw = (w - 42 - gap) / 2;
        int by = y + h - 39;
        int cancelX = x + 21;
        int leaveX = cancelX + bw + gap;
        drawButton(graphics, cancelX, by, bw, 25, "CANCEL",
                inside(mouseX, mouseY, cancelX, by, bw, 25), false);
        drawButton(graphics, leaveX, by, bw, 25, "LEAVE ROLE",
                inside(mouseX, mouseY, leaveX, by, bw, 25), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int w = Math.min(350, width - 40);
        int h = 132;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int gap = 10;
        int bw = (w - 42 - gap) / 2;
        int by = y + h - 39;
        int cancelX = x + 21;
        int leaveX = cancelX + bw + gap;
        if (inside(mouseX, mouseY, cancelX, by, bw, 25)) {
            onClose();
            return true;
        }
        if (inside(mouseX, mouseY, leaveX, by, bw, 25)) {
            ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.HUMAN);
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            ScpRoleSelectorNetwork.requestRole(ScpRoleSelectorNetwork.Role.HUMAN);
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h,
            String text, boolean hover, boolean danger) {
        int fill = danger ? (hover ? 0xE0642828 : 0xD53D2022)
                : (hover ? 0xE52A5868 : 0xD518303A);
        int line = danger ? 0xFFE28A7F : 0xFF75B7CC;
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        graphics.drawCenteredString(font, ScpFonts.roboto(text),
                x + w / 2, y + 8, 0xFFFFFFFF);
    }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
