package com.bl4ues.scpclassifieddirective.client.gui;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceFocusClient;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Input/session shell only; the interface itself is rendered on the world model. */
public final class HackingDeviceScreen extends Screen {
    private final BlockPos pos;
    private boolean closing;

    public HackingDeviceScreen(BlockPos pos) {
        super(Component.literal("Hacking Device"));
        this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
    }

    public boolean isFor(BlockPos target) {
        return target != null && pos.equals(target);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        // Intentionally empty. The physical model remains the only GUI surface.
        HackingDeviceMinigameClient.phase();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                || minecraft != null
                && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            closeSession();
            return true;
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A ->
                    HackingDeviceMinigameClient.moveSelection(-1);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D ->
                    HackingDeviceMinigameClient.moveSelection(1);
            case GLFW.GLFW_KEY_SPACE -> HackingDeviceMinigameClient.probe();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER ->
                    HackingDeviceMinigameClient.submit();
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            closeSession();
            return true;
        }
        return true;
    }

    @Override
    public void onClose() {
        closeSession();
    }

    private void closeSession() {
        if (closing) return;
        closing = true;

        /*
         * A replacement StartSession can install a new minigame before Minecraft
         * finishes disposing the previous Screen instance. Never let that stale
         * screen send ExitSession for the new target. Conversely, if the client
         * session has already vanished, still release an orphaned camera focus.
         */
        BlockPos current = HackingDeviceMinigameClient.pos();
        if (current != null && pos.equals(current)) {
            HackingDeviceMinigameClient.requestExit();
        } else if (!HackingDeviceMinigameClient.active()) {
            HackingDeviceFocusClient.end();
        }

        if (minecraft != null && minecraft.screen == this) {
            minecraft.setScreen(null);
        }
    }
}
