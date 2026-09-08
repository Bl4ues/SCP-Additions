package com.bl4ues.scpclassifieddirective.client.gui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.Scp294Block;
import com.bl4ues.scpclassifieddirective.client.Scp294PhysicalClient;
import com.bl4ues.scpclassifieddirective.client.Scp294PhysicalClient.Control;
import com.bl4ues.scpclassifieddirective.client.TeslaTerminalFocusClient;
import com.bl4ues.scpclassifieddirective.network.Scp294GuiButtonMessage;
import com.bl4ues.scpclassifieddirective.world.inventory.Scp294GuiMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Input controller for SCP-294's physical console. Nothing is drawn in 2D: the
 * CRT is rendered on the block itself and clicks are ray-tested against the
 * authored CRT, keyboard and coin panel.
 */
public class Scp294GuiScreen extends AbstractContainerScreen<Scp294GuiMenu> {
    private String order = "";
    private boolean inputFocused;
    private Control hoveredControl = Control.NONE;

    public Scp294GuiScreen(Scp294GuiMenu container, Inventory inventory,
            Component text) {
        super(container, inventory, text);
        this.imageWidth = 1;
        this.imageHeight = 1;
    }

    @Override
    protected void init() {
        super.init();
        TeslaTerminalFocusClient.beginScp294(terminalPos());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY,
            float partialTicks) {
        if (!TeslaTerminalFocusClient.inputReady()) {
            hoveredControl = Control.NONE;
            return;
        }
        BlockState state = menu.world.getBlockState(terminalPos());
        Direction facing = state.hasProperty(Scp294Block.FACING)
                ? state.getValue(Scp294Block.FACING) : Direction.NORTH;
        hoveredControl = Scp294PhysicalClient.controlAt(terminalPos(), facing,
                mouseX, mouseY, width, height);
        // Deliberately no 2D GUI. The ordinary Screen cursor remains available
        // while the real vending machine occupies the entire interaction view.
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
            return true;
        }
        if (button != 0 || !TeslaTerminalFocusClient.inputReady()) {
            return true;
        }

        BlockState state = menu.world.getBlockState(terminalPos());
        Direction facing = state.hasProperty(Scp294Block.FACING)
                ? state.getValue(Scp294Block.FACING) : Direction.NORTH;
        Control control = Scp294PhysicalClient.controlAt(terminalPos(), facing,
                mouseX, mouseY, width, height);

        if (control == Control.COIN) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                    new Scp294GuiButtonMessage(1, menu.x, menu.y, menu.z, ""));
            inputFocused = true;
            return true;
        }
        if (control == Control.INPUT) {
            if (hasCoinInserted()) inputFocused = true;
            return true;
        }
        if (control == Control.ENTER) {
            if (hasCoinInserted() && !order.isBlank()) {
                sendDrinkRequestAndClose();
            }
            return true;
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
            if (!order.isBlank()) sendDrinkRequestAndClose();
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

    private void sendDrinkRequestAndClose() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new Scp294GuiButtonMessage(0, menu.x, menu.y, menu.z, order));
        closeMachine();
    }

    private void closeMachine() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    @Override
    public void removed() {
        hoveredControl = Control.NONE;
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

    public Control hoveredControl() {
        return hoveredControl;
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
