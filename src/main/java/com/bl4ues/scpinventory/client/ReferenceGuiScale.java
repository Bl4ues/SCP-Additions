package com.bl4ues.scpinventory.client;

import net.minecraft.client.Minecraft;

/**
 * Keeps SCP Unity UI elements at the same physical size used by the
 * Forge 1.20.1 edition, whose reference presentation uses GUI scale 2.
 */
public final class ReferenceGuiScale {
    private static final double REFERENCE_GUI_SCALE = 2.0D;

    private ReferenceGuiScale() {
    }

    public static float factor(Minecraft minecraft) {
        if (minecraft == null || minecraft.getWindow() == null) {
            return 1.0F;
        }
        double currentScale = Math.max(1.0D,
                minecraft.getWindow().getGuiScale());
        return (float) (REFERENCE_GUI_SCALE / currentScale);
    }

    public static int logicalSize(int guiSize, float factor) {
        return Math.max(1, Math.round(guiSize
                / Math.max(0.0001F, factor)));
    }
}
