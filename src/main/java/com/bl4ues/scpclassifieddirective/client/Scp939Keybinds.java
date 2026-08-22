package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Scp939Keybinds {
    public static final KeyMapping HOLD_BREATH = new KeyMapping(
            "key.scp_classified_directive.hold_breath",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.scp_classified_directive");

    private Scp939Keybinds() {
    }
}
