package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Scp131Keybinds {
    public static final KeyMapping DISMISS = new KeyMapping(
            "key.scp_classified_directive.scp_131_dismiss",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.scp_classified_directive"
    );

    private Scp131Keybinds() {
    }
}
