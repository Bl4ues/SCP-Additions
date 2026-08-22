package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Scp173Keybinds {
    public static final KeyMapping BLINK = new KeyMapping(
            "key.scp_classified_directive.blink",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.scp_classified_directive"
    );

    private Scp173Keybinds() {
    }
}
