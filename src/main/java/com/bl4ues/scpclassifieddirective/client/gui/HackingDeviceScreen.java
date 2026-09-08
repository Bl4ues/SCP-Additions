package com.bl4ues.scpclassifieddirective.client.gui;

import com.bl4ues.scpclassifieddirective.client.HackingDeviceFocusClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Input/session shell only; the interface itself is rendered on the world model. */
public final class HackingDeviceScreen extends Screen {
    private final BlockPos pos;

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
    }

    @Override
    public void onClose() {
        HackingDeviceFocusClient.end();
        super.onClose();
    }
}
