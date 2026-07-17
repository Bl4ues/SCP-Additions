package com.bl4ues.scpinventory.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

    public static final KeyMapping OPEN_SCP_INVENTORY = new KeyMapping(
            "key.scpinventory.open_inventory",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            "key.categories.scpinventory"
    );

    public static final KeyMapping CONTEXT_INTERACT = new KeyMapping(
            "key.scpinventory.context_interact",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_E,
            "key.categories.scpinventory"
    );

    public static final KeyMapping CONTEXT_CONFIG_SELECT = new KeyMapping(
            "key.scpinventory.context_config_select",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.scpinventory"
    );
}
