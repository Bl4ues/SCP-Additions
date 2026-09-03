package com.bl4ues.scpclassifieddirective.inventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

    public static final KeyMapping CONTEXT_CONFIG_SELECT = new KeyMapping(
            "key.scp_classified_directive.context_config_select",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.scp_classified_directive"
    );

    public static final KeyMapping STOW_HELD_ITEM = new KeyMapping(
            "key.scp_classified_directive.stow_held_item",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.scp_classified_directive"
    );

    // KeyMapping displays an untranslated key verbatim. Using the final English
    // label here avoids exposing a raw lang key while keeping the control fully
    // rebindable; the project can move this string into additional locales later.
    public static final KeyMapping QUICK_SAVE = new KeyMapping(
            "Quicksave",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "key.categories.scp_classified_directive"
    );
}
