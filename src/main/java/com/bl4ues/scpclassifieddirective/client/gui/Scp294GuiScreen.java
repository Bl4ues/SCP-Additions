package com.bl4ues.scpclassifieddirective.client.gui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.TeslaTerminalFocusClient;
import com.bl4ues.scpclassifieddirective.network.Scp294GuiButtonMessage;
import com.bl4ues.scpclassifieddirective.world.inventory.Scp294GuiMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Keyboard-input controller for SCP-294. The physical Context Interaction on
 * the keyboard opens this screen only after payment; nothing is drawn in 2D.
 */
public class Scp294GuiScreen extends AbstractContainerScreen<Scp294GuiMenu> {
    private String order = "";
    private boolean inputFocused;

    public Scp294GuiScreen(Scp294GuiMenu container, Inventory inventory,
            Component text) {
        super(container, inventory, text);
        this.imageWidth = 1;
        this.imageHeight = 1;
    }

    @Override
    protected void init() {
        super.init();
        inputFocused = true;
        TeslaTerminalFocusClient.beginScp294(terminalPos());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY,
            float partialTicks) {
        // Deliberately no 2D GUI or mouse controls. The player is physically at
        // the keyboard; typed characters are rendered on the machine's small
        // payment/status display by Scp294PhysicalClient.
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks,
            int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            closeMachine();
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!TeslaTerminalFocusClient.inputReady() || !hasCoinInserted()) {
            return true;
        }
        inputFocused = true;
        if (isAllowedInputCharacter(codePoint) && order.length() < 80) {
            order += codePoint;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 || minecraft != null
                && minecraft.options.keyInventory.matches(key, scanCode)) {
            closeMachine();
            return true;
        }
        if (!TeslaTerminalFocusClient.inputReady() || !hasCoinInserted()) {
            return true;
        }
        if (key == 257 || key == 335) {
            if (!order.isBlank()) sendDrinkRequest();
            return true;
        }
        if (key == 259 && !order.isEmpty()) {
            inputFocused = true;
            order = order.substring(0, order.length() - 1);
            return true;
        }
        if (key == 86 && hasControlDown()) {
            inputFocused = true;
            String clipboard = minecraft.keyboardHandler.getClipboard();
            for (char ch : clipboard.toCharArray()) {
                if (isAllowedInputCharacter(ch) && order.length() < 80) {
                    order += ch;
                }
            }
            return true;
        }
        return true;
    }

    private static boolean isAllowedInputCharacter(char character) {
        return character >= 32 && character != 127;
    }

    private void sendDrinkRequest() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new Scp294GuiButtonMessage(0, menu.x, menu.y, menu.z, order));
        // A valid order is closed authoritatively by the server. An OUT OF RANGE
        // request deliberately keeps this session open so the user can correct it.
    }

    private void closeMachine() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    @Override
    public void removed() {
        if (TeslaTerminalFocusClient.scp294ActiveFor(terminalPos())) {
            TeslaTerminalFocusClient.end();
        }
        super.removed();
    }

    public BlockPos terminalPos() {
        return new BlockPos(menu.x, menu.y, menu.z);
    }

    public boolean isFor(BlockPos pos) {
        return pos != null && pos.equals(terminalPos());
    }

    public String physicalOrder() {
        return order;
    }

    public boolean physicalHasCoinInserted() {
        return hasCoinInserted();
    }

    public boolean physicalInputFocused() {
        return inputFocused;
    }

    private boolean hasCoinInserted() {
        Slot slot = menu.get().get(0);
        return slot != null && slot.hasItem();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
