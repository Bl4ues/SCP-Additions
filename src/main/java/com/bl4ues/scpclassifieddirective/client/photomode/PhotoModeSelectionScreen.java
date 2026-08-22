package com.bl4ues.scpclassifieddirective.client.photomode;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/** Frozen-frame picker shown after F8 is pressed. */
public final class PhotoModeSelectionScreen extends Screen {
    private PhotoModeCapture.PhotoTarget hoveredTarget;
    private boolean submitted;

    public PhotoModeSelectionScreen() {
        super(Component.literal("Photo Mode"));
    }

    @Override
    public boolean isPauseScreen() {
        // In an integrated development world this freezes ticks as well as input,
        // keeping animated entities on the exact pose visible in the frozen frame.
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation frozen = PhotoModeCapture.frozenTexture();
        if (frozen == null || !PhotoModeCapture.hasFrozenFrame()) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            graphics.drawCenteredString(font, "Capturing clean frame...",
                    width / 2, height / 2 - 4, 0xFFFFFFFF);
            return;
        }

        graphics.blit(frozen, 0, 0, width, height,
                0.0F, 0.0F,
                PhotoModeCapture.frozenWidth(), PhotoModeCapture.frozenHeight(),
                PhotoModeCapture.frozenWidth(), PhotoModeCapture.frozenHeight());

        if (!submitted) {
            hoveredTarget = PhotoModeCapture.pick(mouseX, mouseY, width, height);
        }

        graphics.fill(8, 8, Math.min(width - 8, 330), 44, 0xB0000000);
        graphics.drawString(font, "Photo Mode - frozen frame", 16, 15,
                0xFFFFFFFF, false);
        graphics.drawString(font, "Left click: isolate object   Esc/right click: cancel",
                16, 29, 0xFFD8D8D8, false);

        if (!submitted) {
            int crosshair = hoveredTarget == null ? 0xFFFFFFFF : 0xFF7CFF7C;
            graphics.hLine(mouseX - 5, mouseX - 2, mouseY, crosshair);
            graphics.hLine(mouseX + 2, mouseX + 5, mouseY, crosshair);
            graphics.vLine(mouseX, mouseY - 5, mouseY - 2, crosshair);
            graphics.vLine(mouseX, mouseY + 2, mouseY + 5, crosshair);

            if (hoveredTarget != null) {
                Component label = hoveredTarget.label();
                int textWidth = font.width(label);
                int labelX = Math.min(mouseX + 9, Math.max(4, width - textWidth - 9));
                int labelY = Math.min(mouseY + 9, height - font.lineHeight - 5);
                graphics.fill(labelX - 3, labelY - 2,
                        labelX + textWidth + 3, labelY + font.lineHeight + 2,
                        0xB0000000);
                graphics.drawString(font, label, labelX, labelY,
                        0xFFFFFFFF, false);
            }
        } else {
            graphics.fill(0, height / 2 - 15, width, height / 2 + 15, 0xA0000000);
            graphics.drawCenteredString(font, "Rendering transparent PNG...",
                    width / 2, height / 2 - 4, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (submitted) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            PhotoModeCapture.cancel();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && hoveredTarget != null) {
            submitted = true;
            PhotoModeCapture.requestCapture(hoveredTarget);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_F8) {
            PhotoModeCapture.cancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        PhotoModeCapture.cancel();
    }
}
